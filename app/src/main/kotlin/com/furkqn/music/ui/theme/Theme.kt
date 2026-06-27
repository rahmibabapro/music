/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

@Composable
fun MusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useBrandScheme = themeColor == DefaultThemeColor

    val baseColorScheme = when {
        useBrandScheme && darkTheme -> YoutubeMusicDarkScheme
        useBrandScheme && !darkTheme -> YoutubeMusicDarkScheme.copy(
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF0F0F0F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F0F0F),
            surfaceContainer = Color(0xFFF1F1F1),
            surfaceContainerHigh = Color(0xFFE5E5E5),
            onSurfaceVariant = Color(0xFF606060),
            primary = Color(0xFF0F0F0F),
            onPrimary = Color(0xFFFFFFFF),
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> rememberDynamicColorScheme(
            seedColor = themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot,
        )
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme, useBrandScheme) {
        when {
            useBrandScheme && darkTheme -> baseColorScheme.pureBlack(true)
            darkTheme && pureBlack -> baseColorScheme.pureBlack(true)
            else -> baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black,
        surfaceContainer = Color(0xFF0F0F0F),
        surfaceContainerLow = Color.Black,
        surfaceContainerLowest = Color.Black,
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
