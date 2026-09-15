from config import db
from models import Artist, Playlist
from exceptions import AppException, BadRequestException, ConflictException, NotFoundException
from utils import has_more_results, get_user_admin


class ArtistsService:
    """
    El maestro de artistas y la playlist que se le genera. Antes esto era la
    mitad de SongsService.sync_artist_playlists, que cruzaba playlists por
    nombre cada vez que se tocaba una cancion.
    """

    @staticmethod
    def get_or_create(name):
        """
        Resuelve el artista por nombre y lo crea si no existe. La comparacion
        la hace MySQL con su collation por defecto, que ignora mayusculas y
        tildes: "rosalia" encuentra a "Rosalía" en vez de duplicarla.
        """
        name = (name or "").strip()
        if not name:
            raise BadRequestException("Nombre de artista vacío")

        artist = Artist.query.filter(Artist.name == name).first()
        if artist is None:
            artist = Artist(name=name)
            db.session.add(artist)
            db.session.flush()
        return artist

    @staticmethod
    def sync_playlist(artist):
        """
        Pone al dia la playlist del artista: la crea si falta, iguala sus
        canciones a las del artista, y borra ambos cuando el artista se queda
        sin ninguna. Un artista sin canciones no pinta nada en la biblioteca,
        que es como se comportaban ya las playlists de artista.
        """
        if not artist.songs:
            if artist.playlist is not None:
                # En este orden y explicito: borrando solo el artista,
                # SQLAlchemy dejaria la playlist con artist_id a NULL en vez
                # de dejar que actue el ON DELETE CASCADE, y quedaria suelta.
                db.session.delete(artist.playlist)
            db.session.delete(artist)
            db.session.flush()
            return

        playlist = artist.playlist
        if playlist is None:
            playlist = Playlist(
                name=artist.name,
                is_public=True,
                artist=artist,
                user=get_user_admin(),
            )
            db.session.add(playlist)
        else:
            playlist.name = artist.name

        playlist.songs = list(artist.songs)
        db.session.flush()

    @staticmethod
    def get_all(offset=0, limit=20, search="", current_user_id=None):
        try:
            # El join deja fuera a los artistas sin playlist, que no deberian
            # existir, y conserva el orden por popularidad que ya tenia el
            # listado cuando un artista era una playlist con bandera.
            query = Artist.query.join(Playlist, Playlist.artist_id == Artist.id)

            if search:
                query = query.filter(Artist.name.ilike(f"%{search}%"))

            query = query.order_by(Playlist.favorites_count.desc(), Artist.id)
            artists = query.offset(offset).limit(limit).all()

            return {
                "artists": [a.to_dto(current_user_id) for a in artists],
                "has_more": has_more_results(query, offset, limit),
            }
        except Exception:
            raise AppException()

    @staticmethod
    def rename(artist_id, new_name):
        new_name = (new_name or "").strip()
        if not new_name:
            raise BadRequestException()

        try:
            artist = Artist.query.get(artist_id)
            if not artist:
                raise NotFoundException()

            if Artist.query.filter(Artist.name == new_name, Artist.id != artist_id).first():
                raise ConflictException()

            artist.name = new_name
            # Arrastra el nombre a su playlist, que es lo que ve el cliente.
            ArtistsService.sync_playlist(artist)
            db.session.commit()
            return artist.to_dto()
        except AppException:
            db.session.rollback()
            raise
        except Exception:
            db.session.rollback()
            raise AppException()
