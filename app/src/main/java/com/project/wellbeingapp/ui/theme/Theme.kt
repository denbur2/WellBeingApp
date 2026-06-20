package com.project.wellbeingapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Festes Terminal-/ASCII-Theme: weiße Schrift auf schwarzem Hintergrund,
 * grüner Akzent, Monospace. Kein Dynamic Color / kein Light-Mode — bewusst,
 * damit der Look auf allen Geräten identisch ist und zu den ASCII-Bäumen passt.
 */
private val TerminalColorScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = TerminalBackground,
    secondary = TerminalGreen,
    onSecondary = TerminalBackground,
    background = TerminalBackground,
    onBackground = TerminalForeground,
    surface = TerminalSurface,
    onSurface = TerminalForeground,
    surfaceVariant = TerminalSurface,
    onSurfaceVariant = TerminalDim,
    error = TerminalError,
    onError = TerminalBackground,
    outline = TerminalDim
)

@Composable
fun WellbeingAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = Typography,
        content = content
    )
}
