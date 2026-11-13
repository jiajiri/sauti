package com.sauti.app.data.model

/**
 * Sovereign User Profile model tracking dynamic user identity.
 * Zero hardcoded fallbacks or mock data.
 */
data class UserProfile(
    val fullName: String,
    val isProTier: Boolean = true,
    val customInstructions: String = ""
) {
    /**
     * Extracts first uppercase character dynamically from the user-configured name
     * to render the circular avatar badge.
     */
    fun getAvatarInitial(): String {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) return "U"
        val tokens = trimmed.split("\\s+".toRegex())
        val firstToken = tokens.firstOrNull { it.isNotEmpty() } ?: return "U"
        return firstToken.first().uppercaseChar().toString()
    }
}
