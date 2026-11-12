package com.sauti.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sauti.app.ui.theme.PrimaryBlue
import com.sauti.app.ui.theme.AccentTeal

/**
 * Centered pulsating acoustic voice orb.
 * Reacts dynamically to real-time audio amplitude [0.0f, 1.0f].
 */
@Composable
fun PulsatingVoiceOrb(
    isRecording: Boolean,
    normalizedAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_scale"
    )

    val currentScale = if (isRecording) {
        1.0f + (normalizedAmplitude * 0.45f)
    } else {
        idleScale
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(180.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Outer glowing halo
        Canvas(modifier = Modifier.size(160.dp)) {
            val radius = (size.minDimension / 2.0f) * currentScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentTeal.copy(alpha = if (isRecording) 0.55f else 0.25f),
                        PrimaryBlue.copy(alpha = if (isRecording) 0.25f else 0.08f),
                        Color.Transparent
                    )
                ),
                radius = radius
            )
        }

        // Inner solid core
        Canvas(modifier = Modifier.size(90.dp)) {
            val coreRadius = (size.minDimension / 2.0f) * (if (isRecording) 1.0f + (normalizedAmplitude * 0.15f) else 1.0f)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryBlue, AccentTeal)
                ),
                radius = coreRadius
            )
        }

        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Input Orb",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}
