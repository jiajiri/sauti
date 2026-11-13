package com.sauti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sauti.app.data.local.ChatMessage
import com.sauti.app.data.local.ChatSession
import com.sauti.app.data.local.ChatSessionDao
import com.sauti.app.data.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val chatSessionDao: ChatSessionDao
) : ViewModel() {

    // 1. User Profile State Container
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // 2. Active workspace state
    private val _currentWorkspace = MutableStateFlow(SautiWorkspace.CHAT_WORKSPACE)
    val currentWorkspace: StateFlow<SautiWorkspace> = _currentWorkspace.asStateFlow()

    // 3. Synchronized active conversation session ID
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // 4. Live database flow bound directly from SQLite DAO (Oldest first: ORDER BY timestamp ASC)
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
        loadUserProfile()
        // Ensure at least one session exists or is created on startup
        viewModelScope.launch {
            chatSessions.collect { list ->
                if (list.isEmpty()) {
                    createNewSession()
                } else if (_activeSessionId.value == null) {
                    _activeSessionId.value = list.last().id
                }
            }
        }
    }

    private fun loadUserProfile() {
        _userProfile.value = UserProfile(
            fullName = "Amina Mwangi",
            isProTier = true
        )
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

    fun updateProfile(newProfile: UserProfile) {
        _userProfile.value = newProfile
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
}
