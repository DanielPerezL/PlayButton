package com.zice.playbutton.data.local.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Caché en disco de las canciones de cada playlist.
 *
 * En la app React Native esta caché existía solo para el propietario de la
 * playlist, así que el detalle de un artista —que pertenece siempre al
 * administrador— nunca se cacheaba para un usuario normal y volvía a la red
 * cada vez. Aquí se cachea para todo el mundo, con una marca de tiempo que
 * permite caducarla en lugar de tener que vaciarla entera al arrancar.
 */
@Entity(tableName = "cached_songs", primaryKeys = ["playlistId", "songId"])
data class CachedSongEntity(
    val playlistId: Int,
    val songId: Int,
    val title: String,
    /** Los artistas, unidos por [ARTIST_SEPARATOR]. Ver [joinArtists]. */
    val artists: String,
    val position: Int,
    val imageUrl: String? = null,
)

/**
 * Los artistas se guardan en una sola columna, y la coma no sirve de separador
 * porque un artista puede llevarla en el nombre ("Tyler, The Creator"). Este
 * carácter de control no aparece en un nombre escrito por nadie.
 */
private const val ARTIST_SEPARATOR = "\u001F"

fun joinArtists(artists: List<String>): String = artists.joinToString(ARTIST_SEPARATOR)

fun splitArtists(stored: String): List<String> =
    if (stored.isEmpty()) emptyList() else stored.split(ARTIST_SEPARATOR)

@Entity(tableName = "playlist_cache_meta")
data class PlaylistCacheMetaEntity(
    @androidx.room.PrimaryKey val playlistId: Int,
    val updatedAt: Long,
)

/**
 * Playlists cuyo audio está entero en el dispositivo.
 *
 * Existe porque sin conexión no hay de dónde sacar esta lista: los listados se
 * cachean en memoria y mueren con el proceso, así que al abrir la app en un
 * sitio sin cobertura no quedaba nada que ofrecer aunque las canciones
 * estuvieran guardadas. Aquí se anota lo suficiente para pintar la fila —el
 * nombre, el autor, cuántas son— y poder entrar a reproducirla.
 */
@Entity(tableName = "downloaded_playlists")
data class DownloadedPlaylistEntity(
    @androidx.room.PrimaryKey val playlistId: Int,
    val name: String,
    val ownerName: String,
    val isArtist: Boolean,
    val songCount: Int,
    val updatedAt: Long,
    val imageUrl: String? = null,
)

@Dao
interface DownloadedPlaylistDao {

    @Query("SELECT * FROM downloaded_playlists ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<DownloadedPlaylistEntity>>

    @Query("SELECT * FROM downloaded_playlists WHERE playlistId = :playlistId")
    suspend fun find(playlistId: Int): DownloadedPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: DownloadedPlaylistEntity)

    @Query("DELETE FROM downloaded_playlists WHERE playlistId = :playlistId")
    suspend fun delete(playlistId: Int)

    @Query("DELETE FROM downloaded_playlists")
    suspend fun clear()
}

@Dao
interface SongCacheDao {

    @Query("SELECT * FROM cached_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun songsFor(playlistId: Int): List<CachedSongEntity>

    @Query("SELECT updatedAt FROM playlist_cache_meta WHERE playlistId = :playlistId")
    suspend fun updatedAt(playlistId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<CachedSongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: PlaylistCacheMetaEntity)

    @Query("DELETE FROM cached_songs WHERE playlistId = :playlistId")
    suspend fun deleteSongs(playlistId: Int)

    @Query("DELETE FROM playlist_cache_meta WHERE playlistId = :playlistId")
    suspend fun deleteMeta(playlistId: Int)

    @Transaction
    suspend fun replace(playlistId: Int, songs: List<CachedSongEntity>) {
        deleteSongs(playlistId)
        insertSongs(songs)
        insertMeta(PlaylistCacheMetaEntity(playlistId, System.currentTimeMillis()))
    }

    @Transaction
    suspend fun invalidate(playlistId: Int) {
        deleteSongs(playlistId)
        deleteMeta(playlistId)
    }

    @Query("DELETE FROM cached_songs")
    suspend fun clearSongs()

    @Query(
        "DELETE FROM cached_songs WHERE playlistId NOT IN " +
            "(SELECT playlistId FROM downloaded_playlists)",
    )
    suspend fun clearSongsNotDownloaded()

    @Query("DELETE FROM playlist_cache_meta")
    suspend fun clearMeta()

    @Transaction
    suspend fun clearAll() {
        clearSongs()
        clearMeta()
    }

    /**
     * Vacía la caché de canciones pero deja las de las playlists descargadas:
     * son la lista que hace falta para poder reproducirlas sin conexión, y sin
     * ella una descarga se queda en audio suelto que nadie sabe agrupar.
     *
     * La frescura no se hereda: se borran todas las marcas de tiempo para que
     * la siguiente sesión revalide contra el servidor en cuanto lo tenga.
     */
    @Transaction
    suspend fun clearExceptDownloaded() {
        clearSongsNotDownloaded()
        clearMeta()
    }
}

@Database(
    entities = [
        CachedSongEntity::class,
        PlaylistCacheMetaEntity::class,
        DownloadedPlaylistEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class PlayButtonDatabase : RoomDatabase() {
    abstract fun songCacheDao(): SongCacheDao
    abstract fun downloadedPlaylistDao(): DownloadedPlaylistDao
}

/**
 * El nombre de la cancion se parte en titulo y artistas, igual que ha hecho el
 * backend con su propia migracion.
 *
 * Se migra en lugar de dejar que Room rehaga la base de datos porque aqui no
 * todo es cache: `downloaded_playlists` es la unica lista de lo que esta
 * descargado, y perderla deja el audio en el dispositivo sin nada que lo
 * agrupe ni forma de reproducirlo sin conexion.
 *
 * SQLite no puede quitar una columna en las versiones que cubre minSdk 26, asi
 * que la tabla se rehace. El reparto usa el mismo criterio que usaba la app:
 * la primera aparicion de " - ". Los artistas se quedan en una sola entrada
 * ("Queen, Bowie"), que es exactamente lo que se mostraba antes; la proxima
 * vez que haya servidor, el listado los trae ya separados.
 */
/**
 * La URL de la portada, para poder pintar las filas y las tarjetas sin
 * conexion. Se anade en las dos tablas: `cached_songs` es lo que se muestra al
 * abrir una playlist descargada, y `downloaded_playlists` la lista que la
 * contiene.
 *
 * Nace vacia a proposito: la portada correcta la trae el servidor en la
 * siguiente revalidacion, y hasta entonces se pinta el hueco de siempre.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cached_songs ADD COLUMN imageUrl TEXT")
        db.execSQL("ALTER TABLE downloaded_playlists ADD COLUMN imageUrl TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cached_songs_new (
                playlistId INTEGER NOT NULL,
                songId INTEGER NOT NULL,
                title TEXT NOT NULL,
                artists TEXT NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY(playlistId, songId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO cached_songs_new (playlistId, songId, title, artists, position)
            SELECT playlistId, songId,
                CASE WHEN instr(name, ' - ') > 1
                     THEN substr(name, instr(name, ' - ') + 3)
                     ELSE name END,
                CASE WHEN instr(name, ' - ') > 1
                     THEN substr(name, 1, instr(name, ' - ') - 1)
                     ELSE '' END,
                position
            FROM cached_songs
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE cached_songs")
        db.execSQL("ALTER TABLE cached_songs_new RENAME TO cached_songs")
    }
}
