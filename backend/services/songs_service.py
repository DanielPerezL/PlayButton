from config import app, db
from models import Song, Mp3, Artist, song_artist
from sqlalchemy import or_
from sqlalchemy.sql import func
import os
import base64
from exceptions import BadRequestException, ConflictException, AppException, NotFoundException, BadAudioFileException
from utils import has_more_results
from .artists_service import ArtistsService
from pydub import AudioSegment
import tempfile


class SongsService():

    @staticmethod
    def __allowed_file(filename):
        """Verifica si el archivo tiene una extensión válida (MP3)."""
        return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

    @staticmethod
    def __read_mp3(mp3_file, normalize):
        """Devuelve los bytes del MP3, normalizados de volumen si se pide."""
        if not normalize:
            return mp3_file.read()

        audio = AudioSegment.from_file(mp3_file, format="mp3")

        target_dBFS = -14.0
        change_in_dBFS = target_dBFS - audio.dBFS
        processed_audio = audio.apply_gain(change_in_dBFS)

        with tempfile.NamedTemporaryFile(delete=True, suffix=".mp3") as tmp_file:
            processed_audio.export(tmp_file.name, format="mp3")
            tmp_file.seek(0)
            return tmp_file.read()

    @staticmethod
    def _find_duplicate(title, artist_names, exclude_id=None):
        """
        Dos canciones son la misma si coinciden el título y el conjunto exacto
        de artistas. `Song.name` lo resolvía con un unique de columna, que ya
        no sirve ahora que el artista vive en otra tabla.
        """
        existing = [
            Artist.query.filter(Artist.name == name.strip()).first()
            for name in artist_names
        ]
        if any(artist is None for artist in existing):
            # Hay algún artista que aún no existe, así que no puede haber
            # ninguna canción con este conjunto y no hace falta mirar más.
            return None

        wanted = {artist.id for artist in existing}

        query = Song.query.filter(Song.title == title)
        if exclude_id is not None:
            query = query.filter(Song.id != exclude_id)

        for song in query.all():
            if {artist.id for artist in song.artists} == wanted:
                return song
        return None

    @staticmethod
    def add_song(title, artist_names, mp3_file, shown_zenn=True, normalize=True):
        try:
            if not SongsService.__allowed_file(mp3_file.filename):
                raise BadRequestException()

            # Verificar si el archivo tiene contenido (tamaño > 0)
            mp3_file.seek(0, os.SEEK_END)
            file_size = mp3_file.tell()
            mp3_file.seek(0)

            if file_size == 0:
                raise BadRequestException()

            if not title or not title.strip():
                raise BadRequestException("Falta el título de la canción")

            if SongsService._find_duplicate(title, artist_names):
                raise ConflictException()

            new_song = Song(title=title)
            new_song.shown_zenn = shown_zenn
            db.session.add(new_song)
            db.session.flush()

            SongsService.sync_song_artists(new_song, artist_names)

            # El audio se procesa con la canción ya creada: si falla, el
            # rollback se lleva también los artistas que se hayan creado.
            try:
                mp3_data = SongsService.__read_mp3(mp3_file, normalize)
            except Exception:
                raise BadAudioFileException()

            if not mp3_data:
                raise BadAudioFileException()

            db.session.add(Mp3(
                filename=new_song.get_filename(),
                base64_data=base64.b64encode(mp3_data).decode('utf-8'),
            ))

            db.session.commit()
            return new_song.id

        except AppException:
            db.session.rollback()
            raise
        except Exception as e:
            db.session.rollback()
            raise AppException(e)

    @staticmethod
    def update_song(song_id, new_title, artist_names, new_zenn):
        try:
            song = Song.query.get(song_id)
            if not song:
                raise NotFoundException()

            if not new_title or not new_title.strip():
                raise BadRequestException("Falta el título de la canción")

            if SongsService._find_duplicate(new_title, artist_names, exclude_id=song_id):
                raise ConflictException()

            song.title = new_title
            song.shown_zenn = new_zenn
            SongsService.sync_song_artists(song, artist_names)

            db.session.commit()
        except AppException:
            db.session.rollback()
            raise
        except Exception:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def get_all(offset=0, limit=20, q=None, details=False, random=False):
        try:
            query = Song.query

            if q:
                # Antes bastaba un LIKE contra "Artista - Título" porque ambos
                # iban en la misma cadena; ahora hay que mirar en las dos.
                query = (
                    query
                    .outerjoin(song_artist, song_artist.c.song_id == Song.id)
                    .outerjoin(Artist, Artist.id == song_artist.c.artist_id)
                    .filter(or_(Song.title.ilike(f"%{q}%"), Artist.name.ilike(f"%{q}%")))
                    .distinct()
                )

            if random:
                # Modo Zenn. Antes se activaba por la ausencia del parámetro de
                # búsqueda, lo que obligaba a los clientes a inventarse un
                # término que casara con todo para poder listar la biblioteca.
                query = query.filter(Song.shown_zenn.is_(True))
                songs = query.order_by(func.rand()).limit(limit).all()
            else:
                songs = query.order_by(Song.id.desc()).offset(offset).limit(limit).all()

            to_dto = Song.to_detailed_dto if details else Song.to_dto
            return {
                "songs": [to_dto(song) for song in songs],
                "has_more": has_more_results(query, offset, limit),
            }
        except Exception:
            raise AppException()

    @staticmethod
    def delete_song(song_id):
        try:
            song = Song.query.get(song_id)
            if not song:
                return

            affected_artists = list(song.artists)
            mp3_record = Mp3.query.filter_by(filename=song.get_filename()).first()

            db.session.delete(song)
            if mp3_record:
                db.session.delete(mp3_record)
            db.session.flush()

            # Los artistas que se queden sin canciones desaparecen con ella;
            # de eso ya se encarga sync_playlist.
            for artist in affected_artists:
                db.session.expire(artist, ["songs"])
                ArtistsService.sync_playlist(artist)

            db.session.commit()
        except Exception:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def sync_song_artists(song, artist_names):
        """
        Reescribe los artistas de la canción y deja al día las playlists de
        los que entran y de los que salen.
        """
        previous = list(song.artists)

        artists = []
        for name in artist_names:
            artist = ArtistsService.get_or_create(name)
            # "Bowie, Bowie - X" es un solo artista, no dos.
            if artist not in artists:
                artists.append(artist)

        # La posición no se puede escribir a través de la relación, así que la
        # tabla de asociación se reescribe a mano.
        db.session.execute(song_artist.delete().where(song_artist.c.song_id == song.id))
        if artists:
            db.session.execute(
                song_artist.insert(),
                [
                    {"song_id": song.id, "artist_id": artist.id, "position": position}
                    for position, artist in enumerate(artists)
                ],
            )

        # Las colecciones cargadas no se enteran de los INSERT/DELETE directos.
        db.session.expire(song, ["artists"])
        db.session.flush()

        affected = {artist.id: artist for artist in previous + artists}
        for artist in affected.values():
            db.session.expire(artist, ["songs"])
            ArtistsService.sync_playlist(artist)
