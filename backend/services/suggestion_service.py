from config import db
from models import Suggestion
from exceptions import BadRequestException, AppException
from utils import has_more_results


class SuggestionsService:

    @staticmethod
    def create_suggestion(song_name):
        try:
            if not song_name or not song_name.strip():
                raise BadRequestException()

            # Evitar duplicados
            existing = Suggestion.query.filter_by(song_name=song_name.strip()).first()
            if existing:
                return

            suggestion = Suggestion(song_name=song_name.strip())
            db.session.add(suggestion)
            db.session.commit()
        except Exception as e:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def get_all(offset=0, limit=20):
        try:
            query = Suggestion.query
            suggestions = (
                query.offset(offset)
                .limit(limit)
                .all()
            )

            return {
                "suggestions": [s.to_dto() for s in suggestions],
                "has_more": has_more_results(query, offset, limit),
            }
        except Exception:
            raise AppException()

    @staticmethod
    def delete_suggestion(suggestion_id):
        try:
            suggestion = Suggestion.query.get(suggestion_id)
            if not suggestion:
                raise BadRequestException("Suggestion not found")

            db.session.delete(suggestion)
            db.session.commit()
        except Exception:
            db.session.rollback()
            raise AppException()