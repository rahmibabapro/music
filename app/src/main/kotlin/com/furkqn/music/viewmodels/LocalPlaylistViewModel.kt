/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furkqn.music.innertube.YouTube
import com.furkqn.music.innertube.models.PlaylistItem
import com.furkqn.music.constants.HideVideoSongsKey
import com.furkqn.music.constants.PlaylistSongSortDescendingKey
import com.furkqn.music.constants.PlaylistSongSortType
import com.furkqn.music.constants.PlaylistSongSortTypeKey
import com.furkqn.music.db.MusicDatabase
import com.furkqn.music.db.entities.PlaylistSong
import com.furkqn.music.db.entities.Song
import com.furkqn.music.extensions.reversed
import com.furkqn.music.extensions.toEnum
import com.furkqn.music.utils.BpmLookupService
import com.furkqn.music.utils.BpmMixOrder
import com.furkqn.music.utils.DismissedRecommendationsCache
import com.furkqn.music.utils.PlaylistRecommendationItem
import com.furkqn.music.utils.PlaylistRecommendations
import com.furkqn.music.utils.dataStore
import com.furkqn.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

enum class PlaylistRecommendationState {
    Idle,
    Loading,
    Loaded,
    Empty,
}

data class BpmMixProgress(
    val current: Int = 0,
    val total: Int = 0,
)

@HiltViewModel
class LocalPlaylistViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val playlistId = savedStateHandle.get<String>("playlistId")!!
    val playlist =
        database
            .playlist(playlistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _onlinePlaylist = MutableStateFlow<PlaylistItem?>(null)
    val onlinePlaylist: StateFlow<PlaylistItem?> = _onlinePlaylist
    val playlistSongs: StateFlow<List<PlaylistSong>> =
        combine(
            database.playlistSongs(playlistId),
            context.dataStore.data
                .map {
                    Triple(
                        it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM),
                        it[PlaylistSongSortDescendingKey] ?: true,
                        it[HideVideoSongsKey] ?: false
                    )
                }.distinctUntilChanged(),
        ) { songs, (sortType, sortDescending, hideVideoSongs) ->
            val filteredSongs = if (hideVideoSongs) {
                songs.filter { !it.song.song.isVideo }
            } else {
                songs
            }
            when (sortType) {
                PlaylistSongSortType.CUSTOM -> filteredSongs
                PlaylistSongSortType.CREATE_DATE -> filteredSongs.sortedBy { it.map.id }
                PlaylistSongSortType.NAME -> {
                    val collator = Collator.getInstance(Locale.getDefault())
                    collator.strength = Collator.PRIMARY
                    filteredSongs.sortedWith(compareBy(collator) { it.song.song.title })
                }
                PlaylistSongSortType.ARTIST -> {
                    val collator = Collator.getInstance(Locale.getDefault())
                    collator.strength = Collator.PRIMARY
                    filteredSongs
                        .sortedWith(compareBy(collator) { song -> song.song.artists.joinToString("") { it.name } })
                        .groupBy { it.song.album?.title }
                        .flatMap { (_, songsByAlbum) ->
                            songsByAlbum.sortedBy {
                                it.song.artists.joinToString(
                                    ""
                                ) { it.name }
                            }
                        }
                }

                PlaylistSongSortType.PLAY_TIME -> filteredSongs.sortedBy { it.song.song.totalPlayTime }
            }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _recommendedItems = MutableStateFlow<List<PlaylistRecommendationItem>>(emptyList())
    val recommendedItems: StateFlow<List<PlaylistRecommendationItem>> = _recommendedItems

    private val _recommendationState = MutableStateFlow(PlaylistRecommendationState.Idle)
    val recommendationState: StateFlow<PlaylistRecommendationState> = _recommendationState

    private val _bpmMixProgress = MutableStateFlow<BpmMixProgress?>(null)
    val bpmMixProgress: StateFlow<BpmMixProgress?> = _bpmMixProgress

    init {
        viewModelScope.launch {
            val sortedSongs =
                playlistSongs.first().sortedWith(compareBy({ it.map.position }, { it.map.id }))
            database.transaction {
                sortedSongs.forEachIndexed { index, playlistSong ->
                    if (playlistSong.map.position != index) {
                        update(playlistSong.map.copy(position = index))
                    }
                }
            }
        }

        viewModelScope.launch {
            val localPlaylist = playlist.first { it != null }
            val browseId = localPlaylist?.playlist?.browseId
            if (browseId != null) {
                val page = withContext(Dispatchers.IO) {
                    YouTube.playlist(browseId).getOrNull()
                }
                val online = page?.playlist
                _onlinePlaylist.value = online
            }
        }

        viewModelScope.launch {
            playlistSongs.collect { songs ->
                if (songs.size >= 2) {
                    loadRecommendations(songs.map { it.song })
                } else {
                    _recommendedItems.value = emptyList()
                    _recommendationState.value = PlaylistRecommendationState.Empty
                }
            }
        }
    }

    suspend fun songsInDbOrder(): List<PlaylistSong> =
        withContext(Dispatchers.IO) {
            database.playlistSongsBlocking(playlistId).sortedBy { it.map.position }
        }

    fun loadRecommendations(playlistSongEntities: List<Song>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = playlistSongEntities ?: playlistSongs.first().map { it.song }
            if (songs.size < 2) {
                _recommendedItems.value = emptyList()
                _recommendationState.value = PlaylistRecommendationState.Empty
                return@launch
            }
            _recommendationState.value = PlaylistRecommendationState.Loading
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val recommendations =
                PlaylistRecommendations.load(
                    context = context,
                    database = database,
                    playlistId = playlistId,
                    playlistSongs = songs,
                    hideVideoSongs = hideVideoSongs,
                )
            _recommendedItems.value = recommendations
            _recommendationState.value =
                if (recommendations.isEmpty()) {
                    PlaylistRecommendationState.Empty
                } else {
                    PlaylistRecommendationState.Loaded
                }
        }
    }

    fun dismissRecommendation(songId: String) {
        DismissedRecommendationsCache.dismiss(context, playlistId, songId)
        _recommendedItems.value = _recommendedItems.value.filter { it.song.id != songId }
        if (_recommendedItems.value.isEmpty()) {
            _recommendationState.value = PlaylistRecommendationState.Empty
        }
    }

    suspend fun addRecommendationToPlaylist(songId: String): Boolean =
        withContext(Dispatchers.IO) {
            val playlistEntity = playlist.first() ?: return@withContext false
            if (database.playlistSongsBlocking(playlistId).any { it.song.id == songId }) {
                return@withContext false
            }
            database.addSongToPlaylist(playlistEntity, listOf(songId))
            playlistEntity.playlist.browseId?.let { browseId ->
                YouTube.addToPlaylist(browseId, songId)
            }
            _recommendedItems.value = _recommendedItems.value.filter { it.song.id != songId }
            if (_recommendedItems.value.isEmpty()) {
                _recommendationState.value = PlaylistRecommendationState.Empty
            }
            true
        }

    suspend fun addStarredRecommendationsToPlaylist(): Int =
        withContext(Dispatchers.IO) {
            val ids = _recommendedItems.value.filter { it.isStarred }.map { it.song.id }
            var added = 0
            ids.forEach { id ->
                if (addRecommendationToPlaylist(id)) {
                    added++
                }
            }
            added
        }

    suspend fun computeBpmMixOrder(
        songs: List<PlaylistSong>,
        onProgress: (BpmMixProgress) -> Unit,
    ): List<PlaylistSong> =
        withContext(Dispatchers.IO) {
            val bpmBySongId = linkedMapOf<String, Float>()
            songs.forEachIndexed { index, playlistSong ->
                onProgress(BpmMixProgress(current = index + 1, total = songs.size))
                val song = playlistSong.song
                val artist = song.artists.firstOrNull()?.name.orEmpty()
                val bpm =
                    BpmLookupService.lookupWithCache(
                        context = context,
                        songId = song.id,
                        title = song.song.title,
                        artist = artist,
                    )
                if (bpm != null) {
                    bpmBySongId[song.id] = bpm
                }
            }
            BpmMixOrder.order(songs, bpmBySongId)
        }

    suspend fun applyPlaylistOrder(order: List<PlaylistSong>) {
        withContext(Dispatchers.IO) {
            val offset = order.size + 1
            database.transaction {
                order.forEachIndexed { index, playlistSong ->
                    update(playlistSong.map.copy(position = index + offset))
                }
                order.forEachIndexed { index, playlistSong ->
                    update(playlistSong.map.copy(position = index))
                }
            }
            val browseId = playlist.first()?.playlist?.browseId
            if (browseId != null) {
                val oldMaps = database.playlistSongMaps(playlistId, 0)
                order.forEachIndexed { newIndex, playlistSong ->
                    val oldIndex = oldMaps.indexOfFirst { it.id == playlistSong.map.id }
                    if (oldIndex != -1 && oldIndex != newIndex) {
                        val setVideoId = playlistSong.map.setVideoId
                        val successorSetVideoId = order.getOrNull(newIndex + 1)?.map?.setVideoId
                        if (setVideoId != null) {
                            YouTube.moveSongPlaylist(browseId, setVideoId, successorSetVideoId)
                        }
                    }
                }
            }
        }
    }

    fun clearBpmMixProgress() {
        _bpmMixProgress.value = null
    }

    fun setBpmMixProgress(progress: BpmMixProgress) {
        _bpmMixProgress.value = progress
    }
}
