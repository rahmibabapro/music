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
private data class BpmCacheFile(
    val entries: Map<String, Float> = emptyMap(),
)

object BpmCache {
    private const val FILE_NAME = "song_bpm_cache.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun get(context: Context, songId: String): Float? = loadAll(context)[songId]

    fun put(context: Context, songId: String, bpm: Float) {
        val all = loadAll(context).toMutableMap()
        all[songId] = bpm
        saveAll(context, all)
    }

    fun loadAll(context: Context): Map<String, Float> {
        val file = context.filesDir.resolve(FILE_NAME)
        if (!file.exists()) return emptyMap()
        return runCatching {
            json.decodeFromString<BpmCacheFile>(file.readText()).entries
        }.getOrDefault(emptyMap())
    }

    private fun saveAll(context: Context, entries: Map<String, Float>) {
        val file = context.filesDir.resolve(FILE_NAME)
        file.writeText(json.encodeToString(BpmCacheFile(entries)))
    }
}
