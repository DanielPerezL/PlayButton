from config import app, db, SECRET_KEY
from controllers import (
    auth_controller,
    playlist_controller,
    songs_controller,
    suggestions_controller,
    user_controller,
)
from sqlalchemy import inspect, text
from sqlalchemy.exc import SQLAlchemyError
from models import User, Suggestion
import os
from exceptions import AppException
from flask import request, jsonify, send_from_directory
from services.users_service import UsersService
import sys


def ensure_token_version_column():
    """
    create_all() crea las tablas que faltan, pero no altera las que ya existen.
    Al actualizar un despliegue anterior hay que anadir la columna antes de que
    cualquier consulta la mencione, o todas fallarian con "Unknown column".
    """
    table = User.__tablename__
    existing = {column["name"] for column in inspect(db.engine).get_columns(table)}
    if "token_version" in existing:
        return
    db.session.execute(
        text(f"ALTER TABLE `{table}` ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0")
    )
    db.session.commit()


def ensure_suggestion_created_at_column():
    """
    Mismo caso que la columna anterior: la tabla suggestion ya existe en los
    despliegues antiguos, asi que create_all() no le anade created_at. Las
    filas previas se quedan con la fecha de la migracion, que es lo mas
    aproximado a "cuando se sugirio" que se puede reconstruir.
    La tabla suggestion_user si la crea create_all() por ser nueva.
    """
    table = Suggestion.__tablename__
    existing = {column["name"] for column in inspect(db.engine).get_columns(table)}
    if "created_at" in existing:
        return
    db.session.execute(
        text(
            f"ALTER TABLE `{table}` ADD COLUMN created_at DATETIME NOT NULL "
            "DEFAULT CURRENT_TIMESTAMP"
        )
    )
    db.session.commit()


with app.app_context():
    db.create_all()
    ensure_token_version_column()
    ensure_suggestion_created_at_column()
    duplicates = User.query.filter(User.nickname == "admin", User.id != 1).all()
    for user in duplicates:
        try:
            UsersService.delete_account(user)
        except Exception as e:
            sys.exit(1)

    admin = User.query.get(1)

    if admin:
        admin.nickname = "admin"
        if not admin.check_password(os.environ['ADMIN_PASSWORD']):
            admin.set_password(os.environ['ADMIN_PASSWORD'])
    else:
        admin = User(
            nickname="admin",
            password=os.environ['ADMIN_PASSWORD'],
        )
        admin.id = 1
        db.session.add(admin)

    try:
        db.session.commit()
    except SQLAlchemyError as e:
        db.session.rollback()
        sys.exit(1)


@app.errorhandler(AppException)
def handle_app_exception(error):
    response = jsonify(error.to_dict())
    response.status = error.status_code
    return response

@app.before_request
def cors_headers():
    if request.method != "OPTIONS":
        return
    response = app.make_response("")
    response.status_code = 204
    response.headers["Access-Control-Allow-Origin"] = "*"
    response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS, PATCH"
    response.headers["Access-Control-Allow-Headers"] = "*"
    return response

@app.after_request
def handle_connection_header(response):
    # Si la petición NO es a un archivo .mp3, forzamos el cierre
    if not request.path.endswith('.mp3'):
        response.headers["Connection"] = "close"
    return response

@app.route('/')
def serve():
    return send_from_directory(app.static_folder, 'index.html')

@app.route('/<path:path>')
def static_files(path):
    return send_from_directory(app.static_folder, path)

@app.errorhandler(404)
def not_found(e):
        return send_from_directory(app.static_folder, "index.html")

# Ejecutar la creación de las tablas dentro del contexto de la aplicación
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
