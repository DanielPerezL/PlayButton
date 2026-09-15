package com.zice.playbutton.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.zice.playbutton.data.local.AudioCache
import com.zice.playbutton.data.local.AudioDownloads
import com.zice.playbutton.data.repo.OfflineLibrary
import com.zice.playbutton.domain.Playlist
import com.zice.playbutton.domain.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Descarga una playlist entera para poder escucharla sin datos y sin que
 * caduque.
 *
 * El trabajo vive en un scope propio de la aplicación, no en el de la pantalla
 * que lo arranca: salir del detalle o mandar la app al fondo no puede cortar
 * una descarga de cuarenta canciones. El estado se publica aquí, así que al
 * volver a entrar la pantalla lo recupera tal cual.
 *
 * Solo hay una descarga a la vez, a propósito: son del mismo usuario en la
 * misma red, y en paralelo solo se estorbarían.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlaylistDownloader @Inject constructor(
    private val audioSources: AudioSources,
    private val audioDownloads: AudioDownloads,
    private val audioCache: AudioCache,
    private val offlineLibrary: OfflineLibrary,
    private val notifications: DownloadNotifications,
    private val imageLoader: ImageLoader,
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        /**
         * Lo que se supone que va a pesar la siguiente canción cuando aún no
         * se ha bajado ninguna con la que comparar: unos cuatro minutos a
         * calidad normal.
         */
        const val TYPICAL_SONG_BYTES = 5_000_000L

        /**
         * Hueco del dispositivo que no se toca. Descargar música no puede
         * dejar el móvil sin sitio para lo demás.
         */
        const val RESERVED_FREE_BYTES = 300_000_000L
    }

    data class Progress(val playlistId: Int, val done: Int, val total: Int)

    sealed interface Result {
        val playlistId: Int

        /** Está entera en el dispositivo. */
        data class Saved(override val playlistId: Int, val songs: Int) : Result

        /** Se paró para no llenar el dispositivo; queda lo ya descargado. */
        data class Partial(
            override val playlistId: Int,
            val saved: Int,
            val total: Int,
        ) : Result

        /** Se cortó por el camino; lo descargado hasta ahí se queda. */
        data class Failed(override val playlistId: Int, val saved: Int) : Result
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress.asStateFlow()

    private var job: Job? = null

    fun start(playlist: Playlist, songs: List<Song>) {
        if (songs.isEmpty() || job?.isActive == true) return

        job = scope.launch {
            _progress.value = Progress(playlist.id, 0, songs.size)
            notifications.showProgress(playlist.name, 0, songs.size)

            val outcome = try {
                download(playlist, songs)
            } catch (_: CancellationException) {
                // Cancelar no es un resultado que haya que contarle a nadie:
                // lo ha pedido el usuario y ya ve lo que quedó guardado.
                null
            } finally {
                // Pase lo que pase hay que dejar el registro acorde con lo que
                // de verdad hay en disco, también al cancelar a mitad.
                withContext(NonCancellable) {
                    val saved = audioDownloads.count(songs.map(Song::id))
                    offlineLibrary.register(playlist, songs.size, saved)
                }
                _progress.value = null
                notifications.clearProgress()
            }

            // El resultado se cuenta por notificación y no en la pantalla: la
            // descarga sigue con la app en el fondo, así que lo normal es que
            // al terminar nadie esté mirando el detalle de la playlist.
            if (outcome != null) notifications.showResult(playlist.name, outcome)
        }
    }

    /** Al cancelar se conserva lo ya descargado: no tiene sentido tirarlo. */
    fun cancel() {
        job?.cancel()
        job = null
    }

    /**
     * Va canción por canción en el orden de la playlist. Las que ya están
     * descargadas se saltan sin gastar red, y se para en cuanto el dispositivo
     * se queda sin hueco de sobra: es mejor dejar las primeras enteras y
     * decirlo que llenar el móvil.
     */
    private suspend fun download(playlist: Playlist, songs: List<Song>): Result {
        val playlistId = playlist.id
        val dataSource = audioSources.downloadDataSourceOrNull()
            ?: return Result.Failed(playlistId, 0)

        var saved = 0
        var downloadedBytes = 0L
        var downloadedSongs = 0

        for (song in songs) {
            currentCoroutineContext().ensureActive()

            if (!audioDownloads.holds(song.id)) {
                // El presupuesto se estima con lo que están pesando las de
                // esta misma playlist, que es la mejor referencia que hay.
                val expected = if (downloadedSongs > 0) {
                    downloadedBytes / downloadedSongs
                } else {
                    TYPICAL_SONG_BYTES
                }
                if (audioDownloads.freeSpaceBytes() - expected < RESERVED_FREE_BYTES) {
                    return Result.Partial(playlistId, saved, songs.size)
                }

                var songBytes = 0L
                val ok = write(dataSource, song.id) { songBytes += it }
                if (!ok) return Result.Failed(playlistId, saved)

                downloadedBytes += songBytes
                downloadedSongs++
            }

            // Ya está a salvo en el almacenamiento: la copia que pudiera
            // quedar en la caché no se va a volver a leer, así que ese sitio
            // se devuelve a las canciones que sí dependen de ella.
            audioCache.forget(listOf(song.id))

            saved++
            _progress.value = Progress(playlistId, saved, songs.size)
            notifications.showProgress(playlist.name, saved, songs.size)
        }

        cacheArtwork(playlist, songs)

        return Result.Saved(playlistId, saved)
    }

    /**
     * Deja las portadas en la cache de disco de Coil, que es de donde tiran
     * las pantallas. Sin esto una playlist descargada se escucha sin conexion
     * pero se ve llena de huecos.
     *
     * Las URL se dedupican antes: casi todas las canciones de un artista
     * comparten la suya, asi que bajarlas una a una seria repetir la misma
     * decenas de veces. No gastan presupuesto de espacio porque pesan menos
     * del uno por ciento de lo que pesa el audio, y la cache tiene su propio
     * limite con desalojo por uso.
     */
    private suspend fun cacheArtwork(playlist: Playlist, songs: List<Song>) {
        val urls = (songs.mapNotNull { it.imageUrl } + listOfNotNull(playlist.imageUrl)).toSet()

        for (url in urls) {
            currentCoroutineContext().ensureActive()
            runCatching {
                imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(url)
                        // Solo interesa que quede en disco: nadie la va a
                        // pintar ahora mismo y ocuparia memoria para nada.
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build(),
                )
            }
        }
    }

    private suspend fun write(
        dataSource: CacheDataSource,
        songId: Int,
        onBytes: (Long) -> Unit,
    ): Boolean {
        val spec = DataSpec(MediaItems.uriFor(songId))
        val writer = CacheWriter(dataSource, spec, null) { _, _, newBytesCached ->
            onBytes(newBytesCached)
        }

        // `cache()` bloquea el hilo hasta acabar y no sabe de corrutinas: sin
        // esto, cancelar la descarga la dejaba corriendo por detrás gastando
        // los datos que el usuario acaba de decir que no quiere gastar.
        val handle = currentCoroutineContext().job.invokeOnCompletion { writer.cancel() }
        return try {
            writer.cache()
            true
        } catch (_: Exception) {
            // Cancelar también sale por aquí: si es eso, se relanza y no se
            // cuenta como un fallo de red.
            currentCoroutineContext().ensureActive()
            false
        } finally {
            handle.dispose()
        }
    }
}
