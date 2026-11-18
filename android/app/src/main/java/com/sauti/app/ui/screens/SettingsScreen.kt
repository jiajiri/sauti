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

private val DangerRed = Color(0xFFEF4444)

/**
 * Main Settings Screen adhering to MaterialTheme dynamic light/dark tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ttsEngine: TextToSpeechEngine?,
    viewModel: SautiNavigationViewModel?,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { SautiPreferencesStore(context) }

    val colors = MaterialTheme.colorScheme
    val cardBg = colors.surface
    val dividerColor = colors.outline
    val textPrimary = colors.onSurface
    val textSecondary = colors.onSurfaceVariant
    val accentColor = colors.primary

    var currentTheme by remember { mutableStateOf(prefs.appTheme) }
    var currentName by remember { mutableStateOf(prefs.userName) }
    var customInstructions by remember { mutableStateOf(prefs.customInstructions) }
    var biometricLock by remember { mutableStateOf(prefs.biometricLockEnabled) }
    var selectedModel by remember { mutableStateOf(prefs.modelTier) }

    var showNameDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
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
                    .background(colors.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }

            Text(
                text = "Settings",
                color = textPrimary,
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
            // ── Group 1: Account & Performance ────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.Person,
                    title = "Manage account",
                    subtitle = currentName,
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { showNameDialog = true }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Speed,
                    title = "Offline Assistant & Storage",
                    subtitle = selectedModel.displayName,
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { showModelDialog = true }
                )
            }

            // ── Group 2: Preferences ──────────────────────────────────────────
            SettingsSectionHeader(title = "Preferences", color = textSecondary)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Notifications */ }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Psychology,
                    title = "Assistant Personalization",
                    subtitle = if (customInstructions.isBlank()) "Set custom response style" else "Customized",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { showInstructionsDialog = true }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Face,
                    title = "Avatar",
                    subtitle = "Sovereign Profile (${currentName.firstOrNull()?.uppercase() ?: "U"})",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { showNameDialog = true }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Tune,
                    title = "Task settings",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Task settings */ }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                // Theme Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeMenu = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = textPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Theme", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))

                    Text(currentTheme.displayName, color = textSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                // Voice Setting
                SettingsActionRow(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Voice",
                    subtitle = prefs.voicePersona.personaName,
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { viewModel?.selectWorkspace(SautiWorkspace.VOICE_SETTINGS_WORKSPACE) }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Headphones,
                    title = "Media",
                    subtitle = "Hands-free automatic audio routing",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Media routing */ }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Devices,
                    title = "Device controls",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Device controls */ }
                )
            }

            // ── Group 3: Data & Privacy ───────────────────────────────────────
            SettingsSectionHeader(title = "Data & Privacy", color = textSecondary)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.Share,
                    title = "Export chat history",
                    subtitle = "Save all chats as clean .txt transcript",
                    textColor = textPrimary,
                    secColor = textSecondary,
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

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                // Biometric Lock Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = textPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Lock (Fingerprint / PIN)", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                        Text("Lock conversations on shared devices", color = textSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = biometricLock,
                        onCheckedChange = {
                            biometricLock = it
                            prefs.biometricLockEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy Help Hub",
                    subtitle = "100% on-device sovereign computing guarantee",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Privacy details */ }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Clear all history",
                    titleColor = DangerRed,
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { showClearDialog = true }
                )
            }

            // ── Group 4: Get Support ──────────────────────────────────────────
            SettingsSectionHeader(title = "Get support", color = textSecondary)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Help */ }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Feedback,
                    title = "Report a problem",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Report */ }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { viewModel?.selectWorkspace(SautiWorkspace.ABOUT_WORKSPACE) }
                )

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                SettingsActionRow(
                    icon = Icons.Outlined.Update,
                    title = "Updates",
                    subtitle = "Check offline model packs",
                    textColor = textPrimary,
                    secColor = textSecondary,
                    onClick = { /* Updates */ }
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
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Theme", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                AppTheme.values().forEach { themeOption ->
                    val isSel = currentTheme == themeOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) colors.surfaceVariant else Color.Transparent)
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
                            color = textPrimary,
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
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Model Tier Bottom Sheet (Clean without storage footprint size)
    if (showModelDialog) {
        ModalBottomSheet(
            onDismissRequest = { showModelDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Offline Assistant Tier", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                ModelTier.values().forEach { tier ->
                    val isSel = selectedModel == tier
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) colors.surfaceVariant else Color.Transparent)
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
                            Text(tier.displayName, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(tier.description, color = textSecondary, fontSize = 13.sp)
                        }

                        RadioButton(
                            selected = isSel,
                            onClick = {
                                selectedModel = tier
                                prefs.modelTier = tier
                                showModelDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
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
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) },
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Account Name", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = dividerColor
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary)
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
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
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
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) },
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Assistant Personalization", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tell Sauti how you would like it to respond (e.g. 'Reply in concise Swahili').", color = textSecondary, fontSize = 14.sp)
                OutlinedTextField(
                    value = tempInstructions,
                    onValueChange = { tempInstructions = it },
                    minLines = 4,
                    maxLines = 6,
                    placeholder = { Text("e.g. Always answer in clear, formal Swahili...", color = textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = dividerColor
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary)
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
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }

    // Clear All History Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = colors.surface,
            title = {
                Text("Clear All Conversations?", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will permanently erase all local conversation history and start a fresh state.", color = textSecondary, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
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
    titleColor: Color? = null,
    textColor: Color,
    secColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = titleColor ?: textColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor ?: textColor, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            if (subtitle != null) {
                Text(subtitle, color = secColor, fontSize = 12.sp)
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = secColor, modifier = Modifier.size(20.dp))
    }
}
