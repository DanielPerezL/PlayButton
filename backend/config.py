import os
from datetime import timedelta
from flask import Flask, jsonify
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from flask_jwt_extended import JWTManager
from sqlalchemy.exc import OperationalError
from itsdangerous import TimestampSigner


# Inicializar Flask, SQLAlchemy y JWT
app = Flask(__name__, static_folder='./static')

CORS_ENABLED = os.environ['CORS_ENABLED'].lower() == 'true'
if CORS_ENABLED:
     CORS(app, 
     supports_credentials=True, 
     resources={r"/*": {"origins": "*"}},  # Permite acceso desde cualquier dominio
     expose_headers="*",  # Permite que el cliente acceda a cualquier cabecera de la respuesta
     allow_headers="*",  # Permite que el cliente envíe cualquier cabecera en la petición
     methods=["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"])  # Todos los métodos permitidos


SECRET_KEY = os.environ['SECRET_KEY']
signer = TimestampSigner(SECRET_KEY)

app.config['ALLOWED_EXTENSIONS'] = {'mp3'}
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SQLALCHEMY_DATABASE_URI'] = f"mysql+mysqlconnector://{os.environ['DATABASE_USER']}:{os.environ['DATABASE_PASSWORD']}@{os.environ['DATABASE_HOST']}/{os.environ['DATABASE_NAME']}"
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['JWT_SECRET_KEY'] = os.environ['JWT_SECRET_KEY'] 
app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(days=os.environ.get('JWT_TOKEN_EXPIRATION_DAYS', 90))
app.config['JWT_ACCESS_COOKIE_PATH'] = '/api/'
app.config['JWT_REFRESH_COOKIE_PATH'] = '/api/auth/refresh'
app.config['JWT_COOKIE_SECURE'] = True
app.config['JWT_CSRF_METHODS'] = ["POST", "PUT", "PATCH", "DELETE"]
app.config['JWT_COOKIE_CSRF_PROTECT'] = True 

db = SQLAlchemy(app)
jwt = JWTManager(app)

NICKNAME_MAX_LENGTH = os.environ.get('NICKNAME_MAX_LENGTH', 20)
MAX_AGE_LINKS = os.environ.get('MAX_AGE_LINKS', 6 * 60) # 6 minutos en segundos

# Manejo de errores de base de datos
@app.errorhandler(OperationalError)
def handle_db_error():
    return jsonify({"msg": "Database connection error"}), 500
