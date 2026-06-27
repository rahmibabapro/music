/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CrossfadeAudio {
    /** Equal-power crossfade: constant perceived loudness during the transition. */
    fun equalPowerGains(progress: Float): Pair<Float, Float> {
        val clamped = progress.coerceIn(0f, 1f)
        val fadeIn = sin(clamped * (PI / 2.0)).toFloat()
        val fadeOut = cos(clamped * (PI / 2.0)).toFloat()
        return fadeIn to fadeOut
    }

    /**
     * Heuristic outro trigger: start crossfade in the last [crossfadeMs] or final 15% of track,
     * whichever leaves more room for a natural handoff.
     */
    fun computeTriggerPosition(
        durationMs: Long,
        crossfadeMs: Long,
    ): Long {
        if (durationMs <= crossfadeMs) return 0L
        val percentBased = (durationMs * 0.85f).toLong()
        val durationBased = durationMs - crossfadeMs
        return maxOf(percentBased, durationBased)
    }
}
