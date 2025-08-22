from flask import request, jsonify, make_response
from config import app
from services import PlaylistsService
from exceptions import *
from flask_jwt_extended import (
                                jwt_required, 
                                get_jwt, 
                                )
from utils import get_user_from_token, has_permission
from models import Playlist


@app.route('/api/playlists', methods=['GET'])
@jwt_required()
def get_all_playlists():
    # Obtener parámetros offset y limit de la query string (con valores por defecto)
    offset = request.args.get('offset', 0, type=int)
    limit = request.args.get('limit', 20, type=int)

    playlists_data = PlaylistsService.get_all_playlists(offset, limit)
    return jsonify(playlists_data), 200

@app.route('/api/playlists/<int:playlist_id>', methods=['DELETE'])
@jwt_required()
def delete_playlist(playlist_id):
    client = get_user_from_token(get_jwt())
    playlist = Playlist.query.get(playlist_id)

    if not has_permission(client, playlist):
        raise UnauthorizedException()
    PlaylistsService.delete_playlist(playlist_id)
    return '', 204

@app.route('/api/playlists/<int:playlist_id>', methods=['PUT'])
@jwt_required()
def update_playlist(playlist_id):
    client = get_user_from_token(get_jwt())
    playlist = Playlist.query.get(playlist_id)

    if not has_permission(client, playlist):
        raise UnauthorizedException()

    data = request.get_json()
    new_name = data.get('name')
    is_public = data.get('is_public')

    if new_name is None or is_public is None:
        raise BadRequestException()

    PlaylistsService.update_playlist(playlist_id, new_name, is_public)
    return '', 200


@app.route('/api/playlists/<int:playlist_id>/songs/<int:song_id>', methods=['POST'])
@jwt_required()
def add_song_to_playlist(playlist_id, song_id):
    client = get_user_from_token(get_jwt())
    playlist = Playlist.query.get(playlist_id)

    if not has_permission(client, playlist):
        raise UnauthorizedException()
    
    PlaylistsService.add_song_to_playlist(playlist_id, song_id)
    return '', 201


@app.route('/api/playlists/<int:playlist_id>/songs/<int:song_id>', methods=['DELETE'])
@jwt_required()
def remove_song_from_playlist(playlist_id, song_id):
    client = get_user_from_token(get_jwt())
    playlist = Playlist.query.get(playlist_id)

    if not has_permission(client, playlist):
        raise UnauthorizedException()

    PlaylistsService.remove_song_from_playlist(playlist_id, song_id)
    return '', 204


@app.route('/api/playlists/<int:playlist_id>/songs', methods=['GET'])
@jwt_required()
def get_playlist_songs(playlist_id):
    client = get_user_from_token(get_jwt())
    playlist = Playlist.query.get(playlist_id)

    if not playlist.is_public and not has_permission(client, playlist):
        raise UnauthorizedException()

    songs = PlaylistsService.get_playlist_songs(playlist_id)
    return jsonify(songs), 200
