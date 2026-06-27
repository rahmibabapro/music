package com.furkqn.music.innertube.pages

import com.furkqn.music.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
