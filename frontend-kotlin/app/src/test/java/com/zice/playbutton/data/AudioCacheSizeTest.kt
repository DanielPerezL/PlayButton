package com.zice.playbutton.data

import com.zice.playbutton.data.local.AudioCacheSize
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioCacheSizeTest {

    /**
     * Las claves viajan a los ajustes del dispositivo: renombrar una le
     * cambiaría la opción a quien ya la tuviera elegida, sin avisar.
     */
    @Test
    fun `las claves guardadas no cambian`() {
        assertEquals("off", AudioCacheSize.Off.key)
        assertEquals("small", AudioCacheSize.Small.key)
        assertEquals("medium", AudioCacheSize.Medium.key)
        assertEquals("large", AudioCacheSize.Large.key)
    }

    @Test
    fun `una clave desconocida o ausente cae en la opcion por defecto`() {
        assertEquals(AudioCacheSize.Medium, AudioCacheSize.Default)
        assertEquals(AudioCacheSize.Default, AudioCacheSize.fromKey(null))
        assertEquals(AudioCacheSize.Default, AudioCacheSize.fromKey("enorme"))
    }

    @Test
    fun `los limites son los megas decimales que se ensenan al usuario`() {
        assertEquals(0L, AudioCacheSize.Off.limitBytes)
        assertEquals(150_000_000L, AudioCacheSize.Small.limitBytes)
        assertEquals(400_000_000L, AudioCacheSize.Medium.limitBytes)
        assertEquals(1_000_000_000L, AudioCacheSize.Large.limitBytes)
    }
}
