package com.zice.playbutton.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.zice.playbutton.data.local.AudioDownloads
import com.zice.playbutton.data.remote.ServerReachability
import com.zice.playbutton.data.repo.AuthRepository
import com.zice.playbutton.data.repo.OfflineLibrary
import com.zice.playbutton.data.repo.PlaylistRepository
import com.zice.playbutton.data.repo.SongRepository
import com.zice.playbutton.domain.DownloadedPlaylist
import com.zice.playbutton.domain.Playlist
import com.zice.playbutton.domain.Song
import com.zice.playbutton.player.PlayerConnection
import com.zice.playbutton.player.PlayerVisibility
import com.zice.playbutton.player.PlaylistDownloader
import com.zice.playbutton.ui.nav.PlaylistDetailRoute
import com.zice.playbutton.util.UiError
import com.zice.playbutton.util.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Lo que hay descargado de esta playlist y en qué punto va la descarga, si
 * está en marcha. Cuenta solo lo descargado a mano: lo que esté de paso en la
 * caché no vale, porque puede desaparecer en cualquier momento.
 */
data class PlaylistStorageState(
    val savedSongs: Int = 0,
    /** Si el usuario ha pedido esta playlist, no otra que comparta canciones. */
    val isRegistered: Boolean = false,
    val progress: PlaylistDownloader.Progress? = null,
) {
    val isSaving: Boolean get() = progress != null

    /** Todo su audio está en el dispositivo, venga de donde venga. */
    fun isComplete(total: Int): Boolean = total > 0 && savedSongs >= total

    /**
     * La playlist está descargada de verdad: el usuario la pidió y su audio
     * sigue entero. Que esté completa sin haberla pedido no cuenta —serán
     * canciones bajadas con otras listas—, porque entonces la descarga no es
     * suya y se iría al borrar aquellas.
     */
    fun isDownloaded(total: Int): Boolean = isRegistered && isComplete(total)
}

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isRetrying: Boolean = false,
    val isOwner: Boolean = false,
    /**
     * Los datos de la playlist salen del registro de descargas, no del
     * servidor: hay nombre y autor, pero nada de favoritos.
     */
    val fromDownloads: Boolean = false,
    val filter: String = "",
    val error: UiError? = null,
    val editVisible: Boolean = false,
    val editName: String = "",
    val editIsPublic: Boolean = true,
    val isSaving: Boolean = false,
    val storage: PlaylistStorageState = PlaylistStorageState(),
) {
    /**
     * El filtro es local: la lista de canciones ya está entera en memoria.
     * Mira en el título y en los artistas, igual que la búsqueda del servidor;
     * antes bastaba con una comparación porque ambos iban en la misma cadena.
     */
    val visibleSongs: List<Song>
        get() = if (filter.isBlank()) {
            songs
        } else {
            val term = filter.trim()
            songs.filter { song ->
                song.title.contains(term, ignoreCase = true) ||
                    song.artists.any { it.contains(term, ignoreCase = true) }
            }
        }
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val authRepository: AuthRepository,
    private val playerConnection: PlayerConnection,
    private val playerVisibility: PlayerVisibility,
    private val playlistDownloader: PlaylistDownloader,
    private val audioDownloads: AudioDownloads,
    private val offlineLibrary: OfflineLibrary,
    private val serverReachability: ServerReachability,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val playlistId: Int = savedStateHandle.toRoute<PlaylistDetailRoute>().playlistId

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // La descarga vive fuera de la pantalla: al entrar hay que recoger en
        // qué punto va, que puede haber arrancado antes y seguir corriendo.
        viewModelScope.launch {
            playlistDownloader.progress.collect { progress ->
                if (progress == null || progress.playlistId == playlistId) {
                    updateStorage { it.copy(progress = progress) }
                    if (progress == null) refreshSavedCount()
                }
            }
        }
    }

    /**
     * Carga inicial y revalidación al volver a la pantalla —la llama la propia
     * pantalla, no el `init`, porque al volver de "añadir canciones" el
     * ViewModel sigue vivo y solo se recompone el contenido—.
     *
     * El indicador de carga solo aparece cuando no hay nada que enseñar: si ya
     * hay canciones en pantalla se quedan mientras se revalida, y como añadir
     * una canción invalida la caché, la vuelta trae la lista actualizada sin
     * que la pantalla parpadee.
     */
    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.songs.isEmpty(),
                error = null,
            )
            fetch(forceRefresh)
        }
    }

    /**
     * Reintento tras un fallo, a mano o automático. No vuelve al indicador de
     * carga ni borra el error: el mensaje se queda quieto en pantalla hasta que
     * la petición sale bien, así reintentar cada pocos segundos no hace
     * parpadear nada.
     */
    fun retry() {
        if (_uiState.value.isRetrying) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRetrying = true)
            fetch(forceRefresh = true)
        }
    }

    private suspend fun fetch(forceRefresh: Boolean) {
        // Los metadatos vienen del listado del que se llegó; el backend no
        // expone un endpoint para una playlist suelta. Sin conexión no hay
        // listado del que venir —se cachea en memoria y se fue con el proceso—,
        // así que se tira del registro de descargadas, que es lo único que
        // sabe cómo se llama esto cuando no hay a quién preguntar.
        val cached = playlistRepository.findCached(playlistId)
        val playlist = cached ?: offlineLibrary.find(playlistId)?.toPlaylist()
        val fromDownloads = cached == null && playlist != null
        val isOwner = playlist != null &&
            (playlist.ownerId == authRepository.currentUserId() || authRepository.isAdmin())

        runCatching { songRepository.playlistSongs(playlistId, forceRefresh) }
            .onSuccess { songs ->
                _uiState.value = _uiState.value.copy(
                    playlist = playlist,
                    songs = songs,
                    isOwner = isOwner,
                    fromDownloads = fromDownloads,
                    isLoading = false,
                    isRetrying = false,
                    error = null,
                )
                refreshSavedCount()
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    playlist = playlist,
                    isOwner = isOwner,
                    fromDownloads = fromDownloads,
                    isLoading = false,
                    isRetrying = false,
                    error = error.toUiError(),
                )
            }
    }

    fun onFilterChange(value: String) {
        _uiState.value = _uiState.value.copy(filter = value)
    }

    /**
     * Reproduce la playlist entera, barajada, y abre el reproductor grande:
     * igual que el Modo Zen, poner algo a sonar es pedir verlo.
     */
    fun play() {
        val state = _uiState.value
        if (state.songs.isEmpty()) return
        playerConnection.playPlaylist(
            playlistId = playlistId,
            playlistName = state.playlist?.name.orEmpty(),
            songs = state.songs,
        )
        playerVisibility.expand()
    }

    /** Al pulsar una canción, suena esa y el resto de la playlist va detrás. */
    fun playFrom(song: Song) {
        val state = _uiState.value
        if (state.songs.isEmpty()) return
        playerConnection.playPlaylistFrom(
            playlistId = playlistId,
            playlistName = state.playlist?.name.orEmpty(),
            songs = state.songs,
            startSong = song,
        )
        playerVisibility.expand()
    }

    // --- Guardar en el dispositivo ---------------------------------------

    /**
     * Descarga la playlist entera para escucharla sin datos y sin que caduque.
     * El trabajo lo lleva [PlaylistDownloader], que no depende de esta
     * pantalla: se puede salir y sigue.
     */
    fun saveToStorage() {
        val state = _uiState.value
        val playlist = state.playlist ?: return
        if (state.songs.isEmpty()) return
        playlistDownloader.start(playlist, state.songs)
    }

    /** Al cancelar se conserva lo ya descargado: no tiene sentido tirarlo. */
    fun cancelSaveToStorage() {
        playlistDownloader.cancel()
    }

    /** Borra la descarga de esta playlist y la saca de la lista sin conexión. */
    fun removeDownload() {
        viewModelScope.launch {
            offlineLibrary.removeDownload(playlistId)
            refreshSavedCount()
        }
    }

    /**
     * Cuántas de las canciones de la playlist están descargadas, y de paso el
     * registro de lo que se puede escuchar sin conexión: este es el único
     * sitio donde se sabe a la vez cuántas canciones tiene la playlist y
     * cuántas hay en el dispositivo.
     *
     * Solo pone al día lo que ya estaba descargado, y con el servidor al
     * alcance: sin él la lista de canciones puede venir de una caché vieja, y
     * darla por completa a partir de eso dejaría en la lista playlists a las
     * que les falta audio.
     */
    private fun refreshSavedCount() {
        val state = _uiState.value
        val songIds = state.songs.map(Song::id)
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { audioDownloads.count(songIds) }

            val playlist = state.playlist
            if (playlist != null && serverReachability.isReachable.value) {
                offlineLibrary.refresh(playlist, songIds.size, saved)
            }
            val registered = offlineLibrary.isDownloaded(playlistId)
            updateStorage { it.copy(savedSongs = saved, isRegistered = registered) }
        }
    }

    private fun updateStorage(transform: (PlaylistStorageState) -> PlaylistStorageState) {
        _uiState.value = _uiState.value.copy(storage = transform(_uiState.value.storage))
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(playlistId)
            _uiState.value = _uiState.value.copy(
                playlist = playlistRepository.findCached(playlistId) ?: _uiState.value.playlist,
            )
        }
    }

    fun removeSong(song: Song) {
        viewModelScope.launch {
            if (songRepository.removeSongFromPlaylist(playlistId, song.id)) {
                _uiState.value = _uiState.value.copy(
                    songs = _uiState.value.songs.filterNot { it.id == song.id },
                )
            }
        }
    }

    // --- Edición ---------------------------------------------------------

    fun openEdit() {
        val playlist = _uiState.value.playlist ?: return
        _uiState.value = _uiState.value.copy(
            editVisible = true,
            editName = playlist.name,
            editIsPublic = playlist.isPublic,
        )
    }

    fun closeEdit() {
        _uiState.value = _uiState.value.copy(editVisible = false)
    }

    fun onEditNameChange(value: String) {
        _uiState.value = _uiState.value.copy(editName = value)
    }

    fun onEditPublicChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(editIsPublic = value)
    }

    fun saveEdit() {
        val state = _uiState.value
        val name = state.editName.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            val ok = playlistRepository.updatePlaylist(playlistId, name, state.editIsPublic)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                editVisible = !ok,
                playlist = playlistRepository.findCached(playlistId) ?: _uiState.value.playlist,
            )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (playlistRepository.deletePlaylist(playlistId)) {
                songRepository.invalidatePlaylistSongs(playlistId)
                offlineLibrary.forget(playlistId)
                onDeleted()
            }
        }
    }
}

/**
 * Lo que se puede reconstruir de una playlist con solo el registro de
 * descargadas. El autor queda sin id porque no se guarda: así la pantalla no
 * ofrece visitar un perfil que sin servidor no se puede abrir, y tampoco toma
 * a nadie por propietario.
 *
 * De los favoritos no se sabe nada, y por eso van a cero: la pantalla los
 * esconde en vez de enseñar un «0 me gusta» y un corazón vacío que no son la
 * realidad, solo lo que no se ha podido preguntar.
 */
private fun DownloadedPlaylist.toPlaylist() = Playlist(
    id = id,
    name = name,
    ownerName = ownerName,
    ownerId = 0,
    isPublic = true,
    isArtist = isArtist,
    favoritesCount = 0,
    isFavorite = false,
)
