package com.sauti.app.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Deterministic Finite State Machine (FSM) enforcing CAP Consistency (C).
 * State transitions are atomic, synchronous, and thread-safe.
 */
class KeyboardStateMachine(
    initialState: KeyboardState = KeyboardState()
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    fun dispatch(intent: KeyboardIntent) {
        _state.update { current ->
            reduce(current, intent)
        }
    }

    private fun reduce(current: KeyboardState, intent: KeyboardIntent): KeyboardState {
        return when (intent) {
            is KeyboardIntent.ToggleVoiceCapture -> {
                if (!current.isListening) {
                    current.copy(
                        isListening = true,
                        isTranscribing = false,
                        streamingPreview = "Listening...",
                        lastError = null
                    )
                } else {
                    current.copy(
                        isListening = false,
                        isTranscribing = true,
                        streamingPreview = "Transcribing..."
                    )
                }
            }
            is KeyboardIntent.AudioAmplitudeUpdated -> {
                if (current.isListening) {
                    current.copy(rmsAmplitude = intent.normalizedRms.coerceIn(0.0f, 1.0f))
                } else {
                    current
                }
            }
            is KeyboardIntent.PartialTranscriptionReceived -> {
                current.copy(
                    streamingPreview = intent.text,
                    isTranscribing = false
                )
            }
            is KeyboardIntent.FinalTextCommitted -> {
                current.copy(
                    isListening = false,
                    isTranscribing = false,
                    streamingPreview = "",
                    rmsAmplitude = 0.0f
                )
            }
            is KeyboardIntent.ToggleLanguage -> {
                val nextLang = when (current.activeLanguage) {
                    LanguageMode.SWAHILI -> LanguageMode.CODE_SWITCH
                    LanguageMode.CODE_SWITCH -> LanguageMode.ENGLISH
                    LanguageMode.ENGLISH -> LanguageMode.SWAHILI
                }
                current.copy(activeLanguage = nextLang)
            }
            is KeyboardIntent.MemoryPressureLevelChanged -> {
                current.copy(memoryPressureWarning = intent.isCritical)
            }
            is KeyboardIntent.ClearError -> {
                current.copy(lastError = null)
            }
        }
    }
}
