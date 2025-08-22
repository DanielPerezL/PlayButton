from config import app, db, SECRET_KEY
from controllers import (
    auth_controller,
    playlist_controller,
    songs_controller,
    suggestions_controller,
    user_controller,
)
from sqlalchemy.exc import SQLAlchemyError
from models import User
import os
from exceptions import AppException
from flask import request, jsonify, send_from_directory
from services.users_service import UsersService
import sys


with app.app_context():
    db.create_all()
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
