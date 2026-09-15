package com.zice.playbutton.player

import com.zice.playbutton.domain.Song
import kotlin.random.Random

/**
 * Prepara la cola de reproducción.
 *
 * Una playlist puede contener la misma canción más de una vez, y al barajar
 * pueden quedar dos copias seguidas; además, cuando una tanda termina y se
 * carga la siguiente (Modo Zen o playlist en bucle) la primera canción nueva
 * puede coincidir con la última que acaba de sonar. En ambos casos el usuario
 * oiría dos veces seguidas lo mismo, así que se colapsa: solo se salta si hay
 * otra canción distinta con la que continuar.
 */
object QueueBuilder {

    /** Elimina repeticiones consecutivas, incluida la que enlaza con [previousSongId]. */
    fun dedupeConsecutive(songs: List<Song>, previousSongId: Int? = null): List<Song> {
        if (songs.isEmpty()) return songs

        val result = ArrayList<Song>(songs.size)
        var lastId = previousSongId
        for (song in songs) {
            if (song.id != lastId) {
                result += song
                lastId = song.id
            }
        }
        // Si toda la tanda era la misma canción que ya sonaba, no hay ninguna
        // distinta con la que continuar: mejor repetirla que quedarse en silencio.
        return result.ifEmpty { listOf(songs.first()) }
    }

    /**
     * Baraja y deja la cola lista para reproducir. Si la primera canción
     * coincide con la que acaba de sonar, se intercambia por la siguiente
     * distinta en lugar de volver a barajar.
     */
    fun shuffled(
        songs: List<Song>,
        previousSongId: Int? = null,
        random: Random = Random.Default,
    ): List<Song> {
        if (songs.isEmpty()) return emptyList()

        val shuffled = songs.shuffled(random).toMutableList()
        if (previousSongId != null && shuffled.first().id == previousSongId) {
            val other = shuffled.indexOfFirst { it.id != previousSongId }
            if (other > 0) {
                shuffled[0] = shuffled.set(other, shuffled[0])
            }
        }
        return dedupeConsecutive(shuffled, previousSongId)
    }

    /**
     * Cola que arranca por una canción concreta —la que ha pulsado el usuario—
     * y sigue con el resto barajado, como hace el resto de la app.
     */
    fun startingWith(
        songs: List<Song>,
        first: Song,
        random: Random = Random.Default,
    ): List<Song> {
        if (songs.isEmpty()) return listOf(first)

        val rest = songs.filter { it.id != first.id }.shuffled(random)
        return dedupeConsecutive(listOf(first) + rest)
    }
}
