package com.zice.playbutton.player

import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * Encadena una canción con la siguiente en lugar de cortar en seco.
 *
 * Con un solo reproductor no hay manera de que dos pistas suenen a la vez, así
 * que el encadenado es de volumen: la que acaba se apaga y la que entra sube
 * desde cero.
 *
 * Los dos fundidos no pueden solaparse, así que van uno detrás de otro y el
 * truco para que suenen como si se solapasen está en **recortar los
 * extremos**: la canción que sale se corta en cuanto su fundido llega a cero,
 * sin esperar al último instante grabado, y la que entra arranca ya metida en
 * la pista en lugar de en el segundo cero. Sin ese recorte la transición se
 * oía del doble de larga —apagarse entera y volver a subir— y encima ocupaba
 * los dos trozos más flojos de cualquier grabación: la cola que se desvanece y
 * la intro que aún no ha entrado. Recortándolos, las dos mitades se pegan y el
 * oído lo lee como un solape.
 *
 * El volumen no se guarda en ningún estado propio: se calcula en cada tick a
 * partir de la posición de la canción. Así una pausa, una búsqueda o un salto
 * de pista dejan el volumen correcto sin tener que deshacer nada a mano.
 */
class TrackFader(
    private val player: Player,
    scope: CoroutineScope,
    /** Ventana completa de la transición, o 0 si el usuario la tiene desactivada. */
    private val fadeDurationMs: () -> Long,
) {
    private companion object {
        const val TICK_MS = 100L
    }

    /**
     * Solo se entra con fundido cuando la canción anterior se ha apagado sola.
     * Si el usuario ha pulsado una canción, la quiere oír ya y a su volumen.
     */
    private var fadeInPending = false

    /** Posición desde la que sube el volumen; no es 0 si se recortó la intro. */
    private var fadeInFrom = 0L

    /** Queda por saltarse la intro de la pista que acaba de entrar. */
    private var headTrimPending = false

    /**
     * Canción a la que ha saltado este propio encadenado. El reproductor avisa
     * de ese salto igual que del de un usuario y hay que poder distinguirlos:
     * tratarlo como manual cortaría el fundido de entrada a medias.
     */
    private var chainedMediaId: String? = null

    private var applied = -1f

    init {
        scope.launch {
            while (true) {
                update()
                delay(TICK_MS)
            }
        }
    }

    /** La canción ha empezado al terminar la anterior: entra con fundido. */
    fun onAutoTransition() {
        fadeInPending = true
        fadeInFrom = 0L
    }

    /** Ha sido el usuario quien ha elegido este punto: nada de fundidos. */
    fun onManualTransition() {
        fadeInPending = false
        fadeInFrom = 0L
        headTrimPending = false
        chainedMediaId = null
        apply(1f)
    }

    /**
     * Cierto si el cambio a [mediaId] del que informa el reproductor lo ha
     * provocado el propio encadenado, y no el usuario. Se compara la canción
     * de destino y no el índice porque la cola se recorta por detrás mientras
     * suena, y los índices se mueven bajo los pies.
     */
    fun isChainedTransition(mediaId: String?): Boolean =
        fadeInPending && mediaId != null && mediaId == chainedMediaId

    private fun update() {
        val configured = fadeDurationMs()
        if (configured <= 0L) {
            if (fadeInPending || headTrimPending) onManualTransition()
            apply(1f)
            return
        }
        // Mientras está pausado o buscando no se toca el volumen: la posición
        // que se leería no es la que va a sonar al reanudar.
        if (!player.isPlaying || player.playbackState != Player.STATE_READY) return

        val position = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration
        val ramp = Crossfade.rampFor(configured, duration)
        if (ramp <= 0L) {
            apply(1f)
            return
        }

        // El recorte de la intro se decide aquí y no al saltar porque hasta que
        // la pista no está preparada no se conoce su duración, y en una pista
        // corta no hay margen para tirar nada.
        if (headTrimPending) {
            headTrimPending = false
            if (duration > 0 && position < ramp) {
                fadeInFrom = ramp
                player.seekTo(ramp)
                return
            }
        }

        var gain = 1f
        if (fadeInPending) {
            if (position - fadeInFrom >= ramp) {
                fadeInPending = false
                chainedMediaId = null
            } else {
                gain = min(gain, Crossfade.fadeInGain(position, fadeInFrom, ramp))
            }
        }

        // Solo se recorta el final si hay algo con lo que encadenar; si la cola
        // se acaba aquí, la canción termina como está grabada.
        if (duration > 0 && player.hasNextMediaItem()) {
            if (position >= Crossfade.cutPosition(duration, ramp)) {
                chainToNext()
                return
            }
            gain = min(gain, Crossfade.fadeOutGain(position, duration, ramp))
        }

        apply(gain)
    }

    /** Corta la canción actual —ya apagada— y entra en la siguiente. */
    private fun chainToNext() {
        val next = player.nextMediaItemIndex
        if (next == C.INDEX_UNSET) return

        // Las banderas se dejan puestas antes del salto: el reproductor puede
        // avisar de la discontinuidad dentro de la propia llamada.
        chainedMediaId = player.getMediaItemAt(next).mediaId
        fadeInPending = true
        fadeInFrom = 0L
        headTrimPending = true
        apply(0f)
        player.seekToNextMediaItem()
    }

    private fun apply(volume: Float) {
        if (abs(volume - applied) < 0.01f) return
        applied = volume
        player.volume = volume
    }
}
