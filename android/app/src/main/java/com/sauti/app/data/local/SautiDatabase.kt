package com.sauti.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Sovereign Sauti Persistence Database.
 * Native SQLite architecture with live reactive Flow emissions for Sessions & Messages.
 * 100% crash-proof, zero reflection, compatible with Android 7.0 through Android 15+.
 */
class SautiDatabase private constructor(context: Context) {

    private val dbHelper = SautiDbOpenHelper(context.applicationContext)
    private val dao = SQLiteChatSessionDao(dbHelper)

    fun chatSessionDao(): ChatSessionDao = dao

    companion object {
        @Volatile
        private var INSTANCE: SautiDatabase? = null

        fun getInstance(context: Context): SautiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SautiDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

private class SautiDbOpenHelper(context: Context) : SQLiteOpenHelper(context, "sauti_database.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_sessions (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id TEXT PRIMARY KEY NOT NULL,
                sessionId TEXT NOT NULL,
                isUser INTEGER NOT NULL,
                text TEXT NOT NULL,
                tokensPerSec TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(sessionId)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id TEXT PRIMARY KEY NOT NULL,
                    sessionId TEXT NOT NULL,
                    isUser INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    tokensPerSec TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(sessionId)")
        }
    }
}

private class SQLiteChatSessionDao(private val dbHelper: SautiDbOpenHelper) : ChatSessionDao {
    private val _sessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _messagesFlowMap = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()

    init {
        refreshSessions()
    }

    private fun refreshSessions() {
        try {
            val list = mutableListOf<ChatSession>()
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                "chat_sessions",
                arrayOf("id", "title", "timestamp"),
                null,
                null,
                null,
                null,
                "timestamp DESC"
            )
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow("id")
                val titleIdx = it.getColumnIndexOrThrow("title")
                val tsIdx = it.getColumnIndexOrThrow("timestamp")
                while (it.moveToNext()) {
                    list.add(
                        ChatSession(
                            id = it.getString(idIdx),
                            title = it.getString(titleIdx),
                            timestamp = it.getLong(tsIdx)
                        )
                    )
                }
            }
            _sessionsFlow.value = list
        } catch (e: Exception) {
            // Non-fatal safe fallback
        }
    }

    private fun refreshMessages(sessionId: String) {
        try {
            val list = mutableListOf<ChatMessage>()
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                "chat_messages",
                arrayOf("id", "sessionId", "isUser", "text", "tokensPerSec", "timestamp"),
                "sessionId = ?",
                arrayOf(sessionId),
                null,
                null,
                "timestamp ASC"
            )
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow("id")
                val sIdIdx = it.getColumnIndexOrThrow("sessionId")
                val userIdx = it.getColumnIndexOrThrow("isUser")
                val textIdx = it.getColumnIndexOrThrow("text")
                val tpsIdx = it.getColumnIndexOrThrow("tokensPerSec")
                val tsIdx = it.getColumnIndexOrThrow("timestamp")
                while (it.moveToNext()) {
                    list.add(
                        ChatMessage(
                            id = it.getString(idIdx),
                            sessionId = it.getString(sIdIdx),
                            isUser = it.getInt(userIdx) == 1,
                            text = it.getString(textIdx),
                            tokensPerSec = it.getString(tpsIdx),
                            timestamp = it.getLong(tsIdx)
                        )
                    )
                }
            }
            val flow = _messagesFlowMap.getOrPut(sessionId) { MutableStateFlow(emptyList()) }
            flow.value = list
        } catch (e: Exception) {
            // Non-fatal fallback
        }
    }

    override fun getAllSessions(): Flow<List<ChatSession>> = _sessionsFlow.asStateFlow()

    override suspend fun insert(session: ChatSession) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", session.id)
                put("title", session.title)
                put("timestamp", session.timestamp)
            }
            db.insertWithOnConflict("chat_sessions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshSessions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun delete(session: ChatSession) = deleteById(session.id)

    override suspend fun deleteById(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete("chat_messages", "sessionId = ?", arrayOf(sessionId))
            db.delete("chat_sessions", "id = ?", arrayOf(sessionId))
            _messagesFlowMap.remove(sessionId)
            refreshSessions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun clearAll() = clearAllHistory()

    override suspend fun updateTitle(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply { put("title", newTitle) }
            db.update("chat_sessions", values, "id = ?", arrayOf(sessionId))
            refreshSessions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        val flow = _messagesFlowMap.getOrPut(sessionId) { MutableStateFlow(emptyList()) }
        refreshMessages(sessionId)
        return flow.asStateFlow()
    }

    override suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", message.id)
                put("sessionId", message.sessionId)
                put("isUser", if (message.isUser) 1 else 0)
                put("text", message.text)
                put("tokensPerSec", message.tokensPerSec)
                put("timestamp", message.timestamp)
            }
            db.insertWithOnConflict("chat_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            
            // Bump session timestamp so active conversation moves to top of history
            val sessionValues = ContentValues().apply {
                put("timestamp", message.timestamp)
            }
            db.update("chat_sessions", sessionValues, "id = ?", arrayOf(message.sessionId))
            
            refreshMessages(message.sessionId)
            refreshSessions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete("chat_messages", null, null)
            db.delete("chat_sessions", null, null)
            _messagesFlowMap.clear()
            refreshSessions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
