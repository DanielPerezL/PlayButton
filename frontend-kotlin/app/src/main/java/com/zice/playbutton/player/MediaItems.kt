package com.zice.playbutton.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.zice.playbutton.R
import com.zice.playbutton.domain.Song

/**
 * Las canciones no se reproducen desde una URL fija: el backend firma un
 * enlace que caduca a los seis minutos. Por eso el MediaItem lleva un URI
 * interno con el id, y [SignedUrlResolver] lo cambia por el enlace real justo
 * antes de abrir el stream.
 */
object MediaItems {

    private const val SCHEME = "playbutton"

    fun uriFor(songId: Int): Uri = "$SCHEME://song/$songId".toUri()

    /**
     * El isotipo empaquetado, para las canciones que no tienen portada. Lo
     * piden el servicio y la conexion, que son quienes montan la cola.
     */
    fun fallbackArtwork(packageName: String): Uri =
        "android.resource://$packageName/${R.drawable.notification_artwork}".toUri()

    fun songIdFrom(uri: Uri): Int? =
        if (uri.scheme == SCHEME) uri.lastPathSegment?.toIntOrNull() else null

    /**
     * [fallbackArtwork] es el isotipo local, para las canciones que no tienen
     * portada. La notificacion y la pantalla de bloqueo leen esta URI, asi que
     * dejarla vacia las dejaria sin nada que enseñar.
     */
    fun from(song: Song, fallbackArtwork: Uri?): MediaItem = MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(uriFor(song.id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setDisplayTitle(song.fullName)
                .setArtworkUri(song.imageUrl?.toUri() ?: fallbackArtwork)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        )
        .build()
}
