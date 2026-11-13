package com.sauti.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sauti.app.audio.AudioRecorderManager
import com.sauti.app.audio.AudioRoutingManager
import com.sauti.app.data.local.ChatSession
import com.sauti.app.data.local.SautiDatabase
import com.sauti.app.data.model.UserProfile
import com.sauti.app.power.PowerHardwareManager
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.ui.screens.*
import com.sauti.app.ui.theme.SautiTheme
import com.sauti.app.viewmodel.SautiNavigationViewModel
import com.sauti.app.viewmodel.SautiWorkspace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

// ── Obsidian AMOLED Visual Tokens ─────────────────────────────────────────────
private val DrawerCanvasBg   = Color(0xFF000000)
private val SearchPillBg     = Color(0xFF1E1E1E)
private val ActiveRowBg      = Color(0xFF262626)
private val TextWhite        = Color(0xFFFFFFFF)
private val TextMuted        = Color(0xFF9E9E9E)
private val AccentBlue       = Color(0xFF2563EB)
private val WaveformBtnBg    = Color(0xFF1E1E1E)

class MainActivity : ComponentActivity() {

    private var ttsEngine: TextToSpeechEngine? = null
    private var powerManager: PowerHardwareManager? = null
    private var audioRecorderManager: AudioRecorderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge transparent system bars
        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Async native engines initialization
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.sauti.app.sauti.SautiEngineLoader.ensureAssetsRegistered(this@MainActivity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ttsEngine = TextToSpeechEngine(this)
        powerManager = PowerHardwareManager(this)
        audioRecorderManager = AudioRecorderManager(this)

        val sautiDb = SautiDatabase.getInstance(this)
        val prefs = com.sauti.app.settings.SautiPreferencesStore(this)

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
 * Main application container matching Reference 3 (Drawer) & Reference 6 (Home).
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

    // Native Android Back gesture & button handling:
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    androidx.activity.compose.BackHandler(
        enabled = !drawerState.isOpen && currentWorkspace != SautiWorkspace.CHAT_WORKSPACE
    ) {
        when (currentWorkspace) {
            SautiWorkspace.VOICE_SETTINGS_WORKSPACE -> viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE)
            SautiWorkspace.ABOUT_WORKSPACE -> viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE)
            else -> viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DrawerCanvasBg,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                // ── Sauti Navigation Drawer (Reference 3) ─────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Top Search Bar & Options Trigger (Reference 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = SearchPillBg,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Search",
                                    color = TextMuted,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { /* Menu options */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "Options",
                                tint = TextWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Workspace Section (Reference 3)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DrawerWorkspaceItem(
                            icon = Icons.Outlined.GraphicEq,
                            label = "Audio Mode",
                            isSelected = currentWorkspace == SautiWorkspace.AUDIO_WORKSPACE,
                            onClick = {
                                viewModel.selectWorkspace(SautiWorkspace.AUDIO_WORKSPACE)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )

                        DrawerWorkspaceItem(
                            icon = Icons.Outlined.GridView,
                            label = "Library",
                            isSelected = currentWorkspace == SautiWorkspace.LIBRARY_WORKSPACE,
                            onClick = {
                                viewModel.selectWorkspace(SautiWorkspace.LIBRARY_WORKSPACE)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )

                        DrawerWorkspaceItem(
                            icon = Icons.Outlined.Folder,
                            label = "Notebooks",
                            isSelected = currentWorkspace == SautiWorkspace.NOTEBOOKS_WORKSPACE,
                            onClick = {
                                viewModel.selectWorkspace(SautiWorkspace.NOTEBOOKS_WORKSPACE)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )

                        DrawerWorkspaceItem(
                            icon = Icons.Outlined.Tune,
                            label = "Customize Sauti",
                            isSelected = false,
                            onClick = {
                                viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )

                        DrawerWorkspaceItem(
                            icon = Icons.Outlined.Explore,
                            label = "Explore Tasks",
                            isSelected = false,
                            onClick = { /* Explore */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Conversation History with Highlighted Active Row (Reference 3)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = chatSessions, key = { it.id }) { session ->
                            val isCurrent = session.id == activeSessionId &&
                                    currentWorkspace == SautiWorkspace.CHAT_WORKSPACE

                            Surface(
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) ActiveRowBg else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = session.title,
                                        color = if (isCurrent) TextWhite else TextMuted,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "See all...",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .clickable { /* Expand all history */ }
                            )
                        }
                    }

                    // Persistent Bottom Footer (Reference 3)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Primary Blue "New Chat" Pill Button (Reference 3)
                        Button(
                            onClick = {
                                viewModel.createNewSession()
                                coroutineScope.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Center: Circular User Initials Avatar (Tapping opens Settings)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                                .clickable {
                                    viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE)
                                    coroutineScope.launch { drawerState.close() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.getAvatarInitial(),
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Right: Circular Waveform Icon Button (Reference 3)
                        IconButton(
                            onClick = {
                                viewModel.selectWorkspace(SautiWorkspace.AUDIO_WORKSPACE)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(WaveformBtnBg)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = "Audio Mode",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
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
                SautiWorkspace.VOICE_SETTINGS_WORKSPACE -> {
                    VoiceSettingsScreen(
                        ttsEngine = ttsEngine,
                        onBack = { viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE) }
                    )
                }
                SautiWorkspace.ABOUT_WORKSPACE -> {
                    AboutScreen(
                        onBack = { viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerWorkspaceItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ActiveRowBg else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TextWhite else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (isSelected) TextWhite else Color(0xFFD1D5DB),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun WorkspacePlaceholderView(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onReturnToChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = title,
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onReturnToChat,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Return to Sauti Chat", color = TextWhite)
            }
        }
    }
}
