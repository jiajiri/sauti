package com.sauti.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

        // Request runtime permissions on startup (Microphone + Notifications on Android 13+)
        val permissionsToRequest = mutableListOf<String>()
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                101
            )
        }

        val sautiDb = SautiDatabase.getInstance(this)
        val prefs = com.sauti.app.settings.SautiPreferencesStore(this)

        com.sauti.app.sauti.SautiEngineLoader.customUserInstructions = prefs.customInstructions
        com.sauti.app.sauti.SautiEngineLoader.applyInferenceParams(prefs)

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

    var showExploreSheet by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var selectedSessionForSheet by remember { mutableStateOf<ChatSession?>(null) }

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

    val colors = MaterialTheme.colorScheme
    val isLight = colors.background.red > 0.5f

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                // ── Sauti Navigation Drawer ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Top Search Bar & Options Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = colors.surfaceVariant,
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
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Search",
                                    color = colors.onSurfaceVariant,
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
                                tint = colors.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Workspace Section
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            isSelected = currentWorkspace == SautiWorkspace.SETTINGS_WORKSPACE,
                            onClick = {
                                viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )

                        DrawerWorkspaceItem(
                            icon = Icons.Outlined.Explore,
                            label = "Explore Tasks",
                            isSelected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showExploreSheet = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.outline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Conversation History with Options (Rename & Delete)
                    if (chatSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No previous conversations",
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(items = chatSessions, key = { it.id }) { session ->
                                val isCurrent = session.id == activeSessionId &&
                                        currentWorkspace == SautiWorkspace.CHAT_WORKSPACE

                                @OptIn(ExperimentalFoundationApi::class)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) colors.surfaceVariant else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                viewModel.selectSession(session.id)
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            onLongClick = {
                                                selectedSessionForSheet = session
                                            }
                                        )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Text(
                                            text = session.title,
                                            color = if (isCurrent) colors.onSurface else colors.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "See all...",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .clickable { /* Expand all history */ }
                                )
                            }
                        }
                    }

                    // Persistent Bottom Footer: User Avatar on Left, New Chat Button on Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Circular User Initials Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.selectWorkspace(SautiWorkspace.SETTINGS_WORKSPACE)
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.getAvatarInitial(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = userProfile.fullName,
                                    color = colors.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Settings",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Right: Primary Blue "New Chat" Pill Button
                        Button(
                            onClick = {
                                viewModel.createNewSession()
                                coroutineScope.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New chat", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            when (currentWorkspace) {
                SautiWorkspace.CHAT_WORKSPACE, SautiWorkspace.AUDIO_WORKSPACE -> {
                    ChatScreen(
                        onVoiceClick = {},
                        onDrawerClick = { coroutineScope.launch { drawerState.open() } },
                        viewModel = viewModel
                    )
                }
                SautiWorkspace.LIBRARY_WORKSPACE -> {
                    LibraryScreen(
                        onBack = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE) },
                        onUsePrompt = { prompt ->
                            viewModel.setPendingPrompt(prompt)
                            viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE)
                        }
                    )
                }
                SautiWorkspace.NOTEBOOKS_WORKSPACE -> {
                    NotebooksScreen(
                        onBack = { viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE) },
                        onOpenInChat = { prompt ->
                            viewModel.setPendingPrompt(prompt)
                            viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE)
                        }
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

    // Explore Tasks Bottom Sheet
    if (showExploreSheet) {
        ExploreTasksBottomSheet(
            onDismiss = { showExploreSheet = false },
            onSelectPrompt = { prompt ->
                showExploreSheet = false
                viewModel.setPendingPrompt(prompt)
                viewModel.selectWorkspace(SautiWorkspace.CHAT_WORKSPACE)
            }
        )
    }

    // Long-Press Context Bottom Sheet for Chat Session Actions (Gemini/ChatGPT UX)
    @OptIn(ExperimentalMaterial3Api::class)
    selectedSessionForSheet?.let { session ->
        ModalBottomSheet(
            onDismissRequest = { selectedSessionForSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.onSurfaceVariant.copy(alpha = 0.4f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pin / Unpin Conversation Action
                Surface(
                    onClick = {
                        selectedSessionForSheet = null
                        // Pin toggled
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = colors.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Pin",
                            color = colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Rename Action
                Surface(
                    onClick = {
                        val target = session
                        selectedSessionForSheet = null
                        renameText = target.title
                        sessionToRename = target
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = colors.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Rename",
                            color = colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Delete Action
                Surface(
                    onClick = {
                        val target = session
                        selectedSessionForSheet = null
                        sessionToDelete = target
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Delete",
                            color = Color(0xFFEF4444),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Rename Session Dialog - Gemini style floating rounded dialog
    sessionToRename?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Rename this chat?",
                    color = colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.onSurface,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = colors.outline, thickness = 1.dp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.updateSessionTitle(session.id, renameText.trim())
                        }
                        sessionToRename = null
                    }
                ) {
                    Text(
                        text = "Rename",
                        color = colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text(
                        text = "Cancel",
                        color = colors.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    // Delete Session Confirmation Dialog - Gemini style floating rounded dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Delete chat?",
                    color = colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            },
            text = {
                Text(
                    text = "You'll no longer see this chat here. This will also delete related activity and stored history from your Sauti activity.",
                    color = colors.onSurfaceVariant,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        sessionToDelete = null
                    }
                ) {
                    Text(
                        text = "Confirm",
                        color = colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(
                        text = "Cancel",
                        color = colors.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}

@Composable
private fun DrawerWorkspaceItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.surfaceVariant else Color.Transparent,
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
                tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
