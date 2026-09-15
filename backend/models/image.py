from datetime import datetime

from flask import request
from sqlalchemy.dialects.mysql import LONGBLOB

from config import db


class Image(db.Model):
    """
    Las imagenes viven en la base de datos, como los MP3, para no tener que
    anadir un volumen al despliegue ni tratarlas aparte en las copias de
    seguridad. A diferencia de Mp3 se guardan en binario y no en base64: no
    hay motivo para pagar el 33% de sobrecoste y el paso de decodificado.

    Una fila no se modifica nunca: cambiar la imagen de algo crea otra y borra
    la anterior. Por eso su URL puede cachearse para siempre, y cambiarla
    invalida la cache sola.
    """
    id = db.Column(db.Integer, primary_key=True)
    mime = db.Column(db.String(32), nullable=False)
    data = db.Column(LONGBLOB, nullable=False)
    width = db.Column(db.Integer)
    height = db.Column(db.Integer)
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.utcnow)

    def __init__(self, mime, data, width, height):
        self.mime = mime
        self.data = data
        self.width = width
        self.height = height
        self.created_at = datetime.utcnow()

    def get_url(self):
        origin = request.host_url.rstrip("/")
        return f"{origin}/uploads/images/{self.id}"


def image_url_of(image):
    """URL de una imagen que puede no existir, tal y como viaja en los DTO."""
    return image.get_url() if image is not None else None
