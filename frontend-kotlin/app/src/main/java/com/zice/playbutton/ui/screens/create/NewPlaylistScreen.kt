package com.zice.playbutton.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zice.playbutton.R
import com.zice.playbutton.data.repo.PlaylistRepository
import com.zice.playbutton.ui.components.Artwork
import com.zice.playbutton.ui.components.ArtworkKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewPlaylistUiState(
    val name: String = "",
    val isPublic: Boolean = true,
    /** La elegida en el selector, todavía sin subir. Ver [NewPlaylistViewModel.create]. */
    val imageUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NewPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPlaylistUiState())
    val uiState = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, error = null)
    }

    fun onPublicChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(isPublic = value)
    }

    /** `null` para quitar la que se hubiera elegido. */
    fun onImageChange(uri: Uri?) {
        _uiState.value = _uiState.value.copy(imageUri = uri)
    }

    /** Al crearla se abre su detalle directamente, para poder añadir canciones. */
    fun create(errorMessage: String, onCreated: (Int) -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)
            runCatching { playlistRepository.createPlaylist(name, state.isPublic) }
                .onSuccess { playlist ->
                    // La portada va después y no en el alta: cuelga del id de
                    // la playlist, que no existe hasta que el servidor la
                    // crea. Si la subida falla se sigue adelante igual: la
                    // playlist ya está hecha y volver a intentarlo dejaría
                    // dos, así que se entra a su detalle, que es desde donde
                    // se cambia la portada.
                    state.imageUri?.let {
                        playlistRepository.setPlaylistImage(playlist.id, it)
                    }
                    _uiState.value = NewPlaylistUiState()
                    onCreated(playlist.id)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = errorMessage,
                    )
                }
        }
    }
}

@Composable
fun NewPlaylistScreen(
    onCreated: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = stringResource(R.string.error_generic)

    // Cancelar el selector devuelve `null`, y eso no es quitar la portada: se
    // deja la que hubiera. Para quitarla está su propio botón.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onImageChange) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.playlist_name)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.create(errorMessage, onCreated) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.playlist_public_switch),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = state.isPublic,
                onCheckedChange = viewModel::onPublicChange,
                enabled = !state.isSubmitting,
            )
        }

        // La portada es opcional y se elige antes de existir la playlist, así
        // que aquí no se sube nada: se guarda lo elegido y se enseña tal cual
        // desde el dispositivo hasta que haya a qué colgarla.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Artwork(
                imageUrl = state.imageUri?.toString(),
                kind = ArtworkKind.Song,
                size = 56.dp,
            )
            Column(Modifier.weight(1f)) {
                TextButton(
                    onClick = {
                        pickImage.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    enabled = !state.isSubmitting,
                ) {
                    Text(
                        stringResource(
                            if (state.imageUri == null) {
                                R.string.playlist_cover_choose
                            } else {
                                R.string.playlist_cover_change
                            },
                        ),
                    )
                }
                if (state.imageUri != null) {
                    TextButton(
                        onClick = { viewModel.onImageChange(null) },
                        enabled = !state.isSubmitting,
                    ) {
                        Text(stringResource(R.string.playlist_cover_remove))
                    }
                }
            }
        }

        state.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { viewModel.create(errorMessage, onCreated) },
            enabled = !state.isSubmitting && state.name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.new_playlist))
        }
    }
}
