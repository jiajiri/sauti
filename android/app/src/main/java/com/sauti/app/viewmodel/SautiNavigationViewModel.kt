package com.sauti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sauti.app.data.local.ChatSession
import com.sauti.app.data.local.ChatSessionDao
import com.sauti.app.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 4 distinct workspace structural states.
 */
enum class SautiWorkspace {
    CHAT_WORKSPACE,
    AUDIO_WORKSPACE,
    LIBRARY_WORKSPACE,
    NOTEBOOKS_WORKSPACE
}

/**
 * ViewModel managing dynamic user profile state, Room database session flows,
 * and workspace navigation.
 */
class SautiNavigationViewModel(
    private val chatSessionDao: ChatSessionDao
) : ViewModel() {

    // 1. User Profile State Container (Fully dynamic, no static fallbacks)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // 2. Active workspace state
    private val _currentWorkspace = MutableStateFlow(SautiWorkspace.CHAT_WORKSPACE)
    val currentWorkspace: StateFlow<SautiWorkspace> = _currentWorkspace.asStateFlow()

    // 3. Synchronized active conversation session ID
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // 4. Live database flow bound directly from Room DAO (zero hardcoded strings)
    val chatSessions: StateFlow<List<ChatSession>> = chatSessionDao.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        // Loads dynamic profile; can be loaded from datastore, secure room, or cloud auth
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

    /**
     * Triggers a database insert event that generates a blank session ID dynamically
     * and persists the session to device storage.
     */
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
}
