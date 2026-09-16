from config import db
from flask import request
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

    def __init__(self, title):
        self.title = title

    @property
    def resolved_image(self):
        """
        La imagen que se muestra. Si la cancion no tiene la suya se usa la del
        primer artista, que es lo que evita una biblioteca entera de huecos
        grises por tener que subir una portada cancion a cancion.
        """
        if self.image is not None:
            return self.image
        for artist in self.artists:
            if artist.image is not None:
                return artist.image
        return None

    def to_dto(self):
        return {
            "id": self.id,
            "title": self.title,
            "artists": [{"id": a.id, "name": a.name} for a in self.artists],
            "image_url": image_url_of(self.resolved_image),
        }

    def to_detailed_dto(self):
        return {
            **self.to_dto(),
            "shown_zen": self.shown_zen,
            # El panel necesita distinguir la portada propia de la heredada del
            # artista para saber si hay algo que quitar.
            "own_image_url": image_url_of(self.image),
        }

    def get_filename(self):
        return f"{self.id}.mp3"

    def get_mp3_url(self):
        origin = request.host_url.rstrip("/")
        return f"{origin}/uploads/mp3_files/{self.id}.mp3"
