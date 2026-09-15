package com.zice.playbutton.data.local

import coil3.ImageLoader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La caché de disco de las portadas, vista desde fuera de Coil.
 *
 * No se borran portadas sueltas al quitar una descarga: una misma imagen la
 * comparten todas las canciones de un artista y sus playlists, así que borrar
 * "las de esta playlist" se llevaría por delante las de otras que siguen
 * descargadas. El tamaño ya lo acota la propia caché, que desaloja por uso.
 */
@Singleton
class ImageCache @Inject constructor(
    private val imageLoader: ImageLoader,
) {

    fun sizeBytes(): Long = imageLoader.diskCache?.size ?: 0L

    fun clear() {
        imageLoader.diskCache?.clear()
    }
}
