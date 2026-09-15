package com.zice.playbutton.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * PlayButton es una app de escucha: como la web, se compromete con un único
 * tema oscuro en lugar de seguir al sistema. `isSystemInDarkTheme` no se
 * consulta a propósito.
 */
private val PlayButtonColorScheme = darkColorScheme(
    primary = Brand600,
    onPrimary = Text1,
    primaryContainer = Brand700,
    onPrimaryContainer = Brand200,
    inversePrimary = Brand400,

    secondary = BorderStrong,
    onSecondary = Text1,
    secondaryContainer = Surface3,
    onSecondaryContainer = Text1,

    tertiary = Brand400,
    onTertiary = Surface0,
    tertiaryContainer = Surface3,
    onTertiaryContainer = Brand200,

    background = Surface0,
    onBackground = Text1,

    surface = Surface0,
    onSurface = Text1,
    surfaceVariant = Surface2,
    onSurfaceVariant = Text2,
    surfaceContainerLowest = Surface0,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,

    outline = BorderSubtle,
    outlineVariant = BorderStrong,

    error = DangerText,
    onError = Surface0,
    errorContainer = DangerFill,
    onErrorContainer = Text1,

    scrim = androidx.compose.ui.graphics.Color.Black,
)

/** Radios de la web: 6 / 10 / 14 / 20 px. */
private val PlayButtonShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun PlayButtonTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Iconos claros de barra de estado y navegación: el fondo siempre es oscuro.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = PlayButtonColorScheme,
        typography = PlayButtonTypography,
        shapes = PlayButtonShapes,
        content = content,
    )
}
