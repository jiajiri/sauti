package com.sauti.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ChatSession database entity persisted on device storage.
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey
    val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Backwards compatibility alias if needed
typealias ChatSessionEntity = ChatSession
