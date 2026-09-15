package com.zice.playbutton.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zice.playbutton.R
import com.zice.playbutton.domain.Song
import com.zice.playbutton.player.PlaylistDownloader
import com.zice.playbutton.ui.components.ConfirmDialog
import com.zice.playbutton.ui.components.EmptyMessage
import com.zice.playbutton.ui.components.ErrorMessage
import com.zice.playbutton.ui.components.LoadingBox
import com.zice.playbutton.ui.components.SearchField
import com.zice.playbutton.util.text

/** Tamaño del icono dentro del botón de reproducir y hueco hasta el texto. */
private val PlayIconSize = 24.dp
private val PlayIconGap = 6.dp

/**
 * Relleno de los botones de solo icono de la fila de acciones. El de serie
 * reserva 24dp por lado, pensado para botones con texto, y con tres iconos al
 * lado del de reproducir no le quedaba ancho al que de verdad importa.
 */
private val IconButtonPadding = PaddingValues(horizontal = 12.dp)

@Composable
fun PlaylistDetailScreen(
    onNavigateToAddSongs: (Int) -> Unit,
    onNavigateToUser: (Int, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Se relanza al volver de "añadir canciones": el destino sale de la
    // composición al navegar hacia delante, así que al regresar esto revalida
    // la lista. Si la caché sigue fresca no llega a tocar la red.
    LaunchedEffect(Unit) { viewModel.load() }

    var menuVisible by remember { mutableStateOf(false) }
    var deleteConfirmVisible by remember { mutableStateOf(false) }
    var songToRemove by remember { mutableStateOf<Song?>(null) }
    var removeDownloadVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Cabecera
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.playlist?.name.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // Autor y tamaño en la misma línea; mientras cargan las
                // canciones no se anuncia un cero que no es cierto.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val playlist = state.playlist
                    if (playlist != null &&
                        !playlist.isArtist &&
                        playlist.ownerName.isNotBlank()
                    ) {
                        Text(
                            text = playlist.ownerName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                // El usuario 1 es el sistema y no tiene perfil que visitar.
                                .clickable(enabled = playlist.ownerId > 1 && !state.isOwner) {
                                    onNavigateToUser(playlist.ownerId, playlist.ownerName)
                                }
                                .padding(vertical = 2.dp)
                                .weight(1f, fill = false),
                        )
                        if (!state.isLoading) {
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!state.isLoading) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.playlist_songs_count,
                                state.songs.size,
                                state.songs.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Los favoritos solo se enseñan cuando vienen del servidor: si la
            // playlist se ha reconstruido del registro de descargas no se
            // sabe cuántos tiene ni si es tuya, y un corazón vacío junto a un
            // cero se leería como respuesta cuando es falta de conexión.
            state.playlist?.takeUnless { state.fromDownloads }?.let { playlist ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playlist.favoritesCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (playlist.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (playlist.isFavorite) {
                                    R.string.favorite_remove
                                } else {
                                    R.string.favorite_add
                                },
                            ),
                            tint = if (playlist.isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayButton(
                enabled = state.songs.isNotEmpty(),
                onClick = viewModel::play,
            )

            // Descargar no es cosa del dueño: le sirve a cualquiera que vaya a
            // escuchar la playlist a menudo.
            if (state.songs.isNotEmpty()) {
                SaveToStorageButton(
                    storage = state.storage,
                    total = state.songs.size,
                    onSave = viewModel::saveToStorage,
                    onCancel = viewModel::cancelSaveToStorage,
                    onRemove = { removeDownloadVisible = true },
                )
            }

            if (state.isOwner) {
                OutlinedButton(
                    onClick = { onNavigateToAddSongs(viewModel.playlistId) },
                    contentPadding = IconButtonPadding,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.playlist_add_songs))
                }
                Box {
                    OutlinedButton(
                        onClick = { menuVisible = true },
                        contentPadding = IconButtonPadding,
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.playlist_options),
                        )
                    }
                    DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playlist_edit)) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                menuVisible = false
                                viewModel.openEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.playlist_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuVisible = false
                                deleteConfirmVisible = true
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.songs.size > 8) {
            SearchField(
                value = state.filter,
                onValueChange = viewModel::onFilterChange,
                onSearch = {},
                placeholder = stringResource(R.string.filter_songs),
            )
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.isLoading -> LoadingBox()
            state.error != null && state.songs.isEmpty() -> {
                val error = state.error!!
                ErrorMessage(
                    text = error.text(),
                    isRetrying = state.isRetrying,
                    // Configurar el servidor no es cosa de reintentar.
                    onRetry = if (error.isRetryable) viewModel::retry else null,
                )
            }
            state.visibleSongs.isEmpty() -> EmptyMessage(stringResource(R.string.empty_songs))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    bottom = 16.dp + contentPadding.calculateBottomPadding(),
                ),
            ) {
                items(state.visibleSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        canRemove = state.isOwner,
                        onClick = { viewModel.playFrom(song) },
                        onRemove = { songToRemove = song },
                    )
                }
            }
        }
    }

    if (removeDownloadVisible) {
        ConfirmDialog(
            title = stringResource(R.string.playlist_download_remove),
            message = stringResource(R.string.playlist_download_remove_confirm),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                removeDownloadVisible = false
                viewModel.removeDownload()
            },
            onDismiss = { removeDownloadVisible = false },
        )
    }

    if (state.editVisible) {
        AlertDialog(
            onDismissRequest = viewModel::closeEdit,
            title = { Text(stringResource(R.string.playlist_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.editName,
                        onValueChange = viewModel::onEditNameChange,
                        label = { Text(stringResource(R.string.playlist_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.playlist_public_switch))
                        Switch(
                            checked = state.editIsPublic,
                            onCheckedChange = viewModel::onEditPublicChange,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::saveEdit,
                    enabled = !state.isSaving && state.editName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeEdit) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }

    if (deleteConfirmVisible) {
        ConfirmDialog(
            title = stringResource(R.string.playlist_delete),
            message = stringResource(
                R.string.playlist_delete_confirm,
                state.playlist?.name.orEmpty(),
            ),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                deleteConfirmVisible = false
                viewModel.delete(onBack)
            },
            onDismiss = { deleteConfirmVisible = false },
        )
    }

    songToRemove?.let { song ->
        ConfirmDialog(
            title = stringResource(R.string.song_remove),
            message = stringResource(R.string.song_remove_confirm, song.fullName),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.removeSong(song)
                songToRemove = null
            },
            onDismiss = { songToRemove = null },
        )
    }
}

/**
 * Reproducir la playlist. La palabra solo aparece si cabe entera junto al
 * icono: en una pantalla estrecha, con la fuente del sistema agrandada, en un
 * idioma de palabras largas o simplemente cuando los otros botones de la fila
 * se llevan lo suyo, es mejor quedarse con el icono que partir el texto en dos
 * líneas o recortarlo con puntos suspensivos.
 *
 * Se decide midiendo el texto contra el ancho que la fila le ha dado al botón,
 * no por un umbral de pantalla: los botones que lo acompañan cambian según
 * quién sea el dueño y si hay almacenamiento activo.
 */
@Composable
private fun RowScope.PlayButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.playlist_play)
    val labelStyle = MaterialTheme.typography.labelLarge
    val measurer = rememberTextMeasurer()

    val padding = ButtonDefaults.ContentPadding
    val layoutDirection = LocalLayoutDirection.current
    val fixedWidth = with(LocalDensity.current) {
        (
            padding.calculateStartPadding(layoutDirection) +
                padding.calculateEndPadding(layoutDirection) +
                PlayIconSize + PlayIconGap
            ).roundToPx()
    }

    BoxWithConstraints(Modifier.weight(1f)) {
        val labelWidth = measurer
            .measure(text = label, style = labelStyle, softWrap = false, maxLines = 1)
            .size.width
        val labelFits = fixedWidth + labelWidth <= constraints.maxWidth

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = label.takeIf { !labelFits },
                modifier = Modifier.size(PlayIconSize),
            )
            if (labelFits) {
                Spacer(Modifier.size(PlayIconGap))
                Text(text = label, maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * Descargar la playlist en el dispositivo. El mismo botón lleva los tres
 * estados: descargar, contar el progreso y cancelar mientras baja, y —cuando
 * ya está descargada— borrar la descarga. Así se ve de un golpe si esta
 * playlist gasta datos o no, y se puede recuperar el espacio desde el mismo
 * sitio.
 *
 * Descargada quiere decir pedida por el usuario: si sus canciones ya estaban
 * en el dispositivo por otras listas, el botón sigue ofreciendo descargarla
 * —será cosa de un momento— para que quede claro que aún no es suya.
 */
@Composable
private fun SaveToStorageButton(
    storage: PlaylistStorageState,
    total: Int,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
) {
    val downloaded = storage.isDownloaded(total)

    OutlinedButton(
        onClick = when {
            storage.isSaving -> onCancel
            downloaded -> onRemove
            else -> onSave
        },
        contentPadding = IconButtonPadding,
    ) {
        when {
            storage.isSaving -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.inversePrimary,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        R.string.playlist_save_progress,
                        storage.progress?.done ?: 0,
                        total,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            downloaded -> Icon(
                Icons.Filled.DownloadDone,
                contentDescription = stringResource(R.string.playlist_download_remove),
                tint = MaterialTheme.colorScheme.inversePrimary,
            )

            else -> Icon(
                Icons.Filled.Download,
                contentDescription = stringResource(R.string.playlist_save),
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    canRemove: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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

        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.song_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
