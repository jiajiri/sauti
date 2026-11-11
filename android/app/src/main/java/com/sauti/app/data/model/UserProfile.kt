package com.sauti.app.data.model

/**
 * Clean UserProfile data model tracking user identity, tier, and avatar helper.
 */
data class UserProfile(
    val fullName: String,
    val isProTier: Boolean,
    val email: String? = null
) {
    /**
     * Helper method that automatically splits the fullName string to extract
     * the first letter dynamically to populate the circular avatar circle profile image.
     */
    fun getAvatarInitial(): String {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) return "S"
        // Splits by whitespace to inspect name tokens and extract the first character
        val tokens = trimmed.split("\\s+".toRegex())
        val firstToken = tokens.firstOrNull { it.isNotEmpty() } ?: return "S"
        return firstToken.first().uppercaseChar().toString()
    }
}
