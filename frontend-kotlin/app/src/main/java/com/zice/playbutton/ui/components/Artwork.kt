package com.zice.playbutton.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import coil3.compose.AsyncImage
import com.zice.playbutton.ui.theme.Brand400
import com.zice.playbutton.ui.theme.Brand500

enum class ArtworkKind { Song, Artist }

/**
 * Portada de una canción, artista o playlist.
 *
 * El hueco es el cuadro de acento con icono que ya usaban las tarjetas, y se
 * queda debajo: mientras la imagen carga —o si no hay— es lo que se ve, así
 * que la fila nunca parpadea en blanco ni cambia de tamaño.
 */
@Composable
fun Artwork(
    imageUrl: String?,
    kind: ArtworkKind,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brand500.copy(alpha = 0.12f), shape)
            .border(1.dp, Brand500.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center,
    ) {
        // El icono se queda siempre debajo, no solo cuando falta la URL: es lo
        // que se ve mientras la portada carga, y lo que queda si no llega a
        // cargar. Antes un fallo de red dejaba el cuadro vacio, que no se
        // distingue de una portada en blanco.
        Icon(
            imageVector = when (kind) {
                ArtworkKind.Artist -> Icons.Filled.Person
                ArtworkKind.Song -> Icons.Filled.MusicNote
            },
            contentDescription = null,
            tint = Brand400,
        )

        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}