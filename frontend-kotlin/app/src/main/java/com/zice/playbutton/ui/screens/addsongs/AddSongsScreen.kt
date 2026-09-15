package com.zice.playbutton.ui.screens.addsongs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zice.playbutton.R
import com.zice.playbutton.ui.components.EmptyMessage
import com.zice.playbutton.ui.components.LoadingBox
import com.zice.playbutton.ui.components.SearchField
import com.zice.playbutton.ui.theme.Brand400
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun AddSongsScreen(
    onNavigateToSuggestions: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AddSongsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

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
            .collect { viewModel.loadMore() }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearchSubmit,
            placeholder = stringResource(R.string.search_song),
        )

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> LoadingBox()

            state.visibleResults.isEmpty() -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyMessage(
                        stringResource(R.string.no_results_for, state.query.trim()),
                    )
                    TextButton(onClick = onNavigateToSuggestions) {
                        Text(stringResource(R.string.create_suggestion))
                    }
                    if (state.results.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.already_in_playlist),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    bottom = 16.dp + contentPadding.calculateBottomPadding(),
                ),
            ) {
                items(state.visibleResults, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.shapes.small,
                            )
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            song.artist?.let { artist ->
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (state.addingSongId == song.id) {
                            Box(Modifier.size(48.dp), Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Brand400,
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.addSong(song) }) {
                                Icon(
                                    Icons.Filled.AddCircle,
                                    contentDescription = stringResource(R.string.add_song),
                                    tint = Brand400,
                                )
                            }
                        }
                    }
                }

                if (state.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator(color = Brand400)
                        }
                    }
                }
            }
        }
    }

    if (state.showAddedDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAddedDialog,
            title = { Text(stringResource(R.string.song_added)) },
            text = { Text(stringResource(R.string.song_added_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAddedDialog) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dontShowAddedDialogAgain) {
                    Text(
                        stringResource(R.string.dont_show_again),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}
