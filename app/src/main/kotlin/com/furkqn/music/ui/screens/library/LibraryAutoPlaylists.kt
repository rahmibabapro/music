/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.screens.library

import com.furkqn.music.db.entities.Playlist
import com.furkqn.music.db.entities.PlaylistEntity
import java.time.LocalDateTime

const val LIKED_AUTO_PLAYLIST_KEY = "likedPlaylist"

fun createLikedAutoPlaylist(name: String): Playlist =
    Playlist(
        playlist =
            PlaylistEntity(
                id = PlaylistEntity.LIKED_PLAYLIST_ID,
                name = name,
                createdAt = LocalDateTime.MIN,
                lastUpdateTime = LocalDateTime.MIN,
                isEditable = false,
            ),
        songCount = 0,
        songThumbnails = emptyList(),
    )

fun Playlist.isLikedAutoPlaylist(): Boolean = id == PlaylistEntity.LIKED_PLAYLIST_ID
