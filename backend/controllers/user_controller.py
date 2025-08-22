from config import app
from flask import jsonify, request, make_response
from exceptions import *
from services import UsersService, PlaylistsService
from flask_jwt_extended import (
                                jwt_required, 
                                get_jwt, 
                                )
from utils import get_user_from_token, has_permission, is_admin, check_is_admin


@app.route('/api/users', methods=['POST'])
@jwt_required()
def register():
    check_is_admin()

    data = request.json
    if not data or not all(key in data for key in ('nickname', 'password')):
            raise BadRequestException()
    
    id = UsersService.add_user(nickname=data.get('nickname'), 
                          password=data.get('password'),
                          )
    response = make_response()
    response.status_code = 201

    base_url = request.host_url.rstrip('/')
    response.headers["Location"] = f"{base_url}/api/users/{id}"
    return response

@app.route('/api/users/<string:user_id>/playlists', methods=['GET'])
@jwt_required()
def get_user_playlists(user_id):
    client = get_user_from_token(get_jwt())
    user = UsersService.get_user(user_id)

    if not has_permission(client, user):
        playlists = UsersService.get_user_public_playlists(user_id)    
    else:
        playlists = UsersService.get_user_playlists(user_id)
    return jsonify({"playlists": playlists}), 200

@app.route('/api/users/<string:id>', methods=['GET'])
@jwt_required()
def user_info(id):        
    user = UsersService.get_user(id)
    return jsonify(user.to_dto()), 200

@app.route('/api/users', methods=['GET'])
@jwt_required()
def get_all_users():
    offset = request.args.get('offset', 0, type=int)
    limit = request.args.get('limit', 100, type=int)
    name = request.args.get('name', None, type=str)

    return jsonify(
        UsersService.get_all(offset, limit, name)
    ), 200

@app.route('/api/users/<string:user_id>', methods=['PATCH'])
@jwt_required()
def update_user_password(user_id):
    client = get_user_from_token(get_jwt())

    user = UsersService.get_user(user_id)

    if not has_permission(client, user):
        raise UnauthorizedException()

    data = request.json
    current_password = data.get('current_password')
    new_password = data.get('new_password')

    user = UsersService.get_user(user_id)
    is_admin_client = is_admin(client)

    if not new_password or (not current_password and not is_admin_client):
        raise BadRequestException()

    UsersService.change_password(user, current_password, new_password)
    return '', 204


@app.route('/api/users/<string:user_id>', methods=['DELETE'])
@jwt_required()
def delete_user_account(user_id):
    client = get_user_from_token(get_jwt())
    user = UsersService.get_user(user_id)

    if not has_permission(client, user):
        raise UnauthorizedException()
    
    user = UsersService.get_user(user_id)
    UsersService.delete_account(user)
    return '', 204


@app.route('/api/users/<string:user_id>/playlists', methods=['POST'])
@jwt_required()
def create_playlist(user_id):
    client = get_user_from_token(get_jwt())
    user = UsersService.get_user(user_id)

    if not has_permission(client, user):
        raise UnauthorizedException()
    
    data = request.get_json()
    name = data.get('name')
    is_public = data.get('is_public', True)

    if not user_id or not name:
        raise BadRequestException()

    id = PlaylistsService.create_playlist(user_id, name, is_public)
    response = make_response()
    response.status_code = 201

    base_url = request.host_url.rstrip('/')
    response.headers["Location"] = f"{base_url}/api/playlists/{id}"
    return response