from werkzeug.security import generate_password_hash, check_password_hash
from config import db, NICKNAME_MAX_LENGTH


class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    nickname = db.Column(db.String(NICKNAME_MAX_LENGTH), unique=True, nullable=False)
    password_hash = db.Column(db.String(200), nullable=False)
    

    def __init__(self, nickname, password):
        self.nickname = nickname
        self.password_hash = generate_password_hash(password)

    def check_password(self, password):
        return check_password_hash(self.password_hash, password)

    def set_password(self, password):
        self.password_hash = generate_password_hash(password)
        try:
            db.session.commit()
            return True
        except Exception:
            db.session.rollback()
            return False

    def to_dto(self):
        #Usuario DTO
        return {
            'id': self.id,
            'nickname': self.nickname,
        }