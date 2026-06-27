/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.furkqn.music.LocalDownloadUtil
import com.furkqn.music.LocalNavController
import com.furkqn.music.LocalPlayerAwareWindowInsets
import com.furkqn.music.LocalPlayerConnection
import com.furkqn.music.R
import com.furkqn.music.constants.AccountNameKey
import com.furkqn.music.constants.HideExplicitKey
import com.furkqn.music.constants.SongSortDescendingKey
import com.furkqn.music.constants.SongSortType
import com.furkqn.music.constants.SongSortTypeKey
import com.furkqn.music.db.entities.Song
import com.furkqn.music.extensions.toMediaItem
import com.furkqn.music.playback.ExoDownloadService
import com.furkqn.music.playback.queues.ListQueue
import com.furkqn.music.ui.component.DraggableScrollbar
import com.furkqn.music.ui.component.EmptyPlaceholder
import com.furkqn.music.ui.component.IconButton
import com.furkqn.music.ui.component.LocalMenuState
import androidx.compose.foundation.background
import com.furkqn.music.ui.component.PlaylistAuthorInfo
import com.furkqn.music.ui.component.SongListItem
import com.furkqn.music.ui.component.PlaylistFloatingTopBar
import com.furkqn.music.ui.component.PlaylistGradientBackground
import com.furkqn.music.ui.component.rememberPlaylistContentPadding
import com.furkqn.music.ui.component.SpotifyPlaylistHeader
import com.furkqn.music.utils.PlaylistDownloadHelper
import com.furkqn.music.utils.PlaylistSourceKeys
import com.furkqn.music.ui.component.SortHeader
import com.furkqn.music.game.EatPlaylistRoutes
import com.furkqn.music.ui.menu.CachePlaylistMenu
import com.furkqn.music.ui.menu.SelectionSongMenu
import com.furkqn.music.ui.menu.SongMenu
import com.furkqn.music.ui.utils.backToMain
import com.furkqn.music.utils.rememberEnumPreference
import com.furkqn.music.utils.rememberPreference
import com.furkqn.music.viewmodels.CachePlaylistViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    viewModel: CachePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val cachedSongs by viewModel.cachedSongs.collectAsStateWithLifecycle()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val sortedSongs = remember(cachedSongs, sortType, sortDescending) {
        val sorted = when (sortType) {
            SongSortType.CREATE_DATE -> cachedSongs.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
            SongSortType.NAME -> cachedSongs.sortedBy { it.song.title }
            SongSortType.ARTIST -> cachedSongs.sortedBy { song ->
                song.artists.joinToString(separator = "") { it.name }
            }
            SongSortType.PLAY_TIME -> cachedSongs.sortedBy { it.song.totalPlayTime }
        }
        if (sortDescending) sorted.reversed() else sorted
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val filteredSongs = remember(sortedSongs, query) {
        if (query.text.isEmpty()) sortedSongs
        else sortedSongs.filter { song ->
            song.title.contains(query.text, true) ||
                song.artists.any { it.name.contains(query.text, true) }
        }
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }

        if (selectionAnchorSongId != null && filteredSongs.none { it.id == selectionAnchorSongId }) {
            selectionAnchorSongId = filteredSongs.firstOrNull { it.id in selection }?.id
        }
    }

    val gradientThumbnailUrl = remember(filteredSongs) { filteredSongs.firstOrNull()?.thumbnailUrl }

    val playlistContentPadding = rememberPlaylistContentPadding()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        PlaylistGradientBackground(
            thumbnailUrl = gradientThumbnailUrl,
            cacheKey = "cache_playlist",
            bottomColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        )
        LazyColumn(
            state = lazyListState,
            contentPadding = playlistContentPadding,
        ) {
            if (filteredSongs.isEmpty() && !isSearching) {
                item(key = "empty_placeholder") {
                    EmptyPlaceholder(
                        icon = R.drawable.music_note,
                        text = stringResource(R.string.playlist_is_empty),
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (filteredSongs.isEmpty() && isSearching) {
                item(key = "no_results") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                        modifier = Modifier.animateItem()
                    )
                }
            } else {
                if (filteredSongs.isNotEmpty() && !isSearching) {
                    item(key = "playlist_header") {
                        CachePlaylistHeader(
                            songs = filteredSongs,
                            onSortClick = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(1)
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (filteredSongs.isNotEmpty()) {
                    item(key = "sort_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .animateItem(),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        SongSortType.NAME -> R.string.sort_by_name
                                        SongSortType.ARTIST -> R.string.sort_by_artist
                                        SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                itemsIndexed(filteredSongs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }

                    SongListItem(
                        song = song,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        trailingContent = {
                            if (inSelectMode) {
                                Checkbox(
                                    checked = song.id in selection,
                                    onCheckedChange = onCheckedChange
                                )
                            } else {
                                IconButton(onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            onDismiss = menuState::dismiss,
                                            isFromCache = true,
                                        )
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .combinedClickable(
                                onClick = {
                                    if (inSelectMode) {
                                        onCheckedChange(song.id !in selection)
                                    } else if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Cache Songs",
                                                items = cachedSongs.map { it.toMediaItem() },
                                                startIndex = cachedSongs.indexOfFirst { it.id == song.id }
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!inSelectMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inSelectMode = true
                                        onCheckedChange(true)
                                        selectionAnchorSongId = song.id
                                    } else {
                                        val anchorIndex = selectionAnchorSongId?.let { anchorSongId ->
                                            filteredSongs.indexOfFirst { it.id == anchorSongId }
                                        } ?: -1

                                        if (anchorIndex == -1) {
                                            onCheckedChange(true)
                                            selectionAnchorSongId = song.id
                                        } else {
                                            val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                            for (rangeIndex in range) {
                                                val rangeSongId = filteredSongs[rangeIndex].id
                                                if (rangeSongId !in selection) {
                                                    selection.add(rangeSongId)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 2
        )

        PlaylistFloatingTopBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = {
                when {
                    inSelectMode -> {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    isSearching -> {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    showTopBarTitle -> {
                        Text(
                            stringResource(R.string.cached_playlist),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        isSearching -> {
                            isSearching = false
                            query = TextFieldValue()
                            focusManager.clearFocus()
                        }
                        inSelectMode -> {
                            onExitSelectionMode()
                        }
                        else -> {
                            navController.navigateUp()
                        }
                    }
                }, onLongClick = {
                    if (!isSearching && !inSelectMode) {
                        navController.backToMain()
                    }
                }) {
                    Icon(
                        painter = painterResource(
                            if (inSelectMode) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(
                        checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                        onCheckedChange = {
                            if (selection.size == filteredSongs.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(filteredSongs.map { it.id })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = filteredSongs.filter { it.id in selection },
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun CachePlaylistHeader(
    songs: List<Song>,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    val playlistTitle = stringResource(R.string.cached_playlist)
    val playlistLength = remember(songs) { songs.sumOf { it.song.duration } }
    val previewThumbnail = songs.firstOrNull()?.thumbnailUrl

    val (accountNamePref, _) = rememberPreference(AccountNameKey, "")
    val authorInfo =
        if (accountNamePref.isNotBlank()) {
            PlaylistAuthorInfo(name = accountNamePref)
        } else {
            null
        }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                PlaylistDownloadHelper.computeDownloadState(
                    songIds = songs.map { it.song.id },
                    downloads = downloads,
                )
        }
    }

    val handleDownloadClick = {
        when (downloadState) {
            Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                PlaylistDownloadHelper.cancelAll(context, songs.map { it.song.id })
            }

            else -> {
                PlaylistDownloadHelper.downloadAll(
                    context,
                    songs.map { it.song.id to it.song.title },
                )
            }
        }
    }

    val handleShareClick = {
        val shareText = songs.joinToString("\n") { it.song.title }
        val sendIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    SpotifyPlaylistHeader(
        title = playlistTitle,
        songCount = songs.size,
        durationSeconds = playlistLength,
        author = authorInfo,
        modifier = modifier,
        sourceKey = PlaylistSourceKeys.CACHE,
        enabled = songs.isNotEmpty(),
        downloadState = downloadState,
        gradientThumbnailUrl = previewThumbnail,
        gradientCacheKey = PlaylistSourceKeys.CACHE,
        onDownloadClick = handleDownloadClick,
        onShareClick = handleShareClick,
        onMenuClick = {
            menuState.show {
                CachePlaylistMenu(
                    downloadState = downloadState,
                    onQueue = {
                        playerConnection.addToQueue(
                            songs.map { it.toMediaItem() },
                        )
                    },
                    onDownload = handleDownloadClick,
                    onEatPlaylist = {
                        navController.navigate(
                            EatPlaylistRoutes.route(
                                source = "cache",
                                key = "cache",
                                title = playlistTitle,
                            ),
                        )
                    },
                    onDismiss = { menuState.dismiss() },
                )
            }
        },
        onAddClick = { navController.navigate("search_input") },
        onSortClick = onSortClick,
        onPlay = { shuffleEnabled ->
            playerConnection.playQueue(
                ListQueue(
                    title = playlistTitle,
                    items = songs.map { it.toMediaItem() },
                    sourceKey = PlaylistSourceKeys.CACHE,
                ),
                shuffleEnabled = shuffleEnabled,
            )
        },
        artwork = {
            Surface(
                modifier =
                    Modifier
                        .size(200.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(4.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        ),
                shape = RoundedCornerShape(4.dp),
            ) {
                AsyncImage(
                    model = previewThumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}
