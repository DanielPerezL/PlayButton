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
# Las portadas se reescriben a un cuadrado de este lado. El limite es del
# fichero que entra, y se comprueba en el servicio: MAX_CONTENT_LENGTH lo
# aplicaria Flask a toda peticion y se llevaria por delante la subida de MP3.
IMAGE_SIZE = int(os.environ.get('IMAGE_SIZE', 512))
MAX_IMAGE_BYTES = int(os.environ.get('MAX_IMAGE_BYTES', 10 * 1024 * 1024))
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SQLALCHEMY_DATABASE_URI'] = f"mysql+mysqlconnector://{os.environ['DATABASE_USER']}:{os.environ['DATABASE_PASSWORD']}@{os.environ['DATABASE_HOST']}/{os.environ['DATABASE_NAME']}"
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['JWT_SECRET_KEY'] = os.environ['JWT_SECRET_KEY']
# Los tokens viajan en la cabecera Authorization, no en cookies: se declara de
# forma explicita para no depender del valor por defecto de la libreria.
app.config['JWT_TOKEN_LOCATION'] = ['headers']
app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(days=int(os.environ.get('JWT_TOKEN_EXPIRATION_DAYS', 90)))

db = SQLAlchemy(app)
jwt = JWTManager(app)

NICKNAME_MAX_LENGTH = int(os.environ.get('NICKNAME_MAX_LENGTH', 20))
MAX_AGE_LINKS = int(os.environ.get('MAX_AGE_LINKS', 6 * 60)) # 6 minutos en segundos

# Manejo de errores de base de datos
@app.errorhandler(OperationalError)
def handle_db_error():
    return jsonify({"msg": "Database connection error"}), 500
