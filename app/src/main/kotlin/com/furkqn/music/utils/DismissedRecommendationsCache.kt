/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class DismissedRecommendationsFile(
    val playlists: Map<String, Set<String>> = emptyMap(),
)

object DismissedRecommendationsCache {
    private const val FILE_NAME = "dismissed_playlist_recommendations.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun getDismissed(context: Context, playlistId: String): Set<String> =
        loadAll(context)[playlistId].orEmpty()

    fun dismiss(context: Context, playlistId: String, songId: String) {
        val all = loadAll(context).toMutableMap()
        val dismissed = all.getOrDefault(playlistId, emptySet()).toMutableSet()
        dismissed.add(songId)
        all[playlistId] = dismissed
        saveAll(context, all)
    }

    private fun loadAll(context: Context): Map<String, Set<String>> {
        val file = context.filesDir.resolve(FILE_NAME)
        if (!file.exists()) return emptyMap()
        return runCatching {
            json.decodeFromString<DismissedRecommendationsFile>(file.readText()).playlists
        }.getOrDefault(emptyMap())
    }

    private fun saveAll(context: Context, playlists: Map<String, Set<String>>) {
        val file = context.filesDir.resolve(FILE_NAME)
        file.writeText(json.encodeToString(DismissedRecommendationsFile(playlists)))
    }
}
