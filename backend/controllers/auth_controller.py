from config import app
from flask import jsonify, request, make_response
from exceptions import BadRequestException
from services import AuthService
from utils import is_admin


@app.route('/api/auth/login', methods=['POST'])
def users_login():
    data = request.json
    if not data or not all(key in data for key in ('nickname', 'password')):
        raise BadRequestException()

    nickname = data.get('nickname')
    password = data.get('password')

    access_token, user = AuthService.login(nickname, password)

    # Retorna los tokens en el cuerpo de la respuesta
    response = make_response(jsonify({
        "access_token":access_token, 
        "user_id": user.id,
        "is_admin": is_admin(user),
    }))
    return response, 200
