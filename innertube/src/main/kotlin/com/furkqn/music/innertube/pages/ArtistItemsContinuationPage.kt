package com.furkqn.music.innertube.pages

import com.furkqn.music.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
