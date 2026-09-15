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
import unicodedata
from datetime import datetime

from sqlalchemy import inspect, text

from config import app, db
from utils import ARTIST_TITLE_SEPARATOR, split_artist_title


# El lock es por servidor MySQL, no por conexion, que es justo lo que hace
# falta para coordinar procesos distintos.
LOCK_NAME = "playbutton_migrations"
LOCK_TIMEOUT_SECONDS = 120

_steps = []


class _Step:

    def __init__(self, name, apply, already_applied=None, report=None):
        self.name = name
        self.apply = apply
        # Informe para el dry-run. Las migraciones de datos no son reversibles
        # ni exactas, asi que las que adivinan algo cuentan antes que van a
        # hacer y con que casos dudosos se han encontrado.
        self.report = report
        # Los pasos anteriores a esta tabla no dejaron registro de haberse
        # ejecutado, y en una base de datos nueva create_all() ya deja el
        # esquema final. En ambos casos el paso sobra: esta comprobacion mira
        # el esquema para darlo por hecho en lugar de volver a ejecutarlo.
        self.already_applied = already_applied


def step(name, already_applied=None, report=None):
    """Registra una migracion. El orden de declaracion es el de ejecucion."""
    def register(fn):
        _steps.append(_Step(name, fn, already_applied, report))
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



# ------------------------------------------------- artistas y titulos (0003)

def _fold(name):
    """
    Aproxima en Python la collation por defecto de MySQL, que ignora
    mayusculas y tildes. Solo sirve para agrupar en el informe: quien decide
    de verdad si dos nombres son el mismo artista es la base de datos.
    """
    decomposed = unicodedata.normalize("NFD", name)
    return "".join(c for c in decomposed if not unicodedata.combining(c)).lower()


def _old_songs():
    return db.session.execute(text("SELECT id, name FROM `song` ORDER BY id")).all()


def _report_artists_and_titles():
    songs = _old_songs()
    variants = {}
    sin_artista = []
    con_coma = []

    for song_id, name in songs:
        artists, title = split_artist_title(name)

        if not artists:
            sin_artista.append(f"#{song_id} {name!r}")
        elif "," in name.split(ARTIST_TITLE_SEPARATOR)[0]:
            con_coma.append(f"#{song_id} {name!r} -> artistas {artists}, titulo {title!r}")

        for artist in artists:
            variants.setdefault(_fold(artist), set()).add(artist)

    fusiones = {k: v for k, v in variants.items() if len(v) > 1}

    playlists = db.session.execute(text(
        "SELECT name FROM `playlist` WHERE is_artist_playlist = 1"
    )).all()
    conocidos = set(variants)
    huerfanas = sorted({row[0] for row in playlists if _fold(row[0]) not in conocidos})

    lines = [
        f"{len(songs)} canciones, {len(variants)} artistas distintos",
        "",
    ]

    lines.append(f"Canciones sin separador '{ARTIST_TITLE_SEPARATOR}', se quedaran sin artista: {len(sin_artista)}")
    lines.extend(f"  {s}" for s in sin_artista)

    lines.append("")
    lines.append(f"REVISAR A MANO. Comas en la parte del artista: {len(con_coma)}")
    lines.append("  La coma separa artistas, asi que 'Tyler, The Creator' se partiria en dos.")
    lines.extend(f"  {s}" for s in con_coma)

    lines.append("")
    lines.append(f"Artistas que se fusionan por mayusculas o tildes: {len(fusiones)}")
    lines.extend(f"  {sorted(v)} -> uno solo" for v in fusiones.values())

    lines.append("")
    lines.append(f"Playlists de artista que ya no corresponden a nadie y se borraran: {len(huerfanas)}")
    lines.extend(f"  {name!r}" for name in huerfanas)

    return lines


def _artist_id_for(name):
    """Resuelve el artista por nombre, dejando que MySQL aplique su collation."""
    row = db.session.execute(
        text("SELECT id FROM `artist` WHERE name = :name"), {"name": name}
    ).first()
    if row is not None:
        return row[0]

    db.session.execute(text("INSERT INTO `artist` (name) VALUES (:name)"), {"name": name})
    return db.session.execute(text("SELECT LAST_INSERT_ID()")).scalar()


def _link_artist_playlists():
    """
    Engancha cada playlist de artista existente con su artista, que es como se
    cruzaban hasta ahora: por nombre. Reutilizarlas en lugar de rehacerlas es
    lo que conserva los favoritos que ya tenian.
    """
    rows = db.session.execute(text(
        "SELECT id, name FROM `playlist` WHERE is_artist_playlist = 1 "
        "ORDER BY favorites_count DESC, id"
    )).all()

    taken = {}
    for playlist_id, name in rows:
        artist = db.session.execute(
            text("SELECT id FROM `artist` WHERE name = :name"), {"name": name}
        ).first()

        if artist is None:
            # Ese artista ya no aparece en ninguna cancion.
            db.session.execute(text("DELETE FROM `playlist` WHERE id = :id"), {"id": playlist_id})
            continue

        artist_id = artist[0]
        winner = taken.get(artist_id)

        if winner is None:
            # La primera que llega es la que mas favoritos tenia, por el ORDER BY.
            taken[artist_id] = playlist_id
            db.session.execute(
                text("UPDATE `playlist` SET artist_id = :artist WHERE id = :id"),
                {"artist": artist_id, "id": playlist_id},
            )
            continue

        # "Rosalia" y "Rosalía" eran dos playlists y pasan a ser un solo
        # artista: los favoritos se juntan en la que mas tenia.
        db.session.execute(
            text(
                "INSERT IGNORE INTO `user_favorite_playlist` (user_id, playlist_id) "
                "SELECT user_id, :winner FROM `user_favorite_playlist` WHERE playlist_id = :loser"
            ),
            {"winner": winner, "loser": playlist_id},
        )
        db.session.execute(text("DELETE FROM `playlist` WHERE id = :id"), {"id": playlist_id})

    db.session.commit()

    db.session.execute(text(
        "UPDATE `playlist` p SET favorites_count = "
        "(SELECT COUNT(*) FROM `user_favorite_playlist` f WHERE f.playlist_id = p.id) "
        "WHERE p.artist_id IS NOT NULL"
    ))

    # Los artistas que no tenian playlist (o cuya playlist se borro) reciben una.
    db.session.execute(text(
        "INSERT INTO `playlist` (name, is_public, artist_id, user_id, favorites_count) "
        "SELECT a.name, 1, a.id, 1, 0 FROM `artist` a "
        "LEFT JOIN `playlist` p ON p.artist_id = a.id WHERE p.id IS NULL"
    ))

    # A partir de aqui el nombre y las canciones de la playlist los manda el artista.
    db.session.execute(text(
        "UPDATE `playlist` p JOIN `artist` a ON a.id = p.artist_id SET p.name = a.name"
    ))
    db.session.execute(text(
        "DELETE ps FROM `playlist_song` ps JOIN `playlist` p ON p.id = ps.playlist_id "
        "WHERE p.artist_id IS NOT NULL AND ps.song_id NOT IN "
        "(SELECT sa.song_id FROM `song_artist` sa WHERE sa.artist_id = p.artist_id)"
    ))
    db.session.execute(text(
        "INSERT IGNORE INTO `playlist_song` (playlist_id, song_id) "
        "SELECT p.id, sa.song_id FROM `playlist` p "
        "JOIN `song_artist` sa ON sa.artist_id = p.artist_id"
    ))
    db.session.commit()


@step(
    "0003_artists_and_titles",
    already_applied=lambda: has_column("song", "title") and not has_column("song", "name"),
    report=_report_artists_and_titles,
)
def _artists_and_titles():
    """
    Deshace el formato "Artista1, Artista2 - Titulo" de `song.name`: el titulo
    pasa a columna propia y los artistas al maestro `artist`, enlazados por
    `song_artist`. Las playlists de artista se conservan, ahora apuntando a su
    artista en lugar de cruzarse con el por nombre.

    Es irreversible y adivina donde acaba el artista, asi que conviene pasar
    antes el `--dry-run` y revisar los casos que saca.
    """
    if not has_column("song", "title"):
        db.session.execute(text("ALTER TABLE `song` ADD COLUMN title VARCHAR(255) NULL"))
    if not has_column("playlist", "artist_id"):
        db.session.execute(text(
            "ALTER TABLE `playlist` ADD COLUMN artist_id INT NULL, "
            "ADD CONSTRAINT uq_playlist_artist UNIQUE (artist_id), "
            "ADD CONSTRAINT fk_playlist_artist FOREIGN KEY (artist_id) "
            "REFERENCES `artist` (id) ON DELETE CASCADE"
        ))
    db.session.commit()

    for song_id, name in _old_songs():
        artist_names, title = split_artist_title(name)

        db.session.execute(
            text("UPDATE `song` SET title = :title WHERE id = :id"),
            {"title": title, "id": song_id},
        )

        for position, artist_name in enumerate(artist_names):
            db.session.execute(
                text(
                    "INSERT IGNORE INTO `song_artist` (song_id, artist_id, position) "
                    "VALUES (:song, :artist, :position)"
                ),
                {"song": song_id, "artist": _artist_id_for(artist_name), "position": position},
            )

    db.session.commit()

    # El titulo ya esta relleno, asi que puede ser obligatorio. Y con el nombre
    # viejo se va tambien su unique, que no sabria expresar que una cancion se
    # distingue por titulo mas conjunto de artistas.
    db.session.execute(text("ALTER TABLE `song` MODIFY COLUMN title VARCHAR(255) NOT NULL"))
    db.session.execute(text("ALTER TABLE `song` DROP COLUMN name"))
    db.session.commit()

    _link_artist_playlists()

    db.session.execute(text("ALTER TABLE `playlist` DROP COLUMN is_artist_playlist"))
    db.session.commit()



# ------------------------------------------------------------ imagenes (0004)

_IMAGE_OWNER_TABLES = ("song", "artist", "playlist")


@step(
    "0004_images",
    already_applied=lambda: all(
        has_column(table, "image_id") for table in _IMAGE_OWNER_TABLES
    ),
)
def _images():
    """
    Cuelga una portada opcional de canciones, artistas y playlists. La tabla
    `image` la crea create_all por ser nueva; aqui solo van las claves ajenas
    de las tablas que ya existian.

    Se comprueba tabla a tabla porque `artist` puede haber nacido ya con la
    columna: en una base de datos que no llegara a pasar por la 0003, la crea
    create_all a partir del modelo, que ya la declara.
    """
    for table in _IMAGE_OWNER_TABLES:
        if has_column(table, "image_id"):
            continue
        db.session.execute(text(
            f"ALTER TABLE `{table}` ADD COLUMN image_id INT NULL, "
            f"ADD CONSTRAINT fk_{table}_image FOREIGN KEY (image_id) "
            "REFERENCES `image` (id) ON DELETE SET NULL"
        ))
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
            results.append((migration.name, "ya estaba en el esquema", []))
            continue

        if dry_run:
            detail = migration.report() if migration.report else []
            results.append((migration.name, "pendiente", detail))
            continue

        migration.apply()
        _mark_applied(migration.name)
        results.append((migration.name, "aplicada", []))

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
    for name, outcome, detail in results:
        print(f"  {name}: {outcome}")
        for line in detail:
            print(f"    {line}")


if __name__ == "__main__":
    main(sys.argv[1:])
