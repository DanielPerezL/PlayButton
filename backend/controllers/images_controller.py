from flask import jsonify, make_response, request
from flask_jwt_extended import jwt_required, get_jwt

from config import app
from exceptions import NotFoundException, UnauthorizedException
from models import Artist, Image, Playlist, Song
from services import ImagesService
from utils import check_is_admin, get_user_from_token, has_permission


# Las subidas devuelven la URL de la portada nueva. Es la unica forma de que un
# cliente la pinte al momento: como cada cambio crea una fila, la URL anterior
# ya no vale y no hay endpoint para consultar un recurso suelto.
@app.route('/uploads/images/<int:image_id>', methods=['GET'])
def get_image(image_id):
    """
    Sin JWT y cacheable para siempre.

    No usa el enlace firmado de los MP3 a propósito: esas firmas caducan a los
    seis minutos y anularían cualquier caché, y un <img> del navegador no puede
    mandar la cabecera Authorization. Como una fila de `image` no se modifica
    nunca, cambiar la portada cambia la URL y la caché se invalida sola.
    """
    image = Image.query.get(image_id)
    if not image:
        raise NotFoundException()

    response = make_response(image.data)
    response.headers["Content-Type"] = image.mime
    response.headers["Cache-Control"] = "public, max-age=31536000, immutable"
    response.set_etag(str(image.id))
    return response.make_conditional(request)


def _require_song(song_id):
    check_is_admin()
    song = Song.query.get(song_id)
    if not song:
        raise NotFoundException()
    return song


def _require_artist(artist_id):
    check_is_admin()
    artist = Artist.query.get(artist_id)
    if not artist:
        raise NotFoundException()
    return artist


def _require_own_playlist(playlist_id):
    """La portada de una playlist la pone quien la ha hecho, no solo el admin."""
    playlist = Playlist.query.get(playlist_id)
    if not playlist:
        raise NotFoundException()
    if not has_permission(get_user_from_token(get_jwt()), playlist):
        raise UnauthorizedException()
    if playlist.is_artist_playlist:
        # La suya es la del artista: se cambia en el artista.
        raise UnauthorizedException("La portada de un artista se cambia en el artista")
    return playlist


@app.route('/api/songs/<int:song_id>/image', methods=['PUT'])
@jwt_required()
def set_song_image(song_id):
    image = ImagesService.replace_for(_require_song(song_id), request.files.get('image'))
    return jsonify({"image_url": image.get_url()}), 200


@app.route('/api/songs/<int:song_id>/image', methods=['DELETE'])
@jwt_required()
def delete_song_image(song_id):
    ImagesService.clear_for(_require_song(song_id))
    return '', 204


@app.route('/api/artists/<int:artist_id>/image', methods=['PUT'])
@jwt_required()
def set_artist_image(artist_id):
    image = ImagesService.replace_for(_require_artist(artist_id), request.files.get('image'))
    return jsonify({"image_url": image.get_url()}), 200


@app.route('/api/artists/<int:artist_id>/image', methods=['DELETE'])
@jwt_required()
def delete_artist_image(artist_id):
    ImagesService.clear_for(_require_artist(artist_id))
    return '', 204


@app.route('/api/playlists/<int:playlist_id>/image', methods=['PUT'])
@jwt_required()
def set_playlist_image(playlist_id):
    image = ImagesService.replace_for(_require_own_playlist(playlist_id), request.files.get('image'))
    return jsonify({"image_url": image.get_url()}), 200


@app.route('/api/playlists/<int:playlist_id>/image', methods=['DELETE'])
@jwt_required()
def delete_playlist_image(playlist_id):
    ImagesService.clear_for(_require_own_playlist(playlist_id))
    return '', 204
