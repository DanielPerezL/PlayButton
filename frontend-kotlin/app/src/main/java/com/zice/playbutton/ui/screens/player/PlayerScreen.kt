package com.zice.playbutton.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil3.compose.AsyncImage
import com.zice.playbutton.R
import com.zice.playbutton.player.PlayerState
import com.zice.playbutton.ui.components.Artwork
import com.zice.playbutton.ui.components.ArtworkKind
import com.zice.playbutton.ui.theme.Brand500
import com.zice.playbutton.util.formatTime

/**
 * Lo que mide de alto la cabecera: los 64dp del botón de contraer, que es lo
 * que la estira.
 */
private val HeaderHeight = 64.dp

/**
 * Por debajo de este alto se aprieta todo un poco: separaciones más cortas y
 * botones algo menores. Es lo que hace que en un móvil tumbado quepan los
 * mandos enteros, que es justo lo que no cabía.
 */
private val CompactBelow = 560.dp

/**
 * Lo que piden de alto los mandos —los dos textos, la barra de progreso, los
 * tiempos, los botones y el conmutador de la cola— con las separaciones de
 * cada caso.
 *
 * Se reparte antes que la carátula a propósito. Puestos a elegir, sin la
 * carátula el reproductor se sigue usando y sin los botones no, así que es
 * ella la que se queda con lo que sobre.
 */
private val DetailsHeight = 284.dp
private val DetailsHeightCompact = 236.dp

/**
 * Por debajo de esto la carátula no se dibuja. Un sello de dos dedos no enseña
 * nada y el sitio le hace más falta a lo de abajo.
 */
private val MinArtwork = 96.dp

/**
 * Reproductor a pantalla completa.
 *
 * Se coloca siempre igual —cabecera, carátula, lo que suena y los mandos, uno
 * debajo de otro—, y lo que cambia son las medidas: la carátula se queda con
 * el alto que sobra y desaparece si no sobra nada, y con poco alto las
 * separaciones se acortan. Así no hay una pantalla para cada orientación, sino
 * la misma ajustándose al hueco que tenga.
 *
 * La única que se mueve de sitio es la cola, y solo al abrirla: si hay más
 * ancho que alto se pone al costado en vez de debajo. Tumbado el alto se acaba
 * enseguida y debajo no le caben ni dos canciones, mientras que el ancho está
 * sin usar.
 */
@Composable
fun PlayerScreen(
    state: PlayerState,
    positionMs: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onCollapse: () -> Unit,
    onPlayZen: () -> Unit,
    zenAvailable: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // Mientras se arrastra, el pulgar sigue al dedo y se ignora la posición
    // que reporta el reproductor. La app React Native ataba el valor del
    // slider directamente al progreso, que se refrescaba cada segundo, así que
    // el pulgar saltaba hacia atrás y no había manera de buscar.
    var isSeeking by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    var queueVisible by remember { mutableStateOf(false) }

    val duration = state.durationMs.coerceAtLeast(0L)
    val displayedPosition = if (isSeeking) scrubPosition.toLong() else positionMs

    // La cola lleva delante las tres últimas que ya han sonado, así que se
    // abre colocada en la canción actual: por delante quedan las siguientes y
    // subiendo se ve de dónde viene.
    val queueListState = rememberLazyListState()
    LaunchedEffect(queueVisible) {
        if (queueVisible) queueListState.scrollToCurrent(state)
    }

    // Saltar a una canción de la cola recorta la lista por delante —solo se
    // guardan tres ya escuchadas—, así que la fila pulsada cambia de sitio y
    // la lista parecía haberse movido sola a otra parte de la cola. Se
    // recoloca igual que al abrirla, con lo pulsado arriba del todo.
    //
    // En el toque no se puede: la orden sale por la sesión de medios y el
    // estado no vuelve hasta el siguiente ciclo, así que se apunta a qué
    // canción se va y se recoloca cuando de verdad esté sonando.
    var pendingSongId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingSongId, state.songId, state.currentIndex) {
        if (pendingSongId == null || state.songId != pendingSongId) {
            return@LaunchedEffect
        }
        queueListState.scrollToCurrent(state)
        pendingSongId = null
    }

    val onQueueEntryClick: (Int, Int) -> Unit = { songId, index ->
        pendingSongId = songId
        onQueueItemClick(index)
    }

    // Con la cola cerrada puede que los mandos no quepan —media pantalla, por
    // ejemplo—, y entonces se llega a ellos rodando. Abierta manda ella, que
    // se lleva el hueco que sobra y trae su propio arrastre.
    val scrollState = rememberScrollState()
    val besideScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // safeDrawing y no systemBars: este también esquiva la muesca,
            // que en un móvil de lado se come un canto entero de la pantalla.
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        val compact = maxHeight < CompactBelow
        val gapLarge = if (compact) 10.dp else 20.dp
        val gapSmall = if (compact) 8.dp else 16.dp
        val playSize = if (compact) 60.dp else 72.dp
        val detailsHeight = if (compact) DetailsHeightCompact else DetailsHeight

        // Al costado solo cuando hay más ancho que alto. No se mira la
        // orientación declarada sino el hueco de verdad, que es lo que también
        // vale para media pantalla.
        val queueBeside = queueVisible && maxWidth > maxHeight

        // Encogida deja sitio a la cola cuando esta va debajo. Si se ha ido al
        // costado no le quita nada, así que se queda entera.
        val artworkFraction by animateFloatAsState(
            targetValue = if (queueVisible && !queueBeside) 0.3f else 0.72f,
            label = "artwork",
        )

        // Con la cola al costado, el cuerpo se queda con media pantalla de
        // ancho y hay que medirlo sobre eso.
        val bodyWidth = if (queueBeside) (maxWidth - gapLarge) / 2f else maxWidth

        // Por el lado corto, que de pie es el ancho, y nunca más de lo que
        // dejen los mandos. En una pantalla normal sobra alto y sale la de
        // siempre; en una pantalla baja no sobra nada y desaparece, que es lo
        // que hace que quepa el resto.
        val artworkRoom = min(
            min(bodyWidth, maxHeight) * artworkFraction,
            maxHeight - HeaderHeight - detailsHeight - gapLarge * 2,
        )
        val artworkSide = artworkRoom.coerceAtLeast(0.dp)
        val showArtwork = artworkSide >= MinArtwork

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (queueVisible) Modifier else Modifier.verticalScroll(scrollState),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerHeader(
                state = state,
                zenAvailable = zenAvailable,
                onCollapse = onCollapse,
                onPlayZen = onPlayZen,
            )

            // El cuerpo es el mismo se ponga donde se ponga la cola: carátula
            // si cabe y los mandos debajo.
            val body: @Composable ColumnScope.(inlineQueue: Boolean) -> Unit = { inlineQueue ->
                if (showArtwork) {
                    Spacer(Modifier.height(gapLarge))
                    PlayerArtwork(artworkUri = state.artworkUri, side = artworkSide)
                    Spacer(Modifier.height(gapLarge))
                }
                PlayerDetails(
                    state = state,
                    displayedPosition = displayedPosition,
                    duration = duration,
                    gapLarge = gapLarge,
                    gapSmall = gapSmall,
                    playSize = playSize,
                    onScrub = { value ->
                        isSeeking = true
                        scrubPosition = value
                    },
                    onScrubFinished = {
                        onSeek(scrubPosition.toLong())
                        isSeeking = false
                    },
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    queueVisible = queueVisible,
                    onToggleQueue = { queueVisible = !queueVisible },
                    queueListState = queueListState,
                    onQueueEntryClick = onQueueEntryClick,
                    inlineQueue = inlineQueue,
                )
            }

            if (queueBeside) {
                Row(modifier = Modifier.fillMaxSize()) {
                    PlayerQueue(
                        state = state,
                        queueListState = queueListState,
                        onQueueEntryClick = onQueueEntryClick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    Spacer(Modifier.width(gapLarge))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            // Aquí no hay cola que se lleve el hueco, así que
                            // si los mandos no cupieran se llega a ellos
                            // rodando, igual que con la cola cerrada.
                            .verticalScroll(besideScrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        body(false)
                    }
                }
            } else {
                body(true)
            }
        }
    }
}

/**
 * Contraer, de dónde sale lo que suena y el atajo al Modo Zen.
 */
@Composable
private fun PlayerHeader(
    state: PlayerState,
    zenAvailable: Boolean,
    onCollapse: () -> Unit,
    onPlayZen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Contraer es el gesto que más se usa aquí y el icono por defecto
        // se queda en los 24dp de Material, con los 48dp de área mínima:
        // se le da un área de 64dp y un icono grande para que se acierte
        // sin afinar con el dedo, aunque se venga de la parte de abajo de
        // la pantalla.
        IconButton(onClick = onCollapse, modifier = Modifier.size(HeaderHeight)) {
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = stringResource(R.string.player_collapse),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = state.sourceName ?: stringResource(R.string.player_zen_mode),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // El hueco que equilibraba el boton de contraer se aprovecha para
        // relanzar el Modo Zen sin salir del reproductor. Si ya se esta en
        // Zen se resalta, y pulsarlo pide otra tanda del catalogo.
        //
        // Sin servidor no hay Zen posible —hay que pedir el catálogo—, así
        // que el botón se va y queda solo el hueco: lo que suena puede
        // seguir sonando desde el dispositivo.
        val inZenMode = state.sourceName == null
        if (zenAvailable) {
            IconButton(onClick = onPlayZen, modifier = Modifier.size(HeaderHeight)) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.player_zen_play),
                    modifier = Modifier.size(28.dp),
                    tint = if (inZenMode) {
                        MaterialTheme.colorScheme.inversePrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        } else {
            Spacer(Modifier.size(HeaderHeight))
        }
    }
}

/**
 * Carátula. Sin portada se queda el isotipo sobre un halo del color de marca,
 * que es lo que había antes de que el backend las sirviera. El isotipo se
 * queda también debajo de la portada: es lo que se ve mientras carga y lo que
 * queda si no llega a cargar, en vez del halo vacío.
 */
@Composable
private fun PlayerArtwork(artworkUri: String?, side: Dp) {
    Box(
        modifier = Modifier
            .size(side)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        Brand500.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.default_artwork),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.55f),
        )

        artworkUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/**
 * Lo que suena, por dónde va, los botones y el conmutador de la cola.
 */
@Composable
private fun ColumnScope.PlayerDetails(
    state: PlayerState,
    displayedPosition: Long,
    duration: Long,
    gapLarge: Dp,
    gapSmall: Dp,
    playSize: Dp,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    queueVisible: Boolean,
    onToggleQueue: () -> Unit,
    queueListState: LazyListState,
    onQueueEntryClick: (songId: Int, index: Int) -> Unit,
    inlineQueue: Boolean,
) {
    Text(
        text = when {
            state.title.isNotBlank() -> state.title
            state.isBuffering -> stringResource(R.string.player_loading)
            else -> stringResource(R.string.player_nothing)
        },
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = state.artist ?: stringResource(R.string.player_unknown_artist),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    Spacer(Modifier.height(gapLarge))

    Slider(
        value = displayedPosition.toFloat(),
        onValueChange = onScrub,
        onValueChangeFinished = onScrubFinished,
        valueRange = 0f..(duration.takeIf { it > 0 } ?: 1L).toFloat(),
        enabled = state.hasContent && duration > 0,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.inversePrimary,
            activeTrackColor = MaterialTheme.colorScheme.inversePrimary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(displayedPosition),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(gapLarge))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(playSize)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = state.hasContent, onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Icon(
                    imageVector = if (state.isPlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = stringResource(
                        if (state.isPlaying) R.string.player_pause else R.string.player_play,
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp),
            )
        }
    }

    Spacer(Modifier.height(gapSmall))

    Row(
        modifier = Modifier.clickable { onToggleQueue() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.inversePrimary,
        )
        Text(
            text = stringResource(
                if (queueVisible) R.string.player_queue_hide else R.string.player_queue_show,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.inversePrimary,
        )
    }

    // Debajo se lleva el hueco que sobra, y hay que dárselo explícitamente:
    // sin el peso medía lo que midiera su contenido, la carátula y los mandos
    // se comían el sitio y quedaban una o dos filas asomando. Con tan poco
    // recorrido el arrastre no llegaba a ninguna parte y el scroll parecía no
    // responder.
    if (inlineQueue && queueVisible) {
        PlayerQueue(
            state = state,
            queueListState = queueListState,
            onQueueEntryClick = onQueueEntryClick,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

/**
 * Lo que hay en la tanda: lo que suena resaltado, lo ya escuchado apagado
 * encima y lo que viene debajo.
 */
@Composable
private fun PlayerQueue(
    state: PlayerState,
    queueListState: LazyListState,
    onQueueEntryClick: (songId: Int, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = queueListState,
        modifier = modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(state.queue) { index, entry ->
            val isCurrent = index == state.currentIndex
            val isPlayed = index < state.currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    )
                    // El sitio real en el reproductor, no el de esta lista: lo
                    // escuchado se enseña recortado.
                    .clickable { onQueueEntryClick(entry.songId, entry.index) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Artwork(
                    imageUrl = entry.artworkUri,
                    kind = ArtworkKind.Song,
                    size = 32.dp,
                    shape = MaterialTheme.shapes.extraSmall,
                )
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    // Lo ya escuchado se apaga: sigue estando para poder
                    // volver a él, pero no compite con lo que viene.
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                        isPlayed -> MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Coloca la cola en lo que suena: pasa a ser la primera fila a la vista y lo ya
 * escuchado se queda justo encima, para subir a por ello si hace falta.
 */
private suspend fun LazyListState.scrollToCurrent(state: PlayerState) {
    if (state.queue.isEmpty()) return
    scrollToItem(state.currentIndex.coerceIn(0, state.queue.lastIndex))
}
