package com.sauti.app.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * Dynamic WebRTC Voice Activity Detection (VAD) threshold controller.
 * Tracks shifting acoustic noise floors and hardware route profiles,
 * dynamically calibrating speech trigger thresholds to prevent false activations.
 */
class DynamicVadController {

    // Noise floor moving average estimator (RMS units)
    private var estimatedNoiseFloor: Float = 0.05f
    private val alphaSmoothing: Float = 0.08f // EMA smoothing factor

    // Dynamic threshold state
    private val _vadThreshold = MutableStateFlow(0.18f)
    val vadThreshold: StateFlow<Float> = _vadThreshold.asStateFlow()

    private val _noiseFloor = MutableStateFlow(0.05f)
    val noiseFloor: StateFlow<Float> = _noiseFloor.asStateFlow()

    private val _isSpeechDetected = MutableStateFlow(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    enum class VadOperatingMode(val baseSensitivity: Float) {
        QUIET_INDOOR(0.12f),
        NORMAL_CLINIC(0.18f),
        NOISY_OUTDOOR(0.28f),
        HIGH_GAIN_USB(0.15f)
    }

    private var currentMode: VadOperatingMode = VadOperatingMode.NORMAL_CLINIC

    /**
     * Updates route-based default sensitivity when hardware route changes.
     */
    fun onAudioRouteChanged(route: AudioRoutingManager.AudioRouteType) {
        currentMode = when (route) {
            is AudioRoutingManager.AudioRouteType.UsbCHeadset -> VadOperatingMode.HIGH_GAIN_USB
            is AudioRoutingManager.AudioRouteType.BluetoothSco -> VadOperatingMode.NORMAL_CLINIC
            is AudioRoutingManager.AudioRouteType.WiredHeadset -> VadOperatingMode.NORMAL_CLINIC
            else -> VadOperatingMode.NORMAL_CLINIC
        }
        recalculateThreshold()
    }

    /**
     * Ingests incoming live audio sample RMS and dynamically updates noise floor & speech detection.
     */
    fun processFrame(rms: Float): Boolean {
        // Update noise floor using exponential moving average if RMS is below current threshold
        if (rms < _vadThreshold.value) {
            estimatedNoiseFloor = (alphaSmoothing * rms) + ((1.0f - alphaSmoothing) * estimatedNoiseFloor)
            _noiseFloor.value = estimatedNoiseFloor
            recalculateThreshold()
        }

        // Detect speech if RMS exceeds dynamically calibrated noise gate
        val speech = rms > _vadThreshold.value
        _isSpeechDetected.value = speech
        return speech
    }

    private fun recalculateThreshold() {
        // Dynamic threshold is noise floor + operating mode margin with clamp bounds [0.10f, 0.65f]
        val dynamicMargin = currentMode.baseSensitivity
        val targetThreshold = min(0.65f, max(0.10f, estimatedNoiseFloor + dynamicMargin))
        _vadThreshold.value = targetThreshold
    }

    /**
     * Resets calibration on session restart.
     */
    fun reset() {
        estimatedNoiseFloor = 0.05f
        _noiseFloor.value = 0.05f
        recalculateThreshold()
        _isSpeechDetected.value = false
    }
}
