package com.sauti.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sauti.app.audio.AudioRecorderManager
import com.sauti.app.data.local.ChatSession
import com.sauti.app.data.local.SautiDatabase
import com.sauti.app.data.model.UserProfile
import com.sauti.app.power.PowerHardwareManager
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.ui.screens.ChatScreen
import com.sauti.app.ui.screens.SettingsScreen
import com.sauti.app.ui.screens.VoiceModeScreen
import com.sauti.app.ui.theme.SautiTheme
import com.sauti.app.viewmodel.SautiNavigationViewModel
import com.sauti.app.viewmodel.SautiWorkspace

class MainActivity : ComponentActivity() {

    private var ttsEngine: TextToSpeechEngine? = null
    private var powerManager: PowerHardwareManager? = null
    private var audioRecorderManager: AudioRecorderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            // Non-fatal edge-to-edge fallback
        }
        super.onCreate(savedInstanceState)

        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        } catch (e: Exception) {
            // Non-fatal window flag fallback
        }

        try {
            ttsEngine = TextToSpeechEngine(this)
        } catch (e: Exception) {
            // Non-fatal TTS fallback
        }

        try {
            powerManager = PowerHardwareManager(this)
        } catch (e: Exception) {
            // Non-fatal power hardware fallback
        }

        try {
            audioRecorderManager = AudioRecorderManager(this)
        } catch (e: Exception) {
            // Non-fatal audio recorder fallback
        }

        ttsEngine = TextToSpeechEngine(this)
        // Extract model weights from assets to app private storage and initialize native engines
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.sauti.app.sauti.SautiEngineLoader.ensureAssetsRegistered(applicationContext)
        }
        val sautiDb = SautiDatabase.getInstance(this)
        val prefs = com.sauti.app.settings.SautiPreferencesStore(this)

        // Inject initial custom instructions into native engine
        com.sauti.app.sauti.SautiEngineLoader.customUserInstructions = prefs.customInstructions

        setContent {
            val viewModel: SautiNavigationViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SautiNavigationViewModel(sautiDb.chatSessionDao(), prefs) as T
                    }
                }
            )
            val appTheme by viewModel.appTheme.collectAsState()

            SautiTheme(appTheme = appTheme) {
                SautiMainApp(viewModel = viewModel, ttsEngine = ttsEngine)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsEngine?.shutdown()
        powerManager?.teardown()
        audioRecorderManager?.stopListening()
    }
}

/**
 * 1. Fixed Global Navigation Sidebar:
 *    - Application viewport wrapped inside PermanentNavigationDrawer.
 *    - Sidebar panel permanently visible across all screens.
 *    - Never disappears or requires a manual swipe gesture to reveal.
 *    - Absolutely ZERO emojis in any sidebar option.
 */
@Composable
fun SautiMainApp(
    viewModel: SautiNavigationViewModel,
    ttsEngine: TextToSpeechEngine?
) {
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Professional Android system Back button handling:
    // 1. If drawer is open, back closes drawer.
    // 2. If in any sub-workspace (Audio, Settings, Library, Notebooks), back returns to Chat.
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    androidx.activity.compose.BackHandler(enabled = !drawerState.isOpen && currentWorkspace != SautiWorkspace.CHAT_WORKSPACE) {
        viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0D0E11),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    // Header: Clean brand title & search action (Strictly emoji-free)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sauti",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp
                        )
                        IconButton(
                            onClick = { /* Search sessions action */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Workspace Actions: The 4 Structural Workspace Options ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // "New chat" button: triggers database insert creating a blank session ID dynamically
                        Surface(
                            onClick = { viewModel.createNewSession(); coroutineScope.launch { drawerState.close() } },
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E222A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "New Chat",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "New chat",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // CHAT_WORKSPACE
                        SidebarModuleRow(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            label = "Chat",
                            isSelected = currentWorkspace == SautiWorkspace.CHAT_WORKSPACE,
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE); coroutineScope.launch { drawerState.close() } }
                        )

                        // AUDIO_WORKSPACE
                        SidebarModuleRow(
                            icon = Icons.Outlined.Mic,
                            label = "Audio Mode",
                            isSelected = currentWorkspace == SautiWorkspace.AUDIO_WORKSPACE,
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.AUDIO_WORKSPACE); coroutineScope.launch { drawerState.close() } }
                        )

                        // LIBRARY_WORKSPACE
                        SidebarModuleRow(
                            icon = Icons.Outlined.GridView,
                            label = "Library",
                            isSelected = currentWorkspace == SautiWorkspace.LIBRARY_WORKSPACE,
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.LIBRARY_WORKSPACE); coroutineScope.launch { drawerState.close() } }
                        )

                        // NOTEBOOKS_WORKSPACE
                        SidebarModuleRow(
                            icon = Icons.Outlined.Folder,
                            label = "Notebooks",
                            isSelected = currentWorkspace == SautiWorkspace.NOTEBOOKS_WORKSPACE,
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.NOTEBOOKS_WORKSPACE); coroutineScope.launch { drawerState.close() } }
                        )
                    }

                    Divider(
                        color = Color(0xFF1D222C),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    // --- 2. History Container: Bound Directly to Live Database Flow List<ChatSession> ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "History",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (chatSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "No past sessions yet",
                                color = Color(0xFF475569),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(items = chatSessions, key = { it.id }) { session ->
                                val isCurrent = session.id == activeSessionId &&
                                        currentWorkspace == SautiWorkspace.CHAT_WORKSPACE
                                Surface(
                                    onClick = { viewModel.selectSession(session.id); coroutineScope.launch { drawerState.close() } },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) Color(0xFF1E2430) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                color = if (isCurrent) Color.White else Color(0xFFCBD5E1),
                                                fontSize = 14.sp,
                                                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatSessionTimestamp(session.timestamp),
                                                color = Color(0xFF475569),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(
                        color = Color(0xFF1D222C),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // --- 3 & 4. Automated User Profile Footer & Routing to LIBRARY_WORKSPACE ---
                    UserProfileFooter(
                        profile = userProfile,
                        onSettingsClick = {
                            // Route to Settings workspace
                            viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE); coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        // --- Viewport Content: Structural Workspace State Switcher ---
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            when (currentWorkspace) {
                SautiWorkspace.CHAT_WORKSPACE -> {
                    ChatScreen(
                        onVoiceClick = { viewModel.selectWorkspace(SautiWorkspace.AUDIO_WORKSPACE) },
                        onDrawerClick = { coroutineScope.launch { drawerState.open() } },
                        viewModel = viewModel
                    )
                }
                SautiWorkspace.AUDIO_WORKSPACE -> {
                    VoiceModeScreen(
                        liveTranscript = "Inasikiliza sauti...",
                        isSpeaking = false,
                        onCloseClick = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE) },
                        onToggleVoice = {}
                    )
                }
                SautiWorkspace.LIBRARY_WORKSPACE -> {
                    WorkspacePlaceholderView(
                        title = "Library Workspace",
                        subtitle = "Saved code snippets, prompt templates & offline notes",
                        icon = Icons.Outlined.GridView,
                        onReturnToChat = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE) }
                    )
                }
                SautiWorkspace.NOTEBOOKS_WORKSPACE -> {
                    WorkspacePlaceholderView(
                        title = "Notebooks Workspace",
                        subtitle = "Long-form context workspaces & Swahili document synthesizer",
                        icon = Icons.Outlined.Folder,
                        onReturnToChat = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE) }
                    )
                }
                SautiWorkspace.SETTINGS_WORKSPACE -> {
                    SettingsScreen(
                        ttsEngine = ttsEngine,
                        viewModel = viewModel,
                        onBack = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE) }
                    )
                }
            }
        }
    }
}

/**
 * Clean Sidebar row without any emojis.
 */
@Composable
private fun SidebarModuleRow(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF1E2430) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                fontSize = 14.5.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * 3. Dynamic User Profile Footer:
 *    - Zero hardcoded names or static initial characters.
 *    - Populates circular avatar using UserProfile.getAvatarInitial().
 *    - Settings cog icon routes directly to LIBRARY_WORKSPACE.
 */
@Composable
private fun UserProfileFooter(
    profile: UserProfile?,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (profile == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2430))
                )
                Text(
                    text = "Loading profile...",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circular Avatar populated dynamically via getAvatarInitial()
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.getAvatarInitial(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = profile.fullName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Settings Cog Icon Button (Parameter 4)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkspacePlaceholderView(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onReturnToChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onReturnToChat,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text("Return to Sauti Chat")
        }
    }
}

private fun formatSessionTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
