package com.sauti.app.rag

/**
 * Enterprise O(1) Bilingual Language Classifier & Code-Switching Pipeline.
 *
 * Capabilities:
 * - O(1) average time complexity per token using compiled HashSets.
 * - Accurately categorizes queries into Pure Swahili, Pure English, or Mixed (Code-Switching).
 * - Implements edge-case tie-breaking:
 *     1. If Swahili density > English density -> SWAHILI
 *     2. If English density > Swahili density -> ENGLISH
 *     3. If weights are equal (tie / code-switching): checks the leading token (first query clause).
 *        If first token is Swahili -> SWAHILI response.
 *        If first token is English -> ENGLISH response.
 */
enum class DetectedLanguage {
    SWAHILI,
    ENGLISH
}

data class LanguageDecision(
    val language: DetectedLanguage,
    val isCodeSwitching: Boolean,
    val swahiliScore: Int,
    val englishScore: Int,
    val leadingLanguage: DetectedLanguage
)

object LanguageDetector {

    // O(1) HashSet for Swahili functional roots, question markers, pronouns, and verbs
    private val SWAHILI_ROOTS = hashSetOf(
        "habari", "habar", "hujambo", "jambo", "mambo", "vipi", "salama", "shikamoo",
        "andika", "nielezee", "tengeneza", "eleza", "toa", "fanya", "jinsi",
        "mfano", "kwa", "katika", "yake", "yako", "wangu", "wako", "wao",
        "nini", "gani", "wapi", "lini", "kwanini", "sababu", "lakini",
        "kuwa", "kama", "hapa", "pale", "huyu", "hawa", "kitu", "vitu",
        "mtu", "watu", "tafadhali", "asante", "karibu", "kazi", "namba",
        "hesabu", "ongeza", "punguza", "linganisha", "tafuta", "pata",
        "ndiyo", "hapana", "si", "siyo", "wala", "zaidi", "kidogo", "sana",
        "swahili", "kiswahili", "sehemu", "sawa", "haya", "poa", "vyema",
        "mimi", "wewe", "yeye", "sisi", "nyinyi", "leo", "kesho", "jana",
        "vizuri", "bwana", "dada", "kaka", "mama", "baba", "rafiki", "shida",
        "tafsiri", "sarufi", "sentensi", "lugha", "asubuhi", "mchana", "jioni",
        "usiku", "kueleza", "nisaidie", "ushauri", "muda", "siku", "shairi",
        "hadithi", "pwani", "amani", "upendo", "biashara", "mradi", "barua", "kazi"
    )

    // O(1) HashSet for English functional words, question markers, and common verbs
    private val ENGLISH_ROOTS = hashSetOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her",
        "she", "or", "an", "will", "my", "one", "all", "would", "there",
        "their", "what", "so", "up", "out", "if", "about", "who", "get",
        "which", "go", "me", "when", "make", "can", "like", "time", "no",
        "just", "him", "know", "take", "people", "into", "year", "your",
        "good", "some", "could", "them", "see", "other", "than", "then",
        "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first",
        "well", "way", "even", "new", "want", "because", "any", "these",
        "give", "day", "most", "us", "hello", "hi", "hey", "please", "help",
        "explain", "write", "create", "fix", "code", "why", "where", "how"
    )

    // Swahili agglutinative verb prefix regex (ku-, ali-, ana-, ata-, nili-, nina-, nita-)
    private val SWAHILI_PREFIX_REGEX = Regex(
        "^(ku|kuji|ali|ana|ata|wali|wana|wata|nili|nina|nita|uli|una|uta|tuli|tuna|tuta)[a-z]{3,}$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Analyzes query text and returns a comprehensive LanguageDecision using O(1) hashing.
     */
    fun analyze(text: String): LanguageDecision {
        val clean = text.trim().lowercase()
        if (clean.isBlank()) {
            return LanguageDecision(
                language = DetectedLanguage.ENGLISH,
                isCodeSwitching = false,
                swahiliScore = 0,
                englishScore = 0,
                leadingLanguage = DetectedLanguage.ENGLISH
            )
        }

        val tokens = clean.split(Regex("[^a-zA-Z0-9_-]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            return LanguageDecision(
                language = DetectedLanguage.ENGLISH,
                isCodeSwitching = false,
                swahiliScore = 0,
                englishScore = 0,
                leadingLanguage = DetectedLanguage.ENGLISH
            )
        }

        var swahiliScore = 0
        var englishScore = 0

        // Determine language of the leading token (t_0)
        val firstToken = tokens.first()
        val leadingLang = when {
            SWAHILI_ROOTS.contains(firstToken) || SWAHILI_PREFIX_REGEX.matches(firstToken) -> DetectedLanguage.SWAHILI
            ENGLISH_ROOTS.contains(firstToken) -> DetectedLanguage.ENGLISH
            else -> DetectedLanguage.SWAHILI // Default domain bias
        }

        for (token in tokens) {
            // O(1) Swahili lookup
            if (SWAHILI_ROOTS.contains(token)) {
                swahiliScore += 2
            } else if (SWAHILI_PREFIX_REGEX.matches(token)) {
                swahiliScore += 1
            }

            // O(1) English lookup
            if (ENGLISH_ROOTS.contains(token)) {
                englishScore += 2
            }
        }

        val isCodeSwitching = (swahiliScore > 0 && englishScore > 0)

        val finalLanguage = when {
            swahiliScore > englishScore -> DetectedLanguage.SWAHILI
            englishScore > swahiliScore -> DetectedLanguage.ENGLISH
            else -> {
                // TIE-BREAKING EDGE CASE:
                // When scores are equal, strictly obey the leading token (first query clause)
                leadingLang
            }
        }

        return LanguageDecision(
            language = finalLanguage,
            isCodeSwitching = isCodeSwitching,
            swahiliScore = swahiliScore,
            englishScore = englishScore,
            leadingLanguage = leadingLang
        )
    }

    /**
     * Backward-compatible simple detection helper.
     */
    fun detect(text: String): DetectedLanguage {
        return analyze(text).language
    }
}
