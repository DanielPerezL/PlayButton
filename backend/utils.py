from flask_jwt_extended import create_access_token, get_jwt
from config import jwt
from models import User, Playlist
import os
from exceptions import ForbiddenException


#Comprobar permisos
def has_permission(user, resource):
    if user is None:
        return False
    elif is_admin(user):
        return True
    elif isinstance(resource, User):
        # Lógica para verificar permisos sobre un User
        return resource.id == user.id
    elif isinstance(resource, Playlist):
        # Lógica para verificar permisos sobre una Playlist
        return resource.user_id == user.id
    else:
        return False

def is_admin(user):
    return user.id == 1

def get_user_admin():
    return User.query.get(1)

def has_more_results(query, offset, limit):
    return query.offset(offset + limit).first() is not None

#MANEJO DE TOKENS
# Version de credenciales que viaja dentro del token. Si no coincide con la
# que tiene el usuario en base de datos, el token se emitio antes del ultimo
# cambio de contrasena y ya no vale.
TOKEN_VERSION_CLAIM = "tv"

#CREACION    
def create_tokens(user):
    access_token = create_access_token(
        identity=str(user.id),
        additional_claims={TOKEN_VERSION_CLAIM: user.token_version},
    )
    return access_token 

#REVOCACION
@jwt.token_in_blocklist_loader
def is_token_revoked(jwt_header, jwt_payload):
    """
    Se ejecuta en cada @jwt_required(). Devolver True hace que la peticion
    termine en 401, que es lo que los clientes ya interpretan como sesion
    caducada. Se comprueba aqui, y no en get_user_from_token, para que ninguna
    vista llegue a ejecutarse con un token revocado.
    """
    user = User.query.get(jwt_payload.get("sub"))
    if user is None:
        return True
    return jwt_payload.get(TOKEN_VERSION_CLAIM) != user.token_version

#CONSULTA
def get_user_from_token(decoded_token):
    identity = decoded_token.get("sub")  # La identidad del usuario (el id)
    if not identity:
        return None
    # Busca al usuario en la base de datos
    user = User.query.get(identity)
    if user is None:
        return None
    return user 

def check_is_admin():
    user = get_user_from_token(get_jwt())
    if user is None or not is_admin(user):
        raise ForbiddenException()