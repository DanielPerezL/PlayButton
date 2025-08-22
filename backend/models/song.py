from config import db, signer
from urllib.parse import quote
import time
from flask import request

class Song(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False, unique=True)
    shown_zenn = db.Column(db.Boolean, default=True)

    def __init__(self, name):
        self.name = name
        self.mp3_url = ""

    def to_dto(self):
        return {
            "id": self.id,
            "name": self.name,
        }
    
    def to_detailed_dto(self):
        return {
            "id": self.id,
            "name": self.name,
            "shown_zenn": self.shown_zenn,
        }

    def get_filename(self):
        return f"{self.id}.mp3"

    def get_mp3_url(self):
        origin = request.host_url.rstrip("/")
        return f"{origin}/uploads/mp3_files/{self.id}.mp3"
