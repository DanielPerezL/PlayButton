package com.zice.playbutton.data

import com.zice.playbutton.data.repo.songsOnlyIn
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineLibraryTest {

    @Test
    fun `sin otras descargas se borra todo el audio de la playlist`() {
        assertEquals(listOf(1, 2, 3), songsOnlyIn(own = listOf(1, 2, 3), others = emptyList()))
    }

    /** El caso que importa: una canción en dos playlists descargadas. */
    @Test
    fun `lo compartido con otra playlist descargada se conserva`() {
        val toRemove = songsOnlyIn(
            own = listOf(1, 2, 3, 4),
            others = listOf(listOf(2, 9), listOf(4)),
        )
        assertEquals(listOf(1, 3), toRemove)
    }

    @Test
    fun `si otra playlist tiene las mismas canciones no se borra ninguna`() {
        assertEquals(emptyList<Int>(), songsOnlyIn(listOf(1, 2), listOf(listOf(1, 2))))
    }
}
