package com.zice.playbutton.data.repo

import android.content.Context
import android.net.Uri
import com.zice.playbutton.data.local.ImageCache
import com.zice.playbutton.data.local.SessionProvider
import com.zice.playbutton.data.remote.ApiService
import com.zice.playbutton.data.remote.ServerUrl
import com.zice.playbutton.data.remote.dto.PlaylistBodyRequest
import com.zice.playbutton.domain.Playlist
import com.zice.playbutton.domain.PlaylistSource
import com.zice.playbutton.domain.toDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Identifica un listado y, con él, su entrada de caché. */
data class ListKey(
    val source: PlaylistSource,
    val search: String = "",
    val userId: Int? = null,
)

data class CachedList(
    val items: List<Playlist>,
    val hasMore: Boolean,
    val nextOffset: Int,
    val loadedAt: Long,
)

/**
 * Listados de playlists con caché en memoria.
 *
 * La app React Native no cachea ningún listado: `PlaylistList` guarda el
 * resultado en el estado del componente y vuelve a pedirlo entero en cada
 * montaje, y encima el cliente HTTP añadía un parámetro `cb=<timestamp>` que
 * anulaba también la caché de red. Aquí la caché vive en el repositorio, por
 * encima del ciclo de vida de la pantalla, así que volver a una pestaña ya
 * visitada es instantáneo.
 *
 * La invalidación es explícita: al crear, editar o borrar una playlist se
 * marcan sucias las listas afectadas y se recargan la próxima vez que alguien
 * las pida.
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val api: ApiService,
    private val sessionProvider: SessionProvider,
    private val imageCache: ImageCache,
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        const val PAGE_SIZE = 20
        const val TTL_MILLIS = 5 * 60 * 1000L
    }

    private val cache = MutableStateFlow<Map<ListKey, CachedList>>(emptyMap())
    private val dirty = MutableStateFlow<Set<ListKey>>(emptySet())

    /**
     * Playlists conocidas sueltas, fuera de cualquier listado. Ahora mismo son
     * las recién creadas: se abre su detalle antes de que el listado propio se
     * haya recargado, y sin esto la pantalla no sabría ni su nombre ni de quién
     * es, así que aparecía sin título y sin los botones del propietario hasta
     * salir y volver a entrar.
     */
    private val known = MutableStateFlow<Map<Int, Playlist>>(emptyMap())

    /** Una carga por clave: evita que dos pantallas pidan la misma página a la vez. */
    private val loadMutexes = mutableMapOf<ListKey, Mutex>()
    private val mutexesGuard = Mutex()

    fun observe(key: ListKey): Flow<CachedList?> = cache.map { it[key] }

    fun cached(key: ListKey): CachedList? = cache.value[key]

    /**
     * Carga la primera página si hace falta. No hace nada si ya hay datos
     * frescos, que es lo que hace instantáneo volver a una pestaña.
     */
    suspend fun ensureLoaded(key: ListKey, force: Boolean = false) {
        val entry = cache.value[key]
        val stale = entry == null ||
            key in dirty.value ||
            System.currentTimeMillis() - entry.loadedAt > TTL_MILLIS
        if (!force && !stale) return

        withKeyLock(key) {
            // Otra corrutina puede haber cargado mientras esperábamos el cerrojo.
            val current = cache.value[key]
            val stillStale = force || current == null || key in dirty.value ||
                System.currentTimeMillis() - current.loadedAt > TTL_MILLIS
            if (!stillStale) return@withKeyLock

            val page = fetchPage(key, offset = 0)
            // Lo que llega fresco manda tambien sobre las sueltas: `known` no
            // caduca, asi que sin esto la cabecera del detalle seguiria
            // pintando la portada que la playlist tuviera al crearse.
            val fresh = page.items.associateBy { it.id }
            known.value = known.value.mapValues { (id, playlist) -> fresh[id] ?: playlist }
            cache.value += key to CachedList(
                items = page.items,
                hasMore = page.hasMore,
                nextOffset = page.items.size,
                loadedAt = System.currentTimeMillis(),
            )
            dirty.value -= key
        }
    }

    suspend fun loadMore(key: ListKey) {
        val entry = cache.value[key] ?: return
        if (!entry.hasMore) return

        withKeyLock(key) {
            val current = cache.value[key] ?: return@withKeyLock
            if (!current.hasMore) return@withKeyLock

            val page = fetchPage(key, offset = current.nextOffset)
            // El backend ordena por número de favoritos, que puede cambiar entre
            // páginas: descartamos los que ya tengamos para no duplicar filas.
            val knownIds = current.items.mapTo(HashSet()) { it.id }
            cache.value += key to current.copy(
                items = current.items + page.items.filter { it.id !in knownIds },
                hasMore = page.hasMore,
                nextOffset = current.nextOffset + page.items.size,
            )
        }
    }

    suspend fun refresh(key: ListKey) = ensureLoaded(key, force = true)

    /** Una página ya en dominio: los artistas llegan con su propio DTO. */
    private data class Page(val items: List<Playlist>, val hasMore: Boolean)

    private suspend fun fetchPage(key: ListKey, offset: Int): Page {
        val search = key.search
        if (key.source == PlaylistSource.Artists) {
            val page = api.getArtists(offset, PAGE_SIZE, search)
            return Page(page.artists.map { it.toDomain() }, page.hasMore)
        }

        val page = when (key.source) {
            PlaylistSource.Public -> api.getPublicPlaylists(offset, PAGE_SIZE, search)
            PlaylistSource.Mine, PlaylistSource.OtherUser -> {
                val userId = key.userId ?: requireUserId()
                api.getUserPlaylists(userId, offset, PAGE_SIZE, search)
            }
            PlaylistSource.Favorites -> {
                val userId = key.userId ?: requireUserId()
                api.getUserFavorites(userId, offset, PAGE_SIZE, search)
            }
            PlaylistSource.Artists -> error("Resuelto arriba")
        }
        return Page(page.playlists.map { it.toDomain() }, page.hasMore)
    }

    private suspend fun requireUserId(): Int =
        sessionProvider.currentUserId() ?: error("No hay sesión activa")

    private suspend fun withKeyLock(key: ListKey, block: suspend () -> Unit) {
        val mutex = mutexesGuard.withLock { loadMutexes.getOrPut(key) { Mutex() } }
        mutex.withLock { block() }
    }

    // --- Mutaciones ------------------------------------------------------

    suspend fun createPlaylist(name: String, isPublic: Boolean): Playlist {
        val userId = requireUserId()
        val created = api.createPlaylist(userId, PlaylistBodyRequest(name, isPublic)).toDomain()
        known.value += created.id to created
        invalidateOwned()
        return created
    }

    suspend fun updatePlaylist(playlistId: Int, name: String, isPublic: Boolean): Boolean {
        val ok = api.updatePlaylist(playlistId, PlaylistBodyRequest(name, isPublic)).isSuccessful
        if (ok) {
            // Renombrar altera el nombre en todas las listas donde aparezca.
            updateEverywhere(playlistId) { it.copy(name = name, isPublic = isPublic) }
            invalidateOwned()
        }
        return ok
    }

    /**
     * Sube la portada elegida en el selector de fotos.
     *
     * El backend valida por extension del nombre de fichero, y lo que da el
     * selector es un content:// sin nombre util, asi que se compone uno a
     * partir del tipo que declara el sistema. Un tipo que el servidor no acepta
     * se corta aqui, sin gastar la subida.
     */
    suspend fun setPlaylistImage(playlistId: Int, uri: Uri): Boolean {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: return false
        val extension = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> return false
        }

        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return false

        val part = MultipartBody.Part.createFormData(
            "image",
            "cover.$extension",
            bytes.toRequestBody(mime.toMediaType()),
        )

        val imageUrl = ServerUrl.enforceAppScheme(
            runCatching { api.setPlaylistImage(playlistId, part).imageUrl }.getOrNull(),
        ) ?: return false

        forgetPreviousCover(playlistId)
        updateEverywhere(playlistId) { it.copy(imageUrl = imageUrl) }
        invalidateOwned()
        return true
    }

    suspend fun clearPlaylistImage(playlistId: Int): Boolean {
        val ok = runCatching { api.deletePlaylistImage(playlistId).isSuccessful }
            .getOrDefault(false)
        if (ok) {
            forgetPreviousCover(playlistId)
            updateEverywhere(playlistId) { it.copy(imageUrl = null) }
            invalidateOwned()
        }
        return ok
    }

    suspend fun deletePlaylist(playlistId: Int): Boolean {
        val ok = api.deletePlaylist(playlistId).isSuccessful
        if (ok) {
            cache.value = cache.value.mapValues { (_, list) ->
                list.copy(items = list.items.filterNot { it.id == playlistId })
            }
            known.value -= playlistId
            invalidateOwned()
        }
        return ok
    }

    /**
     * Alterna el favorito y refleja el cambio en todas las listas cacheadas.
     * Las favoritas se marcan sucias en lugar de recargarse al vuelo: el
     * backend ordena por número de favoritos, así que recargar aquí haría
     * saltar la lista bajo el dedo del usuario.
     */
    suspend fun toggleFavorite(playlistId: Int): Int? {
        val previous = findCached(playlistId)
        val optimisticFavorite = !(previous?.isFavorite ?: false)
        updateEverywhere(playlistId) {
            it.copy(
                isFavorite = optimisticFavorite,
                favoritesCount = (it.favoritesCount + if (optimisticFavorite) 1 else -1)
                    .coerceAtLeast(0),
            )
        }

        return runCatching { api.toggleFavorite(playlistId).favoritesCount }
            .onSuccess { count ->
                updateEverywhere(playlistId) { it.copy(favoritesCount = count) }
                markDirty { it.source == PlaylistSource.Favorites }
            }
            .onFailure {
                // Revertimos el cambio optimista.
                updateEverywhere(playlistId) {
                    it.copy(
                        isFavorite = previous?.isFavorite ?: false,
                        favoritesCount = previous?.favoritesCount ?: it.favoritesCount,
                    )
                }
            }
            .getOrNull()
    }

    fun findCached(playlistId: Int): Playlist? =
        cache.value.values.firstNotNullOfOrNull { list ->
            list.items.firstOrNull { it.id == playlistId }
        } ?: known.value[playlistId]

    /**
     * Tira la portada que esta playlist tenía. Se llama con el cambio ya
     * confirmado por el servidor, que es cuando su fila ha dejado de existir
     * allí: cada imagen cuelga de un único dueño, así que soltarla es lo mismo
     * que darla por muerta, y guardarla solo serviría para volver a verla
     * dondequiera que siguiera apuntada.
     */
    private suspend fun forgetPreviousCover(playlistId: Int) {
        val previous = findCached(playlistId)?.imageUrl ?: return
        withContext(Dispatchers.IO) { imageCache.forget(listOf(previous)) }
    }

    private fun updateEverywhere(playlistId: Int, transform: (Playlist) -> Playlist) {
        cache.value = cache.value.mapValues { (_, list) ->
            if (list.items.none { it.id == playlistId }) {
                list
            } else {
                list.copy(items = list.items.map { if (it.id == playlistId) transform(it) else it })
            }
        }
        known.value[playlistId]?.let { known.value += playlistId to transform(it) }
    }

    private fun markDirty(predicate: (ListKey) -> Boolean) {
        dirty.value += cache.value.keys.filter(predicate)
    }

    /** Crear, renombrar o borrar afecta a "mis playlists" y a las públicas. */
    private fun invalidateOwned() = markDirty {
        it.source == PlaylistSource.Mine ||
            it.source == PlaylistSource.Public ||
            it.source == PlaylistSource.OtherUser
    }

    /** Al cerrar sesión no debe quedar rastro del usuario anterior. */
    fun clear() {
        cache.value = emptyMap()
        dirty.value = emptySet()
        known.value = emptyMap()
    }
}
