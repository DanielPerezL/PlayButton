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
 * El backend no tiene modelo de artista: un artista es una playlist con
 * `is_artist_playlist = true`, generada automaticamente a partir del nombre
 * de las canciones y propiedad del administrador. Por eso comparten DTO.
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
)

@Serializable
data class PlaylistPageDto(
    val playlists: List<PlaylistDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
)

/** El nombre viene en formato "Artista - Titulo"; la app lo parte para mostrarlo. */
@Serializable
data class SongDto(
    val id: Int,
    val name: String,
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
