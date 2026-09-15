package com.zice.playbutton.data.local.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
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
    val name: String,
    val position: Int,
)

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
    version = 2,
    exportSchema = false,
)
abstract class PlayButtonDatabase : RoomDatabase() {
    abstract fun songCacheDao(): SongCacheDao
    abstract fun downloadedPlaylistDao(): DownloadedPlaylistDao
}
