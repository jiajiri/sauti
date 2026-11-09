package com.sauti.app

import android.inputmethodservice.InputMethodService
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.*
import com.sauti.app.audio.AudioVisualizerProcessor
import com.sauti.app.ui.keyboard.ComposeKeyboardLayout
import com.sauti.app.ui.theme.SautiThemeLegacy
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sauti Sovereign Offline AI Keyboard Service.
 * Implements Android InputMethodService with modern Jetpack Compose overlay.
 *
 * Guarantees:
 * - Zero network activity (no android.permission.INTERNET).
 * - SPSC O(1) lock-free circular queue audio ingest via JNI.
 * - Reactive state flow without blocking the Android UI thread.
 */
class SautiInputMethodService : InputMethodService() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        init {
            try {
                System.loadLibrary("sauti_native_engine")
            } catch (e: UnsatisfiedLinkError) {
                // Graceful fallback during tests
            }
        }
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                SautiThemeLegacy {
                    var isListeningState by remember { mutableStateOf(false) }
                    var amplitudeState by remember { mutableFloatStateOf(0.0f) }
                    var liveTextPreview by remember { mutableStateOf("") }
                    var currentLanguage by remember { mutableStateOf("SW") }

                    ComposeKeyboardLayout(
                        liveTranscriptionPreview = liveTextPreview,
                        isListening = isListeningState,
                        amplitude = amplitudeState,
                        currentLanguage = currentLanguage,
                        onVoiceToggle = {
                            if (!isListeningState) {
                                isListeningState = true
                                liveTextPreview = "Listening..."
                                startAudioCapture { normAmp ->
                                    amplitudeState = normAmp
                                }
                            } else {
                                isListeningState = false
                                stopAudioCapture()
                                liveTextPreview = "Transcribing..."
                                processAudioAndCommit { recognized ->
                                    liveTextPreview = recognized
                                    currentInputConnection?.commitText(recognized, 1)
                                }
                            }
                        },
                        onCommitText = { text ->
                            currentInputConnection?.commitText(text, 1)
                        },
                        onBackspace = {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                        },
                        onLanguageToggle = {
                            currentLanguage = if (currentLanguage == "SW") "EN" else "SW"
                        }
                    )
                }
            }
        }
    }

    private fun startAudioCapture(onAmplitudeUpdate: (Float) -> Unit) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
            )
            audioRecord?.startRecording()
            isRecording.set(true)

            coroutineScope.launch {
                val buffer = ShortArray(1024)
                while (isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        nativePushAudioSamples(buffer, read)
                        val amp = AudioVisualizerProcessor.computeNormalizedRms(buffer, read)
                        withContext(Dispatchers.Main) {
                            onAmplitudeUpdate(amp)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            isRecording.set(false)
        }
    }

    private fun stopAudioCapture() {
        isRecording.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun processAudioAndCommit(onResult: (String) -> Unit) {
        coroutineScope.launch {
            val recognizedText = nativeTranscribeStreaming()
            withContext(Dispatchers.Main) {
                onResult(recognizedText)
            }
        }
    }

    // Native JNI stubs
    private external fun nativePushAudioSamples(samples: ShortArray, length: Int): Int
    private external fun nativeTranscribeStreaming(): String

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        stopAudioCapture()
    }
}
