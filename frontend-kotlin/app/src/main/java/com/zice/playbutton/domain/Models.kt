package com.zice.playbutton.domain

import com.zice.playbutton.data.remote.dto.PlaylistDto
import com.zice.playbutton.data.remote.dto.SongDto

/**
 * El backend guarda el nombre de la canción como una sola cadena con el
 * formato "Artista - Título". El separador " - " es el contrato semántico de
 * todo el producto (también lo usa el backend para generar las playlists de
 * artista), así que la app lo parte una vez aquí en lugar de repetirlo en
 * cada pantalla.
 */
data class Song(
    val id: Int,
    val name: String,
) {
    private val parts: Pair<String?, String> by lazy {
        val index = name.indexOf(" - ")
        if (index > 0) {
            name.substring(0, index) to name.substring(index + 3)
        } else {
            null to name
        }
    }

    val artist: String? get() = parts.first
    val title: String get() = parts.second
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

fun SongDto.toDomain() = Song(id = id, name = name)

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
