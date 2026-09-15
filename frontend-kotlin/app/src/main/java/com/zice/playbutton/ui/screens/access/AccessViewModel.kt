package com.zice.playbutton.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zice.playbutton.BuildConfig
import com.zice.playbutton.data.remote.AuthEvents
import com.zice.playbutton.data.remote.ServerUrl
import com.zice.playbutton.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccessUiState(
    val nickname: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val serverDialogVisible: Boolean = false,
    val serverDraft: String = "",
)

@HiltViewModel
class AccessViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authEvents: AuthEvents,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessUiState())
    val uiState = _uiState.asStateFlow()

    /** true si se llegó aquí porque caducó la sesión, no por cerrarla. */
    val arrivedByExpiry: Boolean = authEvents.consumeExpired()

    val serverUrl = authRepository.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onNicknameChange(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun openServerDialog() {
        _uiState.value = _uiState.value.copy(
            serverDialogVisible = true,
            serverDraft = ServerUrl.display(serverUrl.value),
        )
    }

    fun closeServerDialog() {
        _uiState.value = _uiState.value.copy(serverDialogVisible = false)
    }

    fun onServerDraftChange(value: String) {
        _uiState.value = _uiState.value.copy(serverDraft = value)
    }

    fun saveServer() {
        val normalized = ServerUrl.normalize(_uiState.value.serverDraft, BuildConfig.DEBUG)
        if (normalized == null) {
            _uiState.value = _uiState.value.copy(serverDialogVisible = false)
            return
        }
        viewModelScope.launch {
            authRepository.setServerUrl(normalized)
            _uiState.value = _uiState.value.copy(serverDialogVisible = false)
        }
    }

    fun login(noServerMessage: String, failedMessage: String) {
        val state = _uiState.value
        if (state.nickname.isBlank() || state.password.isBlank()) return
        if (serverUrl.value == null) {
            _uiState.value = state.copy(error = noServerMessage)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)
            runCatching { authRepository.login(state.nickname.trim(), state.password) }
                .onSuccess {
                    // La contraseña no se conserva en memoria más de lo necesario.
                    _uiState.value = AccessUiState()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        password = "",
                        error = failedMessage,
                    )
                }
        }
    }
}
