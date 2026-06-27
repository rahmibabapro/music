package com.furkqn.music.innertube.pages

import com.furkqn.music.innertube.models.Album
import com.furkqn.music.innertube.models.AlbumItem
import com.furkqn.music.innertube.models.Artist
import com.furkqn.music.innertube.models.ArtistItem
import com.furkqn.music.innertube.models.MusicResponsiveListItemRenderer
import com.furkqn.music.innertube.models.MusicTwoRowItemRenderer
import com.furkqn.music.innertube.models.PlaylistItem
import com.furkqn.music.innertube.models.SongItem
import com.furkqn.music.innertube.models.YTItem
import com.furkqn.music.innertube.models.oddElements
import com.furkqn.music.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
