package com.zice.playbutton.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadeTest {

    private companion object {
        const val WINDOW = 5_000L
        const val THREE_MINUTES = 180_000L
    }

    @Test
    fun `la ventana se reparte en dos mitades iguales`() {
        assertEquals(2_500L, Crossfade.rampFor(WINDOW, THREE_MINUTES))
    }

    @Test
    fun `en una cancion corta la ventana no pasa de un tercio`() {
        // 6 s de cancion: 2 s de ventana, 1 s por fundido.
        assertEquals(1_000L, Crossfade.rampFor(WINDOW, 6_000L))
    }

    @Test
    fun `sin duracion conocida se respeta lo configurado`() {
        assertEquals(2_500L, Crossfade.rampFor(WINDOW, C_TIME_UNSET))
    }

    @Test
    fun `el corte deja fuera el ultimo tramo de la cancion`() {
        val ramp = Crossfade.rampFor(WINDOW, THREE_MINUTES)
        assertEquals(THREE_MINUTES - 2_500L, Crossfade.cutPosition(THREE_MINUTES, ramp))
    }

    @Test
    fun `el fundido de salida arranca a tope y se apaga justo en el corte`() {
        val ramp = Crossfade.rampFor(WINDOW, THREE_MINUTES)
        val cut = Crossfade.cutPosition(THREE_MINUTES, ramp)

        assertEquals(1f, Crossfade.fadeOutGain(cut - ramp, THREE_MINUTES, ramp), 0.001f)
        assertEquals(0f, Crossfade.fadeOutGain(cut, THREE_MINUTES, ramp), 0.001f)
        // Lo que quede detras del corte ya no suena.
        assertEquals(0f, Crossfade.fadeOutGain(THREE_MINUTES, THREE_MINUTES, ramp), 0.001f)
    }

    @Test
    fun `antes de la ventana la cancion suena a su volumen`() {
        val ramp = Crossfade.rampFor(WINDOW, THREE_MINUTES)
        assertEquals(1f, Crossfade.fadeOutGain(0L, THREE_MINUTES, ramp), 0.001f)
    }

    @Test
    fun `el fundido de entrada sube desde el punto en que se recorto la intro`() {
        val ramp = Crossfade.rampFor(WINDOW, THREE_MINUTES)

        // La pista arranca en `ramp`, no en cero: ahi es donde el volumen es 0.
        assertEquals(0f, Crossfade.fadeInGain(ramp, ramp, ramp), 0.001f)
        assertEquals(1f, Crossfade.fadeInGain(ramp * 2, ramp, ramp), 0.001f)
    }

    /**
     * Las dos mitades tienen que sumar potencia constante en el punto de
     * empalme: es lo que evita que la transicion se oiga como un bajon.
     */
    @Test
    fun `las dos mitades se cruzan a media potencia`() {
        val ramp = 2_500L
        val half = Crossfade.equalPower(ramp / 2, ramp)
        assertEquals(0.707f, half, 0.01f)
    }

    @Test
    fun `la curva no es lineal en su tramo medio`() {
        val ramp = 2_500L
        // Una rampa lineal daria 0.5 a la mitad; la de potencia constante sube antes.
        assertTrue(Crossfade.equalPower(ramp / 2, ramp) > 0.6f)
    }

    @Test
    fun `una ventana degenerada no altera el volumen`() {
        assertEquals(1f, Crossfade.equalPower(0L, 0L), 0.001f)
    }
}

/** `C.TIME_UNSET` de Media3, que no esta disponible en los tests de JVM. */
private const val C_TIME_UNSET = Long.MIN_VALUE + 1
