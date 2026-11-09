package com.sauti.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sauti.app.ui.theme.AccentEmeraldOrb

/**
 * Authentic LocalAI Voice Mode Orb with 5 vertical equalizer bars.
 * Scaled dynamically with audio decibel amplitude [0.0f, 1.0f].
 */
@Composable
fun LocalAiVoiceOrb(
    isSpeaking: Boolean,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_idle"
    )

    val scaleMultiplier = if (isSpeaking) 1.0f + (amplitude * 0.25f) else idleScale

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(190.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Outer dark matte ring
        Box(
            modifier = Modifier
                .size((160 * scaleMultiplier).dp)
                .clip(CircleShape)
                .background(Color(0xFF161A22))
        )

        // Inner glowing emerald core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size((110 * scaleMultiplier).dp)
                .clip(CircleShape)
                .background(AccentEmeraldOrb)
        ) {
            // 5 Vertical Audio Level Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EqualizerBar(baseHeight = 16.dp, activeHeight = 28.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 24.dp, activeHeight = 38.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 32.dp, activeHeight = 48.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 24.dp, activeHeight = 38.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 16.dp, activeHeight = 28.dp, isSpeaking = isSpeaking, amplitude = amplitude)
            }
        }
    }
}

@Composable
private fun EqualizerBar(
    baseHeight: androidx.compose.ui.unit.Dp,
    activeHeight: androidx.compose.ui.unit.Dp,
    isSpeaking: Boolean,
    amplitude: Float
) {
    val barHeight = if (isSpeaking) {
        baseHeight + ((activeHeight - baseHeight) * amplitude)
    } else {
        baseHeight
    }

    Box(
        modifier = Modifier
            .width(4.dp)
            .height(barHeight)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF090A0D))
    )
}
