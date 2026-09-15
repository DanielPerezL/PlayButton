package com.zice.playbutton.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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

    fun songIdFrom(uri: Uri): Int? =
        if (uri.scheme == SCHEME) uri.lastPathSegment?.toIntOrNull() else null

    fun from(song: Song, artworkUri: Uri?): MediaItem = MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(uriFor(song.id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setDisplayTitle(song.name)
                .setArtworkUri(artworkUri)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        )
        .build()
}
