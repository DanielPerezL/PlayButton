from flask import  request
from config import app, db
from models import Song, Mp3
from sqlalchemy.sql import func
import os
import base64
from exceptions import BadRequestException, ConflictException, AppException, NotFoundException, BadAudioFileException
from utils import has_more_results
from pydub import AudioSegment
import tempfile


class SongsService():
    
    @staticmethod
    def __allowed_file(filename):
        """Verifica si el archivo tiene una extensión válida (MP3)."""
        return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

    @staticmethod
    def add_song(name, mp3_file, shown_zenn=True, normalize=True):
        try:
            # Verificar si el archivo tiene una extensión permitida
            if not SongsService.__allowed_file(mp3_file.filename):
                raise BadRequestException()
            
            # Verificar si el archivo tiene contenido (tamaño > 0)
            mp3_file.seek(0, os.SEEK_END)  # Mover al final del archivo para obtener su tamaño
            file_size = mp3_file.tell()
            mp3_file.seek(0)  # Volver al inicio del archivo
            
            if file_size == 0:
                raise BadRequestException()
            
            # Verificar si ya existe una canción con ese nombre
            if Song.query.filter_by(name=name).first():
                raise ConflictException()
            
            # Crear la canción y guardarla en la base de datos
            new_song = Song(name=name)
            db.session.add(new_song)
            db.session.commit()
            
            # Generar el nombre del archivo basado en el ID
            filename = f"{new_song.id}.mp3"

            try:
                if normalize:
                    # --- Preprocesado: normalización del volumen ---
                    audio = AudioSegment.from_file(mp3_file, format="mp3")

                    target_dBFS = -14.0
                    change_in_dBFS = target_dBFS - audio.dBFS
                    processed_audio = audio.apply_gain(change_in_dBFS)

                    with tempfile.NamedTemporaryFile(delete=True, suffix=".mp3") as tmp_file:
                        processed_audio.export(tmp_file.name, format="mp3")
                        tmp_file.seek(0)
                        mp3_data = tmp_file.read()
                else:
                    # Si no normalizamos, leemos el archivo directamente
                    mp3_data = mp3_file.read()

                if not mp3_data:
                    raise BadAudioFileException()

                encoded_mp3 = base64.b64encode(mp3_data).decode('utf-8')

                mp3_record = Mp3(filename=filename, base64_data=encoded_mp3)
                db.session.add(mp3_record)
                db.session.commit()
            except Exception:
                db.session.rollback()
                raise BadAudioFileException()
            
            if not shown_zenn:
                new_song.shown_zenn = False
            
            db.session.commit()
            return new_song.id

        except Exception as e:
            db.session.rollback()
            raise AppException(e)

    
    @staticmethod
    def update_song(song_id, new_name, new_zenn):
        try:
            song = Song.query.get(song_id)
            if not song:
                raise NotFoundException()
            
            if Song.query.filter(Song.name == new_name, Song.id != song_id).first():
                raise ConflictException()
            
            song.name = new_name
            song.shown_zenn = new_zenn
            db.session.commit()
        except Exception as e:
            db.session.rollback()

    @staticmethod
    def get_all(offset=0, limit=20, name=None, details=False):
        if details:
            return SongsService.get_all_with_details(offset, limit, name)
        try:
            query = Song.query
            # Filtrado por nombre si se proporciona
            if name:
                query = query.filter(Song.name.ilike(f"%{name}%"))
                songs = query.order_by(Song.id.asc()).offset(offset).limit(limit).all()
            else:
                # Orden aleatorio si no hay filtro de nombre
                query = query.filter(Song.shown_zenn.is_(True))
                songs = query.order_by(db.func.random()).limit(limit).all()
            return {
                "songs": [song.to_dto() for song in songs],
                "has_more": has_more_results(query, offset, limit),
            }
        except Exception as e:
            raise AppException()

    @staticmethod
    def get_all_with_details(offset=0, limit=20, name=None):
        try:
            query = Song.query
            if name:
                query = query.filter(Song.name.ilike(f"%{name}%"))
            songs = query.order_by(Song.id.asc()).offset(offset).limit(limit).all()
        
            return {
                "songs": [song.to_detailed_dto() for song in songs],
                "has_more": has_more_results(query, offset, limit),
            }
        except Exception as e:
            raise AppException()    
       
    @staticmethod
    def delete_song(song_id):
        try:
            # Buscar la canción en la base de datos
            song = Song.query.get(song_id)
            if not song:
                return

            # Eliminar la canción de la base de datos
            filename = f"{song.id}.mp3"
            db.session.delete(song)

            mp3_record = Mp3.query.filter_by(filename=filename).first()
            if mp3_record:
                db.session.delete(mp3_record)

            db.session.commit()
        except Exception as e:
            db.session.rollback()  # Revertir cualquier cambio en caso de error
            raise AppException()
