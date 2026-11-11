package com.sauti.app.audio

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Real-time zero-allocation Root-Mean-Square (RMS) audio visualizer processor.
 * Calculates normalized decibels (0.0f - 1.0f) for fluid UI animation loops.
 */
object AudioVisualizerProcessor {

    /**
     * Computes normalized amplitude [0.0, 1.0] from 16-bit PCM samples.
     * Time Complexity: O(N) linear scan over audio chunk.
     * Space Complexity: O(1) zero memory allocation.
     */
    fun computeNormalizedRms(samples: ShortArray, length: Int): Float {
        if (length <= 0) return 0.0f

        var sumSquares = 0.0
        for (i in 0 until length) {
            val sample = samples[i].toDouble()
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / length)
        if (rms <= 1.0) return 0.0f

        // Convert to decibels with 0 dB reference at max 16-bit PCM (32767)
        val db = 20.0 * log10(rms / 32767.0)
        // Map [-60dB, 0dB] to normalized [0.0, 1.0]
        val clampedDb = max(-60.0, db)
        return ((clampedDb + 60.0) / 60.0).toFloat().coerceIn(0.0f, 1.0f)
    }
}
