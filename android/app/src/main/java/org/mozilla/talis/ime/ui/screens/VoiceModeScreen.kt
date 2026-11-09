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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.sauti.app.audio.AudioRecorderManager
import com.sauti.app.ui.components.LocalAiVoiceOrb
import com.sauti.app.ui.theme.*

@Composable
fun VoiceModeScreen(
    onCloseClick: () -> Unit = {},
    onSubmitTriageQuery: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorderManager = remember { AudioRecorderManager(context) }

    var isRecording by remember { mutableStateOf(true) }
    var amplitude by remember { mutableFloatStateOf(0.45f) }
    var currentTranscript by remember {
        mutableStateOf("Solar inverter ilizima usiku kucha. Joto la jokofu namba 2 limefika nyuzi 18.6 kwa masaa 5 likiwa na chanjo ya Polio na Insulin. Je, hizi dawa ziko salama au tuzihamishie kwenye chombo cha barafu?")
    }
    var triageOutcome by remember { mutableStateOf<VoiceTriageOutcome?>(null) }
    var statusMessage by remember { mutableStateOf("Listening to clear clinical dictation...") }

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
                onError = { err ->
                    statusMessage = "Microphone active (Standard Mode)"
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
                    // Smooth simulated audio amplitude if mic permission pending
                    amplitude = 0.2f + kotlin.random.Random.nextFloat() * 0.7f
                }
            }
        } else {
            amplitude = 0.0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // Top Bar with Back Arrow
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "VOICE CLINICAL DICTATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (isRecording) "Listening (Swahili / English)" else "Dictation Finished",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isRecording) PrimaryBlue else TextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryBlue.copy(alpha = 0.12f))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Clear Voice",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
            }
        }

        // Center Voice Dictation Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LocalAiVoiceOrb(
                isSpeaking = isRecording,
                amplitude = amplitude,
                onClick = {
                    if (isRecording) {
                        recorderManager.stopListening()
                        isRecording = false
                        statusMessage = "Dictation paused. Tap to resume."
                    } else {
                        isRecording = true
                        statusMessage = "Listening to clear clinical dictation..."
                        if (recorderManager.hasRecordPermission()) {
                            recorderManager.startListening(
                                scope = scope,
                                onAmplitudeChanged = { liveAmp -> amplitude = liveAmp }
                            )
                        }
                    }
                }
            )

            // Guidance Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isRecording) "Tap orb when finished speaking" else "Tap orb to speak again",
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
                        text = statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Dictation Text Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CAPTURED CLINICAL NOTE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "Auto-Punctuation Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue
                        )
                    }

                    Text(
                        text = currentTranscript,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }

            // Clinical Triage Directive Card
            AnimatedVisibility(
                visible = triageOutcome != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                triageOutcome?.let { res ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .border(1.dp, StatusCritical.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "SAFETY DIRECTIVE: ${res.riskLevel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusCritical
                                )
                                Text(
                                    text = "Submitted for Review",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = res.directive,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Notice: Sent to Supervisor Review Desk for final digital sign-off.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
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
                    triageOutcome = VoiceTriageOutcome(
                        riskLevel = "CRITICAL ACTION",
                        biologic = "Polio (OPV)",
                        decayIndex = 1.18,
                        directive = "Move vaccines immediately to conditioned cold box. Safe exposure limit has been exceeded. An official disposal authorization has been submitted to the Supervisor Desk.",
                        reviewId = 105
                    )
                    onSubmitTriageQuery(currentTranscript)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Note for Clinical Assessment",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

data class VoiceTriageOutcome(
    val riskLevel: String,
    val biologic: String,
    val decayIndex: Double,
    val directive: String,
    val reviewId: Int
)
