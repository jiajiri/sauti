package com.sauti.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sauti.app.settings.AppTheme
import com.sauti.app.settings.ModelTier
import com.sauti.app.settings.SautiPreferencesStore
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.viewmodel.SautiNavigationViewModel
import com.sauti.app.viewmodel.SautiWorkspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val CardDarkBg   = Color(0xFF171717)
private val DividerColor = Color(0xFF262626)
private val TextPri      = Color(0xFFFFFFFF)
private val TextSec      = Color(0xFF9E9E9E)
private val AccentBlue   = Color(0xFF2563EB)
private val CircleBtnBg  = Color(0xFF1E1E1E)
private val DangerRed    = Color(0xFFEF4444)

/**
 * Main Settings Screen strictly modeled after References 1 & 2.
 * Features grouped rounded dark cards with hairline dividers and trailing chevrons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ttsEngine: TextToSpeechEngine?,
    viewModel: SautiNavigationViewModel?,
    onBack: () -> Unit
) {
    BackHandler(enabled = true) { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { SautiPreferencesStore(context) }

    var currentName by remember { mutableStateOf(prefs.userName) }
    var currentTheme by remember { mutableStateOf(prefs.appTheme) }
    var customInstructions by remember { mutableStateOf(prefs.customInstructions) }
    var selectedModel by remember { mutableStateOf(prefs.modelTier) }
    var biometricLock by remember { mutableStateOf(prefs.biometricLockEnabled) }

    var showNameDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar: Circular back button and centered "Settings" title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CircleBtnBg)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPri
                )
            }

            Text(
                text = "Settings",
                color = TextPri,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Group 1: Account & Performance (Reference 1) ─────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.Person,
                    title = "Manage account",
                    subtitle = currentName,
                    onClick = { showNameDialog = true }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Speed,
                    title = "Offline Intelligence & Storage",
                    subtitle = "${selectedModel.displayName} (${selectedModel.userFacingSize})",
                    onClick = { showModelDialog = true }
                )
            }

            // ── Group 2: Preferences (Reference 1) ───────────────────────────
            SettingsSectionHeader(title = "Preferences")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    onClick = { /* Notifications handler */ }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Psychology,
                    title = "Personal Intelligence",
                    subtitle = if (customInstructions.isBlank()) "Set custom response style" else "Customized",
                    onClick = { showInstructionsDialog = true }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Face,
                    title = "Avatar",
                    subtitle = "Sovereign Profile (${currentName.firstOrNull()?.uppercase() ?: "U"})",
                    onClick = { showNameDialog = true }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Task settings",
                    onClick = { /* Task settings */ }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                // Theme Selector Row with Up/Down Indicator (Reference 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeMenu = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = TextPri, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Theme", color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))

                    Text(currentTheme.displayName, color = TextSec, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = TextSec, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                // Voice Setting -> Navigates to VoiceSettingsScreen (Reference 4)
                SettingsActionRow(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Voice",
                    subtitle = prefs.voicePersona.personaName,
                    onClick = { viewModel?.selectWorkspace(SautiWorkspace.VOICE_SETTINGS_WORKSPACE) }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Headphones,
                    title = "Media",
                    subtitle = "Hands-free automatic audio routing",
                    onClick = { /* Media routing */ }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Devices,
                    title = "Device controls",
                    onClick = { /* Device controls */ }
                )
            }

            // ── Group 3: Data & Privacy (Reference 2) ────────────────────────
            SettingsSectionHeader(title = "Data & Privacy")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.Share,
                    title = "Export chat history",
                    subtitle = "Save all chats as clean .txt transcript",
                    onClick = {
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
                    }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                // Biometric App Lock Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextPri, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Lock (Fingerprint / PIN)", color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                        Text("Lock conversations on shared devices", color = TextSec, fontSize = 12.sp)
                    }
                    Switch(
                        checked = biometricLock,
                        onCheckedChange = {
                            biometricLock = it
                            prefs.biometricLockEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue,
                            uncheckedTrackColor = Color(0xFF262626)
                        )
                    )
                }

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy Help Hub",
                    subtitle = "100% on-device sovereign computing guarantee",
                    onClick = { /* Privacy details */ }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Clear all history",
                    titleColor = DangerRed,
                    onClick = { showClearDialog = true }
                )
            }

            // ── Group 4: Get Support (Reference 2) ───────────────────────────
            SettingsSectionHeader(title = "Get support")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help",
                    onClick = { /* Help center */ }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Feedback,
                    title = "Report a problem",
                    onClick = { /* Report problem */ }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                // About Row -> Navigates to AboutScreen (Reference 5)
                SettingsActionRow(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    onClick = { viewModel?.selectWorkspace(SautiWorkspace.ABOUT_WORKSPACE) }
                )

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Update,
                    title = "Updates",
                    subtitle = "Check offline model packs",
                    onClick = { /* Check updates */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Theme Picker Bottom Sheet
    if (showThemeMenu) {
        ModalBottomSheet(
            onDismissRequest = { showThemeMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Theme", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                AppTheme.values().forEach { themeOption ->
                    val isSel = currentTheme == themeOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFF262626) else Color.Transparent)
                            .clickable {
                                currentTheme = themeOption
                                prefs.appTheme = themeOption
                                viewModel?.updateTheme(themeOption)
                                showThemeMenu = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = themeOption.displayName,
                            color = TextPri,
                            fontSize = 16.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )

                        RadioButton(
                            selected = isSel,
                            onClick = {
                                currentTheme = themeOption
                                prefs.appTheme = themeOption
                                viewModel?.updateTheme(themeOption)
                                showThemeMenu = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Model Tier Bottom Sheet
    if (showModelDialog) {
        ModalBottomSheet(
            onDismissRequest = { showModelDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Offline Intelligence Tier", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                ModelTier.values().forEach { tier ->
                    val isSel = selectedModel == tier
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFF262626) else Color.Transparent)
                            .clickable {
                                selectedModel = tier
                                prefs.modelTier = tier
                                showModelDialog = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tier.displayName, color = TextPri, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(tier.description, color = TextSec, fontSize = 13.sp)
                            Text("Storage footprint: ${tier.userFacingSize}", color = AccentBlue, fontSize = 12.sp)
                        }

                        RadioButton(
                            selected = isSel,
                            onClick = {
                                selectedModel = tier
                                prefs.modelTier = tier
                                showModelDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Edit Name Bottom Sheet
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(currentName) }
        ModalBottomSheet(
            onDismissRequest = { showNameDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) },
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Manage Account", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("This name personalizes your sovereign avatar initials and chat transcripts.", color = TextSec, fontSize = 14.sp)
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    label = { Text("Display Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPri,
                        unfocusedTextColor = TextPri,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DividerColor,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSec
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showNameDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSec)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (tempName.isNotBlank()) {
                                currentName = tempName.trim()
                                prefs.userName = currentName
                                viewModel?.updateUserName(currentName)
                            }
                            showNameDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }

    // Custom Instructions Bottom Sheet
    if (showInstructionsDialog) {
        var tempInstructions by remember { mutableStateOf(customInstructions) }
        ModalBottomSheet(
            onDismissRequest = { showInstructionsDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) },
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Personal Intelligence", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tell Sauti how you would like it to respond (e.g. 'Reply in concise Swahili' or 'Use bullet points').", color = TextSec, fontSize = 14.sp)
                OutlinedTextField(
                    value = tempInstructions,
                    onValueChange = { tempInstructions = it },
                    minLines = 4,
                    maxLines = 6,
                    placeholder = { Text("e.g. Always answer in clear, formal Swahili...", color = TextSec) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPri,
                        unfocusedTextColor = TextPri,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DividerColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showInstructionsDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSec)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            customInstructions = tempInstructions.trim()
                            prefs.customInstructions = customInstructions
                            viewModel?.updateCustomInstructions(customInstructions)
                            showInstructionsDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }

    // Clear History Bottom Sheet
    if (showClearDialog) {
        ModalBottomSheet(
            onDismissRequest = { showClearDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Clear All Chat History?", color = TextPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("This action permanently deletes all saved conversations and local messages. This cannot be undone.", color = TextSec, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showClearDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSec)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel?.clearAllHistory()
                            showClearDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextSec,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = TextPri,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            if (subtitle != null) {
                Text(subtitle, color = TextSec, fontSize = 12.sp)
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSec, modifier = Modifier.size(20.dp))
    }
}
