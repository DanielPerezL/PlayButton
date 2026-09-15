package com.zice.playbutton.data.local

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata

/**
 * Lo común a los dos sitios donde puede acabar el audio de una canción: la
 * caché, que se descarta sola cuando hace falta sitio, y las descargas, que
 * solo se van si el usuario las borra.
 *
 * Comparten formato —el mismo tipo de almacén de Media3 y las mismas claves—
 * porque son el mismo audio guardado con dos políticas distintas. Lo que
 * cambia es quién decide cuándo se borra.
 */
@OptIn(UnstableApi::class)
object AudioStore {

    /**
     * Clave con la que se guarda una canción. Es el id y no la URL: la firmada
     * cambia en cada petición, y así lo guardado se sigue encontrando aunque
     * cambie el esquema del URI interno.
     */
    fun keyFor(songId: Int): String = "song/$songId"

    /** Si la canción está entera y suena sin tocar la red. */
    fun Cache.holdsSong(songId: Int): Boolean {
        val key = keyFor(songId)
        val length = getContentMetadata(key)
            .get(ContentMetadata.KEY_CONTENT_LENGTH, C.LENGTH_UNSET.toLong())
        return length > 0L && isCached(key, 0L, length)
    }

    /** Lo que ocupa y cuántas canciones son; cada clave es una canción. */
    fun Cache.usage(): AudioUsage = AudioUsage(bytes = cacheSpace, songs = keys.size)

    fun Cache.removeSongs(songIds: Collection<Int>) {
        songIds.forEach { removeResource(keyFor(it)) }
    }

    fun Cache.removeEverything() {
        keys.toList().forEach(::removeResource)
    }
}

/** Lo que un almacén de audio ocupa ahora mismo, para poder decírselo al usuario. */
data class AudioUsage(
    val bytes: Long = 0L,
    val songs: Int = 0,
)
