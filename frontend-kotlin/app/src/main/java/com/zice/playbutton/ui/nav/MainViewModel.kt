package com.zice.playbutton.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zice.playbutton.data.remote.AuthEvents
import com.zice.playbutton.data.remote.ServerReachability
import com.zice.playbutton.data.repo.AuthRepository
import com.zice.playbutton.data.repo.PlaylistRepository
import com.zice.playbutton.player.PlayerConnection
import com.zice.playbutton.player.PlayerVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val authRepository: AuthRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerVisibility: PlayerVisibility,
    serverReachability: ServerReachability,
    authEvents: AuthEvents,
) : ViewModel() {

    /**
     * Sin servidor no se ofrece el Modo Zen: baraja todo el catálogo, así que
     * siempre necesita pedirlo. Un botón que no puede funcionar es peor que no
     * tener botón.
     */
    val zenAvailable = serverReachability.isReachable

    init {
        // El token caducado ya lo limpia el interceptor; aqui se tira el resto
        // del estado para que el siguiente usuario no vea datos del anterior.
        viewModelScope.launch {
            authEvents.sessionExpired.collect {
                playerConnection.stopAndClear()
                authRepository.logout()
            }
        }
    }

    val playerState = playerConnection.state
    val positionMs = playerConnection.positionMs

    /**
     * Si el reproductor grande esta delante. No basta con que se haya
     * desplegado: tambien tiene que haber algo que enseñar.
     *
     * El servicio de reproduccion se muere solo cuando no queda nada sonando,
     * asi que al volver a la app la cola puede llegar vacia. La capa seguia
     * encima con un reproductor sin cancion, y como ademas apaga la navegacion
     * de debajo y se come los toques, dejaba la app muerta: ni boton de volver
     * ni pestañas. Atando las dos cosas, la regla vale para la capa, para el
     * escudo de toques y para el gesto de volver a la vez.
     */
    val playerExpanded = combine(
        playerVisibility.isExpanded,
        playerConnection.state,
    ) { expanded, state -> expanded && state.hasPlayer }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isLoggedIn = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Para titular la pantalla de detalle segun sea playlist o artista. */
    fun isArtistPlaylist(playlistId: Int): Boolean =
        playlistRepository.findCached(playlistId)?.isArtist == true

    fun expandPlayer() = playerVisibility.expand()

    fun collapsePlayer() = playerVisibility.collapse()

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun next() = playerConnection.next()
    fun previous() = playerConnection.previous()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun skipToQueueIndex(index: Int) = playerConnection.skipToQueueIndex(index)

    fun playZen() {
        playerConnection.playZen()
        playerVisibility.expand()
    }
}
