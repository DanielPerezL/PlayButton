package com.zice.playbutton.data.local

import coil3.ImageLoader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las portadas guardadas, vistas desde fuera de Coil. Son dos cachés, la de
 * disco y la de memoria, y casi siempre hay que tocar las dos: lo que se está
 * viendo vive en la de memoria, y mientras siga ahí da igual lo que se borre
 * del disco.
 *
 * Es una interfaz por lo mismo que [SessionProvider]: quien la usa se prueba
 * sin arrastrar un ImageLoader, que necesita un contexto de Android.
 */
interface ImageCache {

    fun sizeBytes(): Long

    /** Vacía las portadas guardadas, en disco y en memoria. */
    fun clear()

    /**
     * Olvida portadas que el servidor ya no sirve.
     *
     * Hace falta porque se piden con `immutable` y un año de caducidad. Esa
     * promesa es cierta para el contenido —cambiar la portada de algo crea otra
     * fila, con otra URL— pero solo mientras la imagen exista: al quitarla
     * desde el panel, la URL pasa a dar 404 y aquí nadie se entera, porque con
     * `immutable` no se vuelve a preguntar en todo el año. La copia guardada
     * seguía pintándose en un dispositivo que la tuviera, y como la caché vive
     * en `filesDir` tampoco la barría el sistema al necesitar espacio.
     *
     * Quien llama decide qué está muerto: aquí no se puede saber, porque una
     * misma portada la comparten las canciones de un artista con sus playlists.
     */
    fun forget(urls: Collection<String>)
}

/**
 * La de verdad.
 *
 * No se borran portadas sueltas al quitar una descarga: una misma imagen la
 * comparten todas las canciones de un artista y sus playlists, así que borrar
 * "las de esta playlist" se llevaría por delante las de otras que siguen
 * descargadas. El tamaño ya lo acota la propia caché, que desaloja por uso.
 */
@Singleton
class CoilImageCache @Inject constructor(
    private val imageLoader: ImageLoader,
) : ImageCache {

    override fun sizeBytes(): Long = imageLoader.diskCache?.size ?: 0L

    /**
     * La de memoria hay que vaciarla a mano o el botón no parece hacer nada.
     * Lo que se está viendo ya está descodificado ahí, así que se sigue
     * pintando igual después de borrar el archivo, y encima al volver a
     * pintarse Coil lo guarda de nuevo en disco: la caché se rehacía sola con
     * lo que se acababa de tirar.
     */
    override fun clear() {
        imageLoader.diskCache?.clear()
        imageLoader.memoryCache?.clear()
    }

    override fun forget(urls: Collection<String>) {
        if (urls.isEmpty()) return

        val disk = imageLoader.diskCache
        val memory = imageLoader.memoryCache
        for (url in urls) {
            // En disco la clave es la propia URL. En memoria no: Coil le suma
            // el tamaño con el que se pidió, así que la misma portada está
            // guardada bajo varias claves —la fila de 40dp, la cabecera de
            // 64dp, la carátula del reproductor— y hay que buscarlas todas.
            disk?.remove(url)
            memory?.keys.orEmpty()
                .filter { it.key == url }
                .forEach { memory?.remove(it) }
        }
    }
}
