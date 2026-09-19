from datetime import datetime

from config import db
from flask import request
from sqlalchemy.dialects.mysql import DATETIME as MysqlDateTime
from .image import image_relationship, image_url_of

class Song(db.Model):
    """
    El titulo y los artistas eran una sola cadena con el formato
    "Artista1, Artista2 - Titulo", que los tres clientes tenian que partir por
    su cuenta. Ahora el titulo es suyo y los artistas cuelgan de la tabla
    `artist` a traves de `song_artist` (relacion declarada alli, que deja aqui
    el backref `artists`).

    Con ello se pierde el unique que tenia `name`: una misma cancion se
    distingue por titulo mas conjunto de artistas, y eso no es expresable como
    restriccion de columna. La comprobacion de duplicado vive en SongsService.
    """
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(255), nullable=False)
    shown_zen = db.Column(db.Boolean, default=True)
    image_id = db.Column(db.Integer, db.ForeignKey('image.id', ondelete='SET NULL'), nullable=True)
    image = image_relationship()
    # Cuando cambiaron por ultima vez los metadatos que viajan en el DTO. Los
    # clientes guardan la cancion para verla sin conexion, y sin esto no tienen
    # forma de saber que su copia se ha quedado vieja: el titulo, los artistas
    # o la portada cambian en el servidor y ellos siguen mostrando lo suyo.
    # El MP3 no cuenta, que ese no se toca nunca.
    #
    # Con milisegundos y no con segundos, que es lo que da un DATETIME pelado:
    # un cliente que pidiera la lista en el mismo segundo en que se cambia algo
    # se quedaria con la fecha nueva y la copia vieja, y ya no habria forma de
    # que se enterase.
    updated_at = db.Column(MysqlDateTime(fsp=3), nullable=False, default=datetime.utcnow)

    def __init__(self, title):
        self.title = title
        self.updated_at = datetime.utcnow()

    @property
    def resolved_image(self):
        """
        La imagen que se muestra. Si la cancion no tiene la suya se usa la del
        primer artista, que es lo que evita una biblioteca entera de huecos
        grises por tener que subir una portada cancion a cancion.

        Solo la del primero, que es el principal: la cancion se reconoce por
        el, y no por el colaborador que resulte ser el primero con foto.
        `artists` viene ordenado por `song_artist.position`, asi que el primero
        es el que se escribio primero.
        """
        if self.image is not None:
            return self.image
        return self.artists[0].image if self.artists else None

    def to_dto(self):
        return {
            "id": self.id,
            "title": self.title,
            "artists": [{"id": a.id, "name": a.name} for a in self.artists],
            "image_url": image_url_of(self.resolved_image),
            # En UTC y con sufijo Z, como el resto de fechas de la API.
            "updated_at": self.updated_at.isoformat() + "Z" if self.updated_at else None,
        }

    def to_detailed_dto(self):
        return {
            **self.to_dto(),
            "shown_zen": self.shown_zen,
            # El panel necesita distinguir la portada propia de la heredada del
            # artista para saber si hay algo que quitar.
            "own_image_url": image_url_of(self.image),
        }

    def touch_songs(self):
        """
        Lo que cambia aqui son sus propios metadatos. Existe con el mismo
        nombre en Artist y en Playlist para que quien cambia una portada no
        tenga que saber de quien es.
        """
        touch_songs([self])

    def get_filename(self):
        return f"{self.id}.mp3"

    def get_mp3_url(self):
        origin = request.host_url.rstrip("/")
        return f"{origin}/uploads/mp3_files/{self.id}.mp3"


def touch_songs(songs):
    """
    Marca que los metadatos de estas canciones han cambiado.

    Lo llaman quienes los cambian, y no solo la propia cancion: la portada y el
    nombre de un artista salen en el DTO de todas las suyas, asi que tocarle la
    foto deja vieja la copia que los clientes tengan de cada una.
    """
    now = datetime.utcnow()
    for song in songs:
        song.updated_at = now
