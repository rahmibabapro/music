/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import com.furkqn.music.db.entities.Song

data class PlaylistRecommendationItem(
    val song: Song,
    /** Combined relevance score from seeds, artist overlap, etc. */
    val score: Int,
    /** Number of playlist seeds that suggested this song. */
    val seedCount: Int,
    /** High-confidence match — show as starred pick. */
    val isStarred: Boolean,
)
