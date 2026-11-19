package com.sauti.app.sauti

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {

    @Test
    fun testDetectEnglishSentences() {
        val query1 = "when you get same issue please tell me what to troubleshoot on hp"
        val query2 = "hello how can you help me today"
        val query3 = "write a python function to calculate sum"
        val query4 = "what is the capital city of France"

        assertEquals("en", SautiEngineLoader.detectLanguage(query1))
        assertEquals("en", SautiEngineLoader.detectLanguage(query2))
        assertEquals("en", SautiEngineLoader.detectLanguage(query3))
        assertEquals("en", SautiEngineLoader.detectLanguage(query4))
    }

    @Test
    fun testDetectSwahiliSentences() {
        val query1 = "Habari za asubuhi"
        val query2 = "naomba barua ya kuomba kazi"
        val query3 = "Nifundishe misemo 5 ya Kiswahili inayotumika sana"
        val query4 = "Tafadhali nieleze jinsi ya kupanga muda wangu"

        assertEquals("sw", SautiEngineLoader.detectLanguage(query1))
        assertEquals("sw", SautiEngineLoader.detectLanguage(query2))
        assertEquals("sw", SautiEngineLoader.detectLanguage(query3))
        assertEquals("sw", SautiEngineLoader.detectLanguage(query4))
    }
}
