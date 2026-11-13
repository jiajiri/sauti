package com.sauti.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sauti.app.data.local.ChatMessage
import com.sauti.app.data.local.ChatSession
import com.sauti.app.data.local.ChatSessionDao
import com.sauti.app.data.model.UserProfile
import com.sauti.app.settings.SautiPreferencesStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 5 distinct workspace structural states.
 */
enum class SautiWorkspace {
    CHAT_WORKSPACE,
    AUDIO_WORKSPACE,
    LIBRARY_WORKSPACE,
    NOTEBOOKS_WORKSPACE,
    SETTINGS_WORKSPACE
}

/**
 * ViewModel managing dynamic user profile state, Room/SQLite database session flows,
 * reactive message histories, auto-titling, and workspace navigation.
 */
class SautiNavigationViewModel(
    private val chatSessionDao: ChatSessionDao,
    private val prefs: SautiPreferencesStore
) : ViewModel() {

    // 1. Dynamic User Profile (No hardcoded strings, backed by SautiPreferencesStore)
    private val _userProfile = MutableStateFlow(
        UserProfile(
            fullName = prefs.userName,
            isProTier = false,
            customInstructions = prefs.customInstructions
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // 2. Active workspace state
    private val _currentWorkspace = MutableStateFlow(SautiWorkspace.CHAT_WORKSPACE)
    val currentWorkspace: StateFlow<SautiWorkspace> = _currentWorkspace.asStateFlow()

    // 2.1 Dynamic Theme State
    private val _appTheme = MutableStateFlow(prefs.appTheme)
    val appTheme: StateFlow<com.sauti.app.settings.AppTheme> = _appTheme.asStateFlow()

    // 3. Synchronized active conversation session ID
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // 4. Live database flow bound directly from SQLite DAO (Newest first: ORDER BY timestamp DESC)
    val chatSessions: StateFlow<List<ChatSession>> = chatSessionDao.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 5. Active Session Title (auto-derived dynamically from active session or fallback)
    val activeSessionTitle: StateFlow<String> = combine(_activeSessionId, chatSessions) { activeId, sessions ->
        sessions.find { it.id == activeId }?.title ?: "New Chat"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "New Chat"
    )

    // 6. Active Session Messages (seamlessly reloaded when activeSessionId changes)
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessage>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) {
            chatSessionDao.getMessagesForSession(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Ensure at least one session exists on startup without overriding user selection
        viewModelScope.launch {
            val existing = chatSessionDao.getAllSessions().first()
            if (existing.isEmpty()) {
                createNewSession()
            } else if (_activeSessionId.value == null) {
                // Default to newest session on fresh app start
                _activeSessionId.value = existing.first().id
            }
        }
    }

    fun updateUserName(name: String) {
        val clean = name.trim().ifEmpty { "User" }
        prefs.userName = clean
        _userProfile.value = _userProfile.value.copy(fullName = clean)
    }

    fun updateCustomInstructions(instructions: String) {
        val clean = instructions.trim()
        prefs.customInstructions = clean
        _userProfile.value = _userProfile.value.copy(customInstructions = clean)
    }

    fun updateTheme(theme: com.sauti.app.settings.AppTheme) {
        prefs.appTheme = theme
        _appTheme.value = theme
    }

    fun selectWorkspace(workspace: SautiWorkspace) {
        _currentWorkspace.value = workspace
    }

    fun selectSession(sessionId: String) {
        _activeSessionId.value = sessionId
        _currentWorkspace.value = SautiWorkspace.CHAT_WORKSPACE
    }

    fun createNewSession(initialTitle: String = "New Chat") {
        val newSessionId = UUID.randomUUID().toString()
        val newSession = ChatSession(
            id = newSessionId,
            title = initialTitle,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            chatSessionDao.insert(newSession)
            _activeSessionId.value = newSessionId
            _currentWorkspace.value = SautiWorkspace.CHAT_WORKSPACE
        }
    }

    fun updateSessionTitle(sessionId: String, title: String) {
        viewModelScope.launch {
            chatSessionDao.updateTitle(sessionId, title)
        }
    }

    /**
     * Automatic Session Titling:
     * When user sends the first message in a session whose title is "New Chat",
     * trim to ~35-40 characters and update the session title immediately.
     */
    fun updateSessionTitleIfDefault(sessionId: String, prompt: String) {
        val currentTitle = chatSessions.value.find { it.id == sessionId }?.title ?: "New Chat"
        if (currentTitle == "New Chat" || currentTitle.isBlank()) {
            val candidate = prompt.trim().lines().firstOrNull() ?: prompt.trim()
            val cleanTitle = if (candidate.length > 38) candidate.take(38).trimEnd() + "…" else candidate
            if (cleanTitle.isNotBlank()) {
                updateSessionTitle(sessionId, cleanTitle)
            }
        }
    }

    /**
     * Persists a conversational message (user or assistant) to SQLite.
     */
    fun saveMessage(sessionId: String, isUser: Boolean, text: String, tokensPerSec: String = "") {
        viewModelScope.launch {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                isUser = isUser,
                text = text,
                tokensPerSec = tokensPerSec,
                timestamp = System.currentTimeMillis()
            )
            chatSessionDao.insertMessage(message)
        }
    }

    /**
     * Clears all history from SQLite and resets to a brand-new session.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            chatSessionDao.clearAllHistory()
            createNewSession()
        }
    }

    /**
     * Exports all conversation transcripts to a clean, readable text file in cache for sharing.
     */
    suspend fun exportConversationsToText(context: Context): File {
        val sessions = chatSessions.value
        val exportFile = File(context.cacheDir, "sauti_conversations_export.txt")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val content = buildString {
            append("==========================================\n")
            append("SAUTI AI - CONVERSATION TRANSCRIPT EXPORT\n")
            append("Exported: ${sdf.format(Date())}\n")
            append("User: ${userProfile.value.fullName}\n")
            append("==========================================\n\n")

            for (session in sessions) {
                append("--- Topic: ${session.title} (${sdf.format(Date(session.timestamp))}) ---\n")
                val messages = chatSessionDao.getMessagesForSession(session.id).first()
                for (msg in messages) {
                    val speaker = if (msg.isUser) "User" else "Sauti AI"
                    append("[$speaker - ${sdf.format(Date(msg.timestamp))}]:\n")
                    append(msg.text.trim())
                    append("\n\n")
                }
                append("\n")
            }
        }

        exportFile.writeText(content)
        return exportFile
    }
}
