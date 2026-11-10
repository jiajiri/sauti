package com.sauti.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * On-Device Swahili & English Text-To-Speech (TTS) Engine.
 * Provides two-way hands-free conversational voice response without cloud servers.
 */
class TextToSpeechEngine(
    context: Context,
    private val onInitComplete: (success: Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private val isReady = AtomicBoolean(false)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Prioritize Swahili (Tanzania), fallback to Swahili (Kenya) or English
            val swahiliTz = Locale("sw", "TZ")
            val swahiliKe = Locale("sw", "KE")

            val result = tts?.setLanguage(swahiliTz)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallback = tts?.setLanguage(swahiliKe)
                if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }

            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.95f) // Natural conversational pacing

            isReady.set(true)
            onInitComplete(true)
        } else {
            isReady.set(false)
            onInitComplete(false)
        }
    }

    /**
     * Speaks the response text cleanly out loud through the device speaker.
     */
    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}) {
        if (!isReady.get() || text.isBlank()) {
            onDone()
            return
        }

        val utteranceId = "sauti_voice_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStart()
            }

            override fun onDone(utteranceId: String?) {
                onDone()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDone()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Immediately interrupts speech when the user taps the orb or speaks.
     */
    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady.set(false)
    }
}
