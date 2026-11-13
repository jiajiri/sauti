package com.sauti.app.data.local

/**
 * Persisted conversational message entity linked to a ChatSession by sessionId.
 */
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val isUser: Boolean,
    val text: String,
    val tokensPerSec: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
