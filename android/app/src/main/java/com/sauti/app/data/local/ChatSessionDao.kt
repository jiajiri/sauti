package com.sauti.app.data.local

import kotlinx.coroutines.flow.Flow

/**
 * ChatSession Data Access Interface.
 */
interface ChatSessionDao {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun insert(session: ChatSession)
    suspend fun delete(session: ChatSession)
    suspend fun deleteById(sessionId: String)
    suspend fun clearAll()
}
