package com.zice.playbutton.ui.screens.settings

import android.content.Intent
import android.text.format.Formatter
import androidx.core.net.toUri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zice.playbutton.R
import com.zice.playbutton.data.local.AudioCacheSize
import com.zice.playbutton.data.repo.DownloadsUsage
import com.zice.playbutton.data.local.AudioUsage
import com.zice.playbutton.ui.components.ConfirmDialog
import com.zice.playbutton.ui.components.PasswordField

/** Repositorio publico del proyecto. */
private const val PLAYBUTTON_REPOSITORY = "https://github.com/DanielPerezL/PlayButton"

@Composable
fun SettingsScreen(
    onNavigateToSuggestions: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val serverDisplay by viewModel.serverDisplay.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val cacheSize by viewModel.audioCacheSize.collectAsStateWithLifecycle()
    val cacheUsage by viewModel.audioCacheUsage.collectAsStateWithLifecycle()
    val coverBytes by viewModel.imageCacheBytes.collectAsStateWithLifecycle()
    val downloadsUsage by viewModel.downloadsUsage.collectAsStateWithLifecycle()

    var logoutConfirmVisible by remember { mutableStateOf(false) }
    var deleteConfirmVisible by remember { mutableStateOf(false) }
    var cacheDialogVisible by remember { mutableStateOf(false) }
    var clearDownloadsVisible by remember { mutableStateOf(false) }

    // Entre una visita y otra a los ajustes se ha estado escuchando música, y
    // lo guardado habrá cambiado.
    LaunchedEffect(Unit) { viewModel.refreshCacheUsage() }

    val deleteFailure = stringResource(R.string.settings_delete_account_failed)
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsRow(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.settings_server),
            subtitle = serverDisplay.ifBlank { stringResource(R.string.login_configure_server) },
            onClick = viewModel::openServerDialog,
        )

        SettingsRow(
            icon = Icons.Filled.GraphicEq,
            title = stringResource(R.string.settings_crossfade),
            subtitle = stringResource(R.string.settings_crossfade_summary),
            onClick = { viewModel.setCrossfadeEnabled(!crossfadeEnabled) },
            trailing = {
                Switch(
                    checked = crossfadeEnabled,
                    onCheckedChange = viewModel::setCrossfadeEnabled,
                )
            },
        )

        SettingsRow(
            icon = Icons.Filled.DownloadForOffline,
            title = stringResource(R.string.settings_cache),
            subtitle = cacheSummary(cacheSize, cacheUsage),
            onClick = { cacheDialogVisible = true },
        )

        // El almacenamiento es lo contrario de la caché: no lo llena la app
        // sola ni se vacía sola, así que aquí solo se enseña lo que ocupa y se
        // ofrece recuperarlo.
        SettingsRow(
            icon = Icons.Filled.SdStorage,
            title = stringResource(R.string.settings_downloads),
            subtitle = downloadsSummary(downloadsUsage),
            onClick = { clearDownloadsVisible = true }
                .takeIf { downloadsUsage.playlists > 0 || downloadsUsage.bytes > 0 },
        )

        SettingsRow(
            icon = Icons.Filled.Lightbulb,
            title = stringResource(R.string.settings_suggestions),
            onClick = onNavigateToSuggestions,
        )

        SettingsRow(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.change_password),
            onClick = viewModel::openPasswordDialog,
        )

        SettingsRow(
            icon = Icons.Filled.Code,
            title = stringResource(R.string.settings_github),
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, PLAYBUTTON_REPOSITORY.toUri()),
                )
            },
        )

        SettingsRow(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.settings_version),
            subtitle = state.version,
            onClick = null,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_danger_zone),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )

        SettingsRow(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = stringResource(R.string.settings_logout),
            onClick = { logoutConfirmVisible = true },
        )

        SettingsRow(
            icon = Icons.Filled.Delete,
            title = stringResource(R.string.settings_delete_account),
            tint = MaterialTheme.colorScheme.error,
            onClick = { deleteConfirmVisible = true },
        )

        Spacer(Modifier.height(16.dp))
    }

    if (logoutConfirmVisible) {
        ConfirmDialog(
            title = stringResource(R.string.settings_logout),
            message = stringResource(R.string.settings_logout_confirm),
            destructive = true,
            onConfirm = {
                logoutConfirmVisible = false
                viewModel.logout()
            },
            onDismiss = { logoutConfirmVisible = false },
        )
    }

    if (deleteConfirmVisible) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_account),
            message = stringResource(R.string.settings_delete_account_confirm),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                deleteConfirmVisible = false
                viewModel.deleteAccount(deleteFailure)
            },
            onDismiss = { deleteConfirmVisible = false },
        )
    }

    if (clearDownloadsVisible) {
        val context = LocalContext.current
        ConfirmDialog(
            title = stringResource(R.string.settings_downloads_clear),
            message = stringResource(
                R.string.settings_downloads_clear_confirm,
                pluralStringResource(
                    R.plurals.downloaded_playlists_count,
                    downloadsUsage.playlists,
                    downloadsUsage.playlists,
                ),
                Formatter.formatShortFileSize(context, downloadsUsage.bytes),
            ),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                clearDownloadsVisible = false
                viewModel.clearDownloads()
            },
            onDismiss = { clearDownloadsVisible = false },
        )
    }

    if (cacheDialogVisible) {
        AudioCacheDialog(
            coverBytes = coverBytes,
            selected = cacheSize,
            usage = cacheUsage,
            onSelect = viewModel::setAudioCacheSize,
            onClear = viewModel::clearAudioCache,
            onDismiss = { cacheDialogVisible = false },
        )
    }

    if (state.serverDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::closeServerDialog,
            title = { Text(stringResource(R.string.server_url)) },
            text = {
                OutlinedTextField(
                    value = state.serverDraft,
                    onValueChange = viewModel::onServerDraftChange,
                    placeholder = { Text(stringResource(R.string.server_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.saveServer() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveServer) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeServerDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }

    if (state.password.visible) {
        ChangePasswordDialog(viewModel = viewModel, state = state)
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) {
                    Text(stringResource(R.string.accept))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

/** Los tres campos llevan ojo: teclear tres contraseñas a ciegas es pedir un error. */
@Composable
private fun ChangePasswordDialog(
    viewModel: SettingsViewModel,
    state: SettingsUiState,
) {
    val emptyMessage = stringResource(R.string.password_fields_required)
    val mismatchMessage = stringResource(R.string.password_mismatch)
    val tooShortMessage = stringResource(R.string.password_too_short)
    val successMessage = stringResource(R.string.password_changed)
    val failureMessage = stringResource(R.string.password_change_failed)

    val submit = {
        viewModel.changePassword(
            emptyMessage = emptyMessage,
            mismatchMessage = mismatchMessage,
            tooShortMessage = tooShortMessage,
            successMessage = successMessage,
            failureMessage = failureMessage,
        )
    }

    AlertDialog(
        onDismissRequest = viewModel::closePasswordDialog,
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField(
                    value = state.password.current,
                    onValueChange = viewModel::onCurrentPasswordChange,
                    label = stringResource(R.string.current_password),
                    enabled = !state.password.isSubmitting,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordField(
                    value = state.password.new,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = stringResource(R.string.new_password),
                    enabled = !state.password.isSubmitting,
                    imeAction = ImeAction.Next,
                    contentType = ContentType.NewPassword,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordField(
                    value = state.password.confirm,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = stringResource(R.string.confirm_password),
                    enabled = !state.password.isSubmitting,
                    imeAction = ImeAction.Done,
                    contentType = ContentType.NewPassword,
                    onImeAction = submit,
                    isError = state.password.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.password.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = !state.password.isSubmitting) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closePasswordDialog) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

/**
 * Resumen de la fila: la opción elegida y, sobre todo, lo que está ocupando
 * ahora mismo en el dispositivo. Es el número que de verdad importa, así que
 * se ve sin tener que abrir nada.
 */
@Composable
private fun cacheSummary(size: AudioCacheSize, usage: AudioUsage): String {
    val context = LocalContext.current
    if (size == AudioCacheSize.Off) {
        return stringResource(R.string.settings_cache_summary_off, size.label())
    }
    return stringResource(
        R.string.settings_cache_summary,
        size.label(),
        Formatter.formatShortFileSize(context, usage.bytes),
        Formatter.formatShortFileSize(context, size.limitBytes),
    )
}

/** Lo que ocupan las descargas: playlists y megas, o que no hay ninguna. */
@Composable
private fun downloadsSummary(usage: DownloadsUsage): String {
    if (usage.playlists == 0 && usage.bytes == 0L) {
        return stringResource(R.string.settings_downloads_empty)
    }
    val context = LocalContext.current
    return stringResource(
        R.string.settings_downloads_summary,
        pluralStringResource(
            R.plurals.downloaded_playlists_count,
            usage.playlists,
            usage.playlists,
        ),
        Formatter.formatShortFileSize(context, usage.bytes),
    )
}

@Composable
private fun AudioCacheSize.label(): String = stringResource(
    when (this) {
        AudioCacheSize.Off -> R.string.cache_size_off
        AudioCacheSize.Small -> R.string.cache_size_small
        AudioCacheSize.Medium -> R.string.cache_size_medium
        AudioCacheSize.Large -> R.string.cache_size_large
    },
)

/**
 * Elegir cuánto se guarda. Cada opción dice en megas lo que puede llegar a
 * ocupar —es espacio del móvil del usuario y tiene que saberlo antes de
 * elegir— y abajo se enseña lo que hay guardado de verdad, medido, no
 * estimado: lo que ocupe una canción depende del catálogo.
 */
@Composable
private fun AudioCacheDialog(
    coverBytes: Long,
    selected: AudioCacheSize,
    usage: AudioUsage,
    onSelect: (AudioCacheSize) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cache)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_cache_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                AudioCacheSize.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSelect(option) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = option.label(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (option == AudioCacheSize.Off) {
                                    stringResource(R.string.settings_cache_off_summary)
                                } else {
                                    stringResource(
                                        R.string.settings_cache_limit,
                                        Formatter.formatShortFileSize(context, option.limitBytes),
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (usage.songs == 0) {
                        stringResource(R.string.settings_cache_empty)
                    } else {
                        stringResource(
                            R.string.settings_cache_in_use,
                            Formatter.formatShortFileSize(context, usage.bytes),
                            pluralStringResource(
                                R.plurals.cache_songs_count,
                                usage.songs,
                                usage.songs,
                            ),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (coverBytes > 0) {
                    Text(
                        text = stringResource(
                            R.string.settings_cache_covers,
                            Formatter.formatShortFileSize(context, coverBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
        dismissButton = {
            TextButton(onClick = onClear, enabled = usage.songs > 0 || coverBytes > 0) {
                Text(stringResource(R.string.settings_cache_clear))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val contentColor = if (tint == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        tint
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = contentColor)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}
