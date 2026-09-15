package com.zice.playbutton.data.remote

import com.zice.playbutton.data.remote.dto.ArtistPageDto
import com.zice.playbutton.data.remote.dto.ChangePasswordRequest
import com.zice.playbutton.data.remote.dto.FavoriteCountDto
import com.zice.playbutton.data.remote.dto.LoginRequest
import com.zice.playbutton.data.remote.dto.LoginResponse
import com.zice.playbutton.data.remote.dto.PlaylistBodyRequest
import com.zice.playbutton.data.remote.dto.PlaylistDto
import com.zice.playbutton.data.remote.dto.PlaylistPageDto
import com.zice.playbutton.data.remote.dto.SignedUrlDto
import com.zice.playbutton.data.remote.dto.SongPageDto
import com.zice.playbutton.data.remote.dto.SuggestionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Todas las rutas cuelgan de la URL base que configura el usuario (que ya
 * termina en `/api`). El host real lo inyecta [BaseUrlInterceptor]; la
 * `baseUrl` de Retrofit es solo un marcador.
 */
interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // --- Canciones -------------------------------------------------------

    /**
     * Busca por coincidencia parcial en el titulo y en el nombre de los
     * artistas, paginando por id descendente. Con `random` devuelve en su
     * lugar una seleccion aleatoria de las canciones marcadas como visibles e
     * **ignora el offset**: ese es el Modo Zen.
     */
    @GET("songs")
    suspend fun searchSongs(
        @Query("q") query: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("random") random: Boolean = false,
    ): SongPageDto

    @GET("songs/{id}/signed-url")
    suspend fun getSignedUrl(@Path("id") songId: Int): SignedUrlDto

    // --- Playlists -------------------------------------------------------

    @GET("playlists")
    suspend fun getPublicPlaylists(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String,
    ): PlaylistPageDto

    @GET("artists")
    suspend fun getArtists(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String,
    ): ArtistPageDto

    @GET("users/{userId}/playlists")
    suspend fun getUserPlaylists(
        @Path("userId") userId: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String,
    ): PlaylistPageDto

    @GET("users/{userId}/favorites")
    suspend fun getUserFavorites(
        @Path("userId") userId: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String,
    ): PlaylistPageDto

    /** Devuelve la lista completa: este endpoint no pagina. */
    @GET("playlists/{id}/songs")
    suspend fun getPlaylistSongs(@Path("id") playlistId: Int): SongPageDto

    @POST("users/{userId}/playlists")
    suspend fun createPlaylist(
        @Path("userId") userId: Int,
        @Body body: PlaylistBodyRequest,
    ): PlaylistDto

    @PUT("playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: Int,
        @Body body: PlaylistBodyRequest,
    ): Response<Unit>

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: Int): Response<Unit>

    @POST("playlists/{playlistId}/songs/{songId}")
    suspend fun addSongToPlaylist(
        @Path("playlistId") playlistId: Int,
        @Path("songId") songId: Int,
    ): Response<Unit>

    @DELETE("playlists/{playlistId}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("playlistId") playlistId: Int,
        @Path("songId") songId: Int,
    ): Response<Unit>

    /** Alterna el favorito y devuelve el contador ya actualizado. */
    @POST("playlists/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") playlistId: Int): FavoriteCountDto

    // --- Usuario ---------------------------------------------------------

    @PATCH("users/{userId}")
    suspend fun changePassword(
        @Path("userId") userId: Int,
        @Body body: ChangePasswordRequest,
    ): Response<Unit>

    @DELETE("users/{userId}")
    suspend fun deleteAccount(@Path("userId") userId: Int): Response<Unit>

    // --- Sugerencias -----------------------------------------------------

    @POST("suggestions")
    suspend fun createSuggestion(@Body body: SuggestionRequest): Response<Unit>
}
