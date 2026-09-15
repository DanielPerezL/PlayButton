package com.zice.playbutton.ui.screens.suggestions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zice.playbutton.R
import com.zice.playbutton.data.repo.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestionUiState(
    val artist: String = "",
    val song: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val songRepository: SongRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionUiState())
    val uiState = _uiState.asStateFlow()

    fun onArtistChange(value: String) {
        _uiState.value = _uiState.value.copy(artist = value, message = null)
    }

    fun onSongChange(value: String) {
        _uiState.value = _uiState.value.copy(song = value, message = null)
    }

    fun submit(successMessage: String, errorMessage: String) {
        val state = _uiState.value
        if (state.artist.isBlank() || state.song.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, message = null)
            val ok = runCatching {
                songRepository.createSuggestion(state.artist.trim(), state.song.trim())
            }.getOrDefault(false)

            _uiState.value = if (ok) {
                SuggestionUiState(message = successMessage)
            } else {
                _uiState.value.copy(
                    isSubmitting = false,
                    message = errorMessage,
                    isError = true,
                )
            }
        }
    }
}

@Composable
fun SuggestionsScreen(
    modifier: Modifier = Modifier,
    viewModel: SuggestionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val successMessage = stringResource(R.string.suggestion_sent)
    val errorMessage = stringResource(R.string.suggestion_failed)

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.artist,
            onValueChange = viewModel::onArtistChange,
            label = { Text(stringResource(R.string.suggestion_artist)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.song,
            onValueChange = viewModel::onSongChange,
            label = { Text(stringResource(R.string.suggestion_song)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        state.message?.let { message ->
            Text(
                text = message,
                color = if (state.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
        }

        Button(
            onClick = { viewModel.submit(successMessage, errorMessage) },
            enabled = !state.isSubmitting &&
                state.artist.isNotBlank() &&
                state.song.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.suggestion_send))
        }
    }
}
