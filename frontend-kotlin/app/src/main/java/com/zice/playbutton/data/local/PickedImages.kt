package com.zice.playbutton.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que el selector de fotos ha devuelto, ya leído. */
class PickedImage(val mime: String, val bytes: ByteArray)

/**
 * Lee la imagen que el usuario elige en el selector de fotos.
 *
 * Existe para que el repositorio no tenga que sostener un Context: lo que
 * devuelve el selector es un `content://` sin nombre ni contenido, y los dos
 * hay que pedírselos al sistema.
 */
interface PickedImages {
    /** `null` si no se puede leer, que es lo mismo que no haber elegido nada. */
    fun read(uri: Uri): PickedImage?
}

@Singleton
class ContentResolverPickedImages @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PickedImages {

    override fun read(uri: Uri): PickedImage? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: return null
        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        return PickedImage(mime, bytes)
    }
}
