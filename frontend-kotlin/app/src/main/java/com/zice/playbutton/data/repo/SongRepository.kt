package com.zice.playbutton.data.repo

import com.zice.playbutton.data.local.ImageCache
import com.zice.playbutton.data.local.db.CachedSongEntity
import com.zice.playbutton.data.local.db.joinArtists
import com.zice.playbutton.data.local.db.splitArtists
import com.zice.playbutton.data.local.db.SongCacheDao
import com.zice.playbutton.data.remote.ApiService
import com.zice.playbutton.data.remote.ServerUrl
import com.zice.playbutton.data.remote.dto.SuggestionRequest
import com.zice.playbutton.domain.Song
import com.zice.playbutton.domain.toDomain
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SongPage(
    val songs: List<Song>,
    val hasMore: Boolean,
)

@Singleton
class SongRepository @Inject constructor(
    private val api: ApiService,
    private val songCacheDao: SongCacheDao,
    private val imageCache: ImageCache,
) {
    private companion object {
        const val SEARCH_PAGE_SIZE = 50
        const val ZEN_BATCH_SIZE = 10
        const val CACHE_TTL_MILLIS = 30 * 60 * 1000L
    }

    /**
     * Canciones de una playlist, de la caché si está fresca.
     *
     * El endpoint no pagina: devuelve la lista completa.
     */
    suspend fun playlistSongs(playlistId: Int, forceRefresh: Boolean = false): List<Song> {
        if (!forceRefresh) {
            val updatedAt = songCacheDao.updatedAt(playlistId)
            if (updatedAt != null && System.currentTimeMillis() - updatedAt < CACHE_TTL_MILLIS) {
                val cached = songCacheDao.songsFor(playlistId)
                if (cached.isNotEmpty()) return cached.toSongs()
            }
        }

        val songs = try {
            api.getPlaylistSongs(playlistId).songs.map { it.toDomain() }
        } catch (error: IOException) {
            // Sin servidor se recurre a lo último que se guardó, aunque haya
            // caducado: si el audio está en el dispositivo la playlist se
            // escucha igual, y enseñar un error sería engañoso. Solo se
            // propaga el fallo si tampoco hay nada guardado.
            val cached = songCacheDao.songsFor(playlistId)
            if (cached.isEmpty()) throw error
            return cached.toSongs()
        }
        // Qué portadas tenía antes, para saber cuáles suelta al guardar las
        // nuevas. Se lee ahora porque `replace` reescribe las filas.
        val previousCovers = songCacheDao.imageUrlsFor(playlistId)

        songCacheDao.replace(
            playlistId = playlistId,
            songs = songs.mapIndexed { index, song ->
                CachedSongEntity(
                    playlistId = playlistId,
                    songId = song.id,
                    title = song.title,
                    artists = joinArtists(song.artists),
                    position = index,
                    imageUrl = song.imageUrl,
                    updatedAt = song.updatedAt,
                )
            },
        )
        forgetCoversNoLongerUsed(previousCovers)
        return songs
    }

    /**
     * Tira las portadas que la biblioteca acaba de soltar.
     *
     * Es lo que hace que quitar una portada desde el panel se note en el
     * móvil. El servidor deja de mandar su URL, pero el archivo seguía en el
     * dispositivo y se pintaba desde ahí: las portadas se sirven con
     * `immutable` y un año de caducidad, así que no se vuelve a preguntar por
     * ellas, y la caché vive en `filesDir` para que el sistema no se la lleve.
     * De ahí que la misma canción saliera con portada en la lista y sin ella
     * en el reproductor, que arma su cola con lo recién traído del servidor.
     *
     * Solo se sueltan las que no quede nadie usando: la portada de un artista
     * es también la de todas sus canciones y la de su playlist, y basta con
     * que una siga guardada para que el archivo haga falta.
     */
    private suspend fun forgetCoversNoLongerUsed(previous: List<String>) {
        if (previous.isEmpty()) return

        val unused = previous.filterNot { songCacheDao.isImageInUse(it) }
        withContext(Dispatchers.IO) { imageCache.forget(unused) }
    }

    private fun List<CachedSongEntity>.toSongs(): List<Song> =
        map {
            Song(
                id = it.songId,
                title = it.title,
                artists = splitArtists(it.artists),
                // También al releer: las filas guardadas por una version
                // anterior llevan el `http://` que devolvia el servidor.
                imageUrl = ServerUrl.enforceAppScheme(it.imageUrl),
                updatedAt = it.updatedAt,
            )
        }

    suspend fun invalidatePlaylistSongs(playlistId: Int) = songCacheDao.invalidate(playlistId)

    /**
     * Búsqueda por título o artista. El backend hace coincidencia parcial y
     * pagina ordenando por id descendente, así que las incorporaciones
     * recientes salen primero. Sin término devuelve la biblioteca entera.
     */
    suspend fun searchSongs(query: String, offset: Int): SongPage {
        val page = api.searchSongs(
            query = query.ifEmpty { null },
            offset = offset,
            limit = SEARCH_PAGE_SIZE,
        )
        return SongPage(page.songs.map { it.toDomain() }, page.hasMore)
    }

    /**
     * Lote del Modo Zen: una selección aleatoria de las canciones marcadas
     * como visibles. El backend ignora el offset en este modo.
     */
    suspend fun zenBatch(): List<Song> =
        api.searchSongs(query = null, offset = 0, limit = ZEN_BATCH_SIZE, random = true)
            .songs.map { it.toDomain() }

    suspend fun addSongToPlaylist(playlistId: Int, songId: Int): Boolean {
        val ok = api.addSongToPlaylist(playlistId, songId).isSuccessful
        if (ok) songCacheDao.invalidate(playlistId)
        return ok
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: Int): Boolean {
        val ok = api.removeSongFromPlaylist(playlistId, songId).isSuccessful
        if (ok) songCacheDao.invalidate(playlistId)
        return ok
    }

    /**
     * El backend guarda la sugerencia con el mismo formato "Artista - Título"
     * que usan los nombres de canción.
     */
    suspend fun createSuggestion(artist: String, songName: String): Boolean =
        api.createSuggestion(SuggestionRequest("$artist - $songName")).isSuccessful

    /**
     * Enlace firmado de la canción, con el esquema que impone la app: el
     * backend lo genera con `request.host_url` y tras el túnel devuelve
     * `http://`, que en release ExoPlayer rechaza por tráfico en claro.
     */
    suspend fun signedUrl(songId: Int): String? =
        ServerUrl.enforceAppScheme(api.getSignedUrl(songId).mp3Url)

    /**
     * Repone lo que se tenga guardado de una canción si el servidor la ha
     * tocado desde entonces: el título, los artistas y la portada. El MP3 no,
     * que ese no cambia nunca y por eso basta con mirar los metadatos.
     *
     * Lo llama el reproductor al empezar cada canción. No se hace al pedir el
     * enlace firmado, que sería el sitio evidente, porque lo descargado y lo
     * que ya está en la caché de audio no pasan por ahí: suenan sin gastar una
     * llamada a la API, y son justo las que más tiempo llevan guardadas.
     *
     * Sin conexión no pasa nada: la petición falla y se deja lo que hubiera.
     */
    suspend fun refreshSongIfStale(songId: Int) {
        // Sin nada guardado no hay nada que reponer: la canción llegará entera
        // la próxima vez que se pida su playlist.
        val storedAt = songCacheDao.updatedAtOf(songId) ?: return

        val fresh = runCatching { api.getSong(songId).toDomain() }.getOrNull() ?: return
        if (fresh.updatedAt <= storedAt) return

        val previousCovers = songCacheDao.imageUrlsOf(songId)
        songCacheDao.refreshSong(
            songId = songId,
            title = fresh.title,
            artists = joinArtists(fresh.artists),
            imageUrl = fresh.imageUrl,
            updatedAt = fresh.updatedAt,
        )
        forgetCoversNoLongerUsed(previousCovers)
    }

    suspend fun clearCache() = songCacheDao.clearAll()

    /** Al cerrar sesión: se conserva lo que sostiene las playlists descargadas. */
    suspend fun clearCacheExceptDownloads() = songCacheDao.clearExceptDownloaded()
}
