from config import db
from flask import request

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
    shown_zenn = db.Column(db.Boolean, default=True)

    def __init__(self, title):
        self.title = title

    def to_dto(self):
        return {
            "id": self.id,
            "title": self.title,
            "artists": [{"id": a.id, "name": a.name} for a in self.artists],
        }

    def to_detailed_dto(self):
        return {
            **self.to_dto(),
            "shown_zenn": self.shown_zenn,
        }

    def get_filename(self):
        return f"{self.id}.mp3"

    def get_mp3_url(self):
        origin = request.host_url.rstrip("/")
        return f"{origin}/uploads/mp3_files/{self.id}.mp3"
