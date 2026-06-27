/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.furkqn.music.R
import com.furkqn.music.utils.SongLinkResolver
import com.furkqn.music.utils.SongLinks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongLinkShareDialog(
    songId: String,
    title: String? = null,
    artist: String? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var links by remember(songId, title, artist) { mutableStateOf<SongLinks?>(null) }
    var loading by remember(songId, title, artist) { mutableStateOf(true) }

    LaunchedEffect(songId, title, artist) {
        loading = true
        links = SongLinkResolver.resolve(youtubeVideoId = songId, title = title, artist = artist)
        loading = false
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.copy_link),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            if (loading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(24.dp),
                )
            } else {
                links?.let { resolved ->
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.youtube_music)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    copyLink(context, resolved.youtubeUrl)
                                    onDismiss()
                                },
                    )

                    if (resolved.spotifyUrl != null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.spotify)) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        copyLink(context, resolved.spotifyUrl)
                                        onDismiss()
                                    },
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.spotify_link_not_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End).padding(end = 8.dp),
            ) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    }
}

private fun copyLink(
    context: Context,
    url: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("link", url))
    android.widget.Toast
        .makeText(context, context.getString(R.string.link_copied), android.widget.Toast.LENGTH_SHORT)
        .show()
}

@Composable
fun rememberSongLinkShareHandler(): (String) -> Unit {
    var songId by remember { mutableStateOf<String?>(null) }

    if (songId != null) {
        SongLinkShareDialog(
            songId = songId!!,
            onDismiss = { songId = null },
        )
    }

    return { id -> songId = id }
}
