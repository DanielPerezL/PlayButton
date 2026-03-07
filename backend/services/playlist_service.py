from config import db
from models import Playlist, Song, User
from exceptions import BadRequestException, AppException, NotFoundException, ConflictException
from utils import has_more_results


class PlaylistsService:

    @staticmethod
    def create_playlist(user_id, name, is_public=True):
        try:
            if not name:
                raise BadRequestException()

            user = User.query.get(user_id)
            if not user:
                raise NotFoundException()

            playlist = Playlist(name=name, user=user, is_public=is_public)
            db.session.add(playlist)
            db.session.commit()
            return playlist.to_dto()
        except Exception:
            db.session.rollback()
            raise ConflictException()

    @staticmethod
    def delete_playlist(playlist_id):
        try:
            playlist = Playlist.query.get(playlist_id)
            if not playlist:
                raise NotFoundException()

            db.session.delete(playlist)
            db.session.commit()
        except Exception as e:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def update_playlist(playlist_id, new_name=None, is_public=None):
        try:
            playlist = Playlist.query.get(playlist_id)
            if not playlist:
                raise NotFoundException()

            if new_name is not None:
                playlist.name = new_name
            if is_public is not None:
                playlist.is_public = is_public

            db.session.commit()
        except Exception as e:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def add_song_to_playlist(playlist_id, song_id):
        try:
            playlist = Playlist.query.get(playlist_id)
            song = Song.query.get(song_id)
            if not playlist or not song:
                raise NotFoundException()

            if song in playlist.songs:
                return # Ya está incluida

            playlist.songs.append(song)
            db.session.commit()
        except Exception as e:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def remove_song_from_playlist(playlist_id, song_id):
        try:
            playlist = Playlist.query.get(playlist_id)
            song = Song.query.get(song_id)
            if not playlist or not song:
                raise NotFoundException()
                
            if song not in playlist.songs:
                return

            playlist.songs.remove(song)
            db.session.commit()
        except Exception as e:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def get_playlist_songs(playlist_id):
        try:
            playlist = Playlist.query.get(playlist_id)
            if not playlist:
                raise NotFoundException()

            return playlist.get_songs()
        except Exception as e:
            raise AppException()

    @staticmethod
    def get_playlists_paginated(query, offset, limit, current_user_id=None):
        # Aplicamos paginación
        playlists = query.offset(offset).limit(limit).all()
        # Verificamos si hay más
        has_more = query.offset(offset + limit).first() is not None
        
        return {
            "playlists": [p.to_dto(current_user_id) for p in playlists],
            "has_more": has_more
        }

    @staticmethod
    def get_playlists_by_user(user_id, only_public=True, offset=0, limit=20, search="", current_user_id=None):
        try:
            query = Playlist.query.filter_by(user_id=user_id)
            
            if only_public:
                query = query.filter_by(is_public=True)
            
            if search:
                query = query.filter(Playlist.name.ilike(f"%{search}%"))
            
            query = query.order_by(Playlist.favorites_count.desc(), Playlist.id)
            return PlaylistsService.get_playlists_paginated(query, offset, limit, current_user_id)
        except Exception:
            raise AppException()

    @staticmethod
    def get_all_playlists(offset=0, limit=20, search="", current_user_id=None):
        try:
            query = Playlist.query.filter_by(is_public=True)
            
            if search:
                query = query.filter(Playlist.name.ilike(f"%{search}%"))
                
            query = query.order_by(Playlist.favorites_count.desc(), Playlist.id)
            return PlaylistsService.get_playlists_paginated(query, offset, limit, current_user_id)
        except Exception:
            raise AppException()

    @staticmethod
    def toggle_favorite(user, playlist_id):
        try:
            playlist = Playlist.query.get(playlist_id)
            if not playlist:
                raise NotFoundException()
            if user in playlist.favorited_by:
                playlist.favorited_by.remove(user)
                playlist.favorites_count -= 1 # Decrementamos
            else:
                playlist.favorited_by.append(user)
                playlist.favorites_count += 1 # Incrementamos
            db.session.commit()
            return len(playlist.favorited_by)
        except Exception as e:
            db.session.rollback()
            if isinstance(e, NotFoundException):
                raise e
            raise AppException()
