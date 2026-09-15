package com.zice.playbutton.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.zice.playbutton.R
import com.zice.playbutton.ui.components.MiniPlayer
import com.zice.playbutton.ui.screens.addsongs.AddSongsScreen
import com.zice.playbutton.ui.screens.create.NewPlaylistScreen
import com.zice.playbutton.ui.screens.detail.PlaylistDetailScreen
import com.zice.playbutton.ui.screens.player.PlayerScreen
import androidx.compose.runtime.LaunchedEffect
import com.zice.playbutton.ui.screens.playlists.ArtistsViewModel
import com.zice.playbutton.ui.screens.playlists.BasePlaylistListViewModel
import com.zice.playbutton.ui.screens.playlists.FavoritesViewModel
import com.zice.playbutton.ui.screens.playlists.MyPlaylistsViewModel
import com.zice.playbutton.ui.screens.playlists.PlaylistListScreen
import com.zice.playbutton.ui.screens.playlists.PublicPlaylistsViewModel
import com.zice.playbutton.ui.screens.playlists.UserPlaylistsViewModel
import com.zice.playbutton.ui.screens.settings.SettingsScreen
import com.zice.playbutton.ui.screens.suggestions.SuggestionsScreen

/**
 * Hueco que el FAB ocupa sobre el contenido: 56dp del boton mas los 16dp que
 * el Scaffold le deja hasta el borde. El innerPadding del Scaffold no cuenta
 * el FAB, asi que sin esto el boton tapaba la ultima fila de los listados.
 */
private val FabReservedHeight = 72.dp

/**
 * Estructura de la aplicación: cuatro destinos en la barra inferior, un
 * mini-reproductor persistente justo encima y el reproductor completo como
 * una capa que se despliega sobre todo lo demás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayButtonApp(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val playerExpanded by viewModel.playerExpanded.collectAsStateWithLifecycle()
    val zenAvailable by viewModel.zenAvailable.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val topLevel = TopLevelDestination.entries.firstOrNull { entry ->
        destination?.hierarchy?.any { it.hasRoute(entry.route::class) } == true
    }
    val isTopLevel = topLevel != null

    // Mientras el reproductor esta desplegado, el boton de volver es suyo:
    // contrae la capa y no toca la navegacion de debajo. Declarar el
    // BackHandler de mas abajo no basta, porque entre dos manejadores activos
    // gana el ultimo que se registro y el del NavHost se registra despues: la
    // barra inferior y el reproductor los coloca un Scaffold, que compone su
    // contenido al medir, ya pasados los efectos de esta funcion. Por eso la
    // flecha atras sobre el reproductor abierto se llevaba la pantalla de
    // debajo —salias del detalle al listado sin verlo— y dejaba la capa
    // encima. Dandole a la navegacion su propio despachador se puede apagar
    // entera mientras la capa este delante, que es la misma idea que el escudo
    // de toques de mas abajo pero para el gesto de volver.
    val contentEvents = rememberNavigationEventDispatcherOwner(enabled = !playerExpanded)

    Box(Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides contentEvents) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle(navController, topLevel, viewModel)) },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                actions = {
                    // Crear es una accion sobre las playlists propias, asi que
                    // solo se ofrece en ese listado; el sitio grande de abajo
                    // queda para el Modo Zen, que es lo que se usa a diario.
                    if (topLevel == TopLevelDestination.Mine) {
                        IconButton(onClick = { navController.navigate(NewPlaylistRoute) }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.new_playlist),
                            )
                        }
                    }
                    if (isTopLevel) {
                        IconButton(onClick = { navController.navigate(SettingsRoute) }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Column {
                if (playerState.hasContent) {
                    MiniPlayer(
                        state = playerState,
                        positionMs = positionMs,
                        onPlayPause = viewModel::togglePlayPause,
                        onNext = viewModel::next,
                        onExpand = viewModel::expandPlayer,
                    )
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    // Material3 pinta el label de la pestaña activa con
                    // `secondary`, que en esta paleta es un tono de borde y se
                    // lee peor que el resto. La pestaña activa tiene que ser la
                    // más legible, así que se le da el texto principal.
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    TopLevelDestination.entries.forEach { entry ->
                        val selected = topLevel == entry
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // La configuración no pertenece a ninguna
                                // pestaña. Si sigue apilada al cambiar,
                                // `saveState` la guarda junto con la pestaña de
                                // la que se abrió y al volver a esa pestaña
                                // reaparece en lugar del listado.
                                navController.popBackStack<SettingsRoute>(inclusive = true)
                                navController.navigate(entry.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            label = { Text(stringResource(entry.labelRes)) },
                            colors = itemColors,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // El Modo Zen baraja todo el catalogo, no lo que hay en pantalla:
            // es una accion global y el boton es el mismo en los cuatro
            // listados. Antes el FAB alternaba entre crear y Zen segun la
            // pestaña, y el usuario no podia predecir que iba a pasar.
            if (isTopLevel && zenAvailable) {
                FloatingActionButton(
                    onClick = viewModel::playZen,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.player_zen_mode),
                    )
                }
            }
        },
    ) { innerPadding ->
        PlayButtonNavHost(
            navController = navController,
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = 0.dp,
            ),
            bottomInset = innerPadding.calculateBottomPadding(),
        )
    }
    }

    // El reproductor completo se superpone a todo en lugar de ser un destino
    // mas: asi no se pierde el sitio en el que estaba el usuario.
    AnimatedVisibility(
        visible = playerExpanded,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        Box(Modifier.fillMaxSize()) {
            // Dibujarse encima no basta: sin un modificador de entrada las
            // pulsaciones seguian viaje hasta la pantalla de debajo y se
            // acababa escribiendo en el buscador, abriendo una playlist o
            // cambiando de pestaña a ciegas. Este escudo se come todo lo que
            // el reproductor no use. Va detras y como hermano, no envolviendo
            // al reproductor: puesto por encima de el como ancestro se cruzaba
            // con sus propios gestos y el scroll de la cola dejaba de ir.
            Spacer(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Main)
                                    .changes.forEach { it.consume() }
                            }
                        }
                    },
            )

            PlayerScreen(
                state = playerState,
                positionMs = positionMs,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onSeek = viewModel::seekTo,
                onQueueItemClick = viewModel::skipToQueueIndex,
                onCollapse = viewModel::collapsePlayer,
                onPlayZen = viewModel::playZen,
                zenAvailable = zenAvailable,
            )
        }
    }
    }

    BackHandler(enabled = playerExpanded) { viewModel.collapsePlayer() }
}

@Composable
private fun currentTitle(
    navController: NavHostController,
    topLevel: TopLevelDestination?,
    viewModel: MainViewModel,
): String = when (topLevel) {
    TopLevelDestination.Public -> stringResource(R.string.title_public_playlists)
    TopLevelDestination.Mine -> stringResource(R.string.title_my_playlists)
    TopLevelDestination.Favorites -> stringResource(R.string.title_favorites)
    TopLevelDestination.Artists -> stringResource(R.string.title_artists)
    null -> {
        val destination = navController.currentBackStackEntry?.destination
        when {
            destination?.hasRoute(PlaylistDetailRoute::class) == true -> {
                val isArtist = navController.currentBackStackEntry
                    ?.toRoute<PlaylistDetailRoute>()?.playlistId
                    ?.let { viewModel.isArtistPlaylist(it) } ?: false
                stringResource(
                    if (isArtist) R.string.title_artist_detail else R.string.title_playlist_detail,
                )
            }
            destination?.hasRoute(AddSongsRoute::class) == true ->
                stringResource(R.string.title_add_songs)
            destination?.hasRoute(NewPlaylistRoute::class) == true ->
                stringResource(R.string.title_new_playlist)
            destination?.hasRoute(SuggestionsRoute::class) == true ->
                stringResource(R.string.title_suggestions)
            destination?.hasRoute(SettingsRoute::class) == true ->
                stringResource(R.string.title_settings)
            destination?.hasRoute(UserPlaylistsRoute::class) == true ->
                stringResource(
                    R.string.title_user_playlists,
                    navController.currentBackStackEntry
                        ?.toRoute<UserPlaylistsRoute>()?.userName.orEmpty(),
                )
            else -> stringResource(R.string.app_name)
        }
    }
}

@Composable
private fun PlayButtonNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    // El hueco del FAB lo reserva cada destino segun lleve boton o no, en vez
    // de mirar si el destino actual es de primer nivel: al navegar conviven el
    // que se va y el que llega, asi que ese calculo le quitaba los 72dp de
    // golpe al listado que seguia en pantalla. Si estaba al final, el
    // contenido se corria hacia abajo hasta quedar tapado por el propio boton
    // —el salto que se veia al pulsar la ultima playlist— y era esa posicion
    // desplazada la que se guardaba para cuando el usuario volvia.
    val listPadding = PaddingValues(bottom = bottomInset + FabReservedHeight)
    val plainPadding = PaddingValues(bottom = bottomInset)

    NavHost(
        navController = navController,
        startDestination = PublicPlaylistsRoute,
        modifier = modifier,
    ) {
        composable<PublicPlaylistsRoute> {
            val viewModel: PublicPlaylistsViewModel = hiltViewModel()
            PlaylistListRoute(
                viewModel = viewModel,
                navController = navController,
                contentPadding = listPadding,
                searchPlaceholderRes = R.string.search_playlist,
                emptyMessageRes = R.string.empty_playlists,
            )
        }
        composable<MyPlaylistsRoute> {
            val viewModel: MyPlaylistsViewModel = hiltViewModel()
            PlaylistListRoute(
                viewModel = viewModel,
                navController = navController,
                contentPadding = listPadding,
                searchPlaceholderRes = R.string.search_playlist,
                emptyMessageRes = R.string.empty_playlists,
            )
        }
        composable<FavoritesRoute> {
            val viewModel: FavoritesViewModel = hiltViewModel()
            PlaylistListRoute(
                viewModel = viewModel,
                navController = navController,
                contentPadding = listPadding,
                searchPlaceholderRes = R.string.search_playlist,
                emptyMessageRes = R.string.empty_playlists,
            )
        }
        composable<ArtistsRoute> {
            val viewModel: ArtistsViewModel = hiltViewModel()
            PlaylistListRoute(
                viewModel = viewModel,
                navController = navController,
                contentPadding = listPadding,
                searchPlaceholderRes = R.string.search_artist,
                emptyMessageRes = R.string.empty_artists,
            )
        }
        composable<UserPlaylistsRoute> {
            val viewModel: UserPlaylistsViewModel = hiltViewModel()
            PlaylistListRoute(
                viewModel = viewModel,
                navController = navController,
                contentPadding = plainPadding,
                searchPlaceholderRes = R.string.search_playlist,
                emptyMessageRes = R.string.empty_playlists,
            )
        }

        composable<PlaylistDetailRoute> {
            PlaylistDetailScreen(
                onNavigateToAddSongs = { navController.navigate(AddSongsRoute(it)) },
                onNavigateToUser = { userId, userName ->
                    navController.navigate(UserPlaylistsRoute(userId, userName))
                },
                onBack = { navController.navigateUp() },
                contentPadding = plainPadding,
            )
        }

        composable<AddSongsRoute> {
            AddSongsScreen(
                onNavigateToSuggestions = { navController.navigate(SuggestionsRoute) },
                contentPadding = plainPadding,
            )
        }

        composable<NewPlaylistRoute> {
            NewPlaylistScreen(
                onCreated = { playlistId ->
                    navController.navigate(PlaylistDetailRoute(playlistId)) {
                        popUpTo(NewPlaylistRoute) { inclusive = true }
                    }
                },
            )
        }

        composable<SuggestionsRoute> { SuggestionsScreen() }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateToSuggestions = { navController.navigate(SuggestionsRoute) },
                contentPadding = plainPadding,
            )
        }
    }
}

/**
 * Conecta cualquiera de los listados con su pantalla. Todos comparten cuerpo:
 * solo cambian los textos y qué ocurre al pulsar una fila. Los textos se pasan
 * uno a uno en vez de deducirlos de la pestaña: "Mis favoritos" guarda sobre
 * todo artistas, así que busca por artista aunque no sea el listado de estos.
 */
@Composable
private fun PlaylistListRoute(
    viewModel: BasePlaylistListViewModel,
    navController: NavHostController,
    contentPadding: PaddingValues,
    @StringRes searchPlaceholderRes: Int,
    @StringRes emptyMessageRes: Int,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloaded by viewModel.downloadedPlaylists.collectAsStateWithLifecycle()

    // Al volver a la pestaña se revalida; si la caché sigue fresca no hay red.
    LaunchedEffect(Unit) { viewModel.load() }

    PlaylistListScreen(
        state = state,
        downloadedPlaylists = downloaded,
        searchPlaceholder = stringResource(searchPlaceholderRes),
        emptyMessage = stringResource(emptyMessageRes),
        onSearchChange = viewModel::onSearchChange,
        onSearchSubmit = viewModel::onSearchSubmit,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPlaylistClick = { navController.navigate(PlaylistDetailRoute(it.id)) },
        onDownloadedClick = { navController.navigate(PlaylistDetailRoute(it.id)) },
        onToggleFavorite = viewModel::toggleFavorite,
        contentPadding = contentPadding,
    )
}
