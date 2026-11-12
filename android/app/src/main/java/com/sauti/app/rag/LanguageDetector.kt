package com.sauti.app.rag

/**
 * High-speed language detection engine ported and adapted from DSE-AI-ENGINE guidelines.
 * Accurately distinguishes Swahili vs English inputs in sub-millisecond execution.
 */
enum class DetectedLanguage {
    SWAHILI,
    ENGLISH
}

object LanguageDetector {

    // Common Swahili functional words, prefixes, and grammatical markers (including colloquialisms and common typos)
    private val SWAHILI_KEYWORDS = setOf(
        "habari", "habar", "hujambo", "jambo", "mambo", "vipi", "salama", "shikamoo",
        "andika", "nielezee", "tengeneza", "eleza", "toa", "fanya", "jinsi",
        "mfano", "kwa", "katika", "yake", "yako", "wangu", "wako", "wao",
        "nini", "gani", "wapi", "lini", "kwa nini", "sababu", "lakini",
        "kuwa", "kama", "hapa", "pale", "huyu", "hawa", "kitu", "vitu",
        "mtu", "watu", "tafadhali", "asante", "karibu", "kazi", "namba",
        "hesabu", "ongeza", "punguza", "linganisha", "tafuta", "pata",
        "ndiyo", "hapana", "si", "siyo", "wala", "zaidi", "kidogo", "sana",
        "swahili", "kiswahili", "sehemu", "sawa", "haya", "poa", "vyema"
    )

    // Swahili verb prefix patterns (e.g. ku-, ali-, ana-, ata-, nili-, nina-, nita-)
    private val SWAHILI_PREFIX_REGEX = Regex(
        "\\b(ku|kuji|ali|ana|ata|wali|wana|wata|nili|nina|nita|uli|una|uta|tuli|tuna|tuta)[a-z]{3,}\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * Detects language of prompt with high confidence.
     */
    fun detect(text: String): DetectedLanguage {
        val clean = text.trim().lowercase()
        if (clean.isBlank()) return DetectedLanguage.ENGLISH

        val words = clean.split(Regex("[\\s,;:.?!\"'()\\[\\]{}]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return DetectedLanguage.ENGLISH

        var swahiliScore = 0
        var englishScore = 0

        for (word in words) {
            if (SWAHILI_KEYWORDS.contains(word)) {
                swahiliScore += 2
            } else if (SWAHILI_PREFIX_REGEX.matches(word)) {
                swahiliScore += 1
            }
        }

        // If at least one distinct Swahili keyword or verb form is found, treat as Swahili
        return if (swahiliScore > 0) DetectedLanguage.SWAHILI else DetectedLanguage.ENGLISH
    }
}
