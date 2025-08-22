from flask import request, jsonify
from flask_jwt_extended import jwt_required
from config import app
from services import SuggestionsService
from exceptions import BadRequestException
from utils import check_is_admin


@app.route('/api/suggestions', methods=['POST'])
@jwt_required()
def create_suggestion():
    data = request.get_json()
    
    song_name = data.get('song_name') if data else None
    if not song_name:
        raise BadRequestException()

    SuggestionsService.create_suggestion(song_name)
    return '', 201


@app.route('/api/suggestions', methods=['GET'])
@jwt_required()
def get_all_suggestions():
    check_is_admin()
    offset = request.args.get('offset', 0, type=int)
    limit = request.args.get('limit', 100, type=int)

    return jsonify(
        SuggestionsService.get_all(offset, limit)
    ), 200

@app.route('/api/suggestions/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_suggestion(id):
    check_is_admin()
    
    if not id:
        raise BadRequestException()

    SuggestionsService.delete_suggestion(id)
    return '', 204