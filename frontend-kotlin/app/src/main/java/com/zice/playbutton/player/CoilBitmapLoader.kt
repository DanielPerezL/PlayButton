package com.zice.playbutton.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * La portada de la notificación, resuelta con el mismo Coil que pinta la app.
 *
 * El cargador que Media3 trae de serie abre la URI de la carátula por HTTP y
 * no conoce más caché que la de la última imagen, así que la notificación y la
 * pantalla de bloqueo se quedaban sin portada en cuanto no había red —justo
 * cuando se escucha lo descargado, que es cuando más se nota—. La imagen
 * estaba en el dispositivo desde la propia descarga, pero en la caché de disco
 * de Coil, que aquel cargador no mira.
 *
 * Pasando por Coil se lee de ahí, y de paso lo que se haya visto una vez en la
 * app queda servido para las siguientes sin volver a la red.
 */
@OptIn(UnstableApi::class)
class CoilBitmapLoader(
    context: Context,
    private val imageLoader: ImageLoader,
) : BitmapLoader {

    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Decodificar bytes que ya vienen dados no tiene nada que ver con las
     * cachés, así que eso se queda en manos del cargador de serie.
     *
     * Por el Builder, que es lo que queda: los constructores de
     * DataSourceBitmapLoader están todos obsoletos. Sin nada que ajustarle,
     * monta lo mismo que ponía el de un solo argumento.
     */
    private val decoder = DataSourceBitmapLoader.Builder(this.context).build()

    override fun supportsMimeType(mimeType: String): Boolean =
        decoder.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        decoder.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        val job = scope.launch {
            try {
                val result = imageLoader.execute(
                    ImageRequest.Builder(context)
                        // Como texto y no como Uri de Android: Coil tiene su
                        // propio tipo y así resuelve igual la portada remota
                        // que el isotipo empaquetado de las canciones sin ella.
                        .data(uri.toString())
                        // El bitmap acaba viajando por IPC hasta la
                        // notificación, y uno de hardware no se puede leer
                        // fuera de la GPU.
                        .allowHardware(false)
                        .build(),
                )
                when (result) {
                    is SuccessResult -> future.set(result.image.toBitmap())
                    // Salga como salga hay que resolverlo: Media3 espera por
                    // este future y una portada que no llega nunca lo deja
                    // colgado.
                    else -> future.setException(
                        (result as? ErrorResult)?.throwable
                            ?: IllegalStateException("Portada no disponible: $uri"),
                    )
                }
            } catch (error: Throwable) {
                future.setException(error)
            }
        }

        // Media3 cancela lo que ha dejado de interesarle —al cambiar de
        // canción antes de que llegue la portada, por ejemplo—; sin esto la
        // carga seguiría hasta el final para nada.
        future.addListener(
            { if (future.isCancelled) job.cancel() },
            MoreExecutors.directExecutor(),
        )

        return future
    }
}
