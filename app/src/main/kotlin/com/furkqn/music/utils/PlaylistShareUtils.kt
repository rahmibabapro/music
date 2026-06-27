/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

object PlaylistShareUtils {
    fun localPlaylistDeepLink(playlistId: String): String = "music://local-playlist/$playlistId"

    fun youTubePlaylistUrl(browseId: String): String =
        "https://music.youtube.com/playlist?list=$browseId"
}
