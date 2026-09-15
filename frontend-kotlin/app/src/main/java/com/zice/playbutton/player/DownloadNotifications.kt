package com.zice.playbutton.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zice.playbutton.MainActivity
import com.zice.playbutton.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avisos de las descargas de playlists.
 *
 * La descarga sigue con la app cerrada o en el fondo, así que la barra de
 * notificaciones es el único sitio donde el usuario puede ver que va, cuánto
 * queda y cuándo ha acabado. Antes el resultado se contaba con un diálogo, que
 * solo aparecía si por casualidad seguías en la pantalla al terminar.
 */
@Singleton
class DownloadNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        const val PROGRESS_CHANNEL = "download_progress"
        const val DONE_CHANNEL = "download_done"
        const val PROGRESS_ID = 4001
        const val DONE_ID = 4002
    }

    private val manager = NotificationManagerCompat.from(context)

    /**
     * En marcha: sin sonido y sin poder descartarse, como cualquier descarga.
     * Se reutiliza el mismo id, así que las actualizaciones no apilan avisos.
     */
    fun showProgress(playlistName: String, done: Int, total: Int) {
        ensureChannels()
        val notification = baseBuilder(PROGRESS_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.download_notification_title, playlistName))
            .setContentText(context.getString(R.string.download_notification_progress, done, total))
            .setProgress(total, done, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        notify(PROGRESS_ID, notification)
    }

    /** Terminada, con el nombre de la playlist y en qué ha quedado. */
    fun showResult(playlistName: String, result: PlaylistDownloader.Result) {
        ensureChannels()
        val (title, text) = when (result) {
            is PlaylistDownloader.Result.Saved -> {
                context.getString(R.string.download_done_title, playlistName) to
                    context.resources.getQuantityString(
                        R.plurals.playlist_songs_count,
                        result.songs,
                        result.songs,
                    )
            }

            is PlaylistDownloader.Result.Partial -> {
                context.getString(R.string.download_partial_title, playlistName) to
                    context.getString(
                        R.string.download_partial_text,
                        result.saved,
                        result.total,
                    )
            }

            is PlaylistDownloader.Result.Failed -> {
                context.getString(R.string.download_failed_title, playlistName) to
                    context.getString(R.string.download_failed_text, result.saved)
            }
        }

        val notification = baseBuilder(DONE_CHANNEL)
            // Ya no está descargando, así que el aviso lleva la marca. El
            // monocromo sale del icono del lanzador, recortado y centrado por
            // su centro de masas para que no se vea escorado.
            .setSmallIcon(R.drawable.ic_stat_playbutton)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        notify(DONE_ID, notification)
    }

    fun clearProgress() = manager.cancel(PROGRESS_ID)


    private fun baseBuilder(channelId: String) = NotificationCompat.Builder(context, channelId)
        .setContentIntent(openAppIntent())
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setSilent(channelId == PROGRESS_CHANNEL)
        // Tine el icono y el acento con el morado de la marca. Android reserva
        // el fondo de color (setColorized) para los avisos de un servicio en
        // primer plano, y estos no lo son, asi que lo ignoraria.
        .setColor(ContextCompat.getColor(context, R.color.brand_500))

    /**
     * Dos canales y no uno: así el usuario puede silenciar el «ya está» sin
     * perder la barra de progreso, o al revés, desde los ajustes de Android.
     */
    private fun ensureChannels() {
        val progress = NotificationChannel(
            PROGRESS_CHANNEL,
            context.getString(R.string.download_channel_progress),
            NotificationManager.IMPORTANCE_LOW,
        )
        val done = NotificationChannel(
            DONE_CHANNEL,
            context.getString(R.string.download_channel_done),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(progress)
        manager.createNotificationChannel(done)
    }

    /** Lleva a la app tal y como la abriría el icono del lanzador. */
    private fun openAppIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        // Sin permiso de notificaciones no se muestra nada, y la descarga no
        // puede caerse por eso: se sigue viendo el progreso en la pantalla.
        runCatching { manager.notify(id, notification) }
    }
}
