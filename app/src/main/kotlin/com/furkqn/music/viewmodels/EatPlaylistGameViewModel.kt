/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furkqn.music.game.EatPlaylistGameState
import com.furkqn.music.game.EatPlaylistLoadResult
import com.furkqn.music.game.EatPlaylistSnakeEngine
import com.furkqn.music.game.EatPlaylistSong
import com.furkqn.music.game.EatPlaylistSongLoader
import com.furkqn.music.constants.HideExplicitKey
import com.furkqn.music.constants.HideVideoSongsKey
import com.furkqn.music.game.SnakeDirection
import com.furkqn.music.playback.queues.filterExplicit
import com.furkqn.music.playback.queues.filterVideoSongs
import com.furkqn.music.utils.dataStore
import com.furkqn.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLDecoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EatPlaylistUiState {
    data object Loading : EatPlaylistUiState()

    data class Error(val message: String) : EatPlaylistUiState()

    data class Ready(
        val playlistTitle: String,
        val songs: List<EatPlaylistSong>,
        val sourceKey: String?,
        val gameState: EatPlaylistGameState,
        val highScore: Int,
        val cols: Int,
        val rows: Int,
    ) : EatPlaylistUiState()
}

@HiltViewModel
class EatPlaylistGameViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val songLoader: EatPlaylistSongLoader,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val source = savedStateHandle.get<String>("source")!!
    private val rawKey = savedStateHandle.get<String>("key")!!
    private val key = URLDecoder.decode(rawKey, Charsets.UTF_8.name())
    private val titleHint = savedStateHandle.get<String>("title")?.let {
        URLDecoder.decode(it, Charsets.UTF_8.name())
    }

    private val cols = GRID_COLS
    private val rows = GRID_ROWS

    private val _uiState = MutableStateFlow<EatPlaylistUiState>(EatPlaylistUiState.Loading)
    val uiState: StateFlow<EatPlaylistUiState> = _uiState.asStateFlow()

    private var engine: EatPlaylistSnakeEngine? = null
    private var loadResult: EatPlaylistLoadResult? = null
    private var tickJob: Job? = null
    private val isPaused = MutableStateFlow(false)

    private val _eatPlaybackEvents = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val eatPlaybackEvents = _eatPlaybackEvents.asSharedFlow()

    fun setPaused(paused: Boolean) {
        isPaused.value = paused
    }

    init {
        viewModelScope.launch {
            songLoader
                .load(source, key, titleHint)
                .onSuccess { result ->
                    if (result.songs.isEmpty()) {
                        _uiState.value = EatPlaylistUiState.Error("empty")
                        return@launch
                    }
                    loadResult = result
                    val playableSongs = filterSongsForPlayerQueue(result.songs)
                    if (playableSongs.isEmpty()) {
                        _uiState.value = EatPlaylistUiState.Error("empty")
                        return@launch
                    }
                    val shuffledSongs = playableSongs.shuffled()
                    val snakeEngine = EatPlaylistSnakeEngine(cols, rows, shuffledSongs)
                    engine = snakeEngine
                    val highScore = readHighScore()
                    _uiState.value =
                        EatPlaylistUiState.Ready(
                            playlistTitle = result.title,
                            songs = shuffledSongs,
                            sourceKey = result.sourceKey,
                            gameState = snakeEngine.initialState(),
                            highScore = highScore,
                            cols = cols,
                            rows = rows,
                        )
                    startTickLoop()
                }.onFailure { error ->
                    _uiState.value =
                        EatPlaylistUiState.Error(error.message ?: error.toString())
                }
        }
    }

    fun onSwipe(direction: SnakeDirection) {
        val ready = _uiState.value as? EatPlaylistUiState.Ready ?: return
        val updated = engine?.queueDirection(ready.gameState, direction) ?: return
        _uiState.updateReady { it.copy(gameState = updated) }
    }

    fun tryAgain(followingThumbnailUrl: String? = null) {
        val ready = _uiState.value as? EatPlaylistUiState.Ready ?: return
        val eng = engine ?: return
        val newState = eng.restartKeepingProgress(ready.gameState, followingThumbnailUrl)
        _uiState.updateReady {
            it.copy(
                gameState = newState.copy(isGameOver = false),
            )
        }
        isPaused.value = false
        startTickLoop()
    }

    /** Align game song order with the player queue (avoids shuffle desync). */
    fun applyQueueOrder(
        orderedSongs: List<EatPlaylistSong>,
        playingMediaId: String?,
        followingThumbnailUrl: String?,
    ) {
        if (orderedSongs.isEmpty()) return
        val ready = _uiState.value as? EatPlaylistUiState.Ready ?: return
        val newEngine = EatPlaylistSnakeEngine(cols, rows, orderedSongs)
        engine = newEngine
        val playingIndex =
            playingMediaId?.let { id -> orderedSongs.indexOfFirst { it.id == id } } ?: -1
        val priorState = ready.gameState
        val gameState =
            if (playingIndex >= 0 && !followingThumbnailUrl.isNullOrBlank()) {
                newEngine.stateForResumeWithMusic(playingIndex, followingThumbnailUrl)
            } else {
                // Keep visible board (snake, food cell, score) â€” only remap food song index.
                val currentFoodId = ready.songs.getOrNull(priorState.songIndex)?.id
                val newFoodIndex =
                    currentFoodId?.let { id ->
                        orderedSongs.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                    } ?: priorState.songIndex.coerceIn(0, orderedSongs.lastIndex.coerceAtLeast(0))
                priorState.copy(songIndex = newFoodIndex)
            }
        _uiState.updateReady {
            it.copy(
                songs = orderedSongs,
                gameState = gameState,
            )
        }
    }

    /** Start game with a random track already playing on the snake head. */
    fun applyAutoplayStart(
        orderedSongs: List<EatPlaylistSong>,
        startIndex: Int,
        startThumbnailUrl: String?,
    ) {
        if (orderedSongs.isEmpty()) return
        val ready = _uiState.value as? EatPlaylistUiState.Ready ?: return
        val startSong = orderedSongs.getOrNull(startIndex) ?: return
        val newEngine = EatPlaylistSnakeEngine(cols, rows, orderedSongs)
        engine = newEngine
        val thumb = startThumbnailUrl ?: startSong.resolvedThumbnailUrl()
        val gameState =
            newEngine.stateForAutoplayStart(
                priorState = ready.gameState,
                playingIndex = startIndex,
                playingThumbnailUrl = thumb.orEmpty(),
            )
        _uiState.updateReady {
            it.copy(
                songs = orderedSongs,
                gameState = gameState,
            )
        }
    }

    /** ÅžarkÄ± doÄŸal olarak bitip sÄ±radaki parÃ§aya geÃ§ince yemeÄŸi sanal tÃ¼ket. */
    fun onNaturalSongEndEat() {
        val ready = _uiState.value as? EatPlaylistUiState.Ready ?: return
        if (ready.gameState.isGameOver) return
        val eng = engine ?: return
        val next = eng.consumeFoodBySongEnd(ready.gameState)
        var highScore = ready.highScore
        if (next.isGameOver && next.score > highScore) {
            highScore = next.score
            viewModelScope.launch { saveHighScore(highScore) }
        }
        _uiState.updateReady {
            it.copy(gameState = next, highScore = highScore)
        }
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob =
            viewModelScope.launch {
                while (isActive) {
                    val ready = _uiState.value as? EatPlaylistUiState.Ready ?: break
                    if (ready.gameState.isGameOver) break
                    val eng = engine ?: break
                    val interval = EatPlaylistSnakeEngine.tickIntervalMsForScore(ready.gameState.score)
                    if (isPaused.value) {
                        delay(50)
                        continue
                    }
                    delay(interval)
                    if (isPaused.value) continue
                    val current = _uiState.value as? EatPlaylistUiState.Ready ?: break
                    if (current.gameState.isGameOver) break
                    val next = eng.tick(current.gameState)
                    var highScore = current.highScore
                    if (next.isGameOver && next.score > highScore) {
                        highScore = next.score
                        saveHighScore(highScore)
                    }
                    next.lastEatenSongIndex?.let { eatenIndex ->
                        _eatPlaybackEvents.tryEmit(eatenIndex)
                    }
                    _uiState.updateReady {
                        it.copy(gameState = next, highScore = highScore)
                    }
                }
            }
    }

    private fun MutableStateFlow<EatPlaylistUiState>.updateReady(
        transform: (EatPlaylistUiState.Ready) -> EatPlaylistUiState.Ready,
    ) {
        val current = value
        if (current is EatPlaylistUiState.Ready) {
            value = transform(current)
        }
    }

    private suspend fun filterSongsForPlayerQueue(songs: List<EatPlaylistSong>): List<EatPlaylistSong> {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        if (!hideExplicit && !hideVideoSongs) return songs
        return songs.filter { song ->
            var items = listOf(song.toMediaItem())
            if (hideExplicit) items = items.filterExplicit()
            if (hideVideoSongs) items = items.filterVideoSongs(true)
            items.isNotEmpty()
        }
    }

    private fun highScorePreferenceKey() = intPreferencesKey("snake_high_score_${source}_$key")

    private suspend fun readHighScore(): Int = context.dataStore.get(highScorePreferenceKey(), 0)

    private suspend fun saveHighScore(score: Int) {
        context.dataStore.edit { prefs ->
            prefs[highScorePreferenceKey()] = score
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val GRID_COLS = 12
        const val GRID_ROWS = 18
    }
}
