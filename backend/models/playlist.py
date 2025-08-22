from config import db


# Tabla de asociación entre Playlists y Songs
playlist_song = db.Table(
    'playlist_song',
    db.Column('playlist_id', db.Integer, db.ForeignKey('playlist.id', ondelete="CASCADE"), primary_key=True),
    db.Column('song_id', db.Integer, db.ForeignKey('song.id', ondelete="CASCADE"), primary_key=True)
)

class Playlist(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False)
    is_public = db.Column(db.Boolean, default=True)

    user_id = db.Column(db.Integer, db.ForeignKey('user.id', ondelete='CASCADE'), nullable=False)
    user = db.relationship('User', backref=db.backref('playlists', lazy=True, cascade='all, delete-orphan'))

    songs = db.relationship(
        'Song',
        secondary=playlist_song,
        backref=db.backref('playlists', lazy='dynamic'),
        passive_deletes=True
    )

    def __init__(self, name, user, is_public=True):
        self.name = name
        self.user = user
        self.is_public = is_public

    def to_dto(self):
        return {
            "id": self.id,
            "name": self.name,
            "user": self.user.nickname,
            "user_id": self.user.id,
            "is_public": self.is_public,
        }

    def get_songs(self):
        return {"songs":[song.to_dto() for song in self.songs]}
