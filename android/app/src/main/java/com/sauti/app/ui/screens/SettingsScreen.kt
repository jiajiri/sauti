package com.sauti.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sauti.app.sauti.SautiEngineLoader
import com.sauti.app.security.BiometricAuthManager
import com.sauti.app.settings.ModelTier
import com.sauti.app.settings.SautiPreferencesStore
import com.sauti.app.settings.VadSensitivity
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.tts.VoicePersona
import com.sauti.app.viewmodel.SautiNavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// ─── Production Color Palette ────────────────────────────────────────────────
private val BgDark       = Color(0xFF0D0E11)
private val CardDark     = Color(0xFF13171E)
private val CardSelected = Color(0xFF1E2430)
private val BorderDim    = Color(0xFF1D222C)
private val BorderSel    = Color(0xFF0284C7)
private val TextPri      = Color(0xFFE2E8F0)
private val TextSec      = Color(0xFF94A3B8)
private val TextDim      = Color(0xFF64748B)
private val AccentBlue   = Color(0xFF38BDF8)
private val AccentGreen  = Color(0xFF22C55E)
private val DangerRed    = Color(0xFFEF4444)

/**
 * Clean, Commercial-Grade Production Settings Screen.
 * Strictly free of internal technical jargon, raw file extensions, and C++ metrics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ttsEngine: TextToSpeechEngine?,
    viewModel: SautiNavigationViewModel?,
    onBack: () -> Unit
) {
    // Native Android Back gesture & button support
    androidx.activity.compose.BackHandler(enabled = true) {
        onBack()
    }

    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val prefs     = remember { SautiPreferencesStore(context) }

    // State from preferences store
    var currentName         by remember { mutableStateOf(prefs.userName) }
    var currentTheme        by remember { mutableStateOf(prefs.appTheme) }
    var customInstructions  by remember { mutableStateOf(prefs.customInstructions) }
    var selectedPersona     by remember { mutableStateOf(prefs.voicePersona) }
    var autoSpeak           by remember { mutableStateOf(prefs.autoSpeakReplies) }
    var vadSensitivity      by remember { mutableStateOf(prefs.vadSensitivity) }
    var selectedModel       by remember { mutableStateOf(prefs.modelTier) }
    var autoLowBattery      by remember { mutableStateOf(prefs.autoSwitchLowBattery) }
    var biometricLock       by remember { mutableStateOf(prefs.biometricLockEnabled) }

    var isSwitchingModel    by remember { mutableStateOf(false) }
    var showNameDialog      by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showClearDialog     by remember { mutableStateOf(false) }
    var showPrivacyDialog   by remember { mutableStateOf(false) }

    // ── Edit Name Bottom Sheet ───────────────────────────────────────────────
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(currentName) }
        ModalBottomSheet(
            onDismissRequest = { showNameDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333B48)) },
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Display Name", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("This name is used to personalize your sovereign profile and circular avatar badge.", color = TextSec, fontSize = 14.sp)
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    label = { Text("Display Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPri,
                        unfocusedTextColor = TextPri,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderDim
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showNameDialog = false },
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDim),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSec)
                    }
                    Button(
                        onClick = {
                            val clean = tempName.trim().ifEmpty { "User" }
                            currentName = clean
                            prefs.userName = clean
                            viewModel?.updateUserName(clean)
                            showNameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // ── Custom Instructions Bottom Sheet ──────────────────────────────────────
    if (showInstructionsDialog) {
        var tempInstructions by remember { mutableStateOf(customInstructions) }
        ModalBottomSheet(
            onDismissRequest = { showInstructionsDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333B48)) },
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Custom Instructions", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Tell Sauti how you'd like it to respond (e.g. 'Reply in concise Swahili' or 'Provide bullet points'). These instructions are applied offline to your conversational context.",
                    color = TextSec,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = tempInstructions,
                    onValueChange = { tempInstructions = it },
                    minLines = 4,
                    maxLines = 8,
                    placeholder = { Text("e.g. Always answer in clear, formal Swahili...", color = TextDim) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPri,
                        unfocusedTextColor = TextPri,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderDim
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showInstructionsDialog = false },
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDim),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSec)
                    }
                    Button(
                        onClick = {
                            customInstructions = tempInstructions.trim()
                            prefs.customInstructions = customInstructions
                            viewModel?.updateCustomInstructions(customInstructions)
                            SautiEngineLoader.customUserInstructions = customInstructions
                            showInstructionsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Instructions")
                    }
                }
            }
        }
    }

    // ── Clear All History Bottom Sheet ────────────────────────────────────────
    if (showClearDialog) {
        ModalBottomSheet(
            onDismissRequest = { showClearDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333B48)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = DangerRed, modifier = Modifier.size(26.dp))
                    Text("Clear All Chat History?", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "This action will permanently delete all past conversations and stored message logs from this device. Once deleted, this data cannot be recovered.",
                    color = TextSec,
                    fontSize = 14.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showClearDialog = false },
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDim),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSec)
                    }
                    Button(
                        onClick = {
                            showClearDialog = false
                            viewModel?.clearAllHistory()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                }
            }
        }
    }

    // ── Privacy & Sovereign Policy Bottom Sheet ───────────────────────────────
    if (showPrivacyDialog) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacyDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333B48)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(26.dp))
                    Text("Privacy & Sovereignty Guarantee", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• 100% On-Device AI: All language and speech processing runs locally using device hardware. Zero prompts or speech audio are ever transmitted over the network.", color = TextSec, fontSize = 14.sp)
                    Text("• Hardware-Backed Protection: Local storage and biometric locks are safeguarded by Android Keystore hardware security modules.", color = TextSec, fontSize = 14.sp)
                    Text("• Zero Cloud Telemetry: Sauti does not bundle third-party tracking SDKs, analytics, or behavioral trackers.", color = TextSec, fontSize = 14.sp)
                }
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)
                ) {
                    Text("Understood")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top Bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPri,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Settings",
                color = TextPri,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(color = BorderDim, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ══ 1. Account & Personalization ══════════════════════════════════
            SectionHeader(icon = Icons.Outlined.Person, title = "Account & Personalization")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile & Name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentName.firstOrNull()?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentName, color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Sovereign User Profile", color = TextDim, fontSize = 12.sp)
                    }

                    IconButton(onClick = { showNameDialog = true }) {
                        Icon(Icons.Outlined.Edit, "Edit Name", tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = BorderDim, thickness = 0.5.dp)

                // Custom Instructions row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInstructionsDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom Instructions", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (customInstructions.isBlank()) "Set preferred response style (e.g. concise)" else customInstructions,
                            color = TextSec,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = TextDim)
                }
            }

            // ══ 2. Voice & Audio ══════════════════════════════════════════════
            SectionHeader(icon = Icons.Outlined.VolumeUp, title = "Voice & Audio")

            VoicePersona.values().forEach { persona ->
                val isSelected = selectedPersona == persona
                PersonaRow(
                    persona = persona,
                    isSelected = isSelected,
                    onSelect = {
                        selectedPersona = persona
                        prefs.voicePersona = persona
                        ttsEngine?.setVoicePersona(persona)
                    },
                    onPreview = { ttsEngine?.previewSpeak(persona) }
                )
            }

            // Auto-speak toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Speak Responses", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Read answers aloud in chat mode automatically", color = TextDim, fontSize = 12.sp)
                }
                Switch(
                    checked = autoSpeak,
                    onCheckedChange = {
                        autoSpeak = it
                        prefs.autoSpeakReplies = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                )
            }

            // Hands-Free VAD sensitivity selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Hands-Free Sensitivity", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Controls how quickly the microphone activates in voice mode.", color = TextDim, fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VadSensitivity.values().forEach { sens ->
                        val isSelected = vadSensitivity == sens
                        Surface(
                            onClick = {
                                vadSensitivity = sens
                                prefs.vadSensitivity = sens
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E2430),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = sens.displayName,
                                    color = if (isSelected) Color.White else TextSec,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ══ 3. Offline Intelligence & Battery ═════════════════════════════
            SectionHeader(icon = Icons.Outlined.Memory, title = "Offline Intelligence")

            if (isSwitchingModel) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = AccentBlue,
                    trackColor = CardDark
                )
            }

            ModelTier.values().forEach { tier ->
                val isSelected = selectedModel == tier
                ModelTierCard(
                    tier = tier,
                    isSelected = isSelected,
                    onSelect = {
                        if (!isSwitchingModel && !isSelected) {
                            selectedModel = tier
                            prefs.modelTier = tier
                            isSwitchingModel = true
                            scope.launch(Dispatchers.IO) {
                                SautiEngineLoader.switchModel(context, tier)
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    isSwitchingModel = false
                                }
                            }
                        }
                    }
                )
            }

            // Auto-switch battery row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Switch on Low Battery", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Automatically shift to Battery Saver below 20%", color = TextDim, fontSize = 12.sp)
                }
                Switch(
                    checked = autoLowBattery,
                    onCheckedChange = {
                        autoLowBattery = it
                        prefs.autoSwitchLowBattery = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                )
            }

            // ══ 4. Privacy & Security ═════════════════════════════════════════
            SectionHeader(icon = Icons.Outlined.Lock, title = "Privacy & Security")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Biometric App Lock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Lock (Fingerprint / PIN)", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Lock conversations on shared devices", color = TextDim, fontSize = 12.sp)
                    }
                    Switch(
                        checked = biometricLock,
                        onCheckedChange = {
                            biometricLock = it
                            prefs.biometricLockEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                    )
                }

                HorizontalDivider(color = BorderDim, thickness = 0.5.dp)

                // Export Conversations
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch(Dispatchers.IO) {
                                val file = viewModel?.exportConversationsToText(context)
                                if (file != null && file.exists()) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export Conversations"))
                                }
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Export Conversations", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Save all chats as clean .txt transcript", color = TextDim, fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.Share, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                }

                HorizontalDivider(color = BorderDim, thickness = 0.5.dp)

                // Clear History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Chat History", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Permanently wipe local message logs", color = TextDim, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear All", color = DangerRed, fontSize = 13.sp)
                    }
                }
            }

            // ══ 5. Appearance & Theme ══════════════════════════════════════════
            SectionHeader(icon = Icons.Outlined.Palette, title = "Appearance")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Theme Mode", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Auto matches device system theme", color = TextDim, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.sauti.app.settings.AppTheme.values().forEach { themeOption ->
                        val isSel = currentTheme == themeOption
                        Surface(
                            onClick = {
                                currentTheme = themeOption
                                viewModel?.updateTheme(themeOption)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) AccentBlue else CardSelected,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) AccentBlue else BorderDim
                            )
                        ) {
                            Text(
                                text = themeOption.displayName,
                                color = if (isSel) Color.White else TextSec,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ══ 5. About & Sovereignty ═════════════════════════════════════════
            SectionHeader(icon = Icons.Outlined.Info, title = "About Sauti")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", color = TextSec, fontSize = 13.sp)
                    Text("Sauti AI v1.0.0", color = TextPri, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Architecture", color = TextSec, fontSize = 13.sp)
                    Text("100% On-Device Sovereign", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                HorizontalDivider(color = BorderDim, thickness = 0.5.dp)

                Text(
                    text = "Privacy & Sovereignty Guarantee",
                    color = AccentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { showPrivacyDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ─── Persona Row ──────────────────────────────────────────────────────────────

@Composable
private fun PersonaRow(
    persona: VoicePersona,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    val borderColor = if (isSelected) BorderSel else BorderDim
    val bgColor     = if (isSelected) CardSelected else CardDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick  = onSelect,
            colors   = RadioButtonDefaults.colors(selectedColor = AccentBlue, unselectedColor = TextDim)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(persona.personaName, color = if (isSelected) Color.White else TextPri, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(persona.description, color = TextSec, fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onPreview,
            border  = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else BorderDim),
            shape   = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(14.dp), tint = if (isSelected) AccentBlue else TextSec)
            Spacer(Modifier.width(4.dp))
            Text("Preview", color = if (isSelected) AccentBlue else TextSec, fontSize = 12.sp)
        }
    }
}

// ─── Model Tier Card ──────────────────────────────────────────────────────────

@Composable
private fun ModelTierCard(
    tier: ModelTier,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) BorderSel else BorderDim
    val bgColor     = if (isSelected) CardSelected else CardDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick  = onSelect,
            colors   = RadioButtonDefaults.colors(selectedColor = AccentBlue, unselectedColor = TextDim)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tier.displayName, color = if (isSelected) Color.White else TextPri, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Surface(
                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E2430),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tier.userFacingSize,
                        color = if (isSelected) AccentBlue else TextDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(tier.description, color = TextSec, fontSize = 12.sp)
        }
        if (isSelected) {
            Icon(Icons.Outlined.CheckCircle, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
        Text(
            title,
            color = TextSec,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}
