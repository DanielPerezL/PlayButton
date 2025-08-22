from models import User
from config import NICKNAME_MAX_LENGTH, db
from exceptions import *
import re
from sqlalchemy.exc import SQLAlchemyError, IntegrityError
from utils import has_more_results


class UsersService():

    @staticmethod
    def add_user(nickname, password):    
        if not re.match("^[a-zA-Z0-9ñÑ]*$", nickname):
            raise InvalidNicknameException()
        if len(nickname) > NICKNAME_MAX_LENGTH:
            raise TooLongNicknameException()
        
        new_user = User(nickname=nickname, password=password)
        if not new_user:
            raise UserNickInUseException() 

        try:
            db.session.add(new_user)
            db.session.commit()
            return new_user.id
        except IntegrityError: 
            db.session.rollback()
            raise UserNickInUseException()
        except Exception:
            db.session.rollback()
            return None

    @staticmethod    
    def get_user_playlists(user_id):
        user = User.query.get(user_id)
        if not user:
            raise NotFoundException()

        return [playlist.to_dto() for playlist in user.playlists]
    
    @staticmethod    
    def get_user_public_playlists(user_id):
        user = User.query.get(user_id)
        if not user:
            raise NotFoundException()

        return [playlist.to_dto() for playlist in user.playlists if playlist.is_public]

    @staticmethod
    def get_user(id):
        user = User.query.get(id)
        if user is None:
            #COMPROBAR SI SE RECIBE EL NICKNAME
            user = User.query.filter_by(nickname=id).first()
            if user is None:
                raise NotFoundException()
        return user

    @staticmethod
    def change_password(user, current_password, new_password):
        try:
            if not new_password:
                raise BadRequestException()

            if current_password and not user.check_password(current_password):
                raise UnauthorizedException()

            if not user.set_password(new_password):
                raise AppException()

            db.session.commit()
        except Exception as e:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def delete_account(deleting_user):
        try:
            db.session.delete(deleting_user)
            # DELETE ORPHAN ELIMINA LAS playlists
            db.session.commit()
        except SQLAlchemyError:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def get_all(offset=0, limit=20, name=None):
        try:
            query = User.query
            if name:
                query = query.filter(User.nickname.ilike(f"%{name}%"))  
            users = (
                query.offset(offset)
                .limit(limit)
                .all()
            )

            return {
                "users": [user.to_dto() for user in users],
                "has_more": has_more_results(query, offset, limit),
            }
        except Exception as e:
            raise AppException()

