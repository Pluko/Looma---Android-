package com.yourlooma.vault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = LoomaAccent,
    onPrimary = Color.Black,
    secondary = LoomaAccent,
    onSecondary = Color.Black,
    background = LoomaInk,
    onBackground = LoomaText,
    surface = LoomaSurface,
    onSurface = LoomaText,
    outline = LoomaOutline
)

val LoomaShapes = Shapes(
    small  = RoundedCornerShape(8),
    medium = RoundedCornerShape(14),
    large  = RoundedCornerShape(24)
)

@Composable
fun LoomaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        shapes = LoomaShapes,
        typography = LoomaTypography,   // <- use our fixed Typography
        content = content
    )
}
