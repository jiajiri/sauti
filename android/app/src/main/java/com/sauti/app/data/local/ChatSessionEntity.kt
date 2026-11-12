package com.sauti.app.data.local

/**
 * ChatSession database entity persisted on device storage.
 */
data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Backwards compatibility alias if needed
typealias ChatSessionEntity = ChatSession
