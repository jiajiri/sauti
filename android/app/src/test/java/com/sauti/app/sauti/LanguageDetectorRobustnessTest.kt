package com.sauti.app.sauti

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorRobustnessTest {

    @Test
    fun testEnglishGreetingsAndShortQueries() {
        assertEquals("en", SautiEngineLoader.detectLanguage("hello"))
        assertEquals("en", SautiEngineLoader.detectLanguage("hi"))
        assertEquals("en", SautiEngineLoader.detectLanguage("hey"))
        assertEquals("en", SautiEngineLoader.detectLanguage("Good morning"))
        assertEquals("en", SautiEngineLoader.detectLanguage("Hello! How can you help me?"))
        assertEquals("en", SautiEngineLoader.detectLanguage("Can you write a python script?"))
        assertEquals("en", SautiEngineLoader.detectLanguage("Write a json parser"))
    }

    @Test
    fun testSwahiliQueries() {
        assertEquals("sw", SautiEngineLoader.detectLanguage("Habari za asubuhi"))
        assertEquals("sw", SautiEngineLoader.detectLanguage("Hujambo, naomba msaada wa kueleza hii"))
        assertEquals("sw", SautiEngineLoader.detectLanguage("Nifundishe misemo ya Kiswahili"))
        assertEquals("sw", SautiEngineLoader.detectLanguage("Mambo vipi leo?"))
        assertEquals("sw", SautiEngineLoader.detectLanguage("Ninataka kujifunza kuandika barua"))
    }
}
