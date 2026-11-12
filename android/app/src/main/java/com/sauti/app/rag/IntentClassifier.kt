package com.sauti.app.rag

/**
 * High-speed intent classification logic ported from DSE-AI-ENGINE.
 * Decides whether a query is casual conversation, ethics violation, or requires domain RAG retrieval.
 */
enum class QueryIntent {
    GREETING_CASUAL,
    SAFETY_REFUSAL,
    MATHEMATICAL,
    CODE_INQUIRY,
    DOMAIN_RAG
}

object IntentClassifier {

    private val GREETING_REGEX = Regex(
        "^(habari|hujambo|jambo|mambo|vipi|salama|hello|hi|hey|greetings|bonjour|salut|hola|buenos dias)[!?.]*$",
        RegexOption.IGNORE_CASE
    )

    private val SAFETY_REGEX = Regex(
        "\\b(kuiba|uizi|mwizi|wezi|steal|stealing|theft|rob|robbing|kill|harm|kuua|kudhuru)\\b",
        RegexOption.IGNORE_CASE
    )

    private val MATH_REGEX = Regex(
        "[0-9]+[\\s]*[+\\-*/^=][\\s]*[0-9]+|\\b(solve|hesabu|calculate|equation|algebra|integral|derivative)\\b",
        RegexOption.IGNORE_CASE
    )

    private val CODE_REGEX = Regex(
        "\\b(code|function|class|fun |def |algorithm|python|kotlin|bash|sql|cpp|java|script|api|bug|compile|gradle)\\b",
        RegexOption.IGNORE_CASE
    )

    fun classify(query: String): QueryIntent {
        val clean = query.trim()
        val lower = clean.lowercase()

        if (SAFETY_REGEX.containsMatchIn(lower)) {
            return QueryIntent.SAFETY_REFUSAL
        }

        if (GREETING_REGEX.matches(lower)) {
            return QueryIntent.GREETING_CASUAL
        }

        if (MATH_REGEX.containsMatchIn(lower)) {
            return QueryIntent.MATHEMATICAL
        }

        if (CODE_REGEX.containsMatchIn(lower)) {
            return QueryIntent.CODE_INQUIRY
        }

        return QueryIntent.DOMAIN_RAG
    }
}
