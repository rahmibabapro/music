/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.screens.game

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.furkqn.music.LocalPlayerConnection
import com.furkqn.music.R
import com.furkqn.music.constants.MiniPlayerHeight
import com.furkqn.music.game.EatPlaylistSnakeEngine
import com.furkqn.music.game.EatPlaylistSong
import com.furkqn.music.game.GridCell
import com.furkqn.music.game.SnakeDirection
import com.furkqn.music.game.SnakePlaybackTransition
import com.furkqn.music.game.SnakeSegment
import com.furkqn.music.extensions.indexOfMediaItemById
import com.furkqn.music.playback.queues.ListQueue
import com.furkqn.music.ui.component.SnakeAmbientBackground
import com.furkqn.music.ui.theme.MusicAccentGreen
import com.furkqn.music.viewmodels.EatPlaylistGameViewModel
import com.furkqn.music.viewmodels.EatPlaylistUiState
import kotlin.math.abs

@Composable
fun EatPlaylistGameScreen(
    navController: NavController,
    viewModel: EatPlaylistGameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    when (val state = uiState) {
        EatPlaylistUiState.Loading -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MusicAccentGreen)
            }
        }

        is EatPlaylistUiState.Error -> {
            LaunchedEffect(state.message) {
                val message =
                    if (state.message == "empty") {
                        context.getString(R.string.snake_game_empty_playlist)
                    } else {
                        state.message
                    }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            }
        }

        is EatPlaylistUiState.Ready -> {
            var queuePrepared by remember { mutableStateOf(false) }
            var hasStartedPlayback by remember { mutableStateOf(false) }
            var resumedWithMusic by remember { mutableStateOf(false) }
            var musicHasPlayedInGame by remember { mutableStateOf(false) }

            val isPlaying by
                playerConnection?.isPlaying?.collectAsStateWithLifecycle(initialValue = false)
                    ?: remember { mutableStateOf(false) }
            val playWhenReady by
                playerConnection?.playerPlayWhenReady?.collectAsStateWithLifecycle(initialValue = false)
                    ?: remember { mutableStateOf(false) }
            val snakeStarted by
                playerConnection?.service?.snakeHasStartedPlaybackFlow?.collectAsStateWithLifecycle(initialValue = false)
                    ?: remember { mutableStateOf(false) }
            val playingMetadata by
                playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(null) }

            LaunchedEffect(isPlaying) {
                if (isPlaying) musicHasPlayedInGame = true
            }

            // Pause snake only when user pauses — not during track crossfade / manual advance.
            LaunchedEffect(playWhenReady, musicHasPlayedInGame, playerConnection) {
                val connection = playerConnection ?: return@LaunchedEffect
                if (!musicHasPlayedInGame) {
                    viewModel.setPaused(false)
                    return@LaunchedEffect
                }
                if (connection.service.isSnakeManualAdvance) return@LaunchedEffect
                val paused = !playWhenReady
                viewModel.setPaused(paused)
            }

            DisposableEffect(playerConnection, queuePrepared, resumedWithMusic, hasStartedPlayback) {
                if (!queuePrepared || playerConnection == null) {
                    return@DisposableEffect onDispose {}
                }
                val connection = playerConnection
                connection.service.snakeMetadataPinEnabled = true
                connection.service.snakePlaybackActive = true
                if (!resumedWithMusic && !hasStartedPlayback) {
                    connection.service.snakeHasStartedPlayback = false
                }
                onDispose {
                    connection.service.snakeMetadataPinEnabled = false
                    connection.service.cancelSnakePlayback()
                    SnakePlaybackTransition.releasePreload(connection)
                    connection.syncMediaMetadataFromPlayer()
                }
            }

            LaunchedEffect(state.songs, state.sourceKey, state.playlistTitle) {
                if (state.songs.isEmpty() || queuePrepared) return@LaunchedEffect
                val connection = playerConnection ?: return@LaunchedEffect
                connection.service.snakePlaybackActive = true

                val sameSource = connection.queueSourceKey.value == state.sourceKey
                val hasQueue = connection.player.mediaItemCount > 0
                val musicActive = connection.player.playWhenReady

                if (sameSource && hasQueue && musicActive) {
                    val ordered = orderedSnakeSongsFromPlayer(connection, state.songs)
                    if (ordered.isNotEmpty()) {
                        val playingId = connection.player.currentMediaItem?.mediaId
                        val thumb = connection.mediaMetadata.value?.thumbnailUrl
                        viewModel.applyQueueOrder(ordered, playingId, thumb)
                        connection.service.snakeHasStartedPlayback = true
                        connection.service.snakeLockedMediaId = playingId
                        hasStartedPlayback = true
                        resumedWithMusic = true
                        musicHasPlayedInGame = true
                        queuePrepared = true
                        return@LaunchedEffect
                    }
                }

                connection.playQueue(
                    ListQueue(
                        title = state.playlistTitle,
                        items = state.songs.map { it.toMediaItem() },
                        startIndex = 0,
                        position = 0L,
                        sourceKey = state.sourceKey,
                    ),
                    shuffleEnabled = false,
                    playWhenReady = false,
                )
                var wait = 0
                while (connection.player.mediaItemCount < state.songs.size && wait < 120) {
                    delay(50)
                    wait++
                }
                repeat(3) {
                    connection.player.shuffleModeEnabled = false
                    connection.player.pause()
                    connection.player.playWhenReady = false
                    delay(50)
                }
                connection.playerPlayWhenReady.value = false
                connection.service.snakeLockedMediaId = null
                val ordered = orderedSnakeSongsFromPlayer(connection, state.songs)
                if (ordered.isNotEmpty()) {
                    viewModel.applyQueueOrder(ordered, null, null)
                    val startIndex = kotlin.random.Random.nextInt(ordered.size)
                    val startSong = ordered[startIndex]
                    viewModel.applyAutoplayStart(
                        ordered,
                        startIndex,
                        startSong.resolvedThumbnailUrl(),
                    )
                    connection.service.snakeHasStartedPlayback = true
                    connection.service.snakeLockedMediaId = startSong.id
                    connection.service.snakeFoodMediaId =
                        ordered[(startIndex + 1) % ordered.size].id
                    hasStartedPlayback = true
                    musicHasPlayedInGame = true
                    SnakePlaybackTransition.playInitialRandomTrack(
                        connection,
                        ordered,
                        startIndex,
                    )
                }
                val readyAfterSync = viewModel.uiState.value as? EatPlaylistUiState.Ready
                queuePrepared = true
                val syncSongs = readyAfterSync?.songs ?: state.songs
                val syncFoodIndex = readyAfterSync?.gameState?.songIndex ?: state.gameState.songIndex
                SnakePlaybackTransition.preloadFood(
                    connection,
                    syncSongs,
                    syncFoodIndex,
                )
            }

            LaunchedEffect(state.gameState.songIndex, queuePrepared, state.gameState.lastEatenSongIndex) {
                if (!queuePrepared) return@LaunchedEffect
                if (state.gameState.lastEatenSongIndex != null) return@LaunchedEffect
                val connection = playerConnection ?: return@LaunchedEffect
                SnakePlaybackTransition.preloadFood(
                    connection,
                    state.songs,
                    state.gameState.songIndex,
                )
            }

            LaunchedEffect(state.gameState.songIndex, state.songs, queuePrepared) {
                val connection = playerConnection ?: return@LaunchedEffect
                if (!queuePrepared) return@LaunchedEffect
                connection.service.snakeFoodMediaId =
                    state.songs.getOrNull(state.gameState.songIndex)?.id
            }

            var naturalEatFromCell by remember { mutableStateOf<GridCell?>(null) }

            LaunchedEffect(playerConnection, queuePrepared, viewModel) {
                val connection = playerConnection ?: return@LaunchedEffect
                if (!queuePrepared) return@LaunchedEffect
                connection.service.snakeNaturalEatEvents.collect {
                    naturalEatFromCell = state.gameState.foodCell
                    viewModel.onNaturalSongEndEat()
                    val ready = viewModel.uiState.value as? EatPlaylistUiState.Ready ?: return@collect
                    SnakePlaybackTransition.preloadFood(
                        connection,
                        ready.songs,
                        ready.gameState.songIndex,
                    )
                }
            }

            // Eat food → play that song (stable collector — not cancelled when lastEatenSongIndex clears).
            LaunchedEffect(playerConnection, queuePrepared, viewModel) {
                val connection = playerConnection ?: return@LaunchedEffect
                if (!queuePrepared) return@LaunchedEffect
                viewModel.eatPlaybackEvents.collect { eatenIndex ->
                    val ready = viewModel.uiState.value as? EatPlaylistUiState.Ready ?: return@collect
                    if (ready.gameState.isGameOver) return@collect
                    val eatenSong = ready.songs.getOrNull(eatenIndex) ?: return@collect
                    hasStartedPlayback = true
                    connection.pinSnakeDisplayMetadata(eatenSong.toMediaMetadata())
                    SnakePlaybackTransition.playEatenTrackReliable(
                        connection,
                        ready.songs,
                        eatenIndex,
                    )
                    SnakePlaybackTransition.preloadFood(
                        connection,
                        ready.songs,
                        ready.gameState.songIndex,
                    )
                }
            }

            EatPlaylistGameContent(
                state = state,
                snakeStarted = snakeStarted,
                hasStartedPlayback = hasStartedPlayback,
                naturalEatFromCell = naturalEatFromCell,
                onNaturalEatAnimDone = { naturalEatFromCell = null },
                musicHasPlayedInGame = musicHasPlayedInGame,
                playWhenReady = playWhenReady,
                isPlaying = isPlaying,
                playingThumbnailUrl = playingMetadata?.thumbnailUrl,
                playingMediaId = playingMetadata?.id,
                onBack = { navController.navigateUp() },
                onSwipe = viewModel::onSwipe,
                onTryAgain = {
                    val thumb = playerConnection?.mediaMetadata?.value?.thumbnailUrl
                    viewModel.tryAgain(thumb)
                },
            )
        }
    }
}

@Composable
private fun EatPlaylistGameContent(
    state: EatPlaylistUiState.Ready,
    snakeStarted: Boolean,
    hasStartedPlayback: Boolean,
    naturalEatFromCell: GridCell?,
    onNaturalEatAnimDone: () -> Unit,
    musicHasPlayedInGame: Boolean,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playingThumbnailUrl: String?,
    playingMediaId: String?,
    onBack: () -> Unit,
    onSwipe: (SnakeDirection) -> Unit,
    onTryAgain: () -> Unit,
) {
    val foodSong = state.songs.getOrNull(state.gameState.songIndex)
    val foodThumb = foodSong?.resolvedThumbnailUrl()
    val ambientThumb =
        if (snakeStarted && !playingThumbnailUrl.isNullOrBlank()) {
            playingThumbnailUrl
        } else {
            foodThumb
        }
    val ambientCacheKey =
        if (snakeStarted && !playingMediaId.isNullOrBlank()) {
            playingMediaId
        } else {
            foodSong?.id
        }
    val tickMs = EatPlaylistSnakeEngine.tickIntervalMsForScore(state.gameState.score)
    val showMiniPlayer = hasStartedPlayback || snakeStarted
    val contentBottomPadding = if (showMiniPlayer) MiniPlayerHeight else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        SnakeAmbientBackground(
            thumbnailUrl = ambientThumb,
            cacheKey = ambientCacheKey,
            modifier = Modifier.fillMaxSize(),
            bottomColor = Color(0xFF0A0A0A),
            boostForFoodPreview = !snakeStarted,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = contentBottomPadding),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.playlistTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text =
                            stringResource(R.string.snake_game_score, state.gameState.score) +
                                " · " +
                                stringResource(R.string.snake_game_high_score, state.highScore),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                SnakeGameBoard(
                    cols = state.cols,
                    rows = state.rows,
                    snake = state.gameState.snake,
                    foodCell = state.gameState.foodCell,
                    foodThumbnailUrl = foodThumb,
                    direction = state.gameState.direction,
                    tickIntervalMs = tickMs,
                    tickGeneration = state.gameState.tickGeneration,
                    naturalEatFromCell = naturalEatFromCell,
                    onNaturalEatAnimDone = onNaturalEatAnimDone,
                    pauseAnimationWithMusic = musicHasPlayedInGame,
                    isMusicPlaying = playWhenReady,
                    onSwipe = onSwipe,
                )

                if (state.gameState.isGameOver) {
                    GameOverOverlay(
                        score = state.gameState.score,
                        highScore = state.highScore,
                        onTryAgain = onTryAgain,
                    )
                }
            }
        }

        foodSong?.let { food ->
            Crossfade(
                targetState = food.title,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "snakeNextSong",
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(
                            bottom =
                                if (showMiniPlayer) {
                                    MiniPlayerHeight + 36.dp
                                } else {
                                    20.dp
                                },
                        ),
            ) { foodTitle ->
                Text(
                    text = stringResource(R.string.snake_game_next_song, foodTitle),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** One-tick grow visuals layered on normal move progress (no full-board freeze). */
private data class GrowTickVisual(
    val isNatural: Boolean,
    val headFrom: GridCell,
    val headTo: GridCell,
    /** Eaten food cover — shown on head from t=0 while gliding to food cell. */
    val eatenThumb: String?,
    /** Previous head cover — fixed at old head cell as new neck segment. */
    val neckThumb: String?,
    val frozenBody: List<SnakeSegment>,
    val foodBoardCell: GridCell?,
)

@Composable
private fun SnakeGameBoard(
    cols: Int,
    rows: Int,
    snake: List<SnakeSegment>,
    foodCell: GridCell,
    foodThumbnailUrl: String?,
    direction: SnakeDirection,
    tickIntervalMs: Long,
    tickGeneration: Long,
    naturalEatFromCell: GridCell?,
    onNaturalEatAnimDone: () -> Unit,
    pauseAnimationWithMusic: Boolean,
    isMusicPlaying: Boolean,
    onSwipe: (SnakeDirection) -> Unit,
) {
    val context = LocalContext.current
    var animFromSnake by remember { mutableStateOf(snake) }
    var animToSnake by remember { mutableStateOf(snake) }
    val moveProgress = remember { Animatable(1f) }
    var growTickVisual by remember { mutableStateOf<GrowTickVisual?>(null) }
    val foodAppear = remember { Animatable(1f) }
    var lastSpawnedFoodKey by remember { mutableStateOf<String?>(null) }

    val foodPulse by rememberInfiniteTransition(label = "foodPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3_200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "foodPulseValue",
    )

    LaunchedEffect(snake, tickGeneration) {
        if (tickGeneration == 0L) {
            animFromSnake = snake
            animToSnake = snake
        }
    }

    LaunchedEffect(isMusicPlaying, pauseAnimationWithMusic) {
        if (pauseAnimationWithMusic && !isMusicPlaying) {
            moveProgress.snapTo(1f)
            animFromSnake = animToSnake
            growTickVisual = null
        }
    }

    LaunchedEffect(tickGeneration, tickIntervalMs) {
        if (tickGeneration == 0L) {
            growTickVisual = null
            moveProgress.snapTo(1f)
            return@LaunchedEffect
        }
        val prevSnake = animToSnake
        val prevHeadCell = prevSnake.firstOrNull()?.cell
        animFromSnake = animToSnake
        animToSnake = snake
        val grew = snake.size > prevSnake.size
        val headMoved = prevHeadCell != null && prevHeadCell != snake.first().cell
        val isNatural = grew && !headMoved && naturalEatFromCell != null
        val moveDuration = tickIntervalMs.toInt().coerceAtLeast(1)
        val isManualGrow = grew && headMoved

        if (isManualGrow || isNatural) {
            growTickVisual =
                GrowTickVisual(
                    isNatural = isNatural,
                    headFrom = if (isNatural) naturalEatFromCell!! else prevHeadCell!!,
                    headTo = snake.first().cell,
                    eatenThumb = snake.first().thumbnailUrl,
                    neckThumb = prevSnake.firstOrNull()?.thumbnailUrl,
                    frozenBody = prevSnake.drop(1),
                    foodBoardCell = if (isNatural) naturalEatFromCell else snake.first().cell,
                )
        }

        moveProgress.snapTo(0f)
        try {
            moveProgress.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(
                        durationMillis = moveDuration,
                        easing = LinearEasing,
                    ),
            )
        } finally {
            if (isManualGrow || isNatural) {
                if (isNatural) onNaturalEatAnimDone()
                growTickVisual = null
                animFromSnake = animToSnake
            }
        }
    }

    val progress = moveProgress.value
    val thumbUrls =
        remember(animToSnake, animFromSnake, foodThumbnailUrl, growTickVisual) {
            buildSet {
                animToSnake.forEach { seg -> seg.thumbnailUrl?.let { add(it) } }
                animFromSnake.forEach { seg -> seg.thumbnailUrl?.let { add(it) } }
                growTickVisual?.eatenThumb?.let { add(it) }
                growTickVisual?.neckThumb?.let { add(it) }
                foodThumbnailUrl?.let { add(it) }
            }
        }
    val thumbCache = rememberSnakeThumbnailCache(thumbUrls)

    LaunchedEffect(foodThumbnailUrl, foodCell, thumbCache) {
        val url = foodThumbnailUrl ?: return@LaunchedEffect
        if (thumbCache[url] == null) return@LaunchedEffect
        val spawnKey = "$url@${foodCell.x},${foodCell.y}"
        if (spawnKey == lastSpawnedFoodKey) return@LaunchedEffect
        lastSpawnedFoodKey = spawnKey
        foodAppear.snapTo(0.35f)
        foodAppear.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        )
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var swipeHandled = false
                    val swipeThreshold = 48f
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                            swipeHandled = false
                        },
                        onDragEnd = {
                            totalDragX = 0f
                            totalDragY = 0f
                            swipeHandled = false
                        },
                        onDragCancel = {
                            totalDragX = 0f
                            totalDragY = 0f
                            swipeHandled = false
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        if (swipeHandled) return@detectDragGestures
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        val dx = totalDragX
                        val dy = totalDragY
                        if (abs(dx) < swipeThreshold && abs(dy) < swipeThreshold) return@detectDragGestures
                        swipeHandled = true
                        if (abs(dx) > abs(dy)) {
                            onSwipe(if (dx > 0) SnakeDirection.RIGHT else SnakeDirection.LEFT)
                        } else {
                            onSwipe(if (dy > 0) SnakeDirection.DOWN else SnakeDirection.UP)
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        val cellSize = minOf(maxWidth / cols, maxHeight / rows)
        val boardWidth = cellSize * cols
        val boardHeight = cellSize * rows
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }

        Box(
            modifier =
                Modifier
                    .size(boardWidth, boardHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.32f)),
        ) {
            SnakeGridCanvas(cols = cols, rows = rows, cellPx = cellPx)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = cellPx
                val ch = cellPx
                val pad = 3f
                val grow = growTickVisual
                val manualGrow = grow != null && !grow.isNatural
                val growHeadPos =
                    if (manualGrow) {
                        lerpCell(grow!!.headFrom, grow.headTo, cols, rows, progress)
                    } else {
                        null
                    }
                val growHeadDeparted =
                    manualGrow &&
                        growHeadPos != null &&
                        (
                            growHeadPos.x.toInt() != grow!!.headFrom.x ||
                                growHeadPos.y.toInt() != grow.headFrom.y
                        )

                for (segmentIndex in animToSnake.indices.reversed()) {
                    when {
                        manualGrow && segmentIndex == 1 -> {
                            if (growHeadDeparted) {
                                drawSnakeThumbAtCell(
                                    cell = grow!!.headFrom,
                                    thumb = grow.neckThumb?.let { thumbCache[it] },
                                    cw = cw,
                                    ch = ch,
                                    pad = pad,
                                    alpha = 1f,
                                )
                            }
                        }
                        manualGrow && segmentIndex == 0 -> {
                            // Eaten cover rides on the moving head — never static on the board.
                            drawSnakeThumbLerp(
                                from = grow!!.headFrom,
                                to = grow.headTo,
                                thumb = grow.eatenThumb?.let { thumbCache[it] },
                                cols = cols,
                                rows = rows,
                                t = progress,
                                cw = cw,
                                ch = ch,
                                pad = pad,
                                alpha = 1f,
                            )
                        }
                        manualGrow && segmentIndex >= 2 -> {
                            val seg = grow!!.frozenBody.getOrNull(segmentIndex - 2) ?: continue
                            drawSnakeThumbAtCell(
                                cell = seg.cell,
                                thumb = seg.thumbnailUrl?.let { thumbCache[it] },
                                cw = cw,
                                ch = ch,
                                pad = pad,
                                alpha = 1f,
                            )
                        }
                        grow != null && grow.isNatural && segmentIndex == 0 -> {
                            drawSnakeThumbAtCell(
                                cell = grow.headTo,
                                thumb = grow.eatenThumb?.let { thumbCache[it] },
                                cw = cw,
                                ch = ch,
                                pad = pad,
                                alpha = 1f,
                            )
                        }
                        grow != null && grow.isNatural && segmentIndex == animToSnake.lastIndex -> {
                            val tailAlpha = ((progress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                            if (tailAlpha > 0.01f) {
                                drawSnakeThumbAtCell(
                                    cell = animToSnake[segmentIndex].cell,
                                    thumb = grow.neckThumb?.let { thumbCache[it] },
                                    cw = cw,
                                    ch = ch,
                                    pad = pad,
                                    alpha = tailAlpha,
                                )
                            }
                        }
                        grow != null && grow.isNatural && segmentIndex in 1 until animToSnake.lastIndex -> {
                            val seg = grow.frozenBody.getOrNull(segmentIndex - 1) ?: continue
                            drawSnakeThumbAtCell(
                                cell = seg.cell,
                                thumb = seg.thumbnailUrl?.let { thumbCache[it] },
                                cw = cw,
                                ch = ch,
                                pad = pad,
                                alpha = 1f,
                            )
                        }
                        else -> {
                            val segment = animToSnake[segmentIndex]
                            val fromCell =
                                if (segmentIndex == 0) {
                                    animFromSnake.firstOrNull()?.cell ?: segment.cell
                                } else {
                                    val fromSeg = animFromSnake.getOrNull(segmentIndex)
                                    if (fromSeg != null) {
                                        fromSeg.cell
                                    } else if (animFromSnake.size < animToSnake.size) {
                                        segment.cell
                                    } else {
                                        animFromSnake.getOrNull(segmentIndex - 1)?.cell
                                            ?: segment.cell
                                    }
                                }
                            val toCell = segment.cell
                            val pos = lerpCell(fromCell, toCell, cols, rows, progress)
                            val left = pos.x * cw + pad
                            val top = pos.y * ch + pad
                            val size = minOf(cw, ch) - pad * 2f
                            val thumb = segment.thumbnailUrl?.let { thumbCache[it] }
                            if (thumb != null) {
                                drawImage(
                                    image = thumb,
                                    dstOffset = IntOffset(left.toInt(), top.toInt()),
                                    dstSize = IntSize(size.toInt(), size.toInt()),
                                )
                            }
                        }
                    }
                }

                if (grow != null && grow.isNatural) {
                    val tailCell = animToSnake.lastOrNull()?.cell ?: grow.headTo
                    val foodFrom = grow.foodBoardCell ?: grow.headFrom
                    drawSnakeThumbLerp(
                        from = foodFrom,
                        to = tailCell,
                        thumb = grow.eatenThumb?.let { thumbCache[it] },
                        cols = cols,
                        rows = rows,
                        t = progress,
                        cw = cw,
                        ch = ch,
                        pad = pad,
                        alpha = 1f,
                    )
                }

                foodThumbnailUrl?.let { foodUrl ->
                    val hideFoodForManualGrow = grow != null && !grow.isNatural
                    if (!hideFoodForManualGrow) {
                        val foodBmp = thumbCache[foodUrl]
                        if (foodBmp != null) {
                            val appear = foodAppear.value.coerceIn(0f, 1f)
                            val padFood = 4f
                            val cx = foodCell.x * cw + cw / 2f
                            val cy = foodCell.y * ch + ch / 2f
                            val fullSize = cw - padFood * 2f
                            val drawSize = fullSize * appear
                            val glowAlpha = 0.12f + foodPulse * 0.16f
                            drawCircle(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                Color(0xFFF5F0FF).copy(alpha = glowAlpha + 0.08f),
                                                Color(0xFFFFF5EB).copy(alpha = glowAlpha * 0.7f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(cx, cy),
                                        radius = cw * 0.92f,
                                    ),
                                radius = cw * 0.92f,
                                center = Offset(cx, cy),
                            )
                            withTransform({
                                translate(cx - drawSize / 2f, cy - drawSize / 2f)
                            }) {
                                drawImage(
                                    image = foodBmp,
                                    dstOffset = IntOffset(0, 0),
                                    dstSize = IntSize(drawSize.toInt(), drawSize.toInt()),
                                    alpha = appear,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnakeGridCanvas(
    cols: Int,
    rows: Int,
    cellPx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize()) {
        val cw = cellPx
        val ch = cellPx
        for (x in 0 until cols) {
            for (y in 0 until rows) {
                drawRect(
                    color = Color.White.copy(alpha = 0.04f),
                    topLeft = Offset(x * cw, y * ch),
                    size = Size(cw - 1f, ch - 1f),
                )
            }
        }
    }
}

@Composable
private fun rememberSnakeThumbnailCache(urls: Set<String>): Map<String, ImageBitmap> {
    val context = LocalContext.current
    var cache by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(urls) {
        val missing = urls.filter { it.isNotBlank() && it !in cache }
        if (missing.isEmpty()) return@LaunchedEffect
        val updated = cache.toMutableMap()
        missing.forEach { url ->
            runCatching {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        .size(96, 96)
                        .allowHardware(false)
                        .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap()?.asImageBitmap()
                if (bitmap != null) {
                    updated[url] = bitmap
                }
            }
        }
        cache = updated
    }
    return cache
}

private fun DrawScope.drawSnakeThumbAtCell(
    cell: GridCell,
    thumb: ImageBitmap?,
    cw: Float,
    ch: Float,
    pad: Float,
    alpha: Float,
) {
    if (alpha <= 0.01f || thumb == null) return
    val left = cell.x * cw + pad
    val top = cell.y * ch + pad
    val size = minOf(cw, ch) - pad * 2f
    drawImage(
        image = thumb,
        dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(size.toInt(), size.toInt()),
        alpha = alpha,
    )
}

private fun DrawScope.drawSnakeThumbLerp(
    from: GridCell,
    to: GridCell,
    thumb: ImageBitmap?,
    cols: Int,
    rows: Int,
    t: Float,
    cw: Float,
    ch: Float,
    pad: Float,
    alpha: Float,
) {
    if (alpha <= 0.01f || thumb == null) return
    val pos = lerpCell(from, to, cols, rows, t)
    val left = pos.x * cw + pad
    val top = pos.y * ch + pad
    val size = minOf(cw, ch) - pad * 2f
    drawImage(
        image = thumb,
        dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(size.toInt(), size.toInt()),
        alpha = alpha,
    )
}

private fun lerpCell(
    from: GridCell,
    to: GridCell,
    cols: Int,
    rows: Int,
    t: Float,
): Offset {
    if (from == to || t >= 1f) return Offset(to.x.toFloat(), to.y.toFloat())
    if (t <= 0f) return Offset(from.x.toFloat(), from.y.toFloat())

    var dx = to.x - from.x
    var dy = to.y - from.y
    if (dx > cols / 2) dx -= cols
    if (dx < -cols / 2) dx += cols
    if (dy > rows / 2) dy -= rows
    if (dy < -rows / 2) dy += rows

    var x = from.x + dx * t
    var y = from.y + dy * t
    if (x < 0f) x += cols
    if (y < 0f) y += rows
    if (x >= cols) x -= cols
    if (y >= rows) y -= rows
    return Offset(x, y)
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    onTryAgain: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.snake_game_game_over),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.snake_game_score, score),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                text = stringResource(R.string.snake_game_high_score, highScore),
                style = MaterialTheme.typography.bodyMedium,
                color = MusicAccentGreen,
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(MusicAccentGreen, Color(0xFF1ED760)),
                            ),
                        )
                        .clickable(onClick = onTryAgain)
                        .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.snake_game_try_again),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun orderedSnakeSongsFromPlayer(
    playerConnection: com.furkqn.music.playback.PlayerConnection,
    candidateSongs: List<EatPlaylistSong>,
): List<EatPlaylistSong> {
    val byId = candidateSongs.associateBy { it.id }
    return buildList {
        for (i in 0 until playerConnection.player.mediaItemCount) {
            val id = playerConnection.player.getMediaItemAt(i).mediaId
            byId[id]?.let { add(it) }
        }
    }
}
