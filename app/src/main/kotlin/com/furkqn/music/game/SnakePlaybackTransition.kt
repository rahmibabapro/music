/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.game

import com.furkqn.music.extensions.indexOfMediaItemById
import com.furkqn.music.playback.PlayerConnection
import kotlinx.coroutines.delay

object SnakePlaybackTransition {
    fun crossfadeSettleMs(): Long = 300L

    /** Clamp highlight seek so out-of-range positions never trigger player errors. */
    fun safeHighlightMs(song: EatPlaylistSong): Long {
        val highlight = song.highlightStartMs()
        val durationMs = song.durationSec.takeIf { it > 0 }?.times(1000L) ?: return 0L
        return highlight.coerceIn(0L, (durationMs - 2_000L).coerceAtLeast(0L))
    }

    suspend fun playInitialRandomTrack(
        playerConnection: PlayerConnection,
        songs: List<EatPlaylistSong>,
        index: Int,
    ) {
        val song = songs.getOrNull(index) ?: return
        var attempts = 0
        while (attempts < 10) {
            if (playerConnection.player.indexOfMediaItemById(song.id) !=
                androidx.media3.common.C.INDEX_UNSET
            ) {
                break
            }
            delay(80)
            attempts++
        }
        val startMs = safeHighlightMs(song)
        playerConnection.service.playSnakeTrack(song.id, startMs)
        delay(200)
        playerConnection.syncMediaMetadataFromPlayer()
    }

    suspend fun playEatenTrackReliable(
        playerConnection: PlayerConnection,
        songs: List<EatPlaylistSong>,
        index: Int,
    ) {
        val song = songs.getOrNull(index) ?: return
        var attempts = 0
        while (attempts < 10) {
            if (playerConnection.player.indexOfMediaItemById(song.id) !=
                androidx.media3.common.C.INDEX_UNSET
            ) {
                break
            }
            delay(80)
            attempts++
        }

        val startMs = safeHighlightMs(song)
        val service = playerConnection.service
        val hasStarted = service.snakeHasStartedPlayback
        if (hasStarted) {
            // Overlap old + new audio via crossfade — never cold-seek while music is playing.
            service.crossfadeToSnakeTrack(song.id, startMs, crossfadeMs = 900L)
            delay(1_050)
        } else {
            service.releaseSnakePreload()
            service.playSnakeTrack(song.id, startMs)
            delay(180)
        }

        val actualId = playerConnection.player.currentMediaItem?.mediaId
        if (actualId != song.id) {
            if (service.snakeHasStartedPlayback) {
                service.crossfadeToSnakeTrack(song.id, startMs, crossfadeMs = 900L)
                delay(1_050)
            } else {
                playerConnection.service.playSnakeTrack(song.id, startMs)
                playerConnection.player.playWhenReady = true
                playerConnection.playerPlayWhenReady.value = true
                delay(120)
            }
        }

        if (playerConnection.player.playerError != null) {
            if (service.snakeHasStartedPlayback) {
                service.crossfadeToSnakeTrack(song.id, 0L, crossfadeMs = 900L)
                delay(1_050)
            } else {
                playerConnection.service.playSnakeTrack(song.id, 0L)
                playerConnection.player.playWhenReady = true
                playerConnection.playerPlayWhenReady.value = true
                delay(120)
            }
        }

        playerConnection.syncMediaMetadataFromPlayer()
    }

    fun preloadFood(
        playerConnection: PlayerConnection,
        songs: List<EatPlaylistSong>,
        foodIndex: Int,
    ) {
        val song = songs.getOrNull(foodIndex) ?: return
        playerConnection.service.preloadSnakeTrack(song.id, safeHighlightMs(song))
    }

    fun releasePreload(playerConnection: PlayerConnection) {
        playerConnection.service.releaseSnakePreload()
    }
}
