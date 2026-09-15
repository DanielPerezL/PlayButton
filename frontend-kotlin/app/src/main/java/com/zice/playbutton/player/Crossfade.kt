package com.zice.playbutton.player

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * La aritmética del encadenado, separada del reproductor para poder fijarla en
 * los tests: cuánto dura cada fundido, dónde se corta la pista que sale y qué
 * volumen toca en cada instante.
 *
 * La ventana que pide el usuario —cinco segundos— es el total de la
 * transición, y se reparte en dos mitades iguales: la primera apaga la canción
 * que acaba y la segunda sube la que entra. Como no pueden solaparse, a cambio
 * se recorta una mitad del final de la primera y otra del principio de la
 * segunda; ver [TrackFader] para el porqué.
 */
internal object Crossfade {

    /**
     * La ventana de transición nunca ocupa más de un tercio de la canción: en
     * una pista muy corta, encadenar cinco segundos sería encadenarla entera.
     * Cada mitad se queda por tanto en un sexto como máximo.
     */
    const val MAX_FRACTION = 3

    /**
     * Media ventana: lo que dura cada fundido y, a la vez, lo que se recorta de
     * cada extremo. Con [durationMs] desconocida —un stream sin duración— se
     * respeta lo configurado.
     */
    fun rampFor(configuredMs: Long, durationMs: Long): Long {
        val window =
            if (durationMs > 0) min(configuredMs, durationMs / MAX_FRACTION) else configuredMs
        return (window / 2).coerceAtLeast(0L)
    }

    /** Instante en el que la pista se corta para dar paso a la siguiente. */
    fun cutPosition(durationMs: Long, rampMs: Long): Long = durationMs - rampMs

    /** Volumen de la que entra, que sube desde [fadeInFrom]. */
    fun fadeInGain(positionMs: Long, fadeInFrom: Long, rampMs: Long): Float =
        equalPower(positionMs - fadeInFrom, rampMs)

    /** Volumen de la que sale, que llega a cero justo en el corte. */
    fun fadeOutGain(positionMs: Long, durationMs: Long, rampMs: Long): Float =
        equalPower(cutPosition(durationMs, rampMs) - positionMs, rampMs)

    /**
     * Curva de potencia constante (un cuarto de seno, la de cualquier mesa de
     * mezclas) para que la transición no se perciba como un bajón: una rampa
     * lineal sí se oye hundirse por la mitad.
     */
    fun equalPower(elapsedMs: Long, totalMs: Long): Float =
        if (totalMs <= 0L) 1f else sin((PI / 2) * elapsedMs.coerceIn(0L, totalMs) / totalMs).toFloat()
}
