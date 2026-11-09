package com.sauti.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.theme.*

@Composable
fun HardwareTelemetryCard(
    chipArch: String = "ARM64-v8a (NEON SIMD)",
    usableRamGb: String = "3.1 GB / 4.0 GB",
    activeQuantization: String = "Q4_K_M (INT8 ASR)",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HARDWARE & INFERENCE TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StatusNominal.copy(alpha = 0.12f))
                        .border(1.dp, StatusNominal.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "100% SOVEREIGN OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusNominal,
                        fontSize = 9.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryItem(label = "CPU INSTRUCTIONS", value = chipArch)
                TelemetryItem(label = "MEMORY COMMITTED", value = usableRamGb)
                TelemetryItem(label = "PRECISION TENSOR", value = activeQuantization)
            }
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 9.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimary
        )
    }
}
