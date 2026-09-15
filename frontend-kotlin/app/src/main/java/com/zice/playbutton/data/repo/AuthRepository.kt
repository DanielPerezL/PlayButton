package com.zice.playbutton.data.repo

import com.zice.playbutton.data.local.AudioCache
import com.zice.playbutton.data.local.ImageCache
import com.zice.playbutton.data.local.AudioOwner
import com.zice.playbutton.data.local.SessionStore
import com.zice.playbutton.data.local.SettingsStore
import com.zice.playbutton.data.remote.ApiService
import com.zice.playbutton.data.remote.dto.ChangePasswordRequest
import com.zice.playbutton.data.remote.dto.LoginRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val sessionStore: SessionStore,
    private val settingsStore: SettingsStore,
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val audioCache: AudioCache,
    private val imageCache: ImageCache,
    private val offlineLibrary: OfflineLibrary,
) {
    val isLoggedIn: Flow<Boolean> = sessionStore.isLoggedIn
    val serverUrl: Flow<String?> = settingsStore.serverUrl

    suspend fun currentUserId(): Int? = sessionStore.current()?.userId
    suspend fun isAdmin(): Boolean = sessionStore.current()?.isAdmin ?: false

    suspend fun login(nickname: String, password: String) {
        val response = api.login(LoginRequest(nickname, password))
        sessionStore.save(response.accessToken, response.userId, response.isAdmin)
        dropAudioIfOwnerChanged(response.userId)
    }

    /**
     * Al cerrar sesión no sobrevive nada del usuario anterior salvo el audio
     * que hay en el dispositivo: eso se decide al entrar, no al salir. Cerrar
     * sesión y volver con la misma cuenta es de lo más normal —un token
     * caducado ya lo provoca solo— y no tiene por qué costar las descargas.
     *
     * Lo que sí se va es todo lo que identifica al usuario. El audio, sin
     * sesión, no está al alcance de nadie: la app no pasa de la pantalla de
     * acceso.
     */
    suspend fun logout() {
        sessionStore.clear()
        playlistRepository.clear()
        songRepository.clearCacheExceptDownloads()
    }

    /**
     * El audio guardado pertenece a un usuario y a un servidor concretos. Si
     * al iniciar sesión cambia cualquiera de los dos, se tira todo: son
     * canciones que el nuevo usuario puede no tener derecho a escuchar, y los
     * ids de canción de otro servidor no significan lo mismo, así que lo
     * guardado sonaría como otra cosa.
     */
    private suspend fun dropAudioIfOwnerChanged(userId: Int) {
        val owner = AudioOwner(userId, settingsStore.currentServerUrl().orEmpty())
        val previous = settingsStore.currentAudioOwner()

        if (previous != null && previous != owner) {
            audioCache.clear()
            offlineLibrary.clear()
            songRepository.clearCache()
            // Las portadas se van por lo mismo: son de otro servidor, y sus
            // URL ni siquiera apuntan ya a donde deben.
            imageCache.clear()
        }
        settingsStore.setAudioOwner(owner)
    }

    suspend fun changePassword(current: String, new: String): Boolean {
        val userId = sessionStore.current()?.userId ?: return false
        return api.changePassword(userId, ChangePasswordRequest(current, new)).isSuccessful
    }

    suspend fun deleteAccount(): Boolean {
        val userId = sessionStore.current()?.userId ?: return false
        val ok = api.deleteAccount(userId).isSuccessful
        if (ok) {
            // La cuenta ya no existe: aquí no hay nada que reservar para la
            // próxima vez.
            audioCache.clear()
            offlineLibrary.clear()
            songRepository.clearCache()
            settingsStore.clearAudioOwner()
            logout()
        }
        return ok
    }

    suspend fun setServerUrl(url: String) = settingsStore.setServerUrl(url)
}
