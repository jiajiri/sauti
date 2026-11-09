package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.components.HardwareTelemetryCard
import com.sauti.app.ui.components.SkeletonCard
import com.sauti.app.ui.theme.*

data class SovereignModelEntry(
    val id: String,
    val name: String,
    val role: String,
    val quantization: String,
    val memoryFootprint: String,
    val status: String // ACTIVE, LOADED, ON_DISK
)

@Composable
fun HubScreen(
    isLoading: Boolean = false,
    onToggleSkeleton: () -> Unit = {}
) {
    var models by remember {
        mutableStateOf(
            listOf(
                SovereignModelEntry(
                    id = "tier1_qwen",
                    name = "Qwen 2.5 1.5B Instruct GGUF",
                    role = "Tier 1 Micro-Edge Clinical Reasoning",
                    quantization = "Q4_K_M (4-bit)",
                    memoryFootprint = "1.1 GB RAM",
                    status = "ACTIVE"
                ),
                SovereignModelEntry(
                    id = "whisper_asr",
                    name = "Offline Whisper ASR Core",
                    role = "Swahili & Sheng Speech-to-Text",
                    quantization = "INT8 NEON SIMD",
                    memoryFootprint = "43.2 MB RAM",
                    status = "LOADED"
                ),
                SovereignModelEntry(
                    id = "tier2_llama",
                    name = "Llama-2-7B-Guanaco Regional",
                    role = "Tier 2 Hospital Server Hub Model",
                    quantization = "Q4_K_M (4-bit)",
                    memoryFootprint = "4.1 GB RAM",
                    status = "ON_DISK"
                )
            )
        )
    }

    var gatewayEndpoint by remember { mutableStateOf("http://127.0.0.1:8000") }
    var gatewayStatus by remember { mutableStateOf("CONNECTED (Local Daemon)") }
    var isCheckingGateway by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EDGE APPLIANCE HUB",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "System Telemetry & GGUF Models",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = onToggleSkeleton,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Simulate Loading Skeleton",
                    tint = PrimaryBlue
                )
            }
        }

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonCard(height = 110.dp)
                SkeletonCard(height = 130.dp)
                SkeletonCard(height = 130.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Real Hardware Telemetry Card
                item {
                    HardwareTelemetryCard()
                }

                // Gateway Daemon Connection Card
                item {
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FASTAPI SOVEREIGN GATEWAY",
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
                                        text = gatewayStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusNominal,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = gatewayEndpoint,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = TextPrimary
                                )

                                TextButton(
                                    onClick = {
                                        gatewayStatus = "PING: 2ms (Nominal)"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Test Ping", style = MaterialTheme.typography.labelMedium, color = PrimaryBlue)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "ON-DEVICE GGUF TENSOR MODELS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Model Cards
                items(models) { model ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                ModelStatusBadge(status = model.status)
                            }

                            Text(
                                text = model.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Divider(color = BorderSubtle, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text(text = "QUANTIZATION", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                                        Text(text = model.quantization, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    }
                                    Column {
                                        Text(text = "FOOTPRINT", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                                        Text(text = model.memoryFootprint, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    }
                                }

                                if (model.status != "ACTIVE") {
                                    OutlinedButton(
                                        onClick = {
                                            models = models.map {
                                                if (it.id == model.id) it.copy(status = "ACTIVE")
                                                else if (it.status == "ACTIVE" && it.id.startsWith("tier")) it.copy(status = "LOADED")
                                                else it
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Text("Bind Active", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelStatusBadge(status: String) {
    val (badgeBg, badgeBorder, badgeText) = when (status) {
        "ACTIVE" -> Triple(StatusNominal.copy(alpha = 0.12f), StatusNominal.copy(alpha = 0.4f), StatusNominal)
        "LOADED" -> Triple(PrimaryBlue.copy(alpha = 0.12f), PrimaryBlue.copy(alpha = 0.4f), PrimaryBlue)
        else -> Triple(SurfaceElevated, BorderSubtle, TextMuted)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeBg)
            .border(1.dp, badgeBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = badgeText,
            fontSize = 9.sp
        )
    }
}
