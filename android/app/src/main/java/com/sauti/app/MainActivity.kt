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

        val sautiDb = SautiDatabase.getInstance(this)

        setContent {
            SautiTheme {
                val viewModel: SautiNavigationViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SautiNavigationViewModel(sautiDb.chatSessionDao()) as T
                        }
                    }
                )

                SautiMainApp(viewModel = viewModel)
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
    viewModel: SautiNavigationViewModel
) {
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                drawerContainerColor = Color(0xFF0D0E11),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .navigationBarsPadding()
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
                            onClick = { viewModel.createNewSession() },
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
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE) }
                        )

                        // AUDIO_WORKSPACE
                        SidebarModuleRow(
                            icon = Icons.Outlined.Mic,
                            label = "Audio Mode",
                            isSelected = currentWorkspace == SautiWorkspace.AUDIO_WORKSPACE,
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.AUDIO_WORKSPACE) }
                        )

                        // LIBRARY_WORKSPACE
                        SidebarModuleRow(
                            icon = Icons.Outlined.GridView,
                            label = "Library",
                            isSelected = currentWorkspace == SautiWorkspace.LIBRARY_WORKSPACE,
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.LIBRARY_WORKSPACE) }
                        )

                        // NOTEBOOKS_WORKSPACE
                        SidebarModuleRow(
                            icon = Icons.Outlined.Folder,
                            label = "Notebooks",
                            isSelected = currentWorkspace == SautiWorkspace.NOTEBOOKS_WORKSPACE,
                            onClick = { viewModel.selectWorkspace(SautiWorkspace.NOTEBOOKS_WORKSPACE) }
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
                                    onClick = { viewModel.selectSession(session.id) },
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
                                        Text(
                                            text = session.title,
                                            color = if (isCurrent) Color.White else Color(0xFFCBD5E1),
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                            // Parameter 4: Smoothly route viewport layout to LIBRARY_WORKSPACE subscreen
                            viewModel.selectWorkspace(SautiWorkspace.LIBRARY_WORKSPACE)
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
                        onDrawerClick = { /* No-op: Sidebar is permanently visible */ }
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
                    if (profile.isProTier) {
                        Text(
                            text = "PRO",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
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
