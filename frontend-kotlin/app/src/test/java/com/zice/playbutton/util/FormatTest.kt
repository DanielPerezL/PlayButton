package com.zice.playbutton.util

import com.zice.playbutton.data.remote.ServerUrl
import com.zice.playbutton.domain.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {

    @Test
    fun `formatea duraciones`() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:00", formatTime(-5))
        assertEquals("0:07", formatTime(7_400))
        assertEquals("3:45", formatTime(225_000))
        assertEquals("61:01", formatTime(3_661_000))
    }

    @Test
    fun `parte el nombre en artista y titulo`() {
        val song = Song(1, "Extremoduro - So payaso")
        assertEquals("Extremoduro", song.artist)
        assertEquals("So payaso", song.title)
    }

    @Test
    fun `sin separador el nombre entero es el titulo`() {
        val song = Song(1, "Instrumental")
        assertNull(song.artist)
        assertEquals("Instrumental", song.title)
    }

    @Test
    fun `solo parte por el primer separador`() {
        val song = Song(1, "AC/DC - Highway - to Hell")
        assertEquals("AC/DC", song.artist)
        assertEquals("Highway - to Hell", song.title)
    }

    @Test
    fun `normaliza la url del servidor`() {
        assertEquals("https://mi.servidor.com/api", ServerUrl.normalize("mi.servidor.com", false))
        assertEquals("https://mi.servidor.com/api", ServerUrl.normalize("https://mi.servidor.com/", false))
        assertEquals("https://mi.servidor.com/api", ServerUrl.normalize("mi.servidor.com/api", false))
        // En release el texto claro se promociona a HTTPS.
        assertEquals("https://mi.servidor.com/api", ServerUrl.normalize("http://mi.servidor.com", false))
        // En debug, sin esquema se asume HTTP para poder apuntar a un backend local.
        assertEquals("http://10.0.2.2:5000/api", ServerUrl.normalize("http://10.0.2.2:5000", true))
        assertEquals("http://10.0.2.2:5000/api", ServerUrl.normalize("10.0.2.2:5000", true))
        // Pero un HTTPS explícito se respeta también en debug.
        assertEquals("https://mi.servidor.com/api", ServerUrl.normalize("https://mi.servidor.com", true))
        assertNull(ServerUrl.normalize("   ", false))
    }

    @Test
    fun `muestra la url sin esquema ni sufijo`() {
        assertEquals("mi.servidor.com", ServerUrl.display("https://mi.servidor.com/api"))
        assertEquals("", ServerUrl.display(null))
    }
}
