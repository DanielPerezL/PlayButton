from werkzeug.security import generate_password_hash, check_password_hash
from config import db, NICKNAME_MAX_LENGTH


class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    nickname = db.Column(db.String(NICKNAME_MAX_LENGTH), unique=True, nullable=False)
    password_hash = db.Column(db.String(200), nullable=False)
    # Se incrementa en cada cambio de contrasena para invalidar los tokens que
    # se emitieron antes. server_default cubre las filas ya existentes cuando
    # se anade la columna a una base de datos en produccion.
    token_version = db.Column(db.Integer, nullable=False, default=0, server_default='0')
    

    def __init__(self, nickname, password):
        self.nickname = nickname
        self.password_hash = generate_password_hash(password)

    def check_password(self, password):
        return check_password_hash(self.password_hash, password)

    def set_password(self, password):
        self.password_hash = generate_password_hash(password)
        # Cambiar la contrasena cierra el resto de sesiones abiertas.
        self.token_version = (self.token_version or 0) + 1
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