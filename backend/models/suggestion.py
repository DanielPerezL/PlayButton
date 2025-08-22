from config import db


class Suggestion(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    song_name = db.Column(db.String(200), unique=True, nullable=False)    

    def __init__(self, song_name):
        self.song_name = song_name
    
    def to_dto(self):
        return {
            "id": self.id,
            "song_name": self.song_name
        }
