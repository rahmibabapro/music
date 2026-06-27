/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.game

import android.content.Context
import com.furkqn.music.constants.HideExplicitKey
import com.furkqn.music.constants.HideVideoSongsKey
import com.furkqn.music.constants.MyTopFilter
import com.furkqn.music.constants.SongSortDescendingKey
import com.furkqn.music.constants.SongSortType
import com.furkqn.music.constants.SongSortTypeKey
import com.furkqn.music.db.MusicDatabase
import com.furkqn.music.di.DownloadCache
import com.furkqn.music.di.PlayerCache
import com.furkqn.music.extensions.filterExplicit
import com.furkqn.music.extensions.filterVideoSongs
import com.furkqn.music.extensions.toEnum
import com.furkqn.music.innertube.YouTube
import com.furkqn.music.utils.PlaylistSourceKeys
import com.furkqn.music.utils.dataStore
import com.furkqn.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class EatPlaylistLoadResult(
    val title: String,
    val songs: List<EatPlaylistSong>,
    val sourceKey: String?,
)

@Singleton
class EatPlaylistSongLoader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: androidx.media3.datasource.cache.Cache,
    @DownloadCache private val downloadCache: androidx.media3.datasource.cache.Cache,
) {
    suspend fun load(
        source: String,
        key: String,
        titleHint: String?,
    ): Result<EatPlaylistLoadResult> =
        runCatching {
            when (source) {
                "local" -> loadLocal(key, titleHint)
                "online" -> loadOnline(key, titleHint)
                "auto" -> loadAuto(key, titleHint)
                "cache" -> loadCache(titleHint)
                "top" -> loadTop(key, titleHint)
                else -> error("Unknown playlist source: $source")
            }
        }

    private suspend fun loadLocal(
        playlistId: String,
        titleHint: String?,
    ): EatPlaylistLoadResult {
        val playlist = database.playlist(playlistId).first() ?: error("Playlist not found")
        val songs =
            database
                .playlistSongs(playlistId)
                .first()
                .map { it.song.toEatPlaylistSong() }
        return EatPlaylistLoadResult(
            title = titleHint ?: playlist.playlist.name,
            songs = songs,
            sourceKey = playlistId,
        )
    }

    private suspend fun loadOnline(
        browseId: String,
        titleHint: String?,
    ): EatPlaylistLoadResult {
        val page = YouTube.playlist(browseId).getOrThrow()
        val songs = page.songs.map { it.toEatPlaylistSong() }
        return EatPlaylistLoadResult(
            title = titleHint ?: page.playlist.title,
            songs = songs,
            sourceKey = browseId,
        )
    }

    private suspend fun loadAuto(
        autoKey: String,
        titleHint: String?,
    ): EatPlaylistLoadResult {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val sortType = context.dataStore.get(SongSortTypeKey).toEnum(SongSortType.CREATE_DATE)
        val descending = context.dataStore.get(SongSortDescendingKey, true)

        val flow =
            when (autoKey) {
                "liked" -> database.likedSongs(sortType, descending)
                "downloaded" -> database.downloadedSongs(sortType, descending)
                "uploaded" -> database.uploadedSongs(sortType, descending)
                else -> error("Unknown auto playlist: $autoKey")
            }
        val songs =
            flow
                .first()
                .filterExplicit(hideExplicit)
                .filterVideoSongs(hideVideoSongs)
                .map { it.toEatPlaylistSong() }
        val defaultTitle =
            when (autoKey) {
                "liked" -> "Liked"
                "downloaded" -> "Downloaded"
                "uploaded" -> "Uploaded"
                else -> autoKey
            }
        return EatPlaylistLoadResult(
            title = titleHint ?: defaultTitle,
            songs = songs,
            sourceKey = PlaylistSourceKeys.auto(autoKey),
        )
    }

    private suspend fun loadCache(titleHint: String?): EatPlaylistLoadResult {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val candidateIds = playerCache.keys.toSet() + downloadCache.keys.toSet()
        val songs =
            if (candidateIds.isEmpty()) {
                emptyList()
            } else {
                database
                    .getSongsByIds(candidateIds.toList())
                    .filter { it.song.dateDownload != null }
                    .filter { song ->
                        val contentLength = song.format?.contentLength
                        song.song.isDownloaded ||
                            (
                                contentLength != null &&
                                    (
                                        downloadCache.isCached(song.song.id, 0, contentLength) ||
                                            playerCache.isCached(song.song.id, 0, contentLength)
                                    )
                            )
                    }
                    .sortedByDescending { it.song.dateDownload }
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .map { it.toEatPlaylistSong() }
            }
        return EatPlaylistLoadResult(
            title = titleHint ?: "Cached",
            songs = songs,
            sourceKey = PlaylistSourceKeys.CACHE,
        )
    }

    private suspend fun loadTop(
        topKey: String,
        titleHint: String?,
    ): EatPlaylistLoadResult {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val limit = topKey.toIntOrNull() ?: 50
        val now = LocalDateTime.now()
        val songs =
            database
                .mostPlayedSongs(
                    fromTimeStamp = MyTopFilter.ALL_TIME.toLocalDateTime(),
                    limit = limit,
                    offset = 0,
                    toTimeStamp = now,
                ).first()
                .let { list -> if (hideVideoSongs) list.filter { !it.song.isVideo } else list }
                .map { it.toEatPlaylistSong() }
        return EatPlaylistLoadResult(
            title = titleHint ?: "Top $limit",
            songs = songs,
            sourceKey = PlaylistSourceKeys.top(topKey),
        )
    }
}
