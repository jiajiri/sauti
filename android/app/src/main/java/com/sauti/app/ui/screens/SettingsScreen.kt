package com.sauti.app.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.sauti.app.sauti.SautiEngineLoader
import com.sauti.app.settings.ModelTier
import com.sauti.app.settings.SautiPreferencesStore
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.tts.VoicePersona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

// ─── Color palette ────────────────────────────────────────────────────────────
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
 * Settings screen: voice personas, model switcher, expandable Response Tuning accordion,
 * Storage & Data Management with "Clear All History" confirmation dialog.
 */
@Composable
fun SettingsScreen(
    ttsEngine: TextToSpeechEngine?,
    onBack: () -> Unit,
    onClearAllHistory: () -> Unit = {}
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val prefs     = remember { SautiPreferencesStore(context) }

    var selectedPersona     by remember { mutableStateOf(prefs.voicePersona) }
    var selectedModel       by remember { mutableStateOf(prefs.modelTier) }

    // Accordion state
    var isTuningExpanded    by remember { mutableStateOf(false) }

    // Parameters
    var temperature         by remember { mutableStateOf(prefs.temperature) }
    var topK                by remember { mutableStateOf(prefs.topK.toFloat()) }
    var topP                by remember { mutableStateOf(prefs.topP) }

    var isSwitchingModel    by remember { mutableStateOf(false) }
    var paramsApplied       by remember { mutableStateOf(false) }
    var showClearDialog     by remember { mutableStateOf(false) }

    // Dialog confirmation for Clear All History
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear All History?",
                    color = TextPri,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all past conversation sessions and message logs from device storage. This action cannot be undone.",
                    color = TextSec,
                    fontSize = 14.sp
                )
            },
            containerColor = CardDark,
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDialog = false },
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDim)
                ) {
                    Text("Cancel", color = TextSec)
                }
            }
        )
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

            // ══ SECTION 1: Voice Persona ═══════════════════════════════════════
            SettingsSectionHeader(
                icon = Icons.Outlined.RecordVoiceOver,
                title = "Voice Persona"
            )

            VoicePersona.values().forEach { persona ->
                val isSelected = selectedPersona == persona
                PersonaCard(
                    persona    = persona,
                    isSelected = isSelected,
                    onSelect   = {
                        selectedPersona = persona
                        prefs.voicePersona = persona
                        ttsEngine?.setVoicePersona(persona)
                    },
                    onPreview  = { ttsEngine?.previewSpeak(persona) }
                )
            }

            // ══ SECTION 2: Core Model ═════════════════════════════════════════
            SettingsSectionHeader(
                icon = Icons.Outlined.Memory,
                title = "Core Model"
            )

            if (isSwitchingModel) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentBlue,
                    trackColor = CardDark
                )
                Text(
                    text = "Loading ${selectedModel.displayName}...",
                    color = AccentBlue,
                    fontSize = 13.sp
                )
            }

            ModelTier.values().forEach { tier ->
                val isSelected = selectedModel == tier
                ModelCard(
                    tier       = tier,
                    isSelected = isSelected,
                    context    = context,
                    onSelect   = {
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

            // ══ SECTION 3: Generation Tuning Controls (Advanced Accordion) ═════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
            ) {
                // Header (Click to expand/collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTuningExpanded = !isTuningExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Tune, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "Response Tuning",
                                color = TextPri,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isTuningExpanded) "Tuned for power users" else "Casual mode (tap to customize)",
                                color = TextDim,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isTuningExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Toggle Tuning",
                        tint = TextSec
                    )
                }

                AnimatedVisibility(
                    visible = isTuningExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HorizontalDivider(color = BorderDim, thickness = 0.5.dp)

                        // 1. Temperature Slider (0.0 to 1.5, default 0.7)
                        ParamSlider(
                            label = "Temperature",
                            value = temperature,
                            range = 0.0f..1.5f,
                            steps = 29,
                            display = "%.2f".format(temperature) + if (temperature <= 0.15f) " (Deterministic)" else "",
                            hint = "Lower (0.1) produces deterministic answers; higher yields more varied phrasing.",
                            onValueChange = {
                                temperature = (it * 100).roundToInt() / 100f
                                prefs.temperature = temperature
                                SautiEngineLoader.applyInferenceParams(prefs)
                                paramsApplied = true
                            }
                        )

                        // 2. Top-K Slider (1 to 100, default 40)
                        ParamSlider(
                            label = "Top-K",
                            value = topK,
                            range = 1f..100f,
                            steps = 98,
                            display = topK.roundToInt().toString() + if (topK.roundToInt() == 1) " (Greedy)" else "",
                            hint = "Setting top-k = 1 forces greedy decoding (picks highest probability token).",
                            onValueChange = {
                                topK = it.roundToInt().toFloat()
                                prefs.topK = topK.roundToInt()
                                SautiEngineLoader.applyInferenceParams(prefs)
                                paramsApplied = true
                            }
                        )

                        // 3. Top-P Slider (0.1 to 1.0, default 0.9)
                        ParamSlider(
                            label = "Top-P (Nucleus)",
                            value = topP,
                            range = 0.10f..1.00f,
                            steps = 17,
                            display = "%.2f".format(topP),
                            hint = "Controls nucleus sampling diversity threshold.",
                            onValueChange = {
                                topP = (it * 100).roundToInt() / 100f
                                prefs.topP = topP
                                SautiEngineLoader.applyInferenceParams(prefs)
                                paramsApplied = true
                            }
                        )

                        // Reset to Defaults Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    temperature = 0.70f
                                    topK        = 40f
                                    topP        = 0.90f
                                    prefs.temperature = 0.70f
                                    prefs.topK        = 40
                                    prefs.topP        = 0.90f
                                    SautiEngineLoader.applyInferenceParams(prefs)
                                    paramsApplied = true
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDim),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(16.dp), tint = TextSec)
                                Spacer(Modifier.width(6.dp))
                                Text("Reset to Defaults", color = TextSec, fontSize = 13.sp)
                            }
                        }

                        if (paramsApplied) {
                            Text(
                                text = "Parameters active in native llama_sampler chain",
                                color = AccentGreen,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // ══ SECTION 4: Storage & Data Management ═══════════════════════════
            SettingsSectionHeader(
                icon = Icons.Outlined.Storage,
                title = "Storage & Data Management"
            )

            StorageManagementCard(
                context = context,
                onClearHistoryClick = { showClearDialog = true }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ─── Persona Card ─────────────────────────────────────────────────────────────

@Composable
private fun PersonaCard(
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
            colors   = RadioButtonDefaults.colors(
                selectedColor   = AccentBlue,
                unselectedColor = TextDim
            )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = persona.personaName,
                color      = if (isSelected) Color.White else TextPri,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text     = persona.description,
                color    = TextSec,
                fontSize = 12.sp
            )
            Text(
                text     = "Pitch %.2f  |  Rate %.2f".format(persona.pitch, persona.speechRate),
                color    = TextDim,
                fontSize = 11.sp
            )
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

// ─── Model Card ───────────────────────────────────────────────────────────────

@Composable
private fun ModelCard(
    tier: ModelTier,
    isSelected: Boolean,
    context: Context,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) BorderSel else BorderDim
    val bgColor     = if (isSelected) CardSelected else CardDark

    val fileSize = remember(tier) {
        val f = File(context.filesDir, "sauti/${tier.assetFileName}")
        if (f.exists()) "%.0f MB".format(f.length() / 1_048_576.0) else "Not on device"
    }

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
            colors   = RadioButtonDefaults.colors(
                selectedColor   = AccentBlue,
                unselectedColor = TextDim
            )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text       = tier.displayName,
                    color      = if (isSelected) Color.White else TextPri,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E2430),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text     = tier.paramCount,
                        color    = if (isSelected) AccentBlue else TextDim,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text     = tier.description,
                color    = TextSec,
                fontSize = 12.sp
            )
            Text(
                text     = "Size on device: $fileSize",
                color    = TextDim,
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Icon(Icons.Outlined.CheckCircle, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
    }
}

// ─── Param Slider ─────────────────────────────────────────────────────────────

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: String,
    hint: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1F29))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextPri, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(
                display,
                color      = AccentBlue,
                fontSize   = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = range,
            steps         = steps,
            colors        = SliderDefaults.colors(
                thumbColor              = AccentBlue,
                activeTrackColor        = AccentBlue,
                inactiveTrackColor      = Color(0xFF283141)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(hint, color = TextDim, fontSize = 11.sp)
    }
}

// ─── Storage & Data Management Card ───────────────────────────────────────────

@Composable
private fun StorageManagementCard(
    context: Context,
    onClearHistoryClick: () -> Unit
) {
    val sautiDir = remember { File(context.filesDir, "sauti") }
    val totalModelsSizeMB = remember {
        if (sautiDir.exists() && sautiDir.isDirectory) {
            val totalBytes = sautiDir.listFiles()?.sumOf { it.length() } ?: 0L
            totalBytes / 1_048_576.0
        } else {
            0.0
        }
    }

    val dbFile = remember { context.getDatabasePath("sauti_database.db") }
    val dbSizeKB = remember {
        if (dbFile.exists()) dbFile.length() / 1024.0 else 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Models Store (sauti/)", color = TextSec, fontSize = 13.sp)
            Text("%.1f MB".format(totalModelsSizeMB), color = TextPri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Local Conversation History", color = TextSec, fontSize = 13.sp)
            Text("%.1f KB".format(dbSizeKB), color = TextPri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        HorizontalDivider(color = BorderDim, thickness = 0.5.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Conversation Data", color = TextPri, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Remove all stored sessions and messages", color = TextDim, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onClearHistoryClick,
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = DangerRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear All", color = DangerRed, fontSize = 13.sp)
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(
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
            color      = TextSec,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}
