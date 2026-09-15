"""
Migraciones de esquema.

El proyecto no usa Alembic: las tablas las crea db.create_all() y los cambios
sobre tablas que ya existen se aplican aqui. Cada paso se registra en
`schema_migration` al terminar, asi que reiniciar el contenedor no repite
trabajo por muchas veces que se arranque.

Gunicorn levanta varios workers y todos importan app.py, o sea que todos
entran aqui a la vez. Los pasos se serializan con un lock de MySQL: migra uno
y el resto espera a que acabe antes de seguir arrancando. Mientras los pasos
fueron `ALTER TABLE` idempotentes daba igual, pero una migracion de datos
ejecutada dos veces en paralelo si hace dano.
"""

import sys
from datetime import datetime

from sqlalchemy import inspect, text

from config import app, db


# El lock es por servidor MySQL, no por conexion, que es justo lo que hace
# falta para coordinar procesos distintos.
LOCK_NAME = "playbutton_migrations"
LOCK_TIMEOUT_SECONDS = 120

_steps = []


class _Step:

    def __init__(self, name, apply, already_applied=None):
        self.name = name
        self.apply = apply
        # Los pasos anteriores a esta tabla no dejaron registro de haberse
        # ejecutado, y en una base de datos nueva create_all() ya deja el
        # esquema final. En ambos casos el paso sobra: esta comprobacion mira
        # el esquema para darlo por hecho en lugar de volver a ejecutarlo.
        self.already_applied = already_applied


def step(name, already_applied=None):
    """Registra una migracion. El orden de declaracion es el de ejecucion."""
    def register(fn):
        _steps.append(_Step(name, fn, already_applied))
        return fn
    return register


def has_column(table, column):
    return column in {c["name"] for c in inspect(db.engine).get_columns(table)}


# ---------------------------------------------------------------- migraciones

@step(
    "0001_user_token_version",
    already_applied=lambda: has_column("user", "token_version"),
)
def _user_token_version():
    """
    La version de credenciales que invalida los tokens emitidos antes del
    ultimo cambio de contrasena. Las filas existentes arrancan en 0, que es
    la version que llevan los tokens ya emitidos.
    """
    db.session.execute(
        text("ALTER TABLE `user` ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0")
    )
    db.session.commit()


@step(
    "0002_suggestion_created_at",
    already_applied=lambda: has_column("suggestion", "created_at"),
)
def _suggestion_created_at():
    """
    Fecha de la primera vez que se sugirio la cancion. Las filas previas se
    quedan con la fecha de la migracion, que es lo mas aproximado a "cuando
    se sugirio" que se puede reconstruir.
    """
    db.session.execute(
        text(
            "ALTER TABLE `suggestion` ADD COLUMN created_at DATETIME NOT NULL "
            "DEFAULT CURRENT_TIMESTAMP"
        )
    )
    db.session.commit()


# ------------------------------------------------------------------- registro

def _ensure_registry_table():
    db.session.execute(
        text(
            "CREATE TABLE IF NOT EXISTS schema_migration ("
            "  name VARCHAR(100) NOT NULL PRIMARY KEY,"
            "  applied_at DATETIME NOT NULL"
            ")"
        )
    )
    db.session.commit()


def _applied_names():
    rows = db.session.execute(text("SELECT name FROM schema_migration")).all()
    return {row[0] for row in rows}


def _mark_applied(name):
    db.session.execute(
        text("INSERT INTO schema_migration (name, applied_at) VALUES (:name, :now)"),
        {"name": name, "now": datetime.utcnow()},
    )
    db.session.commit()


# -------------------------------------------------------------------- runner

def _run_pending(dry_run=False):
    applied = _applied_names()
    results = []

    for migration in _steps:
        if migration.name in applied:
            continue

        if migration.already_applied is not None and migration.already_applied():
            if not dry_run:
                _mark_applied(migration.name)
            results.append((migration.name, "ya estaba en el esquema"))
            continue

        if dry_run:
            results.append((migration.name, "pendiente"))
            continue

        migration.apply()
        _mark_applied(migration.name)
        results.append((migration.name, "aplicada"))

    return results


def run_migrations(dry_run=False):
    """
    Aplica las migraciones que falten y devuelve que se ha hecho con cada una.
    Con dry_run no escribe nada: solo informa de lo que quedaria por aplicar.
    """
    _ensure_registry_table()

    # El lock vive en la conexion que lo pide, asi que se reserva una aparte y
    # se mantiene abierta: db.session devuelve la suya al pool en cada commit
    # y con ella se soltaria el lock a mitad de la migracion.
    with db.engine.connect() as lock_connection:
        acquired = lock_connection.execute(
            text("SELECT GET_LOCK(:name, :timeout)"),
            {"name": LOCK_NAME, "timeout": LOCK_TIMEOUT_SECONDS},
        ).scalar()

        if acquired != 1:
            raise RuntimeError(
                f"No se ha podido tomar el lock de migraciones en {LOCK_TIMEOUT_SECONDS}s"
            )

        try:
            return _run_pending(dry_run)
        finally:
            lock_connection.execute(
                text("SELECT RELEASE_LOCK(:name)"), {"name": LOCK_NAME}
            )


def main(argv):
    dry_run = "--dry-run" in argv

    with app.app_context():
        results = run_migrations(dry_run=dry_run)

    if not results:
        print("Migraciones: nada pendiente.")
        return

    print("Migraciones (simulacion):" if dry_run else "Migraciones:")
    for name, outcome in results:
        print(f"  {name}: {outcome}")


if __name__ == "__main__":
    main(sys.argv[1:])
