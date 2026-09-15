package com.zice.playbutton.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import com.zice.playbutton.data.local.AudioCache
import com.zice.playbutton.data.local.AudioDownloads
import com.zice.playbutton.data.local.AudioStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * De dónde sale el audio: descargas → caché → firma → red.
 *
 * Los dos almacenes se consultan en ese orden y por un motivo concreto: lo
 * descargado se sirve **en solo lectura**, de modo que escuchar una playlist
 * descargada no deja una segunda copia en la caché ni gasta su límite. Solo lo
 * que no está descargado pasa por la caché, que sí guarda al leer.
 *
 * Que la caché vaya por encima del resolutor de firmas es lo que hace que todo
 * esto funcione: lo que ve —y por tanto la clave con la que guarda— es el URI
 * interno `playbutton://song/{id}`, no la URL firmada, que lleva un token
 * distinto en cada petición y no serviría de clave. Además así lo que ya está
 * guardado ni pasa por la red ni gasta una llamada a la API.
 *
 * Vive aquí y no dentro del servicio porque descargar una playlist tiene que
 * bajar el audio exactamente igual que lo baja el reproductor: mismas claves y
 * mismos almacenes.
 */
@OptIn(UnstableApi::class)
@Singleton
class AudioSources @Inject constructor(
    private val audioCache: AudioCache,
    private val audioDownloads: AudioDownloads,
    private val signedUrlResolver: SignedUrlResolver,
) : DataSource.Factory {

    private val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("PlayButton")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)
        .setAllowCrossProtocolRedirects(true)

    /** Cambia el URI interno por el enlace firmado justo antes de abrirlo. */
    private val network = ResolvingDataSource.Factory(httpFactory, signedUrlResolver)

    private val keyFactory = CacheKeyFactory { dataSpec ->
        MediaItems.songIdFrom(dataSpec.uri)
            ?.let(AudioStore::keyFor)
            ?: dataSpec.uri.toString()
    }

    override fun createDataSource(): DataSource {
        // Se monta de dentro hacia fuera, así que el último envoltorio es el
        // primero en responder: descargas, luego caché, y si no está en
        // ninguna, la red.
        var factory: DataSource.Factory = network
        audioCache.cacheOrNull()?.let { factory = writing(it, factory) }
        audioDownloads.storeOrNull()?.let { factory = readOnly(it, factory) }
        return factory.createDataSource()
    }

    /**
     * Lector que además guarda en las descargas. Su upstream es la red
     * directamente, sin pasar por la caché: lo que se descarga a mano no tiene
     * por qué ocupar además el espacio de la caché.
     */
    fun downloadDataSourceOrNull(): CacheDataSource? {
        val store = audioDownloads.storeOrNull() ?: return null
        return writing(store, network).createDataSource()
    }

    private fun writing(cache: Cache, upstream: DataSource.Factory) =
        baseFactory(cache, upstream)

    /** Sirve lo que tenga y deja pasar el resto sin quedarse una copia. */
    private fun readOnly(cache: Cache, upstream: DataSource.Factory) =
        baseFactory(cache, upstream).setCacheWriteDataSinkFactory(null)

    private fun baseFactory(cache: Cache, upstream: DataSource.Factory) =
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheKeyFactory(keyFactory)
            // Un problema con el almacén —disco lleno, fichero corrupto— no
            // puede dejar sin música: se sigue por red.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
