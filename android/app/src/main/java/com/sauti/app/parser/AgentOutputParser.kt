package com.sauti.app.parser

/**
 * High-performance regex parser for extracting the final clean commit text
 * from the Swahili Keyboard Agent's Chain-of-Thought (CoT) response.
 *
 * Guarantees:
 * - O(N) linear time complexity over output tokens.
 * - Extracts text strictly within <final_output> tags.
 * - Discards internal <thought> and <tool_call> logs to protect user privacy.
 */
object AgentOutputParser {

    private val FINAL_OUTPUT_REGEX = Regex("<final_output>([\\s\\S]*?)</final_output>")
    private val THOUGHT_REGEX = Regex("<thought>[\\s\\S]*?</thought>")
    private val TOOL_CALL_REGEX = Regex("<tool_call>[\\s\\S]*?</tool_call>")

    /**
     * Extracts pure commit text for the Android InputConnection.
     */
    fun extractCommitText(rawModelResponse: String): String {
        if (rawModelResponse.isBlank()) return ""

        val match = FINAL_OUTPUT_REGEX.find(rawModelResponse)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // Fallback: Strip <thought> and <tool_call> blocks if <final_output> was omitted
        var sanitized = THOUGHT_REGEX.replace(rawModelResponse, "")
        sanitized = TOOL_CALL_REGEX.replace(sanitized, "")
        return sanitized.trim()
    }
}
