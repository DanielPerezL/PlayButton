package com.zice.playbutton.player

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.zice.playbutton.data.repo.SongRepository
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cambia el URI interno `playbutton://song/{id}` por el enlace firmado real.
 *
 * El backend firma los enlaces con una caducidad de seis minutos, así que no
 * sirve resolverlos al construir la cola: hay que hacerlo justo antes de abrir
 * el stream. Como ExoPlayer reabre la conexión al buscar o al reintentar tras
 * un error, cada apertura obtiene una firma nueva y una canción larga o una
 * pausa prolongada dejan de ser un problema.
 */
@OptIn(UnstableApi::class)
@Singleton
class SignedUrlResolver @Inject constructor(
    private val songRepository: SongRepository,
) : ResolvingDataSource.Resolver {

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val songId = MediaItems.songIdFrom(dataSpec.uri) ?: return dataSpec

        // Se ejecuta en el hilo de carga de ExoPlayer, nunca en el principal.
        val signed = runBlocking {
            runCatching { songRepository.signedUrl(songId) }.getOrNull()
        } ?: throw IOException("No se pudo firmar la canción $songId")

        return dataSpec.withUri(signed.toUri())
    }

    private fun String.toUri() = android.net.Uri.parse(this)
}
