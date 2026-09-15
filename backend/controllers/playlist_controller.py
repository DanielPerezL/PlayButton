from flask import request, jsonify, make_response
from config import app
from services import PlaylistsService, ArtistsService
from exceptions import *
from flask_jwt_extended import (
                                jwt_required, 
                                get_jwt, 
                                )
from utils import get_user_from_token, has_permission, check_is_admin
from models import Playlist


@app.route('/api/playlists', methods=['GET'])
@jwt_required()
def get_all_playlists():
    client = get_user_from_token(get_jwt())
    offset = request.args.get('offset', 0, type=int)
    limit = request.args.get('limit', 20, type=int)
    search = request.args.get('search', '', type=str)

    playlists_data = PlaylistsService.get_all_playlists(
        offset=offset,
        limit=limit,
        search=search,
        current_user_id=client.id
    )
    
    return jsonify(playlists_data), 200

@app.route('/api/artists', methods=['GET'])
@jwt_required()
def get_all_artists():
    client = get_user_from_token(get_jwt())
    offset = request.args.get('offset', 0, type=int)
    limit = request.args.get('limit', 20, type=int)
    search = request.args.get('search', '', type=str)

    artists_data = ArtistsService.get_all(
        offset=offset,
        limit=limit,
        search=search,
        current_user_id=client.id
    )

    return jsonify(artists_data), 200


@app.route('/api/artists/<int:artist_id>', methods=['PATCH'])
@jwt_required()
def rename_artist(artist_id):
    check_is_admin()

    data = request.get_json()
    new_name = data.get('name') if data else None
    if not new_name:
        raise BadRequestException("Falta el nuevo nombre del artista")

    return jsonify(ArtistsService.rename(artist_id, new_name)), 200

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

@app.route('/api/playlists/<int:playlist_id>/favorite', methods=['POST'])
@jwt_required()
def toggle_playlist_favorite(playlist_id):
    client = get_user_from_token(get_jwt())
    
    new_count = PlaylistsService.toggle_favorite(client, playlist_id)
    return jsonify({"favorites_count": new_count}), 200