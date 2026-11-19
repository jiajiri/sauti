package com.sauti.app.sauti

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPipelineStreamingTest {

    @Test
    fun testAudioSamplePushReturnsNonNegative() {
        // Test that 16kHz PCM audio buffer can be trapped and pushed to the engine
        val sampleRate = 16000
        val dummyAudioBuffer = ShortArray(1024) { (it % 100).toShort() }
        val written = SautiEngineLoader.pushAudioSamples(dummyAudioBuffer, dummyAudioBuffer.size)
        assertTrue("Audio samples should be ingested into native ring buffer", written >= 0)
    }

    @Test
    fun testTranscribeStreamingDoesNotThrow() {
        // Ensures streaming transcription safely returns a string even with empty audio buffer
        val transcript = SautiEngineLoader.transcribeStreaming()
        assertTrue(transcript is String)
    }

    @Test
    fun testLanguageDetectorVoiceRouting() {
        // Test that English audio transcription routes to English model generation
        val enPrompt = "write a python function to process json"
        val lang = SautiEngineLoader.detectLanguage(enPrompt)
        assertEquals("en", lang)

        // Test that Swahili audio transcription routes to Swahili model generation
        val swPrompt = "nisaidie kuandika barua ya kuomba kazi"
        val swLang = SautiEngineLoader.detectLanguage(swPrompt)
        assertEquals("sw", swLang)
    }
}
