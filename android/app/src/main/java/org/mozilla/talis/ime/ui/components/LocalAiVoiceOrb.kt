package com.sauti.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sauti.app.ui.theme.*

/**
 * Institutional Acoustic Voice Orb with dynamic decibel equalizer bars.
 * Driven by real-time speech amplitude [0.0f, 1.0f].
 */
@Composable
fun LocalAiVoiceOrb(
    isSpeaking: Boolean,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AcousticPulse")
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleScale"
    )

    val scaleMultiplier = if (isSpeaking) 1.0f + (amplitude * 0.28f) else idleScale
    val coreColor = if (isSpeaking) PrimaryBlue else PrimaryBlueMuted

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
        // Outer subtle acoustic wave ring
        Box(
            modifier = Modifier
                .size((164 * scaleMultiplier).dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, CircleShape)
        )

        // Middle aura ring
        Box(
            modifier = Modifier
                .size((136 * scaleMultiplier).dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = if (isSpeaking) 0.18f else 0.08f))
        )

        // Inner solid core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size((106 * scaleMultiplier).dp)
                .clip(CircleShape)
                .background(coreColor)
        ) {
            // 5 Vertical Decibel Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EqualizerBar(baseHeight = 14.dp, activeHeight = 28.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 22.dp, activeHeight = 40.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 30.dp, activeHeight = 52.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 22.dp, activeHeight = 40.dp, isSpeaking = isSpeaking, amplitude = amplitude)
                EqualizerBar(baseHeight = 14.dp, activeHeight = 28.dp, isSpeaking = isSpeaking, amplitude = amplitude)
            }
        }
    }
}

@Composable
private fun EqualizerBar(
    baseHeight: Dp,
    activeHeight: Dp,
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
            .background(TextPrimary)
    )
}
