package com.zice.playbutton.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zice.playbutton.BuildConfig
import com.zice.playbutton.data.local.AudioCache
import com.zice.playbutton.data.local.AudioCacheSize
import com.zice.playbutton.data.local.AudioUsage
import com.zice.playbutton.data.local.ImageCache
import com.zice.playbutton.data.local.SettingsStore
import com.zice.playbutton.data.repo.DownloadsUsage
import com.zice.playbutton.data.repo.OfflineLibrary
import com.zice.playbutton.data.remote.ServerUrl
import com.zice.playbutton.data.repo.AuthRepository
import com.zice.playbutton.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PasswordDialogState(
    val visible: Boolean = false,
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

data class SettingsUiState(
    val serverDisplay: String = "",
    val serverDialogVisible: Boolean = false,
    val serverDraft: String = "",
    val password: PasswordDialogState = PasswordDialogState(),
    val message: String? = null,
    val version: String = BuildConfig.VERSION_NAME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsStore: SettingsStore,
    private val playerConnection: PlayerConnection,
    private val audioCache: AudioCache,
    private val imageCache: ImageCache,
    private val offlineLibrary: OfflineLibrary,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    val serverDisplay = authRepository.serverUrl
        .map { ServerUrl.display(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val crossfadeEnabled = settingsStore.crossfadeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val audioCacheSize = settingsStore.audioCacheSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioCacheSize.Default)

    private val _audioCacheUsage = MutableStateFlow(AudioUsage())
    val audioCacheUsage = _audioCacheUsage.asStateFlow()

    /** Lo que ocupan las portadas. Coil las guarda todas juntas, vengan de
     *  navegar o de una descarga, así que no se pueden separar. */
    private val _imageCacheBytes = MutableStateFlow(0L)
    val imageCacheBytes = _imageCacheBytes.asStateFlow()

    private val _downloadsUsage = MutableStateFlow(DownloadsUsage())
    val downloadsUsage = _downloadsUsage.asStateFlow()

    // --- Reproducción ----------------------------------------------------

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setCrossfadeEnabled(enabled) }
    }

    // --- Caché de canciones ----------------------------------------------

    fun setAudioCacheSize(size: AudioCacheSize) {
        viewModelScope.launch {
            settingsStore.setAudioCacheSize(size)
            // El servicio de reproducción también observa el ajuste, pero
            // puede no estar vivo —nada ha sonado aún— y desactivar la caché
            // tiene que liberar el espacio en ese momento, no la próxima vez
            // que se abra la app.
            withContext(Dispatchers.IO) { audioCache.setSize(size) }
            refreshCacheUsage()
        }
    }

    /**
     * Vacía también las portadas. Es lo que significa vaciar la caché: se
     * vuelven a pedir solas en cuanto haya servidor. Mientras tanto, una
     * playlist descargada se escucha igual pero se ve con los huecos.
     */
    fun clearAudioCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                audioCache.clear()
                imageCache.clear()
            }
            refreshCacheUsage()
        }
    }

    // --- Almacenamiento ---------------------------------------------------

    /**
     * Borra todas las descargas. A diferencia de la caché esto no se recupera
     * solo: hay que volver a descargar cada playlist a mano, así que la
     * pantalla lo pide confirmado.
     */
    fun clearDownloads() {
        viewModelScope.launch {
            offlineLibrary.clear()
            refreshCacheUsage()
        }
    }

    /**
     * Lo llama la pantalla al abrirse: abrir el índice de la caché es disco y
     * no puede ir en el hilo principal, y el dato tiene que estar al día cada
     * vez que se entra —entre medias se ha estado escuchando música—.
     */
    fun refreshCacheUsage() {
        viewModelScope.launch {
            _audioCacheUsage.value = withContext(Dispatchers.IO) { audioCache.usage() }
            _imageCacheBytes.value = withContext(Dispatchers.IO) { imageCache.sizeBytes() }
            _downloadsUsage.value = offlineLibrary.usage()
        }
    }

    // --- Servidor --------------------------------------------------------

    fun openServerDialog() {
        _uiState.value = _uiState.value.copy(
            serverDialogVisible = true,
            serverDraft = serverDisplay.value,
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
            closeServerDialog()
            return
        }
        viewModelScope.launch {
            // Guardar sin tocar la URL —abrir el diálogo y aceptar— no debe
            // costar la sesión: solo cerrarla cuando el servidor cambia de
            // verdad, porque el token pertenece al servidor que lo emitió.
            val changed = normalized != authRepository.serverUrl.first()
            if (changed) {
                authRepository.setServerUrl(normalized)
                logout()
            }
            _uiState.value = _uiState.value.copy(serverDialogVisible = false)
        }
    }

    // --- Contraseña ------------------------------------------------------

    fun openPasswordDialog() {
        _uiState.value = _uiState.value.copy(password = PasswordDialogState(visible = true))
    }

    fun closePasswordDialog() {
        _uiState.value = _uiState.value.copy(password = PasswordDialogState())
    }

    fun onCurrentPasswordChange(value: String) = updatePassword { it.copy(current = value, error = null) }
    fun onNewPasswordChange(value: String) = updatePassword { it.copy(new = value, error = null) }
    fun onConfirmPasswordChange(value: String) = updatePassword { it.copy(confirm = value, error = null) }

    private fun updatePassword(transform: (PasswordDialogState) -> PasswordDialogState) {
        _uiState.value = _uiState.value.copy(password = transform(_uiState.value.password))
    }

    fun changePassword(
        emptyMessage: String,
        mismatchMessage: String,
        tooShortMessage: String,
        successMessage: String,
        failureMessage: String,
    ) {
        val state = _uiState.value.password

        val error = when {
            state.current.isBlank() || state.new.isBlank() || state.confirm.isBlank() -> emptyMessage
            state.new != state.confirm -> mismatchMessage
            state.new.length < 6 -> tooShortMessage
            else -> null
        }
        if (error != null) {
            updatePassword { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            updatePassword { it.copy(isSubmitting = true, error = null) }
            val ok = runCatching { authRepository.changePassword(state.current, state.new) }
                .getOrDefault(false)

            if (ok) {
                _uiState.value = _uiState.value.copy(
                    password = PasswordDialogState(),
                    message = successMessage,
                )
            } else {
                updatePassword { it.copy(isSubmitting = false, error = failureMessage) }
            }
        }
    }

    // --- Cuenta ----------------------------------------------------------

    fun logout() {
        viewModelScope.launch {
            playerConnection.stopAndClear()
            authRepository.logout()
        }
    }

    fun deleteAccount(failureMessage: String) {
        viewModelScope.launch {
            val ok = runCatching { authRepository.deleteAccount() }.getOrDefault(false)
            if (ok) {
                playerConnection.stopAndClear()
            } else {
                _uiState.value = _uiState.value.copy(message = failureMessage)
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
