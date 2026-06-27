/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** YouTube Music–inspired dark palette: pure black surfaces, white primary text. */
val DefaultThemeColor = Color(0xFFFFFFFF)

val YoutubeMusicDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF272727),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFB3B3B3),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF282828),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFB3B3B3),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0F0F0F),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF212121),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainer = Color(0xFF0F0F0F),
    surfaceContainerHigh = Color(0xFF212121),
    surfaceContainerHighest = Color(0xFF272727),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color(0xFF000000),
    outline = Color(0xFF3F3F3F),
    outlineVariant = Color(0xFF272727),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
)

/** Spotify-inspired accent for chips / highlights. */
val MusicAccentGreen = Color(0xFF1DB954)
