from config import db
from .image import image_url_of


# Tabla de asociación entre Playlists y Songs
playlist_song = db.Table(
    'playlist_song',
    db.Column('playlist_id', db.Integer, db.ForeignKey('playlist.id', ondelete="CASCADE"), primary_key=True),
    db.Column('song_id', db.Integer, db.ForeignKey('song.id', ondelete="CASCADE"), primary_key=True)
)

user_favorite_playlist = db.Table(
    'user_favorite_playlist',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id', ondelete="CASCADE"), primary_key=True),
    db.Column('playlist_id', db.Integer, db.ForeignKey('playlist.id', ondelete="CASCADE"), primary_key=True)
)

class Playlist(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False)
    is_public = db.Column(db.Boolean, default=True)
    image_id = db.Column(db.Integer, db.ForeignKey('image.id', ondelete='SET NULL'), nullable=True)
    image = db.relationship('Image')

    # Las playlists de artista se distinguian por una bandera y se cruzaban con
    # el artista por nombre. Ahora lo apuntan: la bandera se deduce de aqui, y
    # renombrar un artista deja de romper el vinculo.
    # Unico: cada artista tiene como mucho una playlist, que es lo que da por
    # supuesto el uselist=False del backref. En MySQL varios NULL no chocan,
    # asi que las playlists normales no se estorban.
    artist_id = db.Column(db.Integer, db.ForeignKey('artist.id', ondelete='CASCADE'),
                          nullable=True, unique=True)
    artist = db.relationship('Artist', backref=db.backref('playlist', uselist=False))

    user_id = db.Column(db.Integer, db.ForeignKey('user.id', ondelete='CASCADE'), nullable=False)
    user = db.relationship('User', backref=db.backref('playlists', lazy=True, cascade='all, delete-orphan'))

    songs = db.relationship(
        'Song',
        secondary=playlist_song,
        backref=db.backref('playlists', lazy='dynamic'),
        passive_deletes=True
    )

    favorites_count = db.Column(db.Integer, default=0, nullable=False)
    favorited_by = db.relationship(
        'User',
        secondary=user_favorite_playlist,
        backref=db.backref('favorite_playlists', lazy='dynamic')
    )

    def __init__(self, name, user, is_public=True, artist=None):
        self.name = name
        self.user = user
        self.is_public = is_public
        self.artist = artist

    @property
    def is_artist_playlist(self):
        return self.artist_id is not None

    @property
    def resolved_image(self):
        """
        La de una playlist de artista es la del artista: es el mismo cromo en
        dos sitios, y mantenerlas por separado solo daria ocasion de que se
        desincronizaran.
        """
        return self.artist.image if self.artist is not None else self.image

    def to_dto(self, current_user_id=None):
        return {
            "id": self.id,
            "name": self.name,
            "user": self.user.nickname if self.user.id != 1 else "Sistema",
            "user_id": self.user.id,
            "is_public": self.is_public,
            "is_artist_playlist": self.is_artist_playlist,
            "image_url": image_url_of(self.resolved_image),
            "favorites_count": len(self.favorited_by),
            "is_favorite": any(u.id == current_user_id for u in self.favorited_by) if current_user_id else False
        }

    def get_songs(self):
        return {"songs":[song.to_dto() for song in self.songs]}
