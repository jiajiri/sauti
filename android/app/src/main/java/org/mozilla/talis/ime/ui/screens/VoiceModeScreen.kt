package com.sauti.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.components.LocalAiVoiceOrb
import com.sauti.app.ui.theme.*

@Composable
fun VoiceModeScreen(
    onCloseClick: () -> Unit = {},
    onSubmitTriageQuery: (String) -> Unit = {}
) {
    var isRecording by remember { mutableStateOf(true) }
    var amplitude by remember { mutableFloatStateOf(0.68f) }
    var currentTranscript by remember {
        mutableStateOf("Solar inverter imekata umeme usiku kucha. Joto la jokofu namba 2 limefika nyuzi 19 kwa masaa 6 likiwa na chanjo ya Oral Polio. Je, hizi chanjo tuzitupe au ziko salama?")
    }
    var triageOutcome by remember { mutableStateOf<VoiceTriageOutcome?>(null) }
    var detectedLanguage by remember { mutableStateOf("Kiswahili / Sheng (sw-TZ)") }

    // Simulate fluctuating amplitude while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                kotlinx.coroutines.delay(120)
                amplitude = (0.25f + kotlin.random.Random.nextFloat() * 0.65f)
            }
        } else {
            amplitude = 0.0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OFFLINE ASR & TRIAGE CORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = if (isRecording) "Listening (INT8 Whisper)..." else "Audio Note Captured",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRecording) PrimaryBlue else TextPrimary
                )
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Voice Screen",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Center Acoustic Voice Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LocalAiVoiceOrb(
                isSpeaking = isRecording,
                amplitude = amplitude,
                onClick = {
                    isRecording = !isRecording
                }
            )

            // State and Instruction Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isRecording) "Tap orb to finish dictation" else "Tap orb to restart recording",
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
                        text = detectedLanguage,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Live Transcription Container
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
                            text = "LIVE SPEECH TRANSCRIPTION:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "GBNF Cleaned",
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

            // Triage Result Outcome Card (Appears after analysis)
            AnimatedVisibility(visible = triageOutcome != null) {
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
                                    text = "TRIAGE DIRECTIVE: ${res.riskLevel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusCritical
                                )
                                Text(
                                    text = "Arrhenius Index: ${res.decayIndex}",
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
                                text = "Status: Quarantined in Supervisor Review Queue (Review #${res.reviewId})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    isRecording = false
                    triageOutcome = VoiceTriageOutcome(
                        riskLevel = "CRITICAL_ACTION",
                        biologic = "OPV",
                        decayIndex = 1.18,
                        directive = "Emergency cold box transfer required. OPV batch has exceeded thermal tolerance. Disposal submission dispatched to Clinical Supervisor.",
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
                    text = "Submit Voice Note to Clinical Triage",
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
