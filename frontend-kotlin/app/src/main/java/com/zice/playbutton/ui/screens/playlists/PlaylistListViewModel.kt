package com.zice.playbutton.ui.screens.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zice.playbutton.data.repo.ListKey
import com.zice.playbutton.data.repo.OfflineLibrary
import com.zice.playbutton.data.repo.PlaylistRepository
import com.zice.playbutton.domain.DownloadedPlaylist
import com.zice.playbutton.domain.Playlist
import com.zice.playbutton.domain.PlaylistSource
import androidx.navigation.toRoute
import com.zice.playbutton.ui.nav.UserPlaylistsRoute
import com.zice.playbutton.util.UiError
import com.zice.playbutton.util.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistListUiState(
    val items: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val search: String = "",
    val error: UiError? = null,
)

/**
 * Base de los cuatro listados. Los datos viven en [PlaylistRepository], que
 * los cachea en memoria: al volver a una pestaña ya visitada la lista aparece
 * de inmediato y solo se va a la red si caducó o si algo la invalidó.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BasePlaylistListViewModel(
    protected val repository: PlaylistRepository,
    offlineLibrary: OfflineLibrary,
    private val source: PlaylistSource,
    private val userId: Int? = null,
) : ViewModel() {

    /**
     * Lo que se puede escuchar sin servidor. La pantalla solo lo enseña cuando
     * el listado no ha podido cargar: es entonces cuando el usuario se queda
     * mirando un error sin nada que hacer, aunque tenga música en el móvil.
     */
    val downloadedPlaylists = offlineLibrary.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<DownloadedPlaylist>())

    /** Texto ya confirmado por el usuario; forma parte de la clave de caché. */
    private val activeSearch = MutableStateFlow("")

    /** Texto que se está escribiendo, aún sin buscar. */
    private val searchDraft = MutableStateFlow("")

    private val loadingMore = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<UiError?>(null)

    private fun keyFor(search: String) = ListKey(source, search, userId)

    val uiState = combine(
        activeSearch.flatMapLatest { repository.observe(keyFor(it)) },
        searchDraft,
        loadingMore,
        refreshing,
        error,
    ) { cached, draft, isLoadingMore, isRefreshing, errorMessage ->
        PlaylistListUiState(
            items = cached?.items.orEmpty(),
            isLoading = cached == null && errorMessage == null,
            isRefreshing = isRefreshing,
            isLoadingMore = isLoadingMore,
            hasMore = cached?.hasMore ?: false,
            search = draft,
            error = errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistListUiState())

    init {
        load()
    }

    /** Se llama también al volver a la pantalla: si hay caché fresca, no hace nada. */
    fun load() {
        viewModelScope.launch {
            error.value = null
            runCatching { repository.ensureLoaded(keyFor(activeSearch.value)) }
                .onFailure { error.value = it.toUiError() }
        }
    }

    fun onSearchChange(text: String) {
        searchDraft.value = text
    }

    fun onSearchSubmit() {
        activeSearch.value = searchDraft.value.trim()
        load()
    }

    /**
     * Recarga por gesto, por botón o por el reintento automático del mensaje de
     * error. El error no se borra al empezar, solo al conseguirlo: si se
     * limpiara antes, cada reintento haría parpadear la pantalla entre el
     * indicador de carga y el mensaje.
     */
    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            runCatching { repository.refresh(keyFor(activeSearch.value)) }
                .onSuccess { error.value = null }
                .onFailure { error.value = it.toUiError() }
            refreshing.value = false
        }
    }

    fun loadMore() {
        if (loadingMore.value) return
        viewModelScope.launch {
            loadingMore.value = true
            runCatching { repository.loadMore(keyFor(activeSearch.value)) }
                .onFailure { error.value = it.toUiError() }
            loadingMore.value = false
        }
    }

    fun toggleFavorite(playlistId: Int) {
        viewModelScope.launch { repository.toggleFavorite(playlistId) }
    }
}

@HiltViewModel
class PublicPlaylistsViewModel @Inject constructor(
    repository: PlaylistRepository,
    offlineLibrary: OfflineLibrary,
) : BasePlaylistListViewModel(repository, offlineLibrary, PlaylistSource.Public)

@HiltViewModel
class MyPlaylistsViewModel @Inject constructor(
    repository: PlaylistRepository,
    offlineLibrary: OfflineLibrary,
) : BasePlaylistListViewModel(repository, offlineLibrary, PlaylistSource.Mine)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    repository: PlaylistRepository,
    offlineLibrary: OfflineLibrary,
) : BasePlaylistListViewModel(repository, offlineLibrary, PlaylistSource.Favorites)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    repository: PlaylistRepository,
    offlineLibrary: OfflineLibrary,
) : BasePlaylistListViewModel(repository, offlineLibrary, PlaylistSource.Artists)

/** Playlists públicas de otro usuario, abiertas desde el autor de una playlist. */
@HiltViewModel
class UserPlaylistsViewModel @Inject constructor(
    repository: PlaylistRepository,
    offlineLibrary: OfflineLibrary,
    savedStateHandle: SavedStateHandle,
) : BasePlaylistListViewModel(
    repository = repository,
    offlineLibrary = offlineLibrary,
    source = PlaylistSource.OtherUser,
    userId = savedStateHandle.toRoute<UserPlaylistsRoute>().userId,
)
