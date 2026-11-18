package com.sauti.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sauti.app.settings.SautiPreferencesStore
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.tts.VoicePersona

/**
 * Dedicated Voice Settings sub-screen.
 * Colors fully delegated to MaterialTheme so Light, Dark, and Auto themes all render correctly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    ttsEngine: TextToSpeechEngine?,
    onBack: () -> Unit
) {
    BackHandler(enabled = true) { onBack() }
    val context = LocalContext.current
    val prefs = remember { SautiPreferencesStore(context) }

    val colors       = MaterialTheme.colorScheme
    val cardBg       = colors.surface
    val dividerColor = colors.outline
    val textPrimary  = colors.onSurface
    val textSecondary = colors.onSurfaceVariant
    val accentColor  = colors.primary
    val circleBtnBg  = colors.surfaceVariant

    var selectedPersona    by remember { mutableStateOf(prefs.voicePersona) }
    var interruptResponses by remember { mutableStateOf(true) }
    var guidedVision       by remember { mutableStateOf(false) }
    var showPersonaSheet   by remember { mutableStateOf(false) }

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
                    .background(circleBtnBg)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }

            Text(
                text = "Voice",
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grouped settings card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
        ) {
            VoiceSettingNavRow(
                icon = Icons.Outlined.GraphicEq,
                title = "Sauti's voice",
                subtitle = "Choose how Sauti will sound (${selectedPersona.personaName})",
                textColor = textPrimary,
                secColor = textSecondary,
                onClick = { showPersonaSheet = true }
            )

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            VoiceSettingToggleRow(
                icon = Icons.Outlined.VoiceOverOff,
                title = "Interrupt Live responses",
                subtitle = "Talk to interrupt Sauti in Live chats",
                checked = interruptResponses,
                accentColor = accentColor,
                textColor = textPrimary,
                secColor = textSecondary,
                surfaceVariant = colors.surfaceVariant,
                onCheckedChange = { interruptResponses = it }
            )

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // External link row (opens device captions settings)
            VoiceSettingNavRow(
                icon = Icons.Outlined.ClosedCaption,
                title = "Caption preferences",
                subtitle = "Manage device captions to see text of Sauti's responses",
                externalLink = true,
                textColor = textPrimary,
                secColor = textSecondary,
                onClick = { /* Device captions trigger */ }
            )

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            VoiceSettingNavRow(
                icon = Icons.Outlined.Language,
                title = "Languages",
                subtitle = "For speaking to Sauti (Auto-detect / Kiswahili / English)",
                textColor = textPrimary,
                secColor = textSecondary,
                onClick = { /* Open language selector */ }
            )

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            VoiceSettingToggleRow(
                icon = Icons.Outlined.Visibility,
                title = "Guided vision in Live",
                subtitle = "Hear detailed audio descriptions when using your camera or screen",
                checked = guidedVision,
                accentColor = accentColor,
                textColor = textPrimary,
                secColor = textSecondary,
                surfaceVariant = colors.surfaceVariant,
                onCheckedChange = { guidedVision = it }
            )
        }
    }

    // Voice persona selector bottom sheet
    if (showPersonaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPersonaSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose Sauti's Voice",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                VoicePersona.values().forEach { persona ->
                    val isSelected = selectedPersona == persona
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.surfaceVariant else Color.Transparent)
                            .clickable {
                                selectedPersona = persona
                                prefs.voicePersona = persona
                                ttsEngine?.setVoicePersona(persona)
                                ttsEngine?.speak("Hujambo, mimi ni ${persona.personaName}. Karibu tuzungumze.")
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = persona.personaName,
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = persona.description,
                                color = textSecondary,
                                fontSize = 13.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedPersona = persona
                                prefs.voicePersona = persona
                                ttsEngine?.setVoicePersona(persona)
                                ttsEngine?.speak("Hujambo, mimi ni ${persona.personaName}. Karibu tuzungumze.")
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = textSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showPersonaSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Navigation/action row (chevron right, or external NorthEast arrow) ────────

@Composable
private fun VoiceSettingNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    externalLink: Boolean = false,
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
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Text(subtitle, color = secColor, fontSize = 12.sp)
        }
        Icon(
            imageVector = if (externalLink) Icons.Outlined.NorthEast else Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = secColor,
            modifier = Modifier.size(if (externalLink) 18.dp else 20.dp)
        )
    }
}

// ── Toggle/switch row ─────────────────────────────────────────────────────────

@Composable
private fun VoiceSettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    textColor: Color,
    secColor: Color,
    surfaceVariant: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Text(subtitle, color = secColor, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedTrackColor = surfaceVariant
            )
        )
    }
}
