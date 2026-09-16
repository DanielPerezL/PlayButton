from config import db
from .image import image_relationship, image_url_of


# Tabla de asociacion entre Songs y Artists. `position` conserva el orden en
# que se escribieron los artistas, que es el que se muestra: "Rosalia, Tokischa"
# y "Tokischa, Rosalia" no son lo mismo para quien lee la ficha.
song_artist = db.Table(
    'song_artist',
    db.Column('song_id', db.Integer, db.ForeignKey('song.id', ondelete="CASCADE"), primary_key=True),
    db.Column('artist_id', db.Integer, db.ForeignKey('artist.id', ondelete="CASCADE"), primary_key=True),
    db.Column('position', db.Integer, nullable=False, default=0)
)


class Artist(db.Model):
    """
    Maestro de artistas. Antes un artista era una Playlist con una bandera, sin
    identidad propia: no se podia renombrar ni darle una imagen sin que el
    siguiente alta de cancion lo deshiciera. La playlist se sigue generando,
    pero ahora cuelga de esta tabla.

    El unique de `name` va contra la collation por defecto de MySQL, que ignora
    mayusculas y tildes, asi que "Rosalia" y "Rosalía" son el mismo artista.
    Es justo lo que se quiere: hasta ahora eran dos playlists distintas.
    """
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False, unique=True)
    image_id = db.Column(db.Integer, db.ForeignKey('image.id', ondelete='SET NULL'), nullable=True)
    image = image_relationship()

    songs = db.relationship(
        'Song',
        secondary=song_artist,
        # selectin en los dos sentidos: los listados serializan muchas
        # canciones con sus artistas y muchos artistas con su numero de
        # canciones, y sin esto cada fila se traia su propia consulta.
        lazy='selectin',
        backref=db.backref('artists', lazy='selectin', order_by=song_artist.c.position),
        passive_deletes=True
    )

    def __init__(self, name):
        self.name = name

    def to_dto(self, current_user_id=None):
        playlist = self.playlist
        return {
            "id": self.id,
            "name": self.name,
            # La playlist del artista es lo que el cliente abre para ver sus
            # canciones, asi que viaja con el artista y no hace falta cruzarlas.
            "playlist_id": playlist.id if playlist else None,
            "songs_count": len(self.songs),
            "image_url": image_url_of(self.image),
            "favorites_count": len(playlist.favorited_by) if playlist else 0,
            "is_favorite": (
                any(u.id == current_user_id for u in playlist.favorited_by)
                if playlist and current_user_id else False
            ),
        }
