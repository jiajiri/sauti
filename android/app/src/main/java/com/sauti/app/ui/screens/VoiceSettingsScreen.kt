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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.settings.SautiPreferencesStore
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.tts.VoicePersona

private val CardDarkBg   = Color(0xFF171717)
private val DividerColor = Color(0xFF262626)
private val TextPri      = Color(0xFFFFFFFF)
private val TextSec      = Color(0xFF9E9E9E)
private val AccentBlue   = Color(0xFF2563EB)
private val CircleBtnBg  = Color(0xFF1E1E1E)

/**
 * Dedicated Voice Settings Sub-Screen strictly modeled after Reference 4.
 * Features a single grouped card with trailing chevrons, Material 3 switches, and voice previews.
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

    var selectedPersona by remember { mutableStateOf(prefs.voicePersona) }
    var autoSpeak by remember { mutableStateOf(prefs.autoSpeakReplies) }
    var showPersonaSheet by remember { mutableStateOf(false) }
    var interruptResponses by remember { mutableStateOf(true) }
    var guidedVision by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar: Circular back button and centered "Voice" title
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
                text = "Voice",
                color = TextPri,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grouped Card (Reference 4)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardDarkBg)
        ) {
            // Row 1: Sauti's voice
            VoiceSettingNavRow(
                icon = Icons.Outlined.GraphicEq,
                title = "Sauti's voice",
                subtitle = "Choose how Sauti will sound (${selectedPersona.personaName})",
                onClick = { showPersonaSheet = true }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // Row 2: Interrupt Live responses
            VoiceSettingToggleRow(
                icon = Icons.Outlined.VoiceOverOff,
                title = "Interrupt Live responses",
                subtitle = "Talk to interrupt Sauti in Live chats",
                checked = interruptResponses,
                onCheckedChange = { interruptResponses = it }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // Row 3: Caption preferences
            VoiceSettingExternalRow(
                icon = Icons.Outlined.ClosedCaption,
                title = "Caption preferences",
                subtitle = "Manage device captions to see text of Sauti's responses",
                onClick = { /* Device captions trigger */ }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // Row 4: Languages
            VoiceSettingNavRow(
                icon = Icons.Outlined.Language,
                title = "Languages",
                subtitle = "For speaking to Sauti (Auto-detect / Kiswahili / English)",
                onClick = { /* Open language selector */ }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // Row 5: Guided vision / Hands-free background mode
            VoiceSettingToggleRow(
                icon = Icons.Outlined.Visibility,
                title = "Guided vision in Live",
                subtitle = "Hear detailed audio descriptions when using your camera or screen",
                checked = guidedVision,
                onCheckedChange = { guidedVision = it }
            )
        }
    }

    // Persona Selector Bottom Sheet
    if (showPersonaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPersonaSheet = false },
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
                Text(
                    text = "Choose Sauti's Voice",
                    color = TextPri,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                VoicePersona.values().forEach { persona ->
                    val isSelected = selectedPersona == persona
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF262626) else Color.Transparent)
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
                                color = TextPri,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = persona.description,
                                color = TextSec,
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
                                selectedColor = AccentBlue,
                                unselectedColor = TextSec
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showPersonaSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun VoiceSettingNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextPri, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Text(subtitle, color = TextSec, fontSize = 12.sp)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSec, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun VoiceSettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextPri, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Text(subtitle, color = TextSec, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                uncheckedTrackColor = Color(0xFF262626)
            )
        )
    }
}

@Composable
private fun VoiceSettingExternalRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextPri, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPri, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Text(subtitle, color = TextSec, fontSize = 12.sp)
        }
        Icon(Icons.Outlined.NorthEast, contentDescription = null, tint = TextSec, modifier = Modifier.size(18.dp))
    }
}
