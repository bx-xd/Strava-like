package io.github.bx_xd.velotrack.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand colors ──────────────────────────────────────────────────
val AccentOrange   = Color(0xFFFF4D00)
val AccentOrange2  = Color(0xFFFF7A33)
val BgDark         = Color(0xFF0A0A0F)
val BgCard         = Color(0xFF12121A)
val BgCard2        = Color(0xFF1A1A26)
val BorderColor    = Color(0xFF2A2A3A)
val TextPrimary    = Color(0xFFF0F0F5)
val TextMuted      = Color(0xFF6B6B85)
val GreenGps       = Color(0xFF00E676)
val BlueWind       = Color(0xFF2979FF)
val YellowPause    = Color(0xFFFFD600)
val RedStop        = Color(0xFFFF3B30)

private val DarkColorScheme = darkColorScheme(
    primary          = AccentOrange,
    onPrimary        = Color.White,
    secondary        = AccentOrange2,
    onSecondary      = Color.White,
    background       = BgDark,
    onBackground     = TextPrimary,
    surface          = BgCard,
    onSurface        = TextPrimary,
    surfaceVariant   = BgCard2,
    onSurfaceVariant = TextMuted,
    outline          = BorderColor,
    error            = RedStop,
)

@Composable
fun VeloTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}
