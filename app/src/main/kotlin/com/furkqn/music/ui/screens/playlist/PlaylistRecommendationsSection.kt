/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.furkqn.music.extensions.toMediaItem
import com.furkqn.music.ui.component.LocalMenuState
import com.furkqn.music.ui.menu.SongMenu
import com.furkqn.music.ui.theme.MusicAccentGreen
import com.furkqn.music.utils.PlaylistRecommendationItem
import com.furkqn.music.LocalPlayerConnection
import com.furkqn.music.R
import com.furkqn.music.playback.queues.ListQueue
import com.furkqn.music.utils.joinByBullet
import com.furkqn.music.utils.makeTimeString
import com.furkqn.music.viewmodels.LocalPlaylistViewModel
import com.furkqn.music.viewmodels.PlaylistRecommendationState
import kotlinx.coroutines.launch

@Composable
fun PlaylistRecommendationsSection(
    recommendationState: PlaylistRecommendationState,
    recommendedItems: List<PlaylistRecommendationItem>,
    editable: Boolean,
    playingMediaId: String?,
    isPlaying: Boolean,
    viewModel: LocalPlaylistViewModel,
    onAddedToPlaylist: () -> Unit,
    onAddedCount: (Int) -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendationState == PlaylistRecommendationState.Idle) return

    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var deckIndex by remember(recommendedItems) { mutableIntStateOf(0) }

    val starred = recommendedItems.filter { it.isStarred }
    val deckItems = recommendedItems.filter { !it.isStarred }
    val currentDeckItem = deckItems.getOrNull(deckIndex)

    Column(modifier = modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.playlist_recommended_songs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (editable && starred.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            val count = viewModel.addStarredRecommendationsToPlaylist()
                            if (count > 0) {
                                onAddedCount(count)
                            }
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.playlist_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.playlist_recommended_add_starred))
                }
            }
        }

        when (recommendationState) {
            PlaylistRecommendationState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            PlaylistRecommendationState.Empty -> {
                Text(
                    text = stringResource(R.string.playlist_recommended_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            PlaylistRecommendationState.Loaded -> {
                if (starred.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.playlist_recommended_starred),
                        style = MaterialTheme.typography.labelLarge,
                        color = MusicAccentGreen,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(starred, key = { it.song.id }) { item ->
                            RecommendationCard(
                                item = item,
                                editable = editable,
                                isActive = playingMediaId == item.song.id,
                                isPlaying = isPlaying,
                                onPlay = {
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = item.song.song.title,
                                            items = listOf(item.song.toMediaItem()),
                                            sourceKey = "recommendation_${item.song.id}",
                                        ),
                                    )
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item.song,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                                onAdd = {
                                    scope.launch {
                                        if (viewModel.addRecommendationToPlaylist(item.song.id)) {
                                            onAddedToPlaylist()
                                        }
                                    }
                                },
                                onDislike = {
                                    viewModel.dismissRecommendation(item.song.id)
                                    onDismissed()
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (currentDeckItem != null) {
                    Text(
                        text = stringResource(R.string.playlist_recommended_discover),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    RecommendationCard(
                        item = currentDeckItem,
                        editable = editable,
                        isActive = playingMediaId == currentDeckItem.song.id,
                        isPlaying = isPlaying,
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        large = true,
                        onPlay = {
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = currentDeckItem.song.song.title,
                                    items = listOf(currentDeckItem.song.toMediaItem()),
                                    sourceKey = "recommendation_${currentDeckItem.song.id}",
                                ),
                            )
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = currentDeckItem.song,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                        onAdd = {
                            scope.launch {
                                if (viewModel.addRecommendationToPlaylist(currentDeckItem.song.id)) {
                                    onAddedToPlaylist()
                                    deckIndex = deckIndex.coerceAtMost((deckItems.size - 2).coerceAtLeast(0))
                                }
                            }
                        },
                        onDislike = {
                            viewModel.dismissRecommendation(currentDeckItem.song.id)
                            onDismissed()
                            if (deckIndex >= deckItems.size - 1) {
                                deckIndex = (deckIndex - 1).coerceAtLeast(0)
                            }
                        },
                    )
                    if (deckItems.size > 1) {
                        Text(
                            text = stringResource(
                                R.string.playlist_recommended_deck_progress,
                                deckIndex + 1,
                                deckItems.size,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.dismissRecommendation(currentDeckItem.song.id)
                                    onDismissed()
                                    if (deckIndex >= deckItems.size - 1) {
                                        deckIndex = (deckIndex - 1).coerceAtLeast(0)
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = stringResource(R.string.playlist_recommended_dislike),
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.playlist_recommended_dislike))
                            }
                            TextButton(
                                enabled = deckIndex < deckItems.size - 1,
                                onClick = { deckIndex++ },
                            ) {
                                Text(stringResource(R.string.playlist_recommended_skip))
                            }
                        }
                    }
                }
            }

            PlaylistRecommendationState.Idle -> Unit
        }
    }
}

@Composable
private fun RecommendationCard(
    item: PlaylistRecommendationItem,
    editable: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    onAdd: () -> Unit,
    onDislike: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val cardModifier = if (large) Modifier.fillMaxWidth() else Modifier.size(160.dp, 220.dp)
    val height = if (large) 240.dp else 220.dp

    Surface(
        modifier =
            modifier
                .then(cardModifier)
                .height(height)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onPlay, onLongClick = onLongClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isActive) 4.dp else 1.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.song.song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
            )
            if (item.isStarred) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                MusicAccentGreen.copy(alpha = 0.9f),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.playlist_recommended_star_pick),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp),
            ) {
                Text(
                    text = item.song.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text =
                        joinByBullet(
                            item.song.artists.firstOrNull()?.name.orEmpty(),
                            makeTimeString(item.song.song.duration * 1000L),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
            if (editable) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilledTonalIconButton(
                        onClick = onDislike,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.playlist_recommended_dislike),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(36.dp),
                        colors =
                            androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MusicAccentGreen.copy(alpha = 0.85f),
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_add),
                            contentDescription = stringResource(R.string.playlist_recommended_add),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
