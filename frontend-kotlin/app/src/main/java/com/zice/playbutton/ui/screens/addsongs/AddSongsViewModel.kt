package com.zice.playbutton.ui.screens.addsongs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.zice.playbutton.data.local.SettingsStore
import com.zice.playbutton.data.repo.SongRepository
import com.zice.playbutton.domain.Song
import com.zice.playbutton.ui.nav.AddSongsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddSongsUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val playlistSongIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val addingSongId: Int? = null,
    val lastSearched: String = "",
    val error: String? = null,
    val showAddedDialog: Boolean = false,
) {
    /** Las canciones que ya están en la playlist no se ofrecen otra vez. */
    val visibleResults: List<Song>
        get() = results.filterNot { it.id in playlistSongIds }
}

@HiltViewModel
class AddSongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsStore: SettingsStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        /**
         * Consulta con la que se abre la pantalla. Un espacio casa con
         * cualquier nombre en formato "Artista - Título", así que el listado
         * llega ya lleno con lo más reciente del catálogo en lugar de con un
         * cartel de "empieza a buscar".
         */
        const val INITIAL_QUERY = " "
    }

    val playlistId: Int = savedStateHandle.toRoute<AddSongsRoute>().playlistId

    private val _uiState = MutableStateFlow(AddSongsUiState())
    val uiState = _uiState.asStateFlow()

    private var hideAddedDialog = false

    init {
        viewModelScope.launch {
            // Para poder descartar de los resultados lo que ya está añadido.
            runCatching { songRepository.playlistSongs(playlistId) }
                .onSuccess { songs ->
                    _uiState.value = _uiState.value.copy(
                        playlistSongIds = songs.mapTo(HashSet()) { it.id },
                    )
                }
        }
        viewModelScope.launch {
            settingsStore.hideAddSongAlert.collect { hideAddedDialog = it }
        }
        search(INITIAL_QUERY)
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun onSearchSubmit() {
        search(_uiState.value.query)
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { songRepository.searchSongs(query, offset = 0) }
                .onSuccess { page ->
                    _uiState.value = _uiState.value.copy(
                        results = page.songs,
                        hasMore = page.hasMore,
                        isLoading = false,
                        lastSearched = query,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                        lastSearched = query,
                    )
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            runCatching { songRepository.searchSongs(state.lastSearched, state.results.size) }
                .onSuccess { page ->
                    val known = state.results.mapTo(HashSet()) { it.id }
                    _uiState.value = _uiState.value.copy(
                        results = state.results + page.songs.filter { it.id !in known },
                        hasMore = page.hasMore,
                        isLoadingMore = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
        }
    }

    fun addSong(song: Song) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(addingSongId = song.id)
            val ok = songRepository.addSongToPlaylist(playlistId, song.id)
            _uiState.value = _uiState.value.copy(
                addingSongId = null,
                playlistSongIds = if (ok) {
                    _uiState.value.playlistSongIds + song.id
                } else {
                    _uiState.value.playlistSongIds
                },
                showAddedDialog = ok && !hideAddedDialog,
            )
        }
    }

    fun dismissAddedDialog() {
        _uiState.value = _uiState.value.copy(showAddedDialog = false)
    }

    fun dontShowAddedDialogAgain() {
        viewModelScope.launch {
            settingsStore.setHideAddSongAlert(true)
            _uiState.value = _uiState.value.copy(showAddedDialog = false)
        }
    }
}
