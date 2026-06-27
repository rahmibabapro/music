/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import com.furkqn.music.db.entities.PlaylistEntity

fun resolveQueueSourceRoute(sourceKey: String): String? =
    when {
        sourceKey == PlaylistSourceKeys.CACHE -> "cache_playlist/cache"
        sourceKey.startsWith("auto:") -> "auto_playlist/${sourceKey.removePrefix("auto:")}"
        sourceKey.startsWith("top:") -> "top_playlist/${sourceKey.removePrefix("top:")}"
        sourceKey.startsWith("top_") -> "top_playlist/${sourceKey.removePrefix("top_")}"
        sourceKey == PlaylistEntity.LIKED_PLAYLIST_ID -> "auto_playlist/liked"
        sourceKey == PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> "auto_playlist/downloaded"
        sourceKey == "liked" || sourceKey == "downloaded" || sourceKey == "uploaded" ->
            "auto_playlist/$sourceKey"
        sourceKey.startsWith("LP") -> "local_playlist/$sourceKey"
        else -> "online_playlist/$sourceKey"
    }

fun resolvePlayingFromRoute(
    sourceKey: String?,
    albumId: String?,
): String? =
    sourceKey?.let(::resolveQueueSourceRoute) ?: albumId?.let { "album/$it" }

val HiddenLibraryPlaylistIds =
    setOf(
        PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID,
        PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID,
    )

fun List<com.furkqn.music.db.entities.Playlist>.withoutHiddenStatsPlaylists() =
    filter { it.id !in HiddenLibraryPlaylistIds }
