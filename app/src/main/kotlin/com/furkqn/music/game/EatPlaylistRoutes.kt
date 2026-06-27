/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.game

object EatPlaylistRoutes {
    fun route(
        source: String,
        key: String,
        title: String? = null,
    ): String {
        val encodedKey = java.net.URLEncoder.encode(key, Charsets.UTF_8.name())
        val titleQuery =
            title?.let { "?title=${java.net.URLEncoder.encode(it, Charsets.UTF_8.name())}" } ?: ""
        return "eat_playlist/$source/$encodedKey$titleQuery"
    }
}
