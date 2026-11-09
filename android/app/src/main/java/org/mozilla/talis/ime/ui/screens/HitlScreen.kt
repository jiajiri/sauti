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
import com.sauti.app.ui.components.SkeletonCard
import com.sauti.app.ui.theme.*

data class HitlReviewItem(
    val reviewId: Int,
    val staffId: String,
    val query: String,
    val proposedAction: String,
    val riskLevel: String,
    val sha256AuditDigest: String,
    val status: String, // PENDING, APPROVED, REJECTED
    val createdAt: String,
    val reviewerNotes: String? = null,
    val decayIndex: Double? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HitlScreen(
    isLoading: Boolean = false,
    supervisorId: String = "dr_kagame"
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var reviewQueue by remember {
        mutableStateOf(
            listOf(
                HitlReviewItem(
                    reviewId = 104,
                    staffId = "nurse_mariam",
                    query = "Solar battery failed at 02:00. Fridge temperature reached 19.2°C for 6 hours. Emergency discard required for OPV batch #TZ-884?",
                    proposedAction = "Quarantine Oral Polio Vaccine (OPV) batch #TZ-884. Arrhenius thermal decay index calculated at 1.18 (exceeds threshold 1.0). Immediate disposal sanctioned under WHO PQS E003 protocol.",
                    riskLevel = "CRITICAL_ACTION",
                    sha256AuditDigest = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                    status = "PENDING",
                    createdAt = "Today, 06:14 AM",
                    decayIndex = 1.18
                ),
                HitlReviewItem(
                    reviewId = 103,
                    staffId = "clinician_omari",
                    query = "Thermostat stuck below freezing (-1.4°C for 2 hours). Should Hepatitis B vials be saved?",
                    proposedAction = "Conduct Shake Test protocol immediately. Hepatitis B is strictly freeze-sensitive. If sedimentation forms flocculant granules within 15 minutes, decommission batch #HB-402.",
                    riskLevel = "CRITICAL_ACTION",
                    sha256AuditDigest = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    status = "PENDING",
                    createdAt = "Today, 04:30 AM",
                    decayIndex = 0.45
                ),
                HitlReviewItem(
                    reviewId = 101,
                    staffId = "nurse_mariam",
                    query = "Sub-dispensary power cut scheduled for 3 hours. BCG vaccine excursion mitigation plan.",
                    proposedAction = "Relocate BCG vials to conditioned water-pack cold box (WHO PQS E004). No chemical disposal warranted.",
                    riskLevel = "ACTION_PROPOSED",
                    sha256AuditDigest = "bc92d192809f0f9b6b80153be992c6b45a0b4d4b1f4142f7c00e57f5c531d279",
                    status = "APPROVED",
                    createdAt = "Yesterday, 14:20 PM",
                    reviewerNotes = "Cold box conditioning confirmed. Temperature logged at +3.8°C.",
                    decayIndex = 0.12
                )
            )
        )
    }

    var activeReviewItem by remember { mutableStateOf<HitlReviewItem?>(null) }
    var reviewRationaleNotes by remember { mutableStateOf("") }

    val filteredList = remember(reviewQueue, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> reviewQueue.filter { it.status == "PENDING" }
            "APPROVED" -> reviewQueue.filter { it.status == "APPROVED" }
            "REJECTED" -> reviewQueue.filter { it.status == "REJECTED" }
            else -> reviewQueue
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HUMAN-IN-THE-LOOP SAFETY GATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "Clinical Supervisor Sign-Off",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryBlue.copy(alpha = 0.12f))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = supervisorId,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
            }
        }

        // Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "PENDING", "APPROVED", "REJECTED").forEach { filter ->
                val isSel = selectedFilter == filter
                val count = when (filter) {
                    "PENDING" -> reviewQueue.count { it.status == "PENDING" }
                    "APPROVED" -> reviewQueue.count { it.status == "APPROVED" }
                    "REJECTED" -> reviewQueue.count { it.status == "REJECTED" }
                    else -> reviewQueue.size
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) PrimaryBlue else SurfaceCard)
                        .border(1.dp, if (isSel) PrimaryBlue else BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "$filter ($count)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSel) TextPrimary else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonCard(height = 160.dp)
                SkeletonCard(height = 160.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredList) { item ->
                    HitlCard(
                        item = item,
                        onClick = {
                            activeReviewItem = item
                            reviewRationaleNotes = item.reviewerNotes ?: ""
                        }
                    )
                }
            }
        }
    }

    // Interactive Clinical Audit Sign-Off Modal
    activeReviewItem?.let { item ->
        AlertDialog(
            onDismissRequest = { activeReviewItem = null },
            confirmButton = {
                if (item.status == "PENDING") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                reviewQueue = reviewQueue.map {
                                    if (it.reviewId == item.reviewId) {
                                        it.copy(
                                            status = "REJECTED",
                                            reviewerNotes = reviewRationaleNotes.ifBlank { "Clinical supervisor rejected disposal action. Proceed with physical vial stability re-test." }
                                        )
                                    } else it
                                }
                                activeReviewItem = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusCritical)
                        ) {
                            Text("Reject Action")
                        }

                        Button(
                            onClick = {
                                reviewQueue = reviewQueue.map {
                                    if (it.reviewId == item.reviewId) {
                                        it.copy(
                                            status = "APPROVED",
                                            reviewerNotes = reviewRationaleNotes.ifBlank { "Disposal authorized under WHO PQS kinetic degradation guidelines." }
                                        )
                                    } else it
                                }
                                activeReviewItem = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Authorize & Sign")
                        }
                    }
                } else {
                    TextButton(onClick = { activeReviewItem = null }) {
                        Text("Close", color = PrimaryBlue)
                    }
                }
            },
            dismissButton = {
                if (item.status == "PENDING") {
                    TextButton(onClick = { activeReviewItem = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
            containerColor = SurfaceCard,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Review #${item.reviewId}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    StatusBadge(status = item.status)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ORIGINAL CLINICIAN INQUIRY:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = item.query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Divider(color = BorderSubtle, thickness = 1.dp)

                    Text(
                        text = "PROPOSED CLINICAL ACTION:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = item.proposedAction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // SHA-256 Audit Seal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceElevated)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "CRYPTOGRAPHIC AUDIT DIGEST (SHA-256):",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                            Text(
                                text = item.sha256AuditDigest,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = TextSecondary,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (item.status == "PENDING") {
                        OutlinedTextField(
                            value = reviewRationaleNotes,
                            onValueChange = { reviewRationaleNotes = it },
                            label = { Text("Clinical Rationale & Modification Notes") },
                            placeholder = { Text("Enter supervisor verification remarks...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    } else if (!item.reviewerNotes.isNullOrBlank()) {
                        Text(
                            text = "SUPERVISOR SIGNED NOTE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = item.reviewerNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.status == "APPROVED") StatusNominal else StatusCritical
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun HitlCard(
    item: HitlReviewItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${item.reviewId}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "•  ${item.staffId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                StatusBadge(status = item.status)
            }

            Text(
                text = item.query,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 2
            )

            Text(
                text = item.proposedAction,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2
            )

            Divider(color = BorderSubtle, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Text(
                    text = "Review Audit ->",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (badgeBg, badgeBorder, badgeText) = when (status) {
        "APPROVED" -> Triple(StatusNominal.copy(alpha = 0.12f), StatusNominal.copy(alpha = 0.5f), StatusNominal)
        "REJECTED" -> Triple(StatusCritical.copy(alpha = 0.12f), StatusCritical.copy(alpha = 0.5f), StatusCritical)
        else -> Triple(StatusWarning.copy(alpha = 0.12f), StatusWarning.copy(alpha = 0.5f), StatusWarning)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeBg)
            .border(1.dp, badgeBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = badgeText
        )
    }
}

