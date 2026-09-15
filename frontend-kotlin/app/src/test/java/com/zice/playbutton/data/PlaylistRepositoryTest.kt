package com.zice.playbutton.data

import com.zice.playbutton.data.local.SessionProvider
import com.zice.playbutton.data.remote.ApiService
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
import com.zice.playbutton.data.repo.ListKey
import com.zice.playbutton.data.repo.PlaylistRepository
import com.zice.playbutton.domain.PlaylistSource
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Comprueba lo que la app React Native no tenía: que un listado ya cargado no
 * vuelve a la red, y que sí lo hace cuando algo lo invalida.
 */
class PlaylistRepositoryTest {

    private open class FakeApi : ApiService {
        var publicCalls = 0
        var artistCalls = 0
        var playlists = listOf(
            PlaylistDto(id = 1, name = "Rock", user = "pepe", userId = 5, favoritesCount = 2),
            PlaylistDto(id = 2, name = "Pop", user = "ana", userId = 6, favoritesCount = 1),
        )
        var favoriteCount = 3

        override suspend fun getPublicPlaylists(offset: Int, limit: Int, search: String): PlaylistPageDto {
            publicCalls++
            return PlaylistPageDto(playlists.drop(offset).take(limit), hasMore = offset + limit < playlists.size)
        }

        override suspend fun getArtists(offset: Int, limit: Int, search: String): PlaylistPageDto {
            artistCalls++
            return PlaylistPageDto(playlists.drop(offset).take(limit), hasMore = false)
        }

        override suspend fun toggleFavorite(playlistId: Int) = FavoriteCountDto(favoriteCount)

        override suspend fun deletePlaylist(playlistId: Int): Response<Unit> = Response.success(null)

        override suspend fun updatePlaylist(playlistId: Int, body: PlaylistBodyRequest): Response<Unit> =
            Response.success(null)

        override suspend fun createPlaylist(userId: Int, body: PlaylistBodyRequest) =
            PlaylistDto(id = 99, name = body.name, user = "yo", userId = userId, isPublic = body.isPublic)

        // El resto no interviene en estas pruebas.
        override suspend fun login(body: LoginRequest): LoginResponse = notUsed()
        override suspend fun searchSongs(name: String?, offset: Int, limit: Int): SongPageDto = notUsed()
        override suspend fun getSignedUrl(songId: Int): SignedUrlDto = notUsed()
        override suspend fun getUserPlaylists(userId: Int, offset: Int, limit: Int, search: String) = notUsed<PlaylistPageDto>()
        override suspend fun getUserFavorites(userId: Int, offset: Int, limit: Int, search: String) = notUsed<PlaylistPageDto>()
        override suspend fun getPlaylistSongs(playlistId: Int): SongPageDto = notUsed()
        override suspend fun addSongToPlaylist(playlistId: Int, songId: Int): Response<Unit> = notUsed()
        override suspend fun removeSongFromPlaylist(playlistId: Int, songId: Int): Response<Unit> = notUsed()
        override suspend fun changePassword(userId: Int, body: ChangePasswordRequest): Response<Unit> = notUsed()
        override suspend fun deleteAccount(userId: Int): Response<Unit> = notUsed()
        override suspend fun createSuggestion(body: SuggestionRequest): Response<Unit> = notUsed()

        private fun <T> notUsed(): T = throw UnsupportedOperationException("no usado en el test")
    }

    private val session = object : SessionProvider {
        override suspend fun currentUserId() = 5
    }

    private fun repository(api: ApiService) = PlaylistRepository(api, session)

    @Test
    fun `la segunda carga sale de la cache y no toca la red`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Public)

        repo.ensureLoaded(key)
        repo.ensureLoaded(key)
        repo.ensureLoaded(key)

        assertEquals(1, api.publicCalls)
        assertEquals(2, repo.cached(key)?.items?.size)
    }

    @Test
    fun `el listado de artistas tambien cachea`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Artists)

        repo.ensureLoaded(key)
        repo.ensureLoaded(key)

        assertEquals(1, api.artistCalls)
    }

    @Test
    fun `cada busqueda tiene su propia entrada de cache`() = runTest {
        val api = FakeApi()
        val repo = repository(api)

        repo.ensureLoaded(ListKey(PlaylistSource.Public, ""))
        repo.ensureLoaded(ListKey(PlaylistSource.Public, "rock"))
        repo.ensureLoaded(ListKey(PlaylistSource.Public, ""))

        // Dos búsquedas distintas, dos peticiones; la repetida sale de caché.
        assertEquals(2, api.publicCalls)
    }

    @Test
    fun `refrescar fuerza una peticion nueva`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Public)

        repo.ensureLoaded(key)
        repo.refresh(key)

        assertEquals(2, api.publicCalls)
    }

    @Test
    fun `borrar una playlist la quita de todas las listas cacheadas`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Public)
        repo.ensureLoaded(key)

        repo.deletePlaylist(1)

        assertEquals(listOf(2), repo.cached(key)?.items?.map { it.id })
    }

    @Test
    fun `crear una playlist invalida el listado propio`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Public)
        repo.ensureLoaded(key)
        assertEquals(1, api.publicCalls)

        repo.createPlaylist("Nueva", isPublic = true)
        repo.ensureLoaded(key)

        // La lista quedó sucia, así que la siguiente lectura vuelve a la red.
        assertEquals(2, api.publicCalls)
    }

    @Test
    fun `marcar favorito actualiza el estado y el contador`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Public)
        repo.ensureLoaded(key)

        repo.toggleFavorite(1)

        val playlist = repo.cached(key)?.items?.first { it.id == 1 }
        assertTrue(playlist!!.isFavorite)
        assertEquals(api.favoriteCount, playlist.favoritesCount)
    }

    @Test
    fun `si falla el favorito se revierte el cambio optimista`() = runTest {
        val api = object : FakeApi() {
            override suspend fun toggleFavorite(playlistId: Int): FavoriteCountDto =
                throw RuntimeException("sin red")
        }
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Public)
        repo.ensureLoaded(key)
        val before = repo.cached(key)!!.items.first { it.id == 1 }

        repo.toggleFavorite(1)

        val after = repo.cached(key)!!.items.first { it.id == 1 }
        assertEquals(before.isFavorite, after.isFavorite)
        assertEquals(before.favoritesCount, after.favoritesCount)
    }

    @Test
    fun `una playlist recien creada se conoce antes de recargar ningun listado`() = runTest {
        val api = FakeApi()
        val repo = repository(api)

        val created = repo.createPlaylist("Nueva", isPublic = true)

        // Su detalle se abre acto seguido: sin esto no tendría ni nombre ni dueño.
        val found = repo.findCached(created.id)
        assertEquals("Nueva", found?.name)
        assertEquals(5, found?.ownerId)
    }

    @Test
    fun `renombrar una playlist recien creada actualiza lo que se conoce de ella`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val created = repo.createPlaylist("Nueva", isPublic = true)

        repo.updatePlaylist(created.id, "Otro nombre", isPublic = false)

        val found = repo.findCached(created.id)
        assertEquals("Otro nombre", found?.name)
        assertEquals(false, found?.isPublic)
    }

    @Test
    fun `cerrar sesion vacia la cache`() = runTest {
        val api = FakeApi()
        val repo = repository(api)
        val key = ListKey(PlaylistSource.Public)
        repo.ensureLoaded(key)
        val created = repo.createPlaylist("Nueva", isPublic = true)

        repo.clear()

        assertEquals(null, repo.cached(key))
        assertEquals(null, repo.findCached(created.id))
    }
}
