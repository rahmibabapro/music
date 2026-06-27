/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class Playlist(
    @Embedded
    val playlist: PlaylistEntity,
    val songCount: Int,
    @ColumnInfo(name = "firstSongThumbnail")
    val firstSongThumbnail: String? = null,
    @ColumnInfo(name = "previewThumbnailUrls")
    val previewThumbnailUrls: String? = null,
    @Relation(
        entity = SongEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        projection = ["thumbnailUrl"],
        associateBy =
        Junction(
            value = PlaylistSongMapPreview::class,
            parentColumn = "playlistId",
            entityColumn = "songId",
        ),
    )
    val songThumbnails: List<String?>,
) : LocalItem() {
    override val id: String
        get() = playlist.id
    override val title: String
        get() = playlist.name
    override val thumbnailUrl: String?
        get() = null
    
    val thumbnails: List<String>
        get() {
            playlist.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { return listOf(it) }

            val fromSql =
                previewThumbnailUrls
                    ?.split(PREVIEW_THUMBNAIL_SEPARATOR)
                    ?.mapNotNull { url -> url.takeIf { it.isNotBlank() } }
                    .orEmpty()
            if (fromSql.isNotEmpty()) return fromSql

            val fromRelation =
                songThumbnails.mapNotNull { url -> url?.takeIf { it.isNotBlank() } }
            if (fromRelation.isNotEmpty()) return fromRelation

            return listOfNotNull(firstSongThumbnail?.takeIf { it.isNotBlank() })
        }

    companion object {
        const val PREVIEW_THUMBNAIL_SEPARATOR = "\u001F"
    }
}
