package com.zice.playbutton.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de PlayButton, portada del sistema de la web
 * (frontend-web/src/styles/_palette.scss) para que app y web se lean como el
 * mismo producto.
 *
 * Los colores de marca están muestreados del logo: el degradado va de
 * Brand200 a Brand500. Cada tono tiene un papel fijo y no son intercambiables:
 * Brand500 es la marca pero falla el contraste AA como texto (3,91:1 sobre
 * Surface0), así que el texto de acento usa Brand400 y el relleno de botón
 * usa Brand600, que sí pasa con blanco encima.
 */

// Marca
val Brand200 = Color(0xFFE5B2F8) // extremo claro del degradado; hover de enlaces
val Brand300 = Color(0xFFC54BF2) // punto medio del degradado
val Brand400 = Color(0xFFD08BF5) // TEXTO de acento y enlaces (8,27:1, AAA)
val Brand500 = Color(0xFFB000F0) // marca canónica: glows, bordes. Nunca como texto
val Brand600 = Color(0xFF9B00D4) // relleno del botón primario (blanco encima, 6,28:1)
val Brand700 = Color(0xFF7A00A8) // pressed/hover del primario

// Superficies. Negro con tinte violeta, no negro puro: los saltos son
// deliberadamente sutiles y la jerarquía la marcan el borde y la sombra.
val Surface0 = Color(0xFF0A0710) // fondo raíz
val Surface1 = Color(0xFF120D19) // tarjetas, contenedores, modales
val Surface2 = Color(0xFF1A1424) // inputs, filas de listado
val Surface3 = Color(0xFF241C31) // hover, estados activos
val BorderSubtle = Color(0xFF2E2440)
val BorderStrong = Color(0xFF45375C)

// Texto
val Text1 = Color(0xFFF2EDF7) // titulares y texto principal
val Text2 = Color(0xFFBDB3CC) // cuerpo
val Text3 = Color(0xFF8E85A0) // atenuado, placeholders

// Semánticos: cada rol tiene un relleno (con blanco encima) y un tono de
// texto legible sobre superficie oscura.
val DangerFill = Color(0xFFC42B3F)
val DangerText = Color(0xFFFF6B7A)
val SuccessFill = Color(0xFF0F7A42)
val SuccessText = Color(0xFF5BE39B)
val InfoFill = Color(0xFF1F7A8C)
val InfoText = Color(0xFF7BD8FF)
val WarningFill = Color(0xFFFFC107) // único con texto negro encima

/** Degradado de marca del logo, para la marca de agua y los acentos. */
val BrandGradient = listOf(Brand200, Brand300, Brand500)
