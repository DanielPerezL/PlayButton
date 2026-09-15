package com.zice.playbutton.player

import android.app.PendingIntent
import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zice.playbutton.MainActivity
import com.zice.playbutton.R
import com.zice.playbutton.data.local.AudioCache
import com.zice.playbutton.data.local.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aloja el reproductor. Al vivir en un servicio y no dentro de una pantalla,
 * la reproducción sobrevive a la navegación y a que la interfaz se destruya
 * — en la app React Native toda la lógica estaba en un componente montado,
 * de ahí buena parte de sus rarezas al cambiar de pantalla.
 *
 * De cuándo morir se encarga el `onTaskRemoved` de Media3, que ya hace lo que
 * queremos: al quitar la app de recientes pausa y se para, salvo que haya
 * música sonando de verdad. Había aquí un sustituto que miraba `playWhenReady`
 * y no se enteraba de la canción que está cargando pero todavía no suena.
 *
 * Para que ese `stopSelf` sirva de algo nadie puede quedarse vinculado al
 * servicio, porque un servicio con clientes atados no se destruye: de soltarse
 * a tiempo se encarga [PlayerConnection.release].
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var audioSources: AudioSources
    @Inject lateinit var playbackQueue: PlaybackQueue
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var audioCache: AudioCache

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    private var fader: TrackFader? = null

    /**
     * El reproductor sin envolver. La sesión recibe [ResumeOnSkipPlayer], que
     * añade comportamiento pensado para cuando la orden viene de una persona;
     * lo que hace el servicio por su cuenta —reabastecer la cola, recortar el
     * historial, recuperarse de un error— va contra este y no arrastra ese
     * añadido.
     */
    private var exoPlayer: ExoPlayer? = null

    /** Última canción que ha sonado, para no repetirla al reabastecer la cola. */
    private var lastSongId: Int? = null
    private var refilling = false

    /** Reintentos ya gastados en recuperarse de un error de reproducción. */
    private var errorRetries = 0

    /** Duración del fundido entre canciones; 0 si está desactivado en ajustes. */
    private var crossfadeMs = 0L

    private val artworkUri by lazy {
        "android.resource://$packageName/${R.drawable.notification_artwork}".toUri()
    }

    private companion object {
        /**
         * Ventana de encadenado entre canciones, cuando el ajuste está activo.
         * Es el total de la transición: la mitad se va en apagar la que sale y
         * la otra en subir la que entra, recortando a cambio la cola de una y
         * la intro de la otra. Subirlo alarga el cruce y tira más música.
         */
        const val CROSSFADE_MS = 5_000L

        /**
         * Canciones que deben quedar siempre por delante. Con margen, el botón
         * de siguiente funciona también en la última de la tanda y el paso de
         * una tanda a otra no pasa por el silencio del final de la cola.
         */
        const val MIN_UPCOMING = 2

        /**
         * Canciones ya escuchadas que se conservan. No es solo para poder
         * retroceder: la cola del reproductor las enseña, así que recortar a
         * ras de la canción actual dejaba la lista sin nada por encima.
         */
        const val KEPT_HISTORY = 20

        /**
         * Intentos de recuperar la canción en curso tras un error antes de
         * darla por perdida y pasar a la siguiente.
         */
        const val MAX_ERROR_RETRIES = 3

        /** Espera del primer reintento; los siguientes van multiplicando. */
        const val ERROR_RETRY_DELAY_MS = 1_000L
    }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(audioSources)
                    .setLoadErrorHandlingPolicy(SignedUrlErrorPolicy()),
            )
            // Gestión de foco de audio delegada en Media3. Solo reanuda tras una
            // pérdida transitoria (una llamada, un aviso) y únicamente si fue él
            // quien pausó; ante una pérdida definitiva —otra app que se queda con
            // el audio— no vuelve solo. La app React Native lo hacía a mano y
            // reanudaba al terminar cualquier interrupción, que es el motivo de
            // que la música arrancase sola al salir de un vídeo.
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Desconectar el Bluetooth del coche o los auriculares pausa y no
            // reanuda después.
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(PlayerCallback(player))
        fader = TrackFader(player, scope) { crossfadeMs }
        exoPlayer = player

        scope.launch {
            settingsStore.crossfadeEnabled.collect { enabled ->
                crossfadeMs = if (enabled) CROSSFADE_MS else 0L
            }
        }

        // Fuera del hilo principal: aplicar la opción puede tener que abrir el
        // índice de la caché o borrar lo guardado, y eso es disco.
        scope.launch(Dispatchers.IO) {
            settingsStore.audioCacheSize.collect(audioCache::setSize)
        }

        mediaSession = MediaSession.Builder(this, ResumeOnSkipPlayer(player))
            // Sin esto la notificacion no lleva intent de contenido y tocarla
            // no hace nada: los botones de control responden, pero el cuerpo
            // es inerte. Es lo que abre la app desde la notificacion, la
            // pantalla de bloqueo o el reproductor del coche.
            .setSessionActivity(openAppIntent())
            .build()
    }

    /**
     * Lleva a la app tal y como la abriria el icono del lanzador: reutiliza la
     * tarea que ya existe en lugar de crear otra. [MainActivity] es
     * `singleTask`, asi que vuelve a lo que estaba en pantalla sin recrearlo.
     */
    private fun openAppIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        scope.cancel()
        fader = null
        exoPlayer?.release()
        exoPlayer = null
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private inner class PlayerCallback(private val player: Player) : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val songId = mediaItem?.mediaId?.toIntOrNull()
            // El encadenado corta el final de la pista y entra en la siguiente
            // por su cuenta, asi que llega como un salto y no como un avance
            // automatico. Para todo lo de aqui abajo cuenta como automatico:
            // no lo ha pedido el usuario.
            val chained = fader?.isChainedTransition(mediaItem?.mediaId) == true

            // Red de seguridad para el avance automático: si la siguiente pista
            // resulta ser la misma que acaba de sonar y hay otra distinta por
            // delante, se salta en lugar de repetirla. La cola ya se construye
            // sin duplicados consecutivos, pero puede modificarse en caliente.
            if ((reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || chained) &&
                songId != null && songId == lastSongId &&
                hasDifferentSongAhead(songId)
            ) {
                player.seekToNextMediaItem()
                return
            }

            if (songId != null) lastSongId = songId

            when {
                // El fundido de entrada ya esta en marcha: tocarlo lo cortaria.
                chained -> Unit
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                -> fader?.onAutoTransition()
                else -> fader?.onManualTransition()
            }

            ensureUpcoming()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // Buscar en la barra de progreso cancela el fundido de entrada:
            // el usuario ha ido a un punto concreto y quiere oírlo ya. Los
            // saltos del propio encadenado —cortar el final, saltarse la
            // intro— tambien pasan por aqui y hay que dejarlos seguir.
            if (reason != Player.DISCONTINUITY_REASON_SEEK) return
            val target = newPosition.mediaItemIndex
                .takeIf { it in 0 until player.mediaItemCount }
                ?.let { player.getMediaItemAt(it).mediaId }
            if (fader?.isChainedTransition(target) != true) fader?.onManualTransition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // Sonando otra vez: lo que fuera se arregló y el siguiente fallo
            // vuelve a tener todos los reintentos.
            if (playbackState == Player.STATE_READY) errorRetries = 0
            if (playbackState == Player.STATE_ENDED) ensureUpcoming()
        }

        override fun onPlayerError(error: PlaybackException) {
            recoverFromError()
        }

        private fun hasDifferentSongAhead(songId: Int): Boolean {
            val next = player.currentMediaItemIndex + 1
            for (index in next until player.mediaItemCount) {
                if (player.getMediaItemAt(index).mediaId.toIntOrNull() != songId) return true
            }
            return false
        }
    }

    /**
     * Mantiene canciones por delante en la cola: en cuanto quedan menos de
     * [MIN_UPCOMING] se pide otra tanda al modo activo (otra selección
     * aleatoria en Modo Zen, la misma playlist rebarajada si se estaba
     * escuchando una) y se añade al final.
     *
     * Antes esto solo ocurría al agotarse la cola por completo, así que en la
     * última canción no había ninguna siguiente a la que saltar y el botón de
     * avanzar no hacía nada. Reabasteciendo con margen la cola nunca se acaba:
     * el avance manual funciona siempre y la playlist enlaza con su siguiente
     * barajada sin pasar por el silencio.
     */
    private fun ensureUpcoming() {
        if (refilling) return

        val player = exoPlayer ?: return
        if (player.mediaItemCount == 0) return
        if (playbackQueue.mode.value == PlaybackMode.Idle) return
        if (player.mediaItemCount - 1 - player.currentMediaItemIndex >= MIN_UPCOMING) return

        refilling = true
        scope.launch {
            try {
                // El empalme se calcula con la última canción de la cola, no
                // con la que suena: es esa la que no debe repetirse al enlazar.
                val lastQueuedId = player.getMediaItemAt(player.mediaItemCount - 1)
                    .mediaId.toIntOrNull()
                val songs = playbackQueue.nextBatch(lastQueuedId ?: lastSongId)

                val current = exoPlayer ?: return@launch
                // Si la cola se había agotado del todo, hay que reanudar a mano
                // sobre lo que acabamos de añadir.
                val exhausted = current.playbackState == Player.STATE_ENDED

                if (songs.isEmpty()) {
                    if (exhausted) {
                        current.stop()
                        current.clearMediaItems()
                    }
                    return@launch
                }

                current.addMediaItems(songs.map { MediaItems.from(it, artworkUri) })
                trimHistory(current)
                if (exhausted) {
                    current.seekToNextMediaItem()
                    current.play()
                }
            } finally {
                refilling = false
            }
        }
    }

    /**
     * Un corte de red o un enlace firmado que ha caducado dejan el reproductor
     * en reposo con el error puesto, y de ahí no sale solo: la canción se
     * quedaba a medias, el progreso parado y ni el botón de reproducir
     * respondía. Volver a preparar reintenta desde donde iba y, al reabrir el
     * stream, [SignedUrlResolver] firma de nuevo. Si tras unos intentos sigue
     * sin arrancar se pasa a la siguiente, que es mejor que el silencio.
     */
    private fun recoverFromError() {
        val player = exoPlayer ?: return

        if (errorRetries >= MAX_ERROR_RETRIES) {
            errorRetries = 0
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
            }
            return
        }

        errorRetries++
        val mediaId = player.currentMediaItem?.mediaId
        val waitMs = ERROR_RETRY_DELAY_MS * errorRetries
        scope.launch {
            delay(waitMs)
            val current = exoPlayer ?: return@launch
            // Si mientras esperábamos se ha recuperado o el usuario ha cambiado
            // de canción, no hay nada que reintentar.
            if (current.playerError == null) return@launch
            if (current.currentMediaItem?.mediaId != mediaId) return@launch
            // `prepare()` conserva el playWhenReady: si estaba en pausa cuando
            // falló, se deja preparado pero en pausa.
            current.prepare()
        }
    }

    /**
     * La cola crece sin parar porque los modos son infinitos, así que se
     * recorta por detrás: se conservan unas pocas canciones ya escuchadas para
     * poder retroceder y se descarta el resto.
     */
    private fun trimHistory(player: Player) {
        val excess = player.currentMediaItemIndex - KEPT_HISTORY
        if (excess > 0) player.removeMediaItems(0, excess)
    }
}

/**
 * Lo que ve la sesión de medios, y con ella la notificación, la pantalla de
 * bloqueo, el coche y la propia app —que también manda por aquí, a través de
 * [PlayerConnection]—. Todas las órdenes de una persona pasan por este único
 * sitio, así que no hay manera de que el botón de la notificación y el de la
 * app se comporten distinto: antes cada camino se apañaba por su cuenta y
 * saltar de canción desde la notificación dejaba la música en silencio.
 *
 * Añade dos cosas al reproductor de serie:
 *
 * - Cambiar de canción a mano es una orden de escuchar: si estaba en pausa se
 *   reanuda. Media3 conserva el `playWhenReady` al saltar de pista, así que
 *   sin esto cambiaba el título y seguía el silencio.
 * - Reproducir después de un error —un enlace caducado, un corte de red—
 *   vuelve a preparar. El reproductor queda en reposo con el error puesto y
 *   ahí `play()` no hace nada: el botón parecía roto.
 *
 * El avance automático al acabar una canción no pasa por aquí: el servicio
 * trabaja contra el reproductor sin envolver.
 */
private class ResumeOnSkipPlayer(player: Player) : ForwardingPlayer(player) {

    override fun play() {
        if (playerError != null) prepare()
        super.play()
    }

    // Los cuatro comandos de salto, porque según de dónde venga la orden llega
    // uno u otro: la notificación manda `seekToNext`/`seekToPrevious` y la app
    // los de item.

    override fun seekToNext() {
        super.seekToNext()
        play()
    }

    override fun seekToNextMediaItem() {
        super.seekToNextMediaItem()
        play()
    }

    override fun seekToPrevious() {
        super.seekToPrevious()
        play()
    }

    override fun seekToPreviousMediaItem() {
        super.seekToPreviousMediaItem()
        play()
    }
}

/**
 * Los enlaces que firma el backend caducan a los seis minutos. Reabrir el
 * stream ya obtiene una firma nueva, pero para Media3 un 403 es un error
 * definitivo y no reintenta: si la firma vencía mientras se cargaba un trozo
 * —una canción larga, una pausa larga, una red lenta— la reproducción se
 * cortaba en seco. Aquí se declara reintentable para que el propio reintento
 * renueve la firma; el resto de errores mantiene el comportamiento de serie.
 */
@OptIn(UnstableApi::class)
private class SignedUrlErrorPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val cause = loadErrorInfo.exception
        val expired = cause is HttpDataSource.InvalidResponseCodeException &&
            (cause.responseCode == 401 || cause.responseCode == 403)
        return if (expired && loadErrorInfo.errorCount <= MAX_RETRIES) {
            RETRY_DELAY_MS
        } else {
            super.getRetryDelayMsFor(loadErrorInfo)
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 500L
    }
}
