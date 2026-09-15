package com.zice.playbutton.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zice.playbutton.data.repo.SongRepository
import com.zice.playbutton.domain.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class QueueEntry(
    val songId: Int,
    val name: String,
    val artworkUri: String? = null,
)

data class PlayerState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val songId: Int? = null,
    val title: String = "",
    val artist: String? = null,
    val fullName: String = "",
    val artworkUri: String? = null,
    val durationMs: Long = 0L,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = 0,
    val sourceName: String? = null,
    /**
     * Se ha pedido musica que todavia no ha llegado. El Modo Zen pide una tanda
     * al catalogo por red, asi que entre el toque y la primera cancion la cola
     * esta vacia sin que eso signifique que no haya nada que escuchar.
     */
    val isPreparing: Boolean = false,
) {
    val hasContent: Boolean get() = songId != null

    /** Hay reproductor que enseñar: algo cargado, o algo en camino. */
    val hasPlayer: Boolean get() = hasContent || isPreparing
}

/**
 * Cliente del servicio de reproducción. La interfaz nunca toca ExoPlayer
 * directamente: observa este estado y envía órdenes por la sesión de medios,
 * de modo que la notificación, los botones del coche y la propia app operan
 * exactamente sobre el mismo reproductor.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playbackQueue: PlaybackQueue,
    private val songRepository: SongRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var controller: MediaController? = null

    /**
     * El vínculo con el servicio, guardado aparte del controlador para poder
     * soltarlo aunque la conexión aún no se haya completado.
     */
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var positionTicker: Job? = null

    /**
     * Ver [PlayerState.isPreparing]. Vive aparte del estado porque [syncState]
     * lo rehace entero a partir del reproductor, y el reproductor no sabe nada
     * de lo que se le ha pedido y todavia no ha llegado.
     */
    private var preparing = false

    private val artworkUri by lazy { MediaItems.fallbackArtwork(context.packageName) }

    /**
     * Se engancha al servicio; la llama la pantalla al volver al primer plano.
     *
     * Conectarse lo vincula —un `MediaController` hace `bindService` por
     * dentro—, y un servicio vinculado no se destruye aunque él mismo pida
     * pararse. Por eso la conexión dura lo que la pantalla y no lo que el
     * proceso: abriéndola en el `init` de este singleton, que no se cierra
     * nunca, la app seguía viva en segundo plano después de cerrarla y de
     * quitar la notificación del reproductor.
     */
    fun connect() {
        if (controllerFuture != null) return

        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                // Si nos han soltado mientras se conectaba, del controlador ya
                // se encarga `releaseFuture` y aquí no hay nada que quedarse.
                if (controllerFuture !== future) return@addListener
                controller = runCatching { future.get() }.getOrNull()
                controller?.let { player ->
                    player.addListener(ControllerListener())
                    syncState()
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    /**
     * Suelta el servicio al irse la pantalla. Lo que esté sonando no se entera
     * —vive en el servicio, no aquí—, pero si no hay nada que reproducir esto
     * es lo que deja que el servicio se destruya y el proceso termine.
     */
    fun release() {
        val future = controllerFuture ?: return
        controllerFuture = null
        controller = null
        stopPositionTicker()
        // Vale también con la conexión a medias: suelta el controlador en
        // cuanto exista.
        MediaController.releaseFuture(future)
        // El resto del estado se conserva: al volver la pantalla se pinta lo
        // último que se sabía y `syncState` lo corrige en cuanto reconecta.
        _state.value = _state.value.copy(isConnected = false)
    }

    /**
     * La posición no se puede observar con eventos: se sondea, pero solo
     * mientras algo suena. El sondeo se arranca y se para con la reproducción
     * —antes era un bucle infinito que seguía despertando el proceso cada medio
     * segundo sin nada que contar—.
     */
    private fun startPositionTicker() {
        if (positionTicker?.isActive == true) return
        positionTicker = scope.launch {
            while (true) {
                val player = controller ?: break
                _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                delay(500)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    private fun syncState() {
        val player = controller ?: return
        val metadata = player.mediaMetadata
        _state.value = PlayerState(
            isConnected = true,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            songId = player.currentMediaItem?.mediaId?.toIntOrNull(),
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString(),
            fullName = metadata.displayTitle?.toString().orEmpty(),
            // Solo la remota. Cuando la cancion no tiene portada, la sesion
            // lleva el isotipo empaquetado para que la notificacion y la
            // pantalla de bloqueo enseñen algo; la app tiene su propio hueco.
            artworkUri = metadata.artworkUri?.toString()?.takeIf { it.startsWith("http") },
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            queue = currentQueue(player),
            currentIndex = player.currentMediaItemIndex,
            sourceName = playbackQueue.sourceName,
            isPreparing = preparing,
        )
        _positionMs.value = player.currentPosition.coerceAtLeast(0L)

        if (player.isPlaying) startPositionTicker() else stopPositionTicker()
    }

    private fun currentQueue(player: Player): List<QueueEntry> =
        (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            QueueEntry(
                songId = item.mediaId.toIntOrNull() ?: -1,
                name = item.mediaMetadata.displayTitle?.toString()
                    ?: item.mediaMetadata.title?.toString().orEmpty(),
                artworkUri = item.mediaMetadata.artworkUri?.toString()
                    ?.takeIf { it.startsWith("http") },
            )
        }

    private inner class ControllerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
        }
    }

    // --- Órdenes ---------------------------------------------------------

    /** Empieza a escuchar una playlist, barajada y sin repeticiones seguidas. */
    fun playPlaylist(playlistId: Int, playlistName: String, songs: List<Song>) {
        if (songs.isEmpty()) return
        playbackQueue.setMode(PlaybackMode.PlaylistLoop(playlistId, playlistName, songs))
        setQueue(QueueBuilder.shuffled(songs))
    }

    /**
     * Reproduce la playlist empezando por la canción que se ha pulsado. El
     * resto va barajado detrás, y al agotarse la cola la playlist se vuelve a
     * barajar entera como en el modo normal.
     */
    fun playPlaylistFrom(
        playlistId: Int,
        playlistName: String,
        songs: List<Song>,
        startSong: Song,
    ) {
        if (songs.isEmpty()) return
        playbackQueue.setMode(PlaybackMode.PlaylistLoop(playlistId, playlistName, songs))
        setQueue(QueueBuilder.startingWith(songs, startSong))
    }

    /** Modo Zen: selección aleatoria de todo el catálogo, en bucle. */
    fun playZen() {
        playbackQueue.setMode(PlaybackMode.Zen)
        // Pedir la tanda tarda lo que tarde la red, y hasta que llegue la cola
        // sigue vacia. La interfaz esconde el reproductor cuando no hay nada
        // que enseñar, asi que sin avisar de que lo pedido viene en camino la
        // capa se cerraba sola justo despues de abrirse.
        setPreparing(true)
        scope.launch {
            try {
                val songs = runCatching { songRepository.zenBatch() }.getOrDefault(emptyList())
                if (songs.isNotEmpty()) setQueue(QueueBuilder.shuffled(songs))
            } finally {
                // Si no ha venido nada, esto es tambien lo que recoge el
                // reproductor en lugar de dejarlo cargando para siempre.
                setPreparing(false)
            }
        }
    }

    private fun setPreparing(value: Boolean) {
        if (preparing == value) return
        preparing = value
        _state.value = _state.value.copy(isPreparing = value)
    }

    private fun setQueue(songs: List<Song>) {
        val player = controller ?: return
        player.setMediaItems(songs.map { MediaItems.from(it, artworkUri) })
        player.prepare()
        player.play()
    }

    // Estas órdenes salen por la sesión de medios y las atiende
    // `ResumeOnSkipPlayer`, exactamente el mismo camino que recorren los
    // botones de la notificación y los del coche. Reanudar al saltar de
    // canción y volver a preparar tras un error viven allí y no aquí: son
    // reglas de «lo ha pedido una persona», y con una sola copia no pueden
    // acabar dependiendo de dónde se haya pulsado.

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        val player = controller ?: return
        // Sin siguiente no se hace nada. El servicio reabastece la cola con
        // margen, así que llegar hasta aquí sin nada por delante significa que
        // de verdad no hay más.
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun previous() {
        // `seekToPrevious` ya trae el comportamiento de cualquier reproductor:
        // si la canción ha avanzado más de tres segundos vuelve a su principio
        // en vez de saltar a la anterior. Estaba escrito a mano aquí, y era
        // una regla más que la notificación no compartía.
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun skipToQueueIndex(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0)
            player.play()
        }
    }

    /** Al cerrar sesión se para y se vacía todo. */
    fun stopAndClear() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        playbackQueue.clear()
        preparing = false
        _state.value = PlayerState(isConnected = controller != null)
        _positionMs.value = 0L
    }
}
