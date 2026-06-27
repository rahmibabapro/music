/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import com.furkqn.music.db.entities.PlaylistSong
import kotlin.math.abs

object BpmMixOrder {
    /**
     * Reorder playlist songs so adjacent tracks have similar BPM (greedy nearest-neighbor).
     * Songs without BPM data are appended at the end in original order.
     */
    fun order(
        songs: List<PlaylistSong>,
        bpmBySongId: Map<String, Float>,
    ): List<PlaylistSong> {
        if (songs.size <= 1) return songs

        val withBpm = songs.filter { bpmBySongId.containsKey(it.song.id) }
        val withoutBpm = songs.filter { !bpmBySongId.containsKey(it.song.id) }
        if (withBpm.isEmpty()) return songs

        val remaining = withBpm.toMutableList()
        val ordered = mutableListOf<PlaylistSong>()
        var current = remaining.removeAt(0)
        ordered.add(current)

        while (remaining.isNotEmpty()) {
            val currentBpm = bpmBySongId[current.song.id]!!
            var bestIndex = 0
            var bestDelta = Float.MAX_VALUE
            remaining.forEachIndexed { index, candidate ->
                val bpm = bpmBySongId[candidate.song.id]!!
                val delta = abs(bpm - currentBpm)
                if (delta < bestDelta || (delta == bestDelta && bpm == currentBpm)) {
                    bestDelta = delta
                    bestIndex = index
                }
            }
            current = remaining.removeAt(bestIndex)
            ordered.add(current)
        }

        return ordered + withoutBpm
    }
}
