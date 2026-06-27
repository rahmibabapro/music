/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.furkqn.music.ui.theme.PlaylistColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlaylistGradientBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    cacheKey: String? = thumbnailUrl,
    bottomColor: Color = Color(0xFF121212),
) {
    val context = LocalContext.current
    var gradientColors by remember(cacheKey) { mutableStateOf<List<Color>>(emptyList()) }
    val colorCache = remember { mutableMapOf<String, List<Color>>() }
    val fallbackColor = Color(0xFF121212).hashCode()

    LaunchedEffect(cacheKey, thumbnailUrl) {
        if (thumbnailUrl.isNullOrBlank()) {
            gradientColors = emptyList()
            return@LaunchedEffect
        }
        val key = cacheKey ?: thumbnailUrl
        colorCache[key]?.let {
            gradientColors = it
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(thumbnailUrl)
                    .size(200, 200)
                    .allowHardware(false)
                    .memoryCacheKey("playlist_gradient_$key")
                    .build()
            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            val bitmap = result?.image?.toBitmap()
            if (bitmap != null) {
                val palette =
                    withContext(Dispatchers.Default) {
                        Palette
                            .from(bitmap)
                            .maximumColorCount(12)
                            .resizeBitmapArea(200 * 200)
                            .generate()
                    }
                val extracted =
                    PlaylistColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor,
                        bottomColor = bottomColor,
                    )
                colorCache[key] = extracted
                withContext(Dispatchers.Main) { gradientColors = extracted }
            }
        }
    }

    Box(modifier = modifier) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(context)
                        .data(thumbnailUrl)
                        .size(200, 200)
                        .allowHardware(false)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(0.12f)
                        .blur(120.dp),
            )
        }

        AnimatedContent(
            targetState = gradientColors,
            transitionSpec = {
                fadeIn(tween(600)).togetherWith(fadeOut(tween(600)))
            },
            label = "playlistGradient",
            modifier = Modifier.fillMaxSize(),
        ) { colors ->
            if (colors.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = PlaylistColorExtractor.gradientColorStops(colors),
                                ),
                            ),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(bottomColor),
                )
            }
        }
    }
}
