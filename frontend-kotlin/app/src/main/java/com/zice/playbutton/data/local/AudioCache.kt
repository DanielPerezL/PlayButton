package com.zice.playbutton.data.local

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.SimpleCache
import com.zice.playbutton.data.local.AudioStore.removeEverything
import com.zice.playbutton.data.local.AudioStore.removeSongs
import com.zice.playbutton.data.local.AudioStore.usage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.TreeSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuánto se permite guardar en el dispositivo de las canciones ya escuchadas.
 *
 * Los límites están en megas decimales —150 MB son 150.000.000 bytes— porque
 * es así como Android cuenta el almacenamiento de cara al usuario: si se
 * usaran múltiplos de 1024, la opción «150 MB» aparecería como 157 MB en los
 * ajustes del sistema y el número dejaría de cuadrar.
 */
enum class AudioCacheSize(val key: String, val limitBytes: Long) {
    Off("off", 0L),
    Small("small", 150_000_000L),
    Medium("medium", 400_000_000L),
    Large("large", 1_000_000_000L),
    ;

    companion object {
        /** Lo bastante para una jornada completa sin volver a descargar nada. */
        val Default = Medium

        /** La clave se guarda en los ajustes: no se puede renombrar. */
        fun fromKey(key: String?): AudioCacheSize =
            entries.firstOrNull { it.key == key } ?: Default
    }
}

/**
 * Caché en disco de las canciones escuchadas. Sin ella cada reproducción se
 * descarga entera otra vez, y escuchar la misma playlist en bucle —el uso
 * normal en un local— significaba bajar el mismo audio una y otra vez.
 *
 * Se llena sola con lo que suena y se descarta sola cuando llega al límite,
 * empezando por lo que lleva más tiempo sin oírse. Lo que el usuario descarga
 * a mano no pasa por aquí: eso va a [AudioDownloads], que no descarta nada y
 * no gasta de este límite.
 *
 * Vive en `cacheDir`, así que «Borrar caché» en los ajustes de Android la
 * vacía y el sistema puede reclamar ese espacio si el móvil se queda sin
 * hueco; el índice se reconstruye solo si eso pasa.
 */
@OptIn(UnstableApi::class)
@Singleton
class AudioCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
) {
    private companion object {
        const val DIRECTORY = "audio"
    }

    private val directory: File by lazy { File(context.cacheDir, DIRECTORY) }
    private val databaseProvider by lazy { StandaloneDatabaseProvider(context) }
    private val evictor = VariableLimitEvictor(AudioCacheSize.Default.limitBytes)

    private var cache: SimpleCache? = null

    /** Opción activa. Nula hasta que se lee de los ajustes por primera vez. */
    private var size: AudioCacheSize? = null

    /**
     * La caché con la que leer y escribir, o `null` si el usuario la tiene
     * desactivada. La llama el hilo de carga de ExoPlayer al abrir cada
     * stream, así que el trabajo pesado —abrir el índice— pasa una sola vez.
     */
    @Synchronized
    fun cacheOrNull(): Cache? {
        if (currentSize() == AudioCacheSize.Off) return null
        return open()
    }

    /**
     * Aplica la opción elegida. El límite es una variable del propio evictor
     * en lugar del parámetro de construcción que usa Media3: así cambiar de
     * opción no obliga a cerrar y reabrir la caché por debajo de la canción
     * que esté sonando, que se cortaría.
     */
    @Synchronized
    fun setSize(size: AudioCacheSize) {
        if (this.size == size) return
        this.size = size

        if (size == AudioCacheSize.Off) {
            // Desactivarla no deja restos: lo guardado hasta ahora se borra.
            evictor.setLimit(0L, cache)
            clearInternal()
            return
        }

        // Al bajar de opción se descarta lo menos escuchado en el momento, no
        // en la siguiente descarga: el usuario acaba de pedir ese espacio.
        evictor.setLimit(size.limitBytes, if (cache != null) cache else open())
    }

    /** Al cerrar sesión no debe quedar audio del usuario anterior. */
    @Synchronized
    fun clear() = clearInternal()

    /**
     * Lo que hay guardado. Abre la caché si aún no lo estaba —hay que leer el
     * índice para saberlo—, así que conviene llamarla fuera del hilo
     * principal.
     */
    @Synchronized
    fun usage(): AudioUsage {
        if (currentSize() == AudioCacheSize.Off) return AudioUsage()
        return open()?.usage() ?: AudioUsage()
    }

    /**
     * Saca estas canciones de la caché. Se usa al descargarlas: una vez están
     * en el almacenamiento, la copia de la caché no se va a volver a leer
     * —lo descargado se consulta antes— y solo estaría ocupando sitio que le
     * corresponde a otra canción.
     */
    @Synchronized
    fun forget(songIds: Collection<Int>) {
        cache?.removeSongs(songIds)
    }

    /**
     * La opción guardada. El servicio de reproducción la observa y la aplica
     * al arrancar, así que aquí casi nunca hay que ir a buscarla; la lectura
     * bloqueante es la red de seguridad para la primera vez, y es un acceso
     * local de unos milisegundos.
     */
    private fun currentSize(): AudioCacheSize {
        size?.let { return it }
        val stored = runBlocking { settingsStore.currentAudioCacheSize() }
        setSize(stored)
        return stored
    }

    private fun open(): SimpleCache? {
        cache?.let { return it }
        return runCatching { SimpleCache(directory, evictor, databaseProvider) }
            .getOrNull()
            ?.also { cache = it }
    }

    private fun clearInternal() {
        val current = cache
        if (current != null) {
            current.removeEverything()
            return
        }
        // Sin instancia abierta se borra el directorio a pelo, que es más
        // barato que abrir el índice solo para vaciarlo.
        runCatching { SimpleCache.delete(directory, databaseProvider) }
    }
}

/**
 * Descarta lo menos escuchado en cuanto se pasa del límite, como el
 * `LeastRecentlyUsedCacheEvictor` de Media3, pero con el límite en una
 * variable en vez de fijado al construirlo.
 *
 * Los métodos de esta interfaz no llevan cerrojo propio, igual que en la
 * implementación de Media3: `SimpleCache` los llama siempre desde dentro de
 * los suyos, que están sincronizados sobre sí mismo. Poner aquí otro cerrojo
 * sería justo lo que no hay que hacer: un hilo cambiando el límite tomaría
 * primero el del evictor y luego el de la caché al descartar, mientras otro
 * que estuviera guardando audio los tomaría en el orden contrario, y basta con
 * que se crucen para que la reproducción se quede colgada. Por eso [setLimit],
 * que es el único que entra de fuera, se sincroniza sobre la propia caché: así
 * todo el mundo pide los cerrojos en el mismo orden.
 */
@OptIn(UnstableApi::class)
private class VariableLimitEvictor(@Volatile private var limitBytes: Long) : CacheEvictor {

    private val spans = TreeSet<CacheSpan>(::compareByLastTouch)
    private var currentSize = 0L

    fun setLimit(bytes: Long, cache: Cache?) {
        if (cache == null) {
            // Sin caché abierta no hay nada que descartar: basta con dejar el
            // techo puesto para cuando se abra.
            limitBytes = bytes
            return
        }
        synchronized(cache) {
            limitBytes = bytes
            evict(cache, 0L)
        }
    }

    /** Hace falta para saber qué es «lo menos escuchado». */
    override fun requiresCacheSpanTouches(): Boolean = true

    /** El tamaño se reconstruye por los `onSpanAdded` de la inicialización. */
    override fun onCacheInitialized() = Unit

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != C.LENGTH_UNSET.toLong()) evict(cache, length)
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        spans.add(span)
        currentSize += span.length
        evict(cache, 0L)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        spans.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    private fun evict(cache: Cache, requiredSpace: Long) {
        while (currentSize + requiredSpace > limitBytes && spans.isNotEmpty()) {
            // `removeSpan` vuelve por `onSpanRemoved`, que descuenta el tamaño.
            cache.removeSpan(spans.first())
        }
    }
}

/**
 * Por antigüedad de uso y, a igualdad de fecha, por el orden natural del
 * fragmento: dos huecos distintos con la misma marca de tiempo tienen que
 * seguir siendo dos entradas y no colapsar en una.
 */
private fun compareByLastTouch(a: CacheSpan, b: CacheSpan): Int {
    val byTouch = a.lastTouchTimestamp.compareTo(b.lastTouchTimestamp)
    return if (byTouch != 0) byTouch else a.compareTo(b)
}
