package com.zice.playbutton.domain

import com.zice.playbutton.data.remote.dto.ArtistSummaryDto
import com.zice.playbutton.data.remote.dto.PlaylistDto
import com.zice.playbutton.data.remote.dto.SongDto

/**
 * Título y artistas vienen ya separados del backend. Antes viajaban juntos en
 * una sola cadena con el formato "Artista - Título" que la app tenía que
 * partir, con lo que un artista no era más que un trozo de texto.
 *
 * Del artista solo se queda el nombre: la app no navega a un artista (para eso
 * está su playlist, que es lo que abre la pestaña Artistas), así que su id no
 * lo usa nadie y guardarlo obligaría a inventárselo al releer la caché.
 */
data class Song(
    val id: Int,
    val title: String,
    val artists: List<String> = emptyList(),
) {
    /** Los artistas tal y como se muestran en una línea. */
    val artist: String? get() = artists.joinToString(", ").ifEmpty { null }

    /** Título y artistas juntos, para donde solo cabe una línea. */
    val fullName: String get() = artist?.let { "$it - $title" } ?: title
}

data class Playlist(
    val id: Int,
    val name: String,
    val ownerName: String,
    val ownerId: Int,
    val isPublic: Boolean,
    val isArtist: Boolean,
    val favoritesCount: Int,
    val isFavorite: Boolean,
)

/**
 * Playlist con todo su audio en el dispositivo. Es lo único que se puede
 * ofrecer cuando no hay servidor al que preguntar.
 */
data class DownloadedPlaylist(
    val id: Int,
    val name: String,
    val ownerName: String,
    val isArtist: Boolean,
    val songCount: Int,
)

fun SongDto.toDomain() = Song(
    id = id,
    title = title,
    artists = artists.map { it.name },
)

/**
 * El artista se presenta como la playlist que lo contiene, que es lo que se
 * abre al tocarlo y lo que la app ya sabe pintar y marcar como favorita.
 */
fun ArtistSummaryDto.toDomain() = Playlist(
    id = playlistId ?: 0,
    name = name,
    ownerName = "Sistema",
    ownerId = 1,
    isPublic = true,
    isArtist = true,
    favoritesCount = favoritesCount,
    isFavorite = isFavorite,
)

fun PlaylistDto.toDomain() = Playlist(
    id = id,
    name = name,
    ownerName = user,
    ownerId = userId,
    isPublic = isPublic,
    isArtist = isArtistPlaylist,
    favoritesCount = favoritesCount,
    isFavorite = isFavorite,
)

/** Origen de un listado de playlists. Identifica también su caché. */
enum class PlaylistSource {
    Public,
    Mine,
    Favorites,
    Artists,
    /** Playlists públicas de otro usuario, abiertas desde el detalle. */
    OtherUser,
}
