from models import User
from utils import create_tokens
from exceptions import NotFoundException, UnauthorizedException


class AuthService():

    @staticmethod
    def login(nickname, password):
        user = User.query.filter_by(nickname=nickname).first()
        if user is None or not user.check_password(password):
            raise UnauthorizedException()
        
        access_token = create_tokens(user)
        return access_token, user