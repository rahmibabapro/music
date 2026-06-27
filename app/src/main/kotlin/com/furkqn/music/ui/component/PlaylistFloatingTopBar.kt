/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.furkqn.music.LocalPlayerAwareWindowInsets
import com.furkqn.music.constants.AppBarHeight

@Composable
fun rememberPlaylistContentPadding(): PaddingValues {
    val full = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    return remember(full, layoutDirection) {
        PaddingValues(
            start = full.calculateLeftPadding(layoutDirection),
            end = full.calculateRightPadding(layoutDirection),
            top = 0.dp,
            bottom = full.calculateBottomPadding(),
        )
    }
}

@Composable
fun rememberPlaylistHeaderTopPadding(): Dp {
    val statusBarTop =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()
    return remember(statusBarTop) {
        statusBarTop + AppBarHeight
    }
}

@Composable
fun rememberPlaylistHeaderOffset(): Int {
    val density = LocalDensity.current
    val systemBarsTop =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()
    return remember(density, systemBarsTop) {
        with(density) {
            -(systemBarsTop + AppBarHeight).roundToPx()
        }
    }
}

@Composable
fun PlaylistFloatingTopBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit,
    title: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .zIndex(1f)
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .height(AppBarHeight)
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon()
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            title()
        }
        actions()
    }
}

fun Modifier.playlistHeaderOffset(headerOffset: Int): Modifier =
    this.then(
        Modifier.offset {
            IntOffset(x = 0, y = headerOffset)
        },
    )
