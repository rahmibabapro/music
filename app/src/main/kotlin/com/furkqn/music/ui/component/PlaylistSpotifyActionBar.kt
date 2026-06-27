/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.furkqn.music.LocalPlayerConnection
import com.furkqn.music.R
import com.furkqn.music.ui.theme.MusicAccentGreen
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED

@Composable
fun PlaylistSpotifyActionBar(
    sourceKey: String,
    enabled: Boolean,
    downloadState: Int,
    onDownloadClick: () -> Unit,
    onMenuClick: () -> Unit,
    onPlay: (shuffleEnabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showShuffle: Boolean = true,
    showDownload: Boolean = true,
    showMenu: Boolean = true,
    showShare: Boolean = true,
    onShareClick: (() -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val queueSourceKey by playerConnection.queueSourceKey.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val snakeStarted by playerConnection.service.snakeHasStartedPlaybackFlow.collectAsStateWithLifecycle()
    val snakeActive = playerConnection.service.snakePlaybackActive
    val playerShuffleEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    var pendingShuffle by rememberSaveable(sourceKey) { mutableStateOf(false) }

    val isActiveQueue = queueSourceKey == sourceKey
    val shuffleActive = if (isActiveQueue) playerShuffleEnabled else pendingShuffle
    val showPauseIcon = isActiveQueue && isPlaying && !(snakeActive && !snakeStarted)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDownload) {
                SecondaryActionIcon(
                    onClick = { if (enabled) onDownloadClick() },
                    enabled = enabled,
                ) {
                    when (downloadState) {
                        STATE_COMPLETED -> {
                            Icon(
                                painter = painterResource(R.drawable.offline),
                                contentDescription = stringResource(R.string.remove_download),
                                tint = MusicAccentGreen,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        STATE_DOWNLOADING, STATE_QUEUED -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MusicAccentGreen,
                            )
                        }

                        else -> {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = stringResource(R.string.action_download),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            if (showShare && onShareClick != null) {
                SecondaryActionIcon(
                    onClick = onShareClick,
                    enabled = enabled,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = stringResource(R.string.share),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (showMenu) {
                SecondaryActionIcon(onClick = onMenuClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showShuffle) {
                Surface(
                    onClick = {
                        if (!enabled) return@Surface
                        if (isActiveQueue) {
                            playerConnection.player.shuffleModeEnabled = !playerShuffleEnabled
                        } else {
                            pendingShuffle = true
                            onPlay(true)
                        }
                    },
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = stringResource(R.string.shuffle),
                            tint =
                                if (shuffleActive) {
                                    MusicAccentGreen
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Surface(
                onClick = {
                    if (!enabled) return@Surface
                    if (isActiveQueue) {
                        if (isPlaying) {
                            playerConnection.togglePlayPause()
                        } else {
                            playerConnection.player.playWhenReady = true
                        }
                    } else {
                        onPlay(shuffleActive)
                    }
                },
                color = if (enabled) MusicAccentGreen else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (showPauseIcon) R.drawable.pause else R.drawable.play,
                            ),
                        contentDescription =
                            if (showPauseIcon) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.play)
                            },
                        tint = if (enabled) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryActionIcon(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(40.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
