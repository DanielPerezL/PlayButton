from io import BytesIO

from PIL import Image as PillowImage, ImageOps, UnidentifiedImageError

from config import db, IMAGE_SIZE, MAX_IMAGE_BYTES
from models import Image
from exceptions import (
    AppException,
    BadImageFileException,
    BadRequestException,
    ImageTooLargeException,
)


class ImagesService:
    """
    Alta y baja de portadas. Todo lo que entra se reescribe a un cuadrado WEBP
    del mismo tamano: asi una foto de movil de cuatro megas no acaba en la base
    de datos ni viajando entera al telefono, y la UI no tiene que lidiar con
    proporciones distintas en cada fila.
    """

    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'webp'}

    @staticmethod
    def __allowed_file(filename):
        return (
            '.' in filename
            and filename.rsplit('.', 1)[1].lower() in ImagesService.ALLOWED_EXTENSIONS
        )

    @staticmethod
    def __normalize(image_file):
        """Devuelve (bytes WEBP, ancho, alto) recortado a cuadrado."""
        picture = PillowImage.open(image_file)
        # Las fotos de movil llevan la orientacion en los EXIF; sin esto salen
        # giradas al perderlos en la reescritura.
        picture = ImageOps.exif_transpose(picture)
        picture = picture.convert("RGB")
        picture = ImageOps.fit(
            picture,
            (IMAGE_SIZE, IMAGE_SIZE),
            method=PillowImage.LANCZOS,
            centering=(0.5, 0.5),
        )

        buffer = BytesIO()
        picture.save(buffer, format="WEBP", quality=80)
        return buffer.getvalue(), picture.width, picture.height

    @staticmethod
    def replace_for(owner, image_file):
        """
        Cuelga una portada nueva de `owner` (canción, artista o playlist) y
        borra la que tuviera. Nunca se modifica una fila de `image`: así su URL
        puede cachearse para siempre y cambiarla invalida la caché sola.

        De borrar la anterior se encarga el `delete-orphan` de la relación, que
        es quien lo hace también cuando lo que desaparece es el dueño entero.
        """
        if image_file is None or not image_file.filename:
            raise BadRequestException("Falta el archivo de imagen")

        if not ImagesService.__allowed_file(image_file.filename):
            raise BadImageFileException()

        image_file.seek(0, 2)
        size = image_file.tell()
        image_file.seek(0)

        if size == 0:
            raise BadRequestException("El archivo de imagen está vacío")
        if size > MAX_IMAGE_BYTES:
            raise ImageTooLargeException()

        try:
            data, width, height = ImagesService.__normalize(image_file)
        except (UnidentifiedImageError, OSError, ValueError):
            raise BadImageFileException()

        try:
            image = Image(mime="image/webp", data=data, width=width, height=height)
            db.session.add(image)
            db.session.flush()

            owner.image = image
            owner.touch_songs()
            db.session.commit()
            return image
        except AppException:
            db.session.rollback()
            raise
        except Exception:
            db.session.rollback()
            raise AppException()

    @staticmethod
    def clear_for(owner):
        """Deja a `owner` sin portada. La fila de `image` se va con ella."""
        try:
            if owner.image is None:
                return

            owner.image = None
            owner.touch_songs()
            db.session.commit()
        except Exception:
            db.session.rollback()
            raise AppException()
