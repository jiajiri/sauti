package com.sauti.app.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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

/**
 * Production-grade Keyboard Layout matching the tablet screenshot:
 * Features standard grey elevation keys, subtle dark text, and top acoustic voice strip.
 */
@Composable
fun ComposeKeyboardLayout(
    liveTranscriptionPreview: String = "",
    isListening: Boolean = false,
    amplitude: Float = 0.0f,
    currentLanguage: String = "SW",
    onVoiceToggle: () -> Unit = {},
    onCommitText: (String) -> Unit = {},
    onBackspace: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD1D5DB)) // Slate light keyboard surface as in screenshot
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Minimalist Voice Transcription strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE5E7EB))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (liveTranscriptionPreview.isNotEmpty()) liveTranscriptionPreview else "Tap voice to speak (Swahili / English)",
                color = if (isListening) Color(0xFF047857) else Color(0xFF4B5563),
                fontSize = 12.sp,
                fontWeight = if (isListening) FontWeight.SemiBold else FontWeight.Normal
            )

            LocalAiVoiceOrb(
                isSpeaking = isListening,
                amplitude = amplitude,
                onClick = onVoiceToggle,
                modifier = Modifier.size(28.dp)
            )
        }

        // Row 1: Numbers & symbols
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { char ->
                KeyButton(label = char, modifier = Modifier.weight(1f)) { onCommitText(char) }
            }
        }

        // Row 2: QWERTYUIOP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P").forEach { char ->
                KeyButton(label = char, modifier = Modifier.weight(1f)) { onCommitText(char.lowercase()) }
            }
        }

        // Row 3: ASDFGHJKL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L").forEach { char ->
                KeyButton(label = char, modifier = Modifier.weight(1f)) { onCommitText(char.lowercase()) }
            }
        }

        // Row 4: ZXCVBNM & Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Z", "X", "C", "V", "B", "N", "M").forEach { char ->
                KeyButton(label = char, modifier = Modifier.weight(1f)) { onCommitText(char.lowercase()) }
            }

            // Backspace key
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF9CA3AF))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Row 5: Space & Return
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(label = currentLanguage, modifier = Modifier.weight(1.5f)) { /* toggle */ }
            KeyButton(label = "Space", modifier = Modifier.weight(5f)) { onCommitText(" ") }
            KeyButton(label = ".", modifier = Modifier.weight(1f)) { onCommitText(".") }

            // Enter / Commit
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3B82F6))
                    .clickable { onCommitText("\n") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF1F2937),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
