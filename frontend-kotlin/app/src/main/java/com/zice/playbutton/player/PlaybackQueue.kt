package com.zice.playbutton.player

import com.zice.playbutton.data.repo.SongRepository
import com.zice.playbutton.domain.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * De dónde salen las canciones que suenan. Ambos modos son infinitos: cuando
 * la cola se agota se pide otra tanda, igual que hacía la app React Native.
 */
sealed interface PlaybackMode {
    data object Idle : PlaybackMode

    /** Reproducción aleatoria de todo el catálogo. */
    data object Zen : PlaybackMode

    /** Una playlist concreta, que se vuelve a barajar al terminar. */
    data class PlaylistLoop(
        val playlistId: Int,
        val playlistName: String,
        val songs: List<Song>,
    ) : PlaybackMode
}

/**
 * Estado compartido entre la interfaz y el servicio de reproducción: la
 * interfaz elige qué suena y el servicio, que sobrevive a la interfaz, sabe
 * cómo reabastecer la cola cuando se acaba.
 */
@Singleton
class PlaybackQueue @Inject constructor(
    private val songRepository: SongRepository,
) {
    private val _mode = MutableStateFlow<PlaybackMode>(PlaybackMode.Idle)
    val mode: StateFlow<PlaybackMode> = _mode.asStateFlow()

    /** Nombre de lo que se está reproduciendo, para la cabecera del reproductor. */
    val sourceName: String?
        get() = when (val current = _mode.value) {
            is PlaybackMode.PlaylistLoop -> current.playlistName
            PlaybackMode.Zen -> null
            PlaybackMode.Idle -> null
        }

    fun setMode(mode: PlaybackMode) {
        _mode.value = mode
    }

    fun clear() {
        _mode.value = PlaybackMode.Idle
    }

    /**
     * Siguiente tanda de canciones. [previousSongId] es la última que sonó,
     * para que la tanda nueva no empiece repitiéndola.
     */
    suspend fun nextBatch(previousSongId: Int?): List<Song> = when (val current = _mode.value) {
        PlaybackMode.Idle -> emptyList()
        PlaybackMode.Zen -> QueueBuilder.shuffled(songRepository.zenBatch(), previousSongId)
        is PlaybackMode.PlaylistLoop -> QueueBuilder.shuffled(current.songs, previousSongId)
    }
}
