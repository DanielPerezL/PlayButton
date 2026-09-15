from datetime import datetime

from config import db


# Tabla de asociación entre Suggestions y los Users que la han propuesto
suggestion_user = db.Table(
    'suggestion_user',
    db.Column('suggestion_id', db.Integer, db.ForeignKey('suggestion.id', ondelete="CASCADE"), primary_key=True),
    db.Column('user_id', db.Integer, db.ForeignKey('user.id', ondelete="CASCADE"), primary_key=True)
)


class Suggestion(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    song_name = db.Column(db.String(200), unique=True, nullable=False)
    # Las sugerencias repetidas se agrupan en la misma fila, asi que la fecha
    # es la de la primera vez que alguien propuso la cancion. Se guarda en UTC
    # y se serializa con sufijo Z para que el cliente la muestre en su zona.
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.utcnow)

    suggested_by = db.relationship(
        'User',
        secondary=suggestion_user,
        backref=db.backref('suggestions', lazy='dynamic'),
        passive_deletes=True
    )

    def __init__(self, song_name):
        self.song_name = song_name
        self.created_at = datetime.utcnow()

    def add_suggester(self, user):
        """Registra al usuario si no habia sugerido ya esta cancion."""
        if user is None:
            return False
        if any(u.id == user.id for u in self.suggested_by):
            return False
        self.suggested_by.append(user)
        return True

    def to_dto(self):
        return {
            "id": self.id,
            "song_name": self.song_name,
            "created_at": self.created_at.isoformat() + "Z" if self.created_at else None,
            "suggested_by": [user.to_dto() for user in self.suggested_by],
        }
