/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SnakeAmbientBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    cacheKey: String? = thumbnailUrl,
    bottomColor: Color = Color(0xFF0A0A0A),
    boostForFoodPreview: Boolean = false,
) {
    val strength = if (boostForFoodPreview) 1.65f else 1.25f
    val context = LocalContext.current
    var activeColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var outgoingColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val colorBlend = remember { Animatable(1f) }
    val colorCache = remember { mutableMapOf<String, List<Color>>() }
    val fallbackColor = Color(0xFF121212).hashCode()

    LaunchedEffect(cacheKey, thumbnailUrl) {
        if (thumbnailUrl.isNullOrBlank()) {
            activeColors = emptyList()
            outgoingColors = emptyList()
            return@LaunchedEffect
        }
        val key = cacheKey ?: thumbnailUrl
        val extracted =
            colorCache[key] ?: withContext(Dispatchers.IO) {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(thumbnailUrl)
                        .size(256, 256)
                        .allowHardware(false)
                        .memoryCacheKey("snake_ambient_$key")
                        .build()
                val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                val bitmap = result?.image?.toBitmap()
                if (bitmap != null) {
                    val palette =
                        withContext(Dispatchers.Default) {
                            Palette
                                .from(bitmap)
                                .maximumColorCount(20)
                                .resizeBitmapArea(256 * 256)
                                .generate()
                        }
                    PlaylistColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor,
                        bottomColor = bottomColor,
                    ).also { colorCache[key] = it }
                } else {
                    emptyList()
                }
            }
        if (extracted.isEmpty()) return@LaunchedEffect
        if (activeColors.isEmpty() || activeColors == extracted) {
            activeColors = extracted
            outgoingColors = emptyList()
            colorBlend.snapTo(1f)
        } else {
            outgoingColors = activeColors
            activeColors = extracted
            colorBlend.snapTo(0f)
            colorBlend.animateTo(1f, animationSpec = tween(durationMillis = 2_400, easing = LinearEasing))
            outgoingColors = emptyList()
            colorBlend.snapTo(1f)
        }
    }

    val blendProgress = colorBlend.value

    val infiniteTransition = rememberInfiniteTransition(label = "snakeAmbient")
    val driftA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 11_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "driftA",
    )
    val driftB by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 7_500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "driftB",
    )
    val swirl by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 36_000, easing = LinearEasing),
            ),
        label = "swirl",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 4_200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse",
    )

    Box(modifier = modifier) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(context)
                        .data(thumbnailUrl)
                        .size(256, 256)
                        .allowHardware(false)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha((0.32f * strength * pulse).coerceAtMost(0.55f))
                        .blur((128.dp / strength.coerceAtLeast(1f)).coerceAtLeast(88.dp)),
            )
        }

        if (outgoingColors.isNotEmpty() && blendProgress < 0.99f) {
            AmbientGradientLayer(
                colors = outgoingColors,
                strength = strength,
                driftA = driftA,
                driftB = driftB,
                swirl = swirl,
                pulse = pulse,
                alpha = 1f - blendProgress,
            )
        }
        if (activeColors.isNotEmpty()) {
            AmbientGradientLayer(
                colors = activeColors,
                strength = strength,
                driftA = driftA,
                driftB = driftB,
                swirl = swirl,
                pulse = pulse,
                alpha = if (outgoingColors.isEmpty()) 1f else blendProgress,
            )
        }
        if (activeColors.isEmpty() && outgoingColors.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(bottomColor),
            )
        }
    }
}

@Composable
private fun AmbientGradientLayer(
    colors: List<Color>,
    strength: Float,
    driftA: Float,
    driftB: Float,
    swirl: Float,
    pulse: Float,
    alpha: Float,
) {
    val primary = colors[0]
    val secondary = colors.getOrElse(1) { primary.copy(alpha = 0.7f) }
    val tertiary = colors.getOrElse(2) { Color(0xFF1A1A1A) }
    val swirlRad = Math.toRadians(swirl.toDouble())
    val cx1 = 0.22f + driftA * 0.56f
    val cy1 = 0.12f + driftB * 0.5f
    val cx2 = 0.78f - driftB * 0.48f
    val cy2 = 0.58f + driftA * 0.38f
    val orbitX = 0.5f + cos(swirlRad).toFloat() * 0.22f
    val orbitY = 0.38f + sin(swirlRad).toFloat() * 0.26f
    val shimmerX = 0.35f + sin(swirlRad * 0.7).toFloat() * 0.15f
    val shimmerY = 0.62f + cos(swirlRad * 0.5).toFloat() * 0.12f

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(alpha)
                .drawBehind {
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colorStops = PlaylistColorExtractor.gradientColorStops(colors),
                            ),
                    )
                    drawRect(
                        brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0f to primary.copy(alpha = (0.58f * strength * pulse).coerceAtMost(0.78f)),
                                        0.55f to secondary.copy(alpha = (0.36f * strength).coerceAtMost(0.52f)),
                                        1f to Color.Transparent,
                                    ),
                                center = Offset(size.width * cx1, size.height * cy1),
                                radius = size.maxDimension * 0.95f,
                            ),
                    )
                    drawRect(
                        brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0f to secondary.copy(alpha = (0.5f * strength * pulse).coerceAtMost(0.68f)),
                                        0.6f to tertiary.copy(alpha = (0.26f * strength).coerceAtMost(0.4f)),
                                        1f to Color.Transparent,
                                    ),
                                center = Offset(size.width * cx2, size.height * cy2),
                                radius = size.maxDimension * 0.88f,
                            ),
                    )
                    drawRect(
                        brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0f to primary.copy(alpha = (0.42f * strength).coerceAtMost(0.6f)),
                                        1f to Color.Transparent,
                                    ),
                                center = Offset(size.width * orbitX, size.height * orbitY),
                                radius = size.maxDimension * 0.68f,
                            ),
                    )
                    drawRect(
                        brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0f to Color.White.copy(alpha = 0.06f * pulse),
                                        0.4f to primary.copy(alpha = 0.12f * pulse),
                                        1f to Color.Transparent,
                                    ),
                                center = Offset(size.width * shimmerX, size.height * shimmerY),
                                radius = size.maxDimension * 0.45f,
                            ),
                    )
                },
    )
}
