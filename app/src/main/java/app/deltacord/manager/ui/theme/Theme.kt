package app.deltacord.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Deltarune-style palette: pitch-black dark world, white text and box borders,
// white for whatever is selected, orange menu buttons, red for danger.
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Orange = Color(0xFFFF7F27)
private val Red = Color(0xFFFF0000)
private val Cyan = Color(0xFF00C8FF)

val DeltaruneColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = White,
    inversePrimary = Orange,
    secondary = Orange,
    onSecondary = Black,
    secondaryContainer = Color(0xFF3A1C08),
    onSecondaryContainer = Orange,
    tertiary = Cyan,
    onTertiary = Black,
    tertiaryContainer = Color(0xFF00303D),
    onTertiaryContainer = Cyan,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Color(0xFF14141C),
    onSurfaceVariant = Color(0xFFC8C8D0),
    surfaceTint = Black,
    inverseSurface = White,
    inverseOnSurface = Black,
    error = Red,
    onError = Black,
    errorContainer = Color(0xFF3D0000),
    onErrorContainer = Color(0xFFFF6B6B),
    outline = White,
    outlineVariant = Color(0xFF55556A),
    scrim = Black,
    surfaceBright = Color(0xFF22222C),
    surfaceDim = Black,
    surfaceContainerLowest = Black,
    surfaceContainerLow = Color(0xFF08080C),
    surfaceContainer = Color(0xFF0E0E14),
    surfaceContainerHigh = Color(0xFF14141C),
    surfaceContainerHighest = Color(0xFF1C1C26),
)

// Boxy, like the game's text boxes and menus.
private val DeltaruneShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)
