/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.furkqn.music.playback.ExoDownloadService

object PlaylistDownloadHelper {
    fun computeDownloadState(
        songIds: List<String>,
        downloads: Map<String, Download>,
    ): Int {
        if (songIds.isEmpty()) return Download.STATE_STOPPED
        return when {
            songIds.all { downloads[it]?.state == STATE_COMPLETED } -> STATE_COMPLETED
            songIds.all {
                val state = downloads[it]?.state
                state == STATE_QUEUED || state == STATE_DOWNLOADING || state == STATE_COMPLETED
            } -> STATE_DOWNLOADING

            else -> Download.STATE_STOPPED
        }
    }

    fun downloadAll(
        context: Context,
        songs: List<Pair<String, String>>,
    ) {
        songs.forEach { (id, title) ->
            val downloadRequest =
                DownloadRequest
                    .Builder(id, id.toUri())
                    .setCustomCacheKey(id)
                    .setData(title.toByteArray())
                    .build()
            DownloadService.sendAddDownload(
                context,
                ExoDownloadService::class.java,
                downloadRequest,
                false,
            )
        }
    }

    fun cancelAll(
        context: Context,
        songIds: List<String>,
    ) {
        songIds.forEach { id ->
            DownloadService.sendRemoveDownload(
                context,
                ExoDownloadService::class.java,
                id,
                false,
            )
        }
    }
}

object PlaylistSourceKeys {
    fun auto(type: String): String = "auto:$type"

    fun top(size: String): String = "top:$size"

    const val CACHE: String = "cache"
}
