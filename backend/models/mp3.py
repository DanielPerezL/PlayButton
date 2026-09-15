from config import db
from sqlalchemy.dialects.mysql import LONGTEXT


class Mp3(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    filename = db.Column(db.String(255), nullable=False, unique=True)
    base64_data = db.Column(LONGTEXT, nullable=False)  # Cambiar de Text a LONGTEXT

    def __init__(self, filename, base64_data):
        self.filename = filename
        self.base64_data = base64_data  # Ahora almacenamos en LONGTEXT