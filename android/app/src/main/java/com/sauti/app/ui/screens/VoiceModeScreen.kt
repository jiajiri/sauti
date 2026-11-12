package com.sauti.app.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import com.sauti.app.audio.AudioRecorderManager
import com.sauti.app.audio.AudioRoutingManager
import com.sauti.app.data.local.ChatSessionEntity
import com.sauti.app.data.local.SautiDatabase
import com.sauti.app.ui.components.LocalAiVoiceOrb
import com.sauti.app.ui.theme.*

/**
 * Extracts first 3 words from recognized speech text for session titles.
 */
fun extractFirstThreeWords(text: String): String {
    val clean = text.trim()
    if (clean.isBlank()) return "New Voice Session"
    val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return if (words.isEmpty()) "New Voice Session" else words.take(3).joinToString(" ")
}

@Composable
fun VoiceModeScreen(
    liveTranscript: String = "",
    isSpeaking: Boolean = false,
    onCloseClick: () -> Unit = {},
    onToggleVoice: () -> Unit = {},
    onSubmitVoiceQuery: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorderManager = remember { AudioRecorderManager(context) }
    val audioRoutingManager = remember { AudioRoutingManager(context) }
    val currentRouteState by audioRoutingManager.currentRoute.collectAsState()
    val sautiDb = remember { SautiDatabase.getInstance(context) }

    var isRecording by remember { mutableStateOf(true) }
    var amplitude by remember { mutableFloatStateOf(0.45f) }
    var selectedNoiseLevel by remember { mutableStateOf("Med") }
    var isVoiceBoostEnabled by remember { mutableStateOf(true) }
    var isDenoisedActive by remember { mutableStateOf(true) }

    var currentTranscript by remember {
        mutableStateOf(
            if (liveTranscript.isNotBlank()) liveTranscript
            else "Bofya orb kuanza kurekodi au kutamka maneno..."
        )
    }

    // Hardware routing dynamic status
    val hardwareRoutingText = remember(currentRouteState) {
        if (currentRouteState.isExternal) {
            "Listening via Headset Microphone"
        } else {
            "Listening via Device Microphone"
        }
    }

    // Hardware Back Navigation
    BackHandler {
        recorderManager.stopListening()
        onCloseClick()
    }

    // Real audio lifecycle management
    DisposableEffect(Unit) {
        if (recorderManager.hasRecordPermission()) {
            recorderManager.startListening(
                scope = scope,
                onAmplitudeChanged = { liveAmp ->
                    amplitude = liveAmp
                },
                onError = {
                    // Handled internally
                }
            )
        }

        onDispose {
            recorderManager.stopListening()
        }
    }

    // Dynamic wave animation when in active dictation
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(100)
                if (!recorderManager.hasRecordPermission()) {
                    amplitude = 0.25f + kotlin.random.Random.nextFloat() * 0.65f
                }
            }
        } else {
            amplitude = 0.0f
        }
    }

    fun persistSessionAndClose(textToSave: String) {
        val snippet = extractFirstThreeWords(textToSave)
        val newSessionId = UUID.randomUUID().toString()
        scope.launch {
            sautiDb.chatSessionDao().insert(
                ChatSessionEntity(
                    id = newSessionId,
                    title = snippet,
                    timestamp = System.currentTimeMillis()
                )
            )
            onSubmitVoiceQuery(textToSave)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // Top Bar: Back arrow, Studio Title & Private badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = {
                        recorderManager.stopListening()
                        onCloseClick()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Sauti Audio Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRecording) "Sauti Voice Active" else "Sauti Voice Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryBlue.copy(alpha = 0.15f))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "100% On-Device",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
            }
        }

        // Center Voice & Studio Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Acoustic Voice Orb with glowing rings
            LocalAiVoiceOrb(
                isSpeaking = isRecording,
                amplitude = amplitude,
                onClick = {
                    if (isRecording) {
                        recorderManager.stopListening()
                        isRecording = false
                        // Automatically persist session when user stops recording
                        persistSessionAndClose(currentTranscript)
                    } else {
                        isRecording = true
                        if (recorderManager.hasRecordPermission()) {
                            recorderManager.startListening(
                                scope = scope,
                                onAmplitudeChanged = { liveAmp -> amplitude = liveAmp }
                            )
                        }
                    }
                    onToggleVoice()
                }
            )

            // Guidance indicator & Hardware Route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isRecording) "Tap orb to interrupt or finish speaking" else "Tap orb to speak aloud",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) StatusNominal else TextMuted)
                    )
                    Text(
                        text = if (isRecording) hardwareRoutingText else "Audio Route Standby",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Audio Player & Processing Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUDIO PROCESSING ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )

                        // Denoised vs Original toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDenoisedActive) PrimaryBlue else Color.Transparent)
                                    .clickable { isDenoisedActive = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Denoised",
                                    color = if (isDenoisedActive) TextPrimary else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!isDenoisedActive) PrimaryBlue else Color.Transparent)
                                    .clickable { isDenoisedActive = false }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Original",
                                    color = if (!isDenoisedActive) TextPrimary else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Noise Reduction Strength Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Noise Fix:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            listOf("Low", "Med", "High").forEach { level ->
                                val isSelected = selectedNoiseLevel == level
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) PrimaryBlue else SurfaceElevated)
                                        .clickable { selectedNoiseLevel = level }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = level,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Voice Boost Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { isVoiceBoostEnabled = !isVoiceBoostEnabled }
                        ) {
                            Text(text = "Voice Boost", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Switch(
                                checked = isVoiceBoostEnabled,
                                onCheckedChange = { isVoiceBoostEnabled = it },
                                modifier = Modifier.height(20.dp),
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                            )
                        }
                    }
                }
            }

            // Transcript Box with Sauti Native Transcription
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SAUTI NATIVE TRANSCRIPTION:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }

                    Text(
                        text = currentTranscript,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Bottom Submission Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    recorderManager.stopListening()
                    isRecording = false
                    persistSessionAndClose(currentTranscript)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit to Sauti Core",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
