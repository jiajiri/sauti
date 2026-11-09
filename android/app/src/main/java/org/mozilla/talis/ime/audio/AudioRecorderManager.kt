package com.sauti.app.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorderManager(private val context: Context) {

    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening(
        scope: CoroutineScope,
        onAmplitudeChanged: (Float) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (isRecording.get()) return

        if (!hasRecordPermission()) {
            onError("Microphone permission required")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else 4096

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Audio recording hardware unavailable")
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(1024)
                while (isRecording.get() && isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val amplitude = AudioVisualizerProcessor.computeNormalizedRms(buffer, read)
                        withContext(Dispatchers.Main) {
                            onAmplitudeChanged(amplitude)
                        }
                    }
                    delay(30)
                }
            }
        } catch (e: SecurityException) {
            onError("Permission denied: ${e.message}")
        } catch (e: Exception) {
            onError("Audio input error: ${e.message}")
        }
    }

    fun stopListening() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignored on teardown
        } finally {
            audioRecord = null
        }
    }
}
