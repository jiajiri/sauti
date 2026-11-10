package com.sauti.app.state

/**
 * Immutable language typing modes.
 */
enum class LanguageMode {
    SWAHILI,
    ENGLISH,
    CODE_SWITCH // Sheng (Swahili-English hybrid)
}

/**
 * Unidirectional MVI State for Sauti Sovereign Keyboard & Platform.
 * Guarantees deterministic UI rendering with zero race conditions.
 */
data class KeyboardState(
    val isListening: Boolean = false,
    val isTranscribing: Boolean = false,
    val rmsAmplitude: Float = 0.0f,
    val streamingPreview: String = "",
    val activeLanguage: LanguageMode = LanguageMode.SWAHILI,
    val memoryPressureWarning: Boolean = false,
    val lastError: String? = null
)

/**
 * User Intents triggering state transitions.
 */
sealed class KeyboardIntent {
    data object ToggleVoiceCapture : KeyboardIntent()
    data class AudioAmplitudeUpdated(val normalizedRms: Float) : KeyboardIntent()
    data class PartialTranscriptionReceived(val text: String) : KeyboardIntent()
    data class FinalTextCommitted(val text: String) : KeyboardIntent()
    data object ToggleLanguage : KeyboardIntent()
    data class MemoryPressureLevelChanged(val isCritical: Boolean) : KeyboardIntent()
    data object ClearError : KeyboardIntent()
}
