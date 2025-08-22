from config import NICKNAME_MAX_LENGTH

class AppException(Exception):
    """Excepción base de la aplicación."""
    status_code = 400
    error_code = "app_error"
    message = "Error inesperado."

    def __init__(self, message=None, status_code=None, error_code=None):
        super().__init__(message or self.message)
        self.status_code = status_code or self.status_code
        self.error_code = error_code or self.error_code

    def to_dict(self):
        return {
            "error": self.error_code,
            "message": str(self)
        }


class BadRequestException(AppException):
    status_code = 400
    error_code = "bad_request"
    message = "Petición malformada."


class BadAudioFileException(AppException):
    status_code = 400
    error_code = "bad_request"
    message = "Formato de audio no soportado."


class UnauthorizedException(AppException):
    status_code = 401
    error_code = "unauthorized"
    message = "No autorizado."


class ForbiddenException(AppException):
    status_code = 403
    error_code = "forbidden"
    message = "No tienes permiso para realizar esta acción."


class NotFoundException(AppException):
    status_code = 404
    error_code = "not_found"
    message = "Recurso no encontrado."


class ConflictException(AppException):
    status_code = 409
    error_code = "conflict"
    message = "Conflicto en la solicitud (ya existe, etc.)."


class MP3RecoveryException(AppException):
    status_code = 500
    error_code = "mp3_recovery_error"
    message = "Error al restaurar el MP3"


class InvalidNicknameException(AppException):
    status_code = 400
    error_code = "invalid_nick_error"
    message = "El nickname solo puede contener letras, números y 'ñ'."


class TooLongNicknameException(AppException):
    status_code = 400
    error_code = "too_long_nick_error"
    message = "El nombre de usuario excede el tamaño permitido ({} caracteres).".format(NICKNAME_MAX_LENGTH)


class UserNickInUseException(AppException):
    status_code = 400
    error_code = "invalid_nick_error"
    message = "Ya existe una cuenta asociada con ese nombre de usuario."
