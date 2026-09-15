package com.zice.playbutton.ui

import com.zice.playbutton.ui.screens.detail.PlaylistStorageState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La diferencia entre tener el audio y tener la playlist descargada: lo
 * primero pasa solo por compartir canciones con otras listas, y el botón no
 * puede darlo por descargado hasta que el usuario lo pida.
 */
class PlaylistStorageStateTest {

    @Test
    fun `pedida y entera cuenta como descargada`() {
        val state = PlaylistStorageState(savedSongs = 3, isRegistered = true)
        assertTrue(state.isDownloaded(total = 3))
    }

    /** El caso que importa: todo su audio está, pero por otras playlists. */
    @Test
    fun `entera sin haberla pedido no cuenta como descargada`() {
        val state = PlaylistStorageState(savedSongs = 3, isRegistered = false)
        assertTrue(state.isComplete(total = 3))
        assertFalse(state.isDownloaded(total = 3))
    }

    /** Al añadirle canciones nuevas deja de estarlo aunque siga registrada. */
    @Test
    fun `si le faltan canciones deja de contar como descargada`() {
        val state = PlaylistStorageState(savedSongs = 3, isRegistered = true)
        assertFalse(state.isDownloaded(total = 5))
    }
}
