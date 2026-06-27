/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.game

import kotlin.random.Random

data class GridCell(
    val x: Int,
    val y: Int,
)

enum class SnakeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

data class SnakeSegment(
    val cell: GridCell,
    val thumbnailUrl: String?,
)

data class EatPlaylistGameState(
    val cols: Int,
    val rows: Int,
    val snake: List<SnakeSegment>,
    val direction: SnakeDirection,
    val pendingDirection: SnakeDirection?,
    val foodCell: GridCell,
    /** Index into [songs] for the food currently on the board. */
    val songIndex: Int,
    /** Set when food is eaten; triggers playback of that exact song. */
    val lastEatenSongIndex: Int? = null,
    val tickGeneration: Long = 0,
    val score: Int,
    val isGameOver: Boolean,
) {
    val currentSong: EatPlaylistSong?
        get() = null
}

class EatPlaylistSnakeEngine(
    private val cols: Int,
    private val rows: Int,
    private val songs: List<EatPlaylistSong>,
) {
    fun initialState(): EatPlaylistGameState {
        val headCell = GridCell(cols / 2, rows / 2)
        val head =
            SnakeSegment(
                cell = headCell,
                thumbnailUrl = null,
            )
        val foodCell = randomEmptyCell(setOf(headCell))
        return EatPlaylistGameState(
            cols = cols,
            rows = rows,
            snake = listOf(head),
            direction = SnakeDirection.RIGHT,
            pendingDirection = null,
            foodCell = foodCell,
            /** First food is the next track after the random autoplay start. */
            songIndex = 0,
            score = 0,
            isGameOver = false,
        )
    }

    fun restartKeepingProgress(
        state: EatPlaylistGameState,
        followingThumbnailUrl: String? = null,
    ): EatPlaylistGameState {
        val fresh = initialState()
        val headCell = fresh.snake.first().cell
        val snake =
            if (!followingThumbnailUrl.isNullOrBlank()) {
                listOf(SnakeSegment(headCell, thumbnailUrl = followingThumbnailUrl))
            } else {
                fresh.snake
            }
        return fresh.copy(
            snake = snake,
            songIndex = state.songIndex,
            foodCell = randomEmptyCell(snake.map { it.cell }.toSet()),
        )
    }

    /** Enter snake while music is already playing — head shows the playing track. */
    fun stateForResumeWithMusic(
        playingSongIndex: Int,
        playingThumbnailUrl: String,
    ): EatPlaylistGameState {
        val fresh = initialState()
        val foodIndex = (playingSongIndex + 1) % songs.size
        val headCell = fresh.snake.first().cell
        return fresh.copy(
            songIndex = foodIndex,
            snake = listOf(SnakeSegment(headCell, thumbnailUrl = playingThumbnailUrl)),
            foodCell = fresh.foodCell,
        )
    }

    /** Fresh game — head shows a randomly chosen starting track. */
    fun stateForAutoplayStart(
        priorState: EatPlaylistGameState,
        playingIndex: Int,
        playingThumbnailUrl: String,
    ): EatPlaylistGameState {
        val foodIndex = (playingIndex + 1) % songs.size
        val headCell = priorState.snake.firstOrNull()?.cell ?: GridCell(cols / 2, rows / 2)
        return priorState.copy(
            songIndex = foodIndex,
            snake = listOf(SnakeSegment(headCell, thumbnailUrl = playingThumbnailUrl)),
            lastEatenSongIndex = null,
        )
    }

    private fun cellBehindHead(
        head: GridCell,
        direction: SnakeDirection,
    ): GridCell {
        val raw =
            when (direction) {
                SnakeDirection.RIGHT -> GridCell(head.x - 1, head.y)
                SnakeDirection.LEFT -> GridCell(head.x + 1, head.y)
                SnakeDirection.UP -> GridCell(head.x, head.y + 1)
                SnakeDirection.DOWN -> GridCell(head.x, head.y - 1)
            }
        return GridCell(
            (raw.x + cols) % cols,
            (raw.y + rows) % rows,
        )
    }

    fun consumeFoodBySongEnd(state: EatPlaylistGameState): EatPlaylistGameState {
        if (state.isGameOver || songs.isEmpty()) return state

        val eatenThumbnail = songs[state.songIndex].resolvedThumbnailUrl()
        val prevHead = state.snake.first()
        val newTailCell = computeTailExtensionCell(state)
        val bodyCells = state.snake.map { it.cell }.toSet()
        if (newTailCell in bodyCells) {
            return state.copy(isGameOver = true)
        }

        // Natural end: head keeps cell but shows eaten art; previous head art extends as tail.
        val newHead = prevHead.copy(thumbnailUrl = eatenThumbnail)
        val shiftedBody =
            if (state.snake.size <= 1) {
                emptyList()
            } else {
                state.snake.drop(1)
            }
        val newSnake =
            listOf(newHead) +
                shiftedBody +
                SnakeSegment(newTailCell, prevHead.thumbnailUrl)
        val nextSongIndex = (state.songIndex + 1) % songs.size
        val occupied = newSnake.map { it.cell }.toSet()
        val nextFood = randomEmptyCell(occupied)

        return state.copy(
            snake = newSnake,
            foodCell = nextFood,
            songIndex = nextSongIndex,
            lastEatenSongIndex = null,
            tickGeneration = state.tickGeneration + 1,
            score = state.score + 1,
        )
    }

    private fun computeTailExtensionCell(state: EatPlaylistGameState): GridCell {
        if (state.snake.size == 1) {
            return cellBehindHead(state.snake.first().cell, state.direction)
        }
        val tail = state.snake.last().cell
        val beforeTail = state.snake[state.snake.size - 2].cell
        return extendCell(tail, beforeTail)
    }

    private fun extendCell(
        from: GridCell,
        prev: GridCell,
    ): GridCell {
        var dx = from.x - prev.x
        var dy = from.y - prev.y
        if (dx > cols / 2) dx -= cols
        if (dx < -cols / 2) dx += cols
        if (dy > rows / 2) dy -= rows
        if (dy < -rows / 2) dy += rows
        return GridCell(
            (from.x + dx + cols) % cols,
            (from.y + dy + rows) % rows,
        )
    }

    fun queueDirection(
        state: EatPlaylistGameState,
        direction: SnakeDirection,
    ): EatPlaylistGameState {
        if (state.isGameOver || !isValidTurn(state.direction, direction)) {
            return state
        }
        return state.copy(pendingDirection = direction)
    }

    fun tick(state: EatPlaylistGameState): EatPlaylistGameState {
        if (state.isGameOver || songs.isEmpty()) return state

        val direction = state.pendingDirection ?: state.direction
        val head = state.snake.first().cell
        val rawNextHead =
            when (direction) {
                SnakeDirection.UP -> GridCell(head.x, head.y - 1)
                SnakeDirection.DOWN -> GridCell(head.x, head.y + 1)
                SnakeDirection.LEFT -> GridCell(head.x - 1, head.y)
                SnakeDirection.RIGHT -> GridCell(head.x + 1, head.y)
            }
        // Chill mode: wrap around edges instead of hitting walls.
        val nextHead =
            GridCell(
                (rawNextHead.x + cols) % cols,
                (rawNextHead.y + rows) % rows,
            )

        val willGrow = nextHead == state.foodCell
        val bodyCells =
            if (willGrow) {
                state.snake.map { it.cell }.toSet()
            } else {
                state.snake.dropLast(1).map { it.cell }.toSet()
            }
        if (nextHead in bodyCells) {
            return state.copy(isGameOver = true, pendingDirection = null)
        }

        val ateFood = willGrow
        val eatenThumbnail = if (ateFood) songs[state.songIndex].resolvedThumbnailUrl() else null
        val prevHead = state.snake.firstOrNull()
        val prevHeadThumb = prevHead?.thumbnailUrl
        val newHead =
            SnakeSegment(
                nextHead,
                thumbnailUrl = if (ateFood) eatenThumbnail else prevHeadThumb,
            )
        val grownSnake =
            if (ateFood) {
                val prev = state.snake
                val oldHeadCell = prevHead!!.cell
                val neckSegment = SnakeSegment(oldHeadCell, prevHeadThumb)
                // Grow: insert neck at old head; body segments keep their cells (no shift).
                listOf(newHead, neckSegment) + prev.drop(1)
            } else {
                val prev = state.snake
                if (prev.size <= 1) {
                    listOf(newHead)
                } else {
                    listOf(newHead) +
                        (1 until prev.size).map { i ->
                            prev[i].copy(cell = prev[i - 1].cell)
                        }
                }
            }

        if (!ateFood) {
            return state.copy(
                snake = grownSnake,
                direction = direction,
                pendingDirection = null,
                lastEatenSongIndex = null,
                tickGeneration = state.tickGeneration + 1,
            )
        }

        val nextSongIndex = (state.songIndex + 1) % songs.size
        val occupied = grownSnake.map { it.cell }.toSet()
        val nextFood = randomEmptyCell(occupied)

        return state.copy(
            snake = grownSnake,
            direction = direction,
            pendingDirection = null,
            foodCell = nextFood,
            songIndex = nextSongIndex,
            lastEatenSongIndex = state.songIndex,
            tickGeneration = state.tickGeneration + 1,
            score = state.score + 1,
        )
    }

    fun tickIntervalMs(score: Int): Long = tickIntervalMsForScore(score)

    private fun isValidTurn(
        current: SnakeDirection,
        next: SnakeDirection,
    ): Boolean =
        when (current) {
            SnakeDirection.UP -> next != SnakeDirection.DOWN
            SnakeDirection.DOWN -> next != SnakeDirection.UP
            SnakeDirection.LEFT -> next != SnakeDirection.RIGHT
            SnakeDirection.RIGHT -> next != SnakeDirection.LEFT
        }

    private fun randomEmptyCell(occupied: Set<GridCell>): GridCell {
        if (occupied.size >= cols * rows) {
            return GridCell(0, 0)
        }
        var cell: GridCell
        do {
            cell = GridCell(Random.nextInt(cols), Random.nextInt(rows))
        } while (cell in occupied)
        return cell
    }

    companion object {
        fun tickIntervalMsForScore(score: Int): Long {
            val base = 300L
            val min = 250L
            val reduction = (score * 1L).coerceAtMost(base - min)
            return base - reduction
        }
    }
}
