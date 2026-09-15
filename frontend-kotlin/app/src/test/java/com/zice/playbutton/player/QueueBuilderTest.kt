package com.zice.playbutton.player

import com.zice.playbutton.domain.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueueBuilderTest {

    private fun songs(vararg ids: Int) = ids.map { Song(it, "Artista $it - Canción $it") }

    @Test
    fun `colapsa duplicados consecutivos`() {
        val result = QueueBuilder.dedupeConsecutive(songs(1, 1, 2, 3, 3, 3, 4))
        assertEquals(listOf(1, 2, 3, 4), result.map { it.id })
    }

    @Test
    fun `mantiene repeticiones no consecutivas`() {
        val result = QueueBuilder.dedupeConsecutive(songs(1, 2, 1, 2))
        assertEquals(listOf(1, 2, 1, 2), result.map { it.id })
    }

    @Test
    fun `no empieza con la cancion que acaba de sonar`() {
        val result = QueueBuilder.dedupeConsecutive(songs(7, 8, 9), previousSongId = 7)
        assertEquals(listOf(8, 9), result.map { it.id })
    }

    @Test
    fun `si no hay ninguna cancion distinta conserva una`() {
        val result = QueueBuilder.dedupeConsecutive(songs(5, 5, 5), previousSongId = 5)
        assertEquals(listOf(5), result.map { it.id })
    }

    @Test
    fun `lista vacia se queda vacia`() {
        assertTrue(QueueBuilder.dedupeConsecutive(emptyList()).isEmpty())
        assertTrue(QueueBuilder.shuffled(emptyList()).isEmpty())
    }

    @Test
    fun `barajar conserva todas las canciones cuando no hay duplicados`() {
        val original = songs(1, 2, 3, 4, 5)
        val result = QueueBuilder.shuffled(original, random = Random(1234))
        assertEquals(original.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun `arranca siempre por la cancion pulsada`() {
        val lista = songs(1, 2, 3, 4, 5)
        // Con muchas semillas: la primera nunca puede depender del azar.
        repeat(100) { seed ->
            val result = QueueBuilder.startingWith(lista, lista[3], Random(seed))
            assertEquals(4, result.first().id)
            assertEquals(lista.map { it.id }.toSet(), result.map { it.id }.toSet())
        }
    }

    @Test
    fun `al arrancar por una cancion no la repite mas adelante`() {
        val result = QueueBuilder.startingWith(songs(1, 2, 3), songs(2)[0], Random(7))
        assertEquals(2, result.first().id)
        assertEquals(1, result.count { it.id == 2 })
    }

    @Test
    fun `arrancar por la unica cancion de la lista funciona`() {
        val unica = songs(9)
        val result = QueueBuilder.startingWith(unica, unica[0])
        assertEquals(listOf(9), result.map { it.id })
    }

    @Test
    fun `empieza por la cancion pulsada`() {
        val list = songs(1, 2, 3, 4, 5)
        val result = QueueBuilder.startingWith(list, list[3], Random(7))
        assertEquals(4, result.first().id)
    }

    @Test
    fun `empezar por una cancion conserva el resto de la playlist`() {
        val list = songs(1, 2, 3, 4, 5)
        val result = QueueBuilder.startingWith(list, list[2], Random(7))
        assertEquals(list.map { it.id }.toSet(), result.map { it.id }.toSet())
        assertEquals(list.size, result.size)
    }

    @Test
    fun `empezar por una cancion la pone primera sea cual sea el barajado`() {
        val list = songs(1, 2, 3, 4, 5, 6)
        repeat(200) { seed ->
            val result = QueueBuilder.startingWith(list, list[4], Random(seed))
            assertEquals("Debe arrancar siempre por la pulsada", 5, result.first().id)
            result.zipWithNext().forEach { (a, b) ->
                assertTrue("No puede haber dos iguales seguidas", a.id != b.id)
            }
        }
    }

    @Test
    fun `empezar por la unica cancion de la playlist`() {
        val list = songs(9)
        val result = QueueBuilder.startingWith(list, list.first())
        assertEquals(listOf(9), result.map { it.id })
    }

    @Test
    fun `barajar nunca deja dos iguales seguidas ni repite la anterior`() {
        // Con muchas semillas distintas: el resultado debe cumplir siempre la
        // invariante, no solo con una ordenación afortunada.
        repeat(200) { seed ->
            val result = QueueBuilder.shuffled(
                songs(1, 1, 2, 2, 3),
                previousSongId = 3,
                random = Random(seed),
            )
            assertTrue("La cola no puede empezar con la canción anterior", result.first().id != 3)
            result.zipWithNext().forEach { (a, b) ->
                assertTrue("No puede haber dos iguales seguidas: ${result.map { it.id }}", a.id != b.id)
            }
        }
    }
}
