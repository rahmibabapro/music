/**

 * Music Project (C) 2026

 * Licensed under GPL-3.0 | See git history for contributors

 */



package com.furkqn.music.game



import androidx.media3.common.MediaItem

import com.furkqn.music.db.entities.Song

import com.furkqn.music.extensions.toMediaItem

import com.furkqn.music.innertube.models.SongItem

import com.furkqn.music.models.MediaMetadata



data class EatPlaylistSong(

    val id: String,

    val title: String,

    val thumbnailUrl: String?,

    val durationSec: Int = -1,

) {

    fun toMediaMetadata(): MediaMetadata =

        MediaMetadata(

            id = id,

            title = title,

            artists = emptyList(),

            duration = durationSec,

            thumbnailUrl = thumbnailUrl,

        )



    fun toMediaItem(): MediaItem = toMediaMetadata().toMediaItem()



    /** Heuristic hook/chorus start — skips intro, lands in the catchiest section. */
    fun highlightStartMs(): Long {
        if (durationSec <= 0) return 0L
        val durationMs = durationSec * 1000L
        if (durationMs < 45_000L) return (durationMs * 0.22).toLong().coerceAtMost(12_000L)
        return (durationMs * 0.38).toLong().coerceIn(0L, (durationMs - 5_000L).coerceAtLeast(0L))
    }

    fun resolvedThumbnailUrl(): String? =
        thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: id.takeIf { it.isNotBlank() }?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
}



fun Song.toEatPlaylistSong(): EatPlaylistSong =
    EatPlaylistSong(
        id = id,
        title = title,
        thumbnailUrl =
            song.thumbnailUrl?.takeIf { it.isNotBlank() }
                ?: album?.thumbnailUrl?.takeIf { it.isNotBlank() },
        durationSec = song.duration,
    )



fun SongItem.toEatPlaylistSong(): EatPlaylistSong =

    EatPlaylistSong(

        id = id,

        title = title,

        thumbnailUrl = thumbnail.ifBlank { null },

        durationSec = duration ?: -1,

    )


