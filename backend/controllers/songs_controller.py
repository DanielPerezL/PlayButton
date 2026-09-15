from flask import  request, jsonify, send_file, make_response
from config import app, signer, MAX_AGE_LINKS
from models import  Mp3
from services import SongsService
from exceptions import NotFoundException, UnauthorizedException
import tempfile
import base64
import json
from exceptions import MP3RecoveryException, BadRequestException, ConflictException
from models import Song
from flask_jwt_extended import (
                                jwt_required, 
                                get_jwt,
                                verify_jwt_in_request, 
                                )
from utils import get_user_from_token, is_admin, check_is_admin
from urllib.parse import urlparse
from itsdangerous import BadSignature, SignatureExpired
from urllib.parse import quote


def artist_names_from(raw):
    """
    Normaliza la lista de artistas que manda el cliente. Viaja como array JSON
    porque un artista puede llevar comas en el nombre ("Tyler, The Creator") y
    partir una cadena por ellas se los inventaba.
    """
    if raw is None:
        raise BadRequestException("Falta la lista de artistas")

    if isinstance(raw, str):
        try:
            raw = json.loads(raw)
        except ValueError:
            raise BadRequestException("Lista de artistas malformada")

    if not isinstance(raw, list) or any(not isinstance(name, str) for name in raw):
        raise BadRequestException("Lista de artistas malformada")

    return [name.strip() for name in raw if name.strip()]


@app.route('/api/songs', methods=['POST'])
@jwt_required()
def create_song():
    check_is_admin()

    if 'mp3' not in request.files or 'title' not in request.form:
        raise BadRequestException("Falta el archivo MP3 o el título de la canción")

    mp3_file = request.files['mp3']
    title = request.form['title']
    artist_names = artist_names_from(request.form.get('artists'))
    shown_zenn = request.form.get('shown_zenn', 'true').lower() == 'true'
    normalize = request.form.get('normalize', 'true').lower() == 'true'

    id = SongsService.add_song(title, artist_names, mp3_file, shown_zenn, normalize)
    response = make_response()
    response.status_code = 201

    base_url = request.host_url.rstrip('/')
    response.headers["Location"] = f"{base_url}/api/songs/{id}"
    return response

@app.route('/uploads/mp3_files/<filename>', methods=['GET'])
def get_mp3(filename):
    token = request.args.get("token")
    if not token:
        raise UnauthorizedException("Falta token")

    try:
        # Verifica que el token es válido y reciente
        verified_filename = signer.unsign(token, max_age=MAX_AGE_LINKS).decode()
    except SignatureExpired:
        raise UnauthorizedException("Token expirado")
    except BadSignature:
        raise UnauthorizedException("Token inválido")

    if verified_filename != filename:
        raise UnauthorizedException("Archivo no coincide")

    # Buscar en la base de datos
    mp3_record = Mp3.query.filter_by(filename=filename).first()
    if not mp3_record:
        raise NotFoundException()

    try:
        mp3_data = base64.b64decode(mp3_record.base64_data)
        with tempfile.NamedTemporaryFile(delete=True, suffix=".mp3") as temp_file:
            temp_file.write(mp3_data)
            temp_file.flush()
            return send_file(temp_file.name, mimetype="audio/mpeg")
    except Exception:
        raise MP3RecoveryException()

@app.route('/api/songs/<int:song_id>', methods=['PATCH'])
@jwt_required()
def update_song(song_id):
    check_is_admin()

    data = request.get_json()
    new_title = data.get("title")
    new_zenn = data.get("shown_zenn")
    if not new_title or new_zenn is None:
        raise BadRequestException("Falta indicar el nuevo título")

    artist_names = artist_names_from(data.get("artists"))

    SongsService.update_song(song_id, new_title, artist_names, new_zenn)
    return '', 204

@app.route('/api/songs', methods=['GET'])
@jwt_required()
def get_all_songs():
    offset = request.args.get('offset', 0, type=int)
    limit = request.args.get('limit', 100, type=int)
    q = request.args.get('q', None, type=str)
    details = request.args.get('details', "false", type=str).lower() == 'true'
    # El modo Zenn se pide de forma explícita. Antes se deducía de que no
    # hubiera término de búsqueda, y listar la biblioteca entera obligaba a
    # los clientes a inventarse uno que casara con todo.
    random = request.args.get('random', "false", type=str).lower() == 'true'

    return jsonify(
        SongsService.get_all(offset, limit, q, details, random)
    ), 200

@app.route('/api/songs/<int:song_id>/signed-url', methods=['GET'])
@jwt_required()
def get_song_signed_url(song_id):
    song = Song.query.get_or_404(song_id)
    filename = song.get_filename()
    signed_token = signer.sign(filename.encode()).decode()
    safe_token = quote(signed_token)

    return jsonify({
        "mp3_url": f"{song.get_mp3_url()}?token={safe_token}"
    }), 200

@app.route('/api/songs/<int:song_id>', methods=['DELETE'])
@jwt_required()
def delete_song(song_id):
    check_is_admin()
        
    SongsService.delete_song(song_id)
    return '', 204
