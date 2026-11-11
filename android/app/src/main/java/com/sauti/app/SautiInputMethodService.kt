package com.sauti.app

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.*
import com.sauti.app.audio.AudioRecorderManager
import com.sauti.app.audio.AudioRoutingManager
import com.sauti.app.audio.AudioVisualizerProcessor
import com.sauti.app.audio.DynamicVadController
import com.sauti.app.memory.MemoryPressureManager
import com.sauti.app.state.KeyboardIntent
import com.sauti.app.state.KeyboardStateMachine
import com.sauti.app.ui.keyboard.ComposeKeyboardLayout
import com.sauti.app.ui.theme.SautiTheme
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sauti Sovereign Offline AI Keyboard Service.
 * Implements Android InputMethodService with MVI architecture,
 * zero-allocation O(1) SPSC ring buffer, dynamic audio routing, and VAD noise threshold adaptation.
 */
class SautiInputMethodService : InputMethodService() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null

    // Hardware routing & dynamic VAD controllers
    private lateinit var audioRoutingManager: AudioRoutingManager
    private val vadController = DynamicVadController()

    // Deterministic MVI State Machine
    private val stateMachine = KeyboardStateMachine()

    // Low-memory defense observer
    private val memoryPressureManager = MemoryPressureManager { isCritical ->
        stateMachine.dispatch(KeyboardIntent.MemoryPressureLevelChanged(isCritical))
        try {
            nativeNotifyMemoryPressure(isCritical)
        } catch (e: UnsatisfiedLinkError) {
            // Ignore in unit tests
        }
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        init {
            try {
                System.loadLibrary("sauti_native_engine")
            } catch (e: UnsatisfiedLinkError) {
                // Graceful fallback for mock unit testing environment
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerComponentCallbacks(memoryPressureManager)
        audioRoutingManager = AudioRoutingManager(this)
        vadController.onAudioRouteChanged(audioRoutingManager.currentRoute.value)
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                SautiTheme {
                    val state by stateMachine.state.collectAsState()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()

                    ComposeKeyboardLayout(
                        liveTranscriptionPreview = state.streamingPreview,
                        isListening = state.isListening,
                        amplitude = state.rmsAmplitude,
                        currentLanguage = state.activeLanguage.name.take(2),
                        clipboardPreviewText = if (!clipText.isNullOrBlank()) clipText else null,
                        onPasteClipboard = { pasteText ->
                            currentInputConnection?.beginBatchEdit()
                            currentInputConnection?.commitText(pasteText, 1)
                            currentInputConnection?.endBatchEdit()
                        },
                        onVoiceToggle = {
                            if (!state.isListening) {
                                stateMachine.dispatch(KeyboardIntent.ToggleVoiceCapture)
                                startAudioCapture()
                            } else {
                                stateMachine.dispatch(KeyboardIntent.ToggleVoiceCapture)
                                stopAudioCapture()
                                processAudioAndCommit()
                            }
                        },
                        onCommitText = { text ->
                            currentInputConnection?.commitText(text, 1)
                        },
                        onBackspace = {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                        },
                        onEnter = {
                            sendDefaultEditorAction(true)
                        }
                    )
                }
            }
        }
    }

    private fun startAudioCapture() {
        vadController.onAudioRouteChanged(audioRoutingManager.currentRoute.value)
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
                        try {
                            nativePushAudioSamples(buffer, read)
                        } catch (e: UnsatisfiedLinkError) {
                            // Mock fallback
                        }
                        val amp = AudioVisualizerProcessor.computeNormalizedRms(buffer, read)
                        vadController.processFrame(amp)
                        stateMachine.dispatch(KeyboardIntent.AudioAmplitudeUpdated(amp))
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
        vadController.reset()
    }

    /**
     * Interface for token-by-token streaming callbacks from the C++ NDK voice engine.
     */
    fun interface NativeTokenCallback {
        fun onToken(token: String)
    }

    private fun processAudioAndCommit() {
        coroutineScope.launch {
            val tokenBuilder = StringBuilder()
            val recognizedText = try {
                nativeConsumeAudioStreaming { token ->
                    tokenBuilder.append(token)
                    stateMachine.dispatch(KeyboardIntent.PartialTranscriptionReceived(tokenBuilder.toString()))
                }
                val result = tokenBuilder.toString().trim()
                if (result.isNotEmpty()) result else nativeTranscribeStreaming()
            } catch (e: UnsatisfiedLinkError) {
                ""
            }
            withContext(Dispatchers.Main) {
                if (recognizedText.isNotBlank()) {
                    stateMachine.dispatch(KeyboardIntent.FinalTextCommitted(recognizedText))
                    currentInputConnection?.beginBatchEdit()
                    currentInputConnection?.commitText(recognizedText, 1)
                    currentInputConnection?.endBatchEdit()
                } else {
                    stateMachine.dispatch(KeyboardIntent.FinalTextCommitted(""))
                }
            }
        }
    }

    // Native JNI stubs
    private external fun nativePushAudioSamples(samples: ShortArray, length: Int): Int
    private external fun nativeTranscribeStreaming(): String
    private external fun nativeConsumeAudioStreaming(tokenCallback: NativeTokenCallback)
    private external fun nativeNotifyMemoryPressure(isCritical: Boolean)
    private external fun nativeInitSautiVoice(sautiVoicePath: String): Boolean
    private external fun nativeInitSautiCore(sautiCorePath: String): Boolean

    /**
     * Initializes Sauti Voice Recognition engine
     */
    fun initSautiVoice(sautiVoicePath: String): Boolean {
        return try {
            nativeInitSautiVoice(sautiVoicePath)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * Initializes Sauti Core engine
     */
    fun initSautiCore(sautiCorePath: String): Boolean {
        return try {
            nativeInitSautiCore(sautiCorePath)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterComponentCallbacks(memoryPressureManager)
        audioRoutingManager.teardown()
        coroutineScope.cancel()
        stopAudioCapture()
    }
}
