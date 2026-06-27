/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import com.furkqn.music.LocalListenTogetherManager
import com.furkqn.music.R
import com.furkqn.music.constants.PlayerHorizontalPadding
import com.furkqn.music.listentogether.RoomRole
import com.furkqn.music.models.MediaMetadata
import com.furkqn.music.ui.component.BottomSheetState
import com.furkqn.music.ui.component.CastButton
import com.furkqn.music.ui.component.Icon as MIcon
import com.furkqn.music.ui.component.LocalBottomSheetPageState
import com.furkqn.music.ui.component.LocalMenuState
import com.furkqn.music.ui.component.PlayerSliderTrack
import com.furkqn.music.ui.menu.PlayerMenu
import com.furkqn.music.ui.theme.MusicAccentGreen
import com.furkqn.music.ui.utils.ShowMediaInfo
import com.furkqn.music.utils.makeTimeString
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SpotifyPlayerHeader(
    queueTitle: String?,
    albumTitle: String?,
    onCollapse: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayingFromClick: (() -> Unit)? = null,
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRole =
        listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val role = listenTogetherRole?.value ?: RoomRole.NONE
            if (role != RoomRole.NONE) {
                Text(
                    text =
                        if (role == RoomRole.HOST) {
                            "Hosting Listen Together"
                        } else {
                            "Listening Together"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = queueTitle.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            } else {
                val playingFrom = queueTitle ?: albumTitle
                if (!playingFrom.isNullOrBlank()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            if (onPlayingFromClick != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onPlayingFromClick,
                                )
                            } else {
                                Modifier
                            },
                    ) {
                        Text(
                            text = stringResource(R.string.player_playing_from).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = playingFrom,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun SpotifyPlayerFooter(
    onShareClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    lyricsActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CastButton(
            modifier = Modifier.size(40.dp),
            tintColor = Color.White,
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onShareClick) {
            Icon(
                painter = painterResource(R.drawable.share),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        IconButton(onClick = onLyricsClick) {
            Icon(
                painter = painterResource(R.drawable.lyrics),
                contentDescription = stringResource(R.string.lyrics),
                tint = if (lyricsActive) MusicAccentGreen else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        IconButton(onClick = onQueueClick) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun SpotifyPlayerControls(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    sliderPosition: Long?,
    effectivePosition: Long,
    duration: Long,
    onSliderChange: (Long) -> Unit,
    onSliderChangeFinished: () -> Unit,
    shuffleEnabled: Boolean,
    onShuffleClick: () -> Unit,
    canSkipPrevious: Boolean,
    onSkipPrevious: () -> Unit,
    canSkipNext: Boolean,
    onSkipNext: () -> Unit,
    effectiveIsPlaying: Boolean,
    playbackState: Int,
    onTogglePlayPause: () -> Unit,
    sleepTimerEnabled: Boolean,
    onSleepTimerClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    isFavorite: Boolean,
    onToggleLike: () -> Unit,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    focusRequester: FocusRequester,
    currentSong: com.furkqn.music.db.entities.Song?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = mediaMetadata.title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "spotifyTitle",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        modifier =
                            Modifier
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .combinedClickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        val albumId =
                                            mediaMetadata.album?.id
                                                ?: currentSong?.album?.id
                                                ?: currentSong?.song?.albumId
                                        if (albumId != null) {
                                            navController.navigate("album/$albumId")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText(copiedTitleStr, title)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast.makeText(context, copiedTitleStr, Toast.LENGTH_SHORT).show()
                                    },
                                ),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mediaMetadata.explicit) {
                        MIcon.Explicit()
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                        val annotatedString =
                            buildAnnotatedString {
                                mediaMetadata.artists.forEachIndexed { index, artist ->
                                    val tag = "artist_${artist.id.orEmpty()}"
                                    pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp)) {
                                        append(artist.name)
                                    }
                                    pop()
                                    if (index != mediaMetadata.artists.lastIndex) append(", ")
                                }
                            }

                        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                        var clickOffset by remember { mutableStateOf<Offset?>(null) }
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { layoutResult = it },
                            modifier =
                                Modifier
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                clickOffset = event.changes.firstOrNull()?.position
                                            }
                                        }
                                    }.combinedClickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            val tapPosition = clickOffset
                                            val layout = layoutResult
                                            if (tapPosition != null && layout != null) {
                                                val offset = layout.getOffsetForPosition(tapPosition)
                                                annotatedString
                                                    .getStringAnnotations(offset, offset)
                                                    .firstOrNull()
                                                    ?.let { ann ->
                                                        val artistId = ann.item
                                                        if (artistId.isNotBlank()) {
                                                            navController.navigate("artist/$artistId")
                                                            state.collapseSoft()
                                                        }
                                                    }
                                            }
                                        },
                                        onLongClick = {
                                            val clip = ClipData.newPlainText(copiedArtistStr, annotatedString.text)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, copiedArtistStr, Toast.LENGTH_SHORT).show()
                                        },
                                    ),
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onToggleLike,
                    enabled = !isListenTogetherGuest,
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                            ),
                        contentDescription = stringResource(R.string.like),
                        tint = if (isFavorite) MusicAccentGreen else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onAddToPlaylistClick) {
                    Icon(
                        painter = painterResource(R.drawable.add_circle),
                        contentDescription = stringResource(R.string.add_to_playlist),
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val sliderColors =
            SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                disabledThumbColor = Color.White,
                disabledActiveTrackColor = Color.White,
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.35f),
            )

        Slider(
            value = (sliderPosition ?: effectivePosition).toFloat(),
            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
            onValueChange = {
                if (!isListenTogetherGuest) {
                    onSliderChange(it.toLong())
                }
            },
            onValueChangeFinished = {
                if (!isListenTogetherGuest) {
                    onSliderChangeFinished()
                }
            },
            enabled = !isListenTogetherGuest,
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                PlayerSliderTrack(
                    sliderState = sliderState,
                    colors = sliderColors,
                )
            },
            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp),
        ) {
            Text(
                text = makeTimeString(sliderPosition ?: effectivePosition),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
            Text(
                text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                IconButton(
                    onClick = onShuffleClick,
                    enabled = !isListenTogetherGuest,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (shuffleEnabled) MusicAccentGreen else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                if (shuffleEnabled) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MusicAccentGreen),
                    )
                }
            }

            IconButton(
                onClick = onSkipPrevious,
                enabled = canSkipPrevious && !isListenTogetherGuest,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            Surface(
                onClick = {
                    if (isListenTogetherGuest) return@Surface
                    onTogglePlayPause()
                },
                shape = CircleShape,
                color = Color.White,
                modifier =
                    Modifier
                        .size(64.dp)
                        .focusRequester(focusRequester),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter =
                            painterResource(
                                when {
                                    isListenTogetherGuest ->
                                        if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                    playbackState == STATE_ENDED -> R.drawable.replay
                                    effectiveIsPlaying -> R.drawable.pause
                                    else -> R.drawable.play
                                },
                            ),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            IconButton(
                onClick = onSkipNext,
                enabled = canSkipNext && !isListenTogetherGuest,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            IconButton(
                onClick = onSleepTimerClick,
                enabled = !isListenTogetherGuest,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.timer),
                    contentDescription = stringResource(R.string.sleep_timer),
                    tint = if (sleepTimerEnabled) MusicAccentGreen else Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun rememberSpotifyMenuClickHandler(
    mediaMetadata: MediaMetadata,
    state: BottomSheetState,
): () -> Unit {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    return remember(mediaMetadata.id) {
        {
            menuState.showPlayerMenu(spotifyStyle = true) {
                PlayerMenu(
                    mediaMetadata = mediaMetadata,
                    playerBottomSheetState = state,
                    spotifyStyle = true,
                    onShowDetailsDialog = {
                        bottomSheetPageState.show {
                            ShowMediaInfo(mediaMetadata.id)
                        }
                    },
                    onDismiss = menuState::dismiss,
                )
            }
        }
    }
}
