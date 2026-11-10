package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.components.SkeletonCard
import com.sauti.app.ui.theme.*

data class ClinicEnginePackage(
    val id: String,
    val name: String,
    val purpose: String,
    val storageSize: String,
    val status: String // ACTIVE, LOADED, READY
)

@Composable
fun HubScreen(
    isLoading: Boolean = false,
    onToggleSkeleton: () -> Unit = {}
) {
    var packages by remember {
        mutableStateOf(
            listOf(
                ClinicEnginePackage(
                    id = "pkg_coldchain",
                    name = "Vaccine Cold-Chain Safety Engine",
                    purpose = "Calculates WHO PQS vaccine degradation and cold storage rules completely offline.",
                    storageSize = "1.1 GB (On Device)",
                    status = "ACTIVE"
                ),
                ClinicEnginePackage(
                    id = "pkg_voice",
                    name = "Swahili & English Clinical Voice Recognition",
                    purpose = "Transcribes rural healthcare dictations directly on device with zero internet.",
                    storageSize = "43 MB (On Device)",
                    status = "LOADED"
                ),
                ClinicEnginePackage(
                    id = "pkg_triage",
                    name = "MOH Clinical Protocol Reference Corpus",
                    purpose = "Authentic East African health guidelines, immunizations, and triage standards.",
                    storageSize = "280 MB (On Device)",
                    status = "READY"
                )
            )
        )
    }

    var localServerStatus by remember { mutableStateOf("Connected (Hospital Local Server)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
    ) {
        // Clean Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CLINIC SYSTEM OVERVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "Device & Storage Health",
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
                    contentDescription = "Simulate Loading Placeholder",
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
                // Device Safety Status Card
                item {
                    Box(
                        modifier = Modifier
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
                                    text = "OFFLINE PRIVACY & INTEGRITY",
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
                                        text = "ON-DEVICE & SOVEREIGN",
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
                                Column {
                                    Text(text = "DATA BOUNDARY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(text = "Local Clinic Only", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                                Column {
                                    Text(text = "DEVICE MEMORY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(text = "Safe & Responsive", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                                Column {
                                    Text(text = "BATTERY PROFILE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(text = "Eco Low-Drain", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // Local Server Connection Status Card
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
                                    text = "LOCAL CLINIC SERVER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "Active Local Link",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusNominal
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = localServerStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )

                                TextButton(
                                    onClick = {
                                        localServerStatus = "Checked Just Now (Ready)"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Verify Sync", style = MaterialTheme.typography.labelMedium, color = PrimaryBlue)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "INSTALLED CLINICAL INTELLIGENCE PACKAGES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Packages List
                items(packages) { pkg ->
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
                                    text = pkg.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                PackageBadge(status = pkg.status)
                            }

                            Text(
                                text = pkg.purpose,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Divider(color = BorderSubtle, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Storage: ${pkg.storageSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )

                                if (pkg.status != "ACTIVE") {
                                    TextButton(
                                        onClick = {
                                            packages = packages.map {
                                                if (it.id == pkg.id) it.copy(status = "ACTIVE")
                                                else if (it.status == "ACTIVE" && it.id.startsWith("pkg_cold")) it.copy(status = "READY")
                                                else it
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("Set Primary", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
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
private fun PackageBadge(status: String) {
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
