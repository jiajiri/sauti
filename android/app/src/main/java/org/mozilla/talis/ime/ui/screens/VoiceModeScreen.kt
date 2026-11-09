package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    liveTranscript: String = "Hello! I greet you too! I'm doing very well. How are you today?",
    isSpeaking: Boolean = true,
    amplitude: Float = 0.5f,
    onCloseClick: () -> Unit = {},
    onToggleVoice: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Minimal Top Right Close Action
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Center Acoustic Voice Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            LocalAiVoiceOrb(
                isSpeaking = isSpeaking,
                amplitude = amplitude,
                onClick = onToggleVoice
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isSpeaking) "Speaking..." else "Listening...",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap orb to interrupt speech",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            // Minimalist Dark Transcript Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = liveTranscript,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        // Minimal Bottom Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Voice button
            IconButton(
                onClick = onToggleVoice,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Red End/Disconnect button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ButtonRedDisconnect)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
