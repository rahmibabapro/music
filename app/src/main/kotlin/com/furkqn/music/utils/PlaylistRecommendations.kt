/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import android.content.Context
import com.furkqn.music.db.MusicDatabase
import com.furkqn.music.db.entities.RelatedSongMap
import com.furkqn.music.db.entities.Song
import com.furkqn.music.extensions.filterVideoSongs
import com.furkqn.music.innertube.YouTube
import com.furkqn.music.innertube.models.SongItem
import com.furkqn.music.innertube.models.WatchEndpoint
import com.furkqn.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object PlaylistRecommendations {
    private const val MAX_SEEDS = 5
    private const val MAX_RESULTS = 24
    private const val STAR_MIN_SCORE = 3
    private const val STAR_MIN_SEEDS = 2

    suspend fun load(
        context: Context,
        database: MusicDatabase,
        playlistId: String,
        playlistSongs: List<Song>,
        hideVideoSongs: Boolean,
    ): List<PlaylistRecommendationItem> =
        withContext(Dispatchers.IO) {
            if (playlistSongs.size < 2) return@withContext emptyList()

            val dismissed = DismissedRecommendationsCache.getDismissed(context, playlistId)
            val playlistIds = playlistSongs.map { it.id }.toSet()
            val playlistArtists =
                playlistSongs
                    .flatMap { it.artists.map { artist -> artist.name.lowercase() } }
                    .toSet()

            val seeds =
                playlistSongs
                    .sortedByDescending { it.song.totalPlayTime }
                    .distinctBy { it.id }
                    .shuffled()
                    .take(MAX_SEEDS)

            val scores = linkedMapOf<String, Int>()
            val seedHits = linkedMapOf<String, Int>()

            coroutineScope {
                seeds.map { seed ->
                    async {
                        collectForSeed(database, seed.id, playlistIds, hideVideoSongs, scores, seedHits)
                    }
                }.awaitAll()
            }

            val ranked =
                scores.entries
                    .filter { (id, _) -> id !in dismissed }
                    .map { (id, score) ->
                        val seedCount = seedHits[id] ?: 0
                        val artistBoost =
                            database.song(id).first()?.artists?.any { artist ->
                                artist.name.lowercase() in playlistArtists
                            } == true
                        val finalScore = score + if (artistBoost) 1 else 0
                        val isStarred = finalScore >= STAR_MIN_SCORE || seedCount >= STAR_MIN_SEEDS
                        RankedRecommendation(id, finalScore, seedCount, isStarred)
                    }.sortedWith(
                        compareByDescending<RankedRecommendation> { it.isStarred }
                            .thenByDescending { it.score }
                            .thenByDescending { it.seedCount },
                    ).take(MAX_RESULTS)

            ranked.mapNotNull { rankedItem ->
                database.song(rankedItem.id).first()?.let { song ->
                    PlaylistRecommendationItem(
                        song = song,
                        score = rankedItem.score,
                        seedCount = rankedItem.seedCount,
                        isStarred = rankedItem.isStarred,
                    )
                }
            }
        }

    private data class RankedRecommendation(
        val id: String,
        val score: Int,
        val seedCount: Int,
        val isStarred: Boolean,
    )

    private suspend fun collectForSeed(
        database: MusicDatabase,
        seedId: String,
        playlistIds: Set<String>,
        hideVideoSongs: Boolean,
        scores: LinkedHashMap<String, Int>,
        seedHits: LinkedHashMap<String, Int>,
    ) {
        val cachedRelated =
            database.getRelatedSongs(seedId).first().filterVideoSongs(hideVideoSongs)
        if (cachedRelated.isNotEmpty()) {
            cachedRelated.forEach { song ->
                if (song.id !in playlistIds) {
                    bumpScore(scores, seedHits, song.id)
                }
            }
            return
        }

        val endpoint =
            YouTube.next(WatchEndpoint(videoId = seedId)).getOrNull()?.relatedEndpoint ?: return
        val relatedPage = YouTube.related(endpoint).getOrNull() ?: return

        database.query {
            relatedPage.songs
                .map(SongItem::toMediaMetadata)
                .onEach(::insert)
                .map { RelatedSongMap(songId = seedId, relatedSongId = it.id) }
                .forEach(::insert)
        }

        relatedPage.songs
            .filter { item ->
                if (item.id in playlistIds) return@filter false
                if (hideVideoSongs && item.isVideoSong) return@filter false
                if (item.explicit) return@filter false
                true
            }.forEach { item ->
                bumpScore(scores, seedHits, item.id)
            }
    }

    private fun bumpScore(
        scores: LinkedHashMap<String, Int>,
        seedHits: LinkedHashMap<String, Int>,
        id: String,
    ) {
        scores[id] = (scores[id] ?: 0) + 1
        seedHits[id] = (seedHits[id] ?: 0) + 1
    }
}
