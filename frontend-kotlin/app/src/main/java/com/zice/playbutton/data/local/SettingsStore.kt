package com.zice.playbutton.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * De quién es el audio que hay guardado en el dispositivo.
 *
 * Se anota para poder conservarlo entre sesiones: cerrar sesión y volver a
 * entrar con la misma cuenta en el mismo servidor no tiene por qué costar las
 * descargas. Con otro usuario u otro servidor sí hay que tirarlo — son
 * canciones que el nuevo puede no tener derecho a escuchar, y un id de canción
 * de otro servidor no significa lo mismo—.
 */
data class AudioOwner(
    val userId: Int,
    val serverUrl: String,
)

/**
 * Ajustes que no son sesion: la URL del servidor autoalojado y las
 * preferencias de interfaz.
 */
@Singleton
class SettingsStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val ServerUrl = stringPreferencesKey("server_url")
        val HideAddSongAlert = booleanPreferencesKey("hide_add_song_alert")
        val Crossfade = booleanPreferencesKey("crossfade_enabled")
        val AudioCache = stringPreferencesKey("audio_cache_size")
        val AudioOwnerUser = intPreferencesKey("audio_owner_user")
        val AudioOwnerServer = stringPreferencesKey("audio_owner_server")
    }

    val serverUrl: Flow<String?> = context.settingsDataStore.data
        .map { it[Keys.ServerUrl]?.takeIf(String::isNotBlank) }

    suspend fun currentServerUrl(): String? = serverUrl.first()

    suspend fun setServerUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.ServerUrl] = url }
    }

    val hideAddSongAlert: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.HideAddSongAlert] ?: false }

    suspend fun setHideAddSongAlert(hide: Boolean) {
        context.settingsDataStore.edit { it[Keys.HideAddSongAlert] = hide }
    }

    /** Fundido entre canciones. Activo salvo que el usuario lo apague. */
    val crossfadeEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.Crossfade] ?: true }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.Crossfade] = enabled }
    }

    /** Cuánto audio ya escuchado se guarda en el dispositivo. */
    val audioCacheSize: Flow<AudioCacheSize> = context.settingsDataStore.data
        .map { AudioCacheSize.fromKey(it[Keys.AudioCache]) }

    suspend fun currentAudioCacheSize(): AudioCacheSize = audioCacheSize.first()

    suspend fun setAudioCacheSize(size: AudioCacheSize) {
        context.settingsDataStore.edit { it[Keys.AudioCache] = size.key }
    }

    /** Nulo cuando no hay nada guardado de nadie: no hay a quién comparar. */
    suspend fun currentAudioOwner(): AudioOwner? = context.settingsDataStore.data
        .map { preferences ->
            val userId = preferences[Keys.AudioOwnerUser] ?: return@map null
            AudioOwner(userId, preferences[Keys.AudioOwnerServer].orEmpty())
        }
        .first()

    suspend fun setAudioOwner(owner: AudioOwner) {
        context.settingsDataStore.edit {
            it[Keys.AudioOwnerUser] = owner.userId
            it[Keys.AudioOwnerServer] = owner.serverUrl
        }
    }

    suspend fun clearAudioOwner() {
        context.settingsDataStore.edit {
            it.remove(Keys.AudioOwnerUser)
            it.remove(Keys.AudioOwnerServer)
        }
    }
}
