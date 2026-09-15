from config import db
from models import Suggestion
from exceptions import BadRequestException, AppException
from utils import has_more_results


class SuggestionsService:

    @staticmethod
    def create_suggestion(song_name, user=None):
        if not song_name or not song_name.strip():
            raise BadRequestException()

        try:
            song_name = song_name.strip()

            # Las repeticiones no crean fila nueva: se suma el usuario a la que
            # ya existe y se conserva la fecha de la primera sugerencia.
            suggestion = Suggestion.query.filter_by(song_name=song_name).first()
            if suggestion is None:
                suggestion = Suggestion(song_name=song_name)
                db.session.add(suggestion)

            suggestion.add_suggester(user)
            db.session.commit()
        except Exception:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def get_all(offset=0, limit=20):
        try:
            # Las mas recientes primero: es el orden util en el panel de admin.
            query = Suggestion.query.order_by(
                Suggestion.created_at.desc(), Suggestion.id.desc()
            )
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
        suggestion = Suggestion.query.get(suggestion_id)
        if not suggestion:
            raise BadRequestException("Suggestion not found")

        try:
            db.session.delete(suggestion)
            db.session.commit()
        except Exception:
            db.session.rollback()
            raise AppException()
