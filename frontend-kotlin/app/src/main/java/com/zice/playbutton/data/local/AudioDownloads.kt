package com.zice.playbutton.data.local

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.zice.playbutton.data.local.AudioStore.holdsSong
import com.zice.playbutton.data.local.AudioStore.removeEverything
import com.zice.playbutton.data.local.AudioStore.removeSongs
import com.zice.playbutton.data.local.AudioStore.usage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las canciones que el usuario ha pedido guardar, playlist a playlist.
 *
 * A diferencia de [AudioCache] esto no se descarta nunca: descargar una
 * playlist y encontrarse con que ha desaparecido por no haberla escuchado en
 * una semana es exactamente lo contrario de lo que se pide al descargarla.
 * Solo se va cuando el usuario borra esa descarga, vacía el almacenamiento o
 * cierra sesión.
 *
 * Por eso vive en `filesDir` y no en `cacheDir`: al sistema no se le puede
 * dejar la puerta abierta para reclamar este espacio cuando el móvil ande
 * justo, y tampoco tiene sentido que «Borrar caché» de los ajustes de Android
 * se lo lleve. Y por eso mismo no cuenta contra el límite de la caché: son
 * dos espacios con dueños distintos.
 */
@OptIn(UnstableApi::class)
@Singleton
class AudioDownloads @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        const val DIRECTORY = "downloads"
    }

    private val directory: File by lazy { File(context.filesDir, DIRECTORY) }
    private val databaseProvider by lazy { StandaloneDatabaseProvider(context) }

    private var store: SimpleCache? = null

    /**
     * El almacén, o `null` si no se ha podido abrir. Sin evictor: nada de lo
     * que entra aquí se descarta por su cuenta.
     */
    @Synchronized
    fun storeOrNull(): Cache? {
        store?.let { return it }
        return runCatching { SimpleCache(directory, NoOpCacheEvictor(), databaseProvider) }
            .getOrNull()
            ?.also { store = it }
    }

    /** Si la canción está descargada y por tanto no puede desaparecer sola. */
    @Synchronized
    fun holds(songId: Int): Boolean = storeOrNull()?.holdsSong(songId) == true

    @Synchronized
    fun count(songIds: List<Int>): Int {
        val current = storeOrNull() ?: return 0
        return songIds.count { current.holdsSong(it) }
    }

    @Synchronized
    fun usage(): AudioUsage = storeOrNull()?.usage() ?: AudioUsage()

    /** Espacio libre del dispositivo, para no llenarlo del todo descargando. */
    fun freeSpaceBytes(): Long = runCatching {
        directory.mkdirs()
        directory.usableSpace
    }.getOrDefault(0L)

    @Synchronized
    fun remove(songIds: Collection<Int>) {
        storeOrNull()?.removeSongs(songIds)
    }

    @Synchronized
    fun clear() {
        val current = store
        if (current != null) {
            current.removeEverything()
            return
        }
        runCatching { SimpleCache.delete(directory, databaseProvider) }
    }
}
