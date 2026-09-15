package com.zice.playbutton.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector
import com.zice.playbutton.R
import kotlinx.serialization.Serializable

@Serializable object PublicPlaylistsRoute
@Serializable object MyPlaylistsRoute
@Serializable object FavoritesRoute
@Serializable object ArtistsRoute

@Serializable data class PlaylistDetailRoute(val playlistId: Int)
@Serializable data class AddSongsRoute(val playlistId: Int)
@Serializable data class UserPlaylistsRoute(val userId: Int, val userName: String)
@Serializable object NewPlaylistRoute
@Serializable object SuggestionsRoute
@Serializable object SettingsRoute
@Serializable object PlayerRoute

/**
 * Los cuatro destinos de la barra inferior. "Mis favoritos" se llamaba antes
 * "Mis playlist favoritas", pero el listado incluye también artistas —que son
 * playlists generadas automáticamente— así que el nombre se quedaba corto.
 */
enum class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Public(PublicPlaylistsRoute, R.string.nav_public, Icons.Filled.Public),
    Mine(MyPlaylistsRoute, R.string.nav_my_playlists, Icons.Filled.LibraryMusic),
    Favorites(FavoritesRoute, R.string.nav_favorites, Icons.Filled.Favorite),
    Artists(ArtistsRoute, R.string.nav_artists, Icons.Filled.Person),
}
