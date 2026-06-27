/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.furkqn.music.R
import com.furkqn.music.ui.component.rememberPlaylistHeaderTopPadding
import com.furkqn.music.utils.makeTimeString

data class PlaylistAuthorInfo(
    val name: String,
    val avatarUrl: String? = null,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun SpotifyPlaylistHeader(
    title: String,
    songCount: Int,
    durationSeconds: Int,
    author: PlaylistAuthorInfo?,
    modifier: Modifier = Modifier,
    artwork: @Composable () -> Unit,
    sourceKey: String,
    enabled: Boolean,
    downloadState: Int,
    gradientThumbnailUrl: String? = null,
    gradientCacheKey: String? = gradientThumbnailUrl,
    onDownloadClick: () -> Unit,
    onShareClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit,
    onPlay: (shuffleEnabled: Boolean) -> Unit,
    onAddClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onEditCoverClick: (() -> Unit)? = null,
    onSortClick: (() -> Unit)? = null,
    showDownload: Boolean = true,
    showShuffle: Boolean = true,
    songCountPluralRes: Int = R.plurals.n_song,
    description: String? = null,
    bottomContent: @Composable (() -> Unit)? = null,
) {
    val headerTopPadding = rememberPlaylistHeaderTopPadding()

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = headerTopPadding, bottom = 12.dp),
        ) {
        Box(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) {
            artwork()
        }

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (author != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp)
                        .then(
                            if (author.onClick != null) {
                                Modifier.combinedClickable(onClick = author.onClick)
                            } else {
                                Modifier
                            },
                        ),
            ) {
                if (author.avatarUrl != null) {
                    AsyncImage(
                        model = author.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = author.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        val nSongs = pluralStringResource(songCountPluralRes, songCount, songCount)
        val durationText = if (durationSeconds > 0) makeTimeString(durationSeconds * 1000L) else null
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Text(
                text =
                    buildString {
                        append(nSongs)
                        if (durationText != null) {
                            append(" · ")
                            append(durationText)
                        }
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            ExpandableText(
                text = description,
                modifier = Modifier.padding(horizontal = 16.dp),
                collapsedMaxLines = 3,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PlaylistSpotifyActionBar(
            sourceKey = sourceKey,
            enabled = enabled,
            downloadState = downloadState,
            onDownloadClick = onDownloadClick,
            onMenuClick = onMenuClick,
            onPlay = onPlay,
            showDownload = showDownload,
            showShuffle = showShuffle,
            onShareClick = onShareClick,
            modifier = Modifier.fillMaxWidth(),
        )

        PlaylistQuickActionChips(
            onAddClick = onAddClick,
            onEditClick = onEditClick,
            onEditCoverClick = onEditCoverClick,
            onSortClick = onSortClick,
            onShareClick = onShareClick,
            modifier = Modifier.padding(top = 8.dp),
        )

        bottomContent?.invoke()
        }
    }
}

@Composable
fun PlaylistQuickActionChips(
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onEditCoverClick: (() -> Unit)? = null,
    onSortClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
) {
    val chips =
        buildList {
            onAddClick?.let { add(Triple(R.string.playlist_chip_add, it, false)) }
            onEditClick?.let { add(Triple(R.string.playlist_chip_edit, it, false)) }
            onEditCoverClick?.let { add(Triple(R.string.edit_playlist_cover, it, false)) }
            onSortClick?.let { add(Triple(R.string.playlist_chip_sort, it, false)) }
            onShareClick?.let { add(Triple(R.string.playlist_chip_share, it, true)) }
        }
    if (chips.isEmpty()) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { (labelRes, onClick, _) ->
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(50),
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    ),
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
