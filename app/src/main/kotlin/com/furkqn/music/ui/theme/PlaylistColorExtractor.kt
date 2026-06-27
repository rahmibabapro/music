/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlaylistColorExtractor {
    private val SpotifyBlack = Color(0xFF121212)
    private val SpotifyDarkGray = Color(0xFF1A1A1A)

    suspend fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int,
        bottomColor: Color = SpotifyBlack,
    ): List<Color> = withContext(Dispatchers.Default) {
        val swatch =
            listOfNotNull(
                palette.darkMutedSwatch,
                palette.mutedSwatch,
                palette.darkVibrantSwatch,
                palette.dominantSwatch,
            ).maxByOrNull { it.population }
                ?: palette.dominantSwatch

        val baseColor =
            if (swatch != null) {
                softenForPlaylist(Color(swatch.rgb))
            } else {
                softenForPlaylist(Color(palette.getDominantColor(fallbackColor)))
            }

        listOf(
            baseColor,
            Color(
                red = baseColor.red * 0.5f,
                green = baseColor.green * 0.5f,
                blue = baseColor.blue * 0.5f,
                alpha = 1f,
            ),
            SpotifyDarkGray,
            bottomColor,
        )
    }

    fun gradientColorStops(colors: List<Color>): Array<Pair<Float, Color>> =
        if (colors.size >= 4) {
            arrayOf(
                0.0f to colors[0],
                0.35f to colors[1],
                0.65f to colors[2],
                1.0f to colors[3],
            )
        } else if (colors.isNotEmpty()) {
            arrayOf(
                0.0f to colors[0],
                0.5f to colors.getOrElse(1) { colors[0].copy(alpha = 0.7f) },
                1.0f to SpotifyBlack,
            )
        } else {
            arrayOf(
                0.0f to SpotifyBlack,
                1.0f to SpotifyBlack,
            )
        }

    private fun softenForPlaylist(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (hsv[1] * 0.6f).coerceIn(0.15f, 0.75f)
        hsv[2] = hsv[2].coerceIn(0.35f, 0.50f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
