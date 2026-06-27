/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.furkqn.music.innertube.models.SongItem
import com.furkqn.music.db.entities.Song
import com.furkqn.music.models.MediaMetadata
import com.furkqn.music.models.toMediaMetadata
import com.furkqn.music.ui.utils.resize
import com.furkqn.music.utils.DEFAULT_ARTIST_NAME
import com.furkqn.music.utils.formatArtistDisplay

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

fun Song.toMediaItem(): MediaItem {
    val artistName = formatArtistDisplay(orderedArtists.map { it.name })
    val albumArtist = orderedArtists.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: DEFAULT_ARTIST_NAME
    return MediaItem.Builder()
    .setMediaId(song.id)
    .setUri(song.id)
    .setCustomCacheKey(song.id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(song.title)
            .setSubtitle(artistName)
            .setArtist(artistName)
            .setArtworkUri(song.thumbnailUrl?.toUri())
            .setAlbumTitle(song.albumName)
            .setAlbumArtist(albumArtist)
            .setDisplayTitle(song.title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                putString("artwork_uri", song.thumbnailUrl)
            })
            .build()
    )
    .build()
}

fun SongItem.toMediaItem(): MediaItem {
    val artistName = formatArtistDisplay(artists.map { it.name })
    val albumArtist = artists.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: DEFAULT_ARTIST_NAME
    return MediaItem.Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artistName)
            .setArtist(artistName)
            .setArtworkUri(thumbnail.resize(1080, 1080).toUri())
            .setAlbumTitle(album?.name)
            .setAlbumArtist(albumArtist)
            .setDisplayTitle(title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                putString("artwork_uri", thumbnail.resize(1080, 1080))
            })
            .build()
    )
    .build()
}

fun MediaMetadata.toMediaItem(): MediaItem {
    val artistName = formatArtistDisplay(artists.map { it.name })
    val albumArtist = artists.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: DEFAULT_ARTIST_NAME
    return MediaItem.Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(this)
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artistName)
            .setArtist(artistName)
            .setArtworkUri(thumbnailUrl?.toUri())
            .setAlbumTitle(album?.title)
            .setAlbumArtist(albumArtist)
            .setDisplayTitle(title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                thumbnailUrl?.let { putString("artwork_uri", it) }
            })
            .build()
    )
    .build()
}
