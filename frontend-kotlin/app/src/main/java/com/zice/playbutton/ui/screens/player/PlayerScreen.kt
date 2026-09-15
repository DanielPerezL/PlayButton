package com.zice.playbutton.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zice.playbutton.R
import com.zice.playbutton.player.PlayerState
import com.zice.playbutton.ui.components.Artwork
import com.zice.playbutton.ui.components.ArtworkKind
import com.zice.playbutton.ui.theme.Brand500
import com.zice.playbutton.util.formatTime

/**
 * Reproductor a pantalla completa.
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

    // Con la cola abierta la carátula cede su sitio: es decorativa —el backend
    // no sirve portadas— y ocupaba casi la mitad de la pantalla, así que la
    // lista se quedaba con dos o tres filas. Encogida caben unas diez y la
    // cola pasa a ser algo que de verdad se puede recorrer.
    var queueVisible by remember { mutableStateOf(false) }
    val artworkFraction by animateFloatAsState(
        targetValue = if (queueVisible) 0.3f else 0.72f,
        label = "artwork",
    )

    val duration = state.durationMs.coerceAtLeast(0L)
    val displayedPosition = if (isSeeking) scrubPosition.toLong() else positionMs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
            IconButton(onClick = onCollapse, modifier = Modifier.size(64.dp)) {
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
                IconButton(onClick = onPlayZen, modifier = Modifier.size(64.dp)) {
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
                Spacer(Modifier.size(64.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Carátula. Sin portada se queda el isotipo sobre un halo del color de
        // marca, que es lo que había antes de que el backend las sirviera. El
        // isotipo se queda también debajo de la portada: es lo que se ve
        // mientras carga y lo que queda si no llega a cargar, en vez del halo
        // vacío.
        Box(
            modifier = Modifier
                .fillMaxWidth(artworkFraction)
                .aspectRatio(1f)
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

            state.artworkUri?.let { artworkUri ->
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

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

        Spacer(Modifier.height(20.dp))

        Slider(
            value = displayedPosition.toFloat(),
            onValueChange = { value ->
                isSeeking = true
                scrubPosition = value
            },
            onValueChangeFinished = {
                onSeek(scrubPosition.toLong())
                isSeeking = false
            },
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

        Spacer(Modifier.height(20.dp))

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
                    .size(72.dp)
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

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.clickable { queueVisible = !queueVisible }.padding(8.dp),
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

        // La cola lleva delante las tres últimas que ya han sonado, así que se
        // abre colocada en la canción actual: por delante quedan las
        // siguientes y subiendo se ve de dónde viene.
        val queueListState = rememberLazyListState()
        LaunchedEffect(queueVisible) {
            if (queueVisible && state.queue.isNotEmpty()) {
                queueListState.scrollToItem(
                    state.currentIndex.coerceIn(0, state.queue.lastIndex),
                )
            }
        }

        // La cola se queda con el hueco que sobra de la pantalla, y hay que
        // dárselo explícitamente: sin el peso medía lo que midiera su
        // contenido, la carátula y los controles se comían el sitio y quedaban
        // una o dos filas asomando. Con tan poco recorrido el arrastre no
        // llegaba a ninguna parte y el scroll parecía no responder.
        AnimatedVisibility(visible = queueVisible, modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = queueListState,
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
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
                            // El sitio real en el reproductor, no el de esta
                            // lista: lo escuchado se enseña recortado.
                            .clickable { onQueueItemClick(entry.index) }
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
    }
}
