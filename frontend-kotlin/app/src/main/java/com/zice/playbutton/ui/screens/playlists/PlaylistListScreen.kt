package com.zice.playbutton.ui.screens.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zice.playbutton.R
import com.zice.playbutton.domain.DownloadedPlaylist
import com.zice.playbutton.domain.Playlist
import com.zice.playbutton.ui.components.EmptyMessage
import com.zice.playbutton.ui.components.ErrorMessage
import com.zice.playbutton.ui.components.LoadingBox
import com.zice.playbutton.ui.components.DownloadedPlaylistCard
import com.zice.playbutton.ui.components.PlaylistCard
import com.zice.playbutton.ui.components.SearchField
import com.zice.playbutton.util.text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Cuerpo compartido por los cuatro listados y por las playlists de otro
 * usuario. Solo cambian los textos y qué se hace al pulsar una fila.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    state: PlaylistListUiState,
    downloadedPlaylists: List<DownloadedPlaylist>,
    searchPlaceholder: String,
    emptyMessage: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDownloadedClick: (DownloadedPlaylist) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOwnerClick: ((Playlist) -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    // Paginación: se pide la siguiente página cuando quedan pocas filas por
    // delante, para que el usuario no llegue a ver el final de la lista.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }

    LaunchedEffect(listState, state.hasMore) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .filter { it && state.hasMore }
            .collect { onLoadMore() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            value = state.search,
            onValueChange = onSearchChange,
            onSearch = onSearchSubmit,
            placeholder = searchPlaceholder,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading -> LoadingBox()

                state.error != null && state.items.isEmpty() -> {
                    // Debajo del error, lo que sí se puede escuchar: quedarse
                    // mirando un «sin conexión» con la música ya en el móvil
                    // era el peor sitio donde dejar al usuario.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp + contentPadding.calculateBottomPadding(),
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            ErrorMessage(
                                text = state.error.text(),
                                isRetrying = state.isRefreshing,
                                // Configurar el servidor no es cosa de reintentar.
                                onRetry = if (state.error.isRetryable) onRefresh else null,
                            )
                        }

                        if (downloadedPlaylists.isNotEmpty()) {
                            item {
                                Column {
                                    Text(
                                        text = stringResource(R.string.downloaded_playlists),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }
                            }

                            items(downloadedPlaylists, key = { it.id }) { playlist ->
                                DownloadedPlaylistCard(
                                    playlist = playlist,
                                    onClick = { onDownloadedClick(playlist) },
                                )
                            }
                        }
                    }
                }

                state.items.isEmpty() -> {
                    EmptyMessage(emptyMessage, Modifier.align(Alignment.TopCenter))
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp + contentPadding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            onToggleFavorite = { onToggleFavorite(playlist.id) },
                            onOwnerClick = onOwnerClick?.let { handler -> { handler(playlist) } },
                        )
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.inversePrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
