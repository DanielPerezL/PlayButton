package com.zice.playbutton.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val nickname: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

/**
 * Las playlists de artista se siguen generando solas y son propiedad del
 * administrador, pero ya no se distinguen por el nombre de las canciones: el
 * backend las cuelga del maestro de artistas. `is_artist_playlist` lo deduce
 * de ahi y sigue viniendo igual.
 */
@Serializable
data class PlaylistDto(
    val id: Int,
    val name: String,
    val user: String = "",
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("is_artist_playlist") val isArtistPlaylist: Boolean = false,
    @SerialName("favorites_count") val favoritesCount: Int = 0,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class PlaylistPageDto(
    val playlists: List<PlaylistDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ArtistDto(
    val id: Int,
    val name: String,
)

@Serializable
data class SongDto(
    val id: Int,
    val title: String,
    val artists: List<ArtistDto> = emptyList(),
    /** Ya resuelta por el servidor: puede ser la del primer artista. */
    @SerialName("image_url") val imageUrl: String? = null,
    /**
     * Cuando cambiaron por última vez estos metadatos, en UTC y con sufijo Z.
     * Es con lo que se sabe si la copia guardada se ha quedado vieja.
     */
    @SerialName("updated_at") val updatedAt: String? = null,
)

/**
 * Artista del maestro. Lo que se abre para ver sus canciones sigue siendo una
 * playlist, y por eso viene con su id: asi la pestana de artistas no tiene que
 * cruzar dos listados.
 */
@Serializable
data class ArtistSummaryDto(
    val id: Int,
    val name: String,
    @SerialName("playlist_id") val playlistId: Int? = null,
    @SerialName("songs_count") val songsCount: Int = 0,
    @SerialName("favorites_count") val favoritesCount: Int = 0,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class ArtistPageDto(
    val artists: List<ArtistSummaryDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class SongPageDto(
    val songs: List<SongDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class SignedUrlDto(
    @SerialName("mp3_url") val mp3Url: String? = null,
)

@Serializable
data class ImageUrlDto(
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class FavoriteCountDto(
    @SerialName("favorites_count") val favoritesCount: Int = 0,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class PlaylistBodyRequest(
    val name: String,
    @SerialName("is_public") val isPublic: Boolean,
)

@Serializable
data class SuggestionRequest(
    @SerialName("song_name") val songName: String,
)

/** Formato de error del backend: {"error": "...", "message": "..."}. */
@Serializable
data class ApiErrorDto(
    val error: String? = null,
    val message: String? = null,
)
