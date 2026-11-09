package com.sauti.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.sauti.app.ui.components.SkeletonCard
import com.sauti.app.ui.theme.*

data class HitlReviewItem(
    val reviewId: Int,
    val staffId: String,
    val staffTitle: String,
    val query: String,
    val proposedAction: String,
    val riskLevel: String,
    val status: String, // PENDING, APPROVED, REJECTED
    val createdAt: String,
    val reviewerNotes: String? = null,
    val verificationSeal: String = "SEAL-SHA256-VERIFIED"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HitlScreen(
    isLoading: Boolean = false,
    supervisorId: String = "Dr. Kagame (Chief Medical Officer)"
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var reviewQueue by remember {
        mutableStateOf(
            listOf(
                HitlReviewItem(
                    reviewId = 104,
                    staffId = "Nurse Mariam",
                    staffTitle = "Dispensary Unit In-Charge",
                    query = "Solar battery failed overnight. Refrigerator reached 19.2°C for 6 hours. Emergency discard required for Oral Polio Vaccine (OPV) batch #TZ-884?",
                    proposedAction = "Quarantine Oral Polio Vaccine (OPV) batch #TZ-884. Safe thermal duration has been exceeded under WHO PQS cold chain protocols. Disposal authorization requested.",
                    riskLevel = "CRITICAL ACTION",
                    status = "PENDING",
                    createdAt = "Today, 06:14 AM"
                ),
                HitlReviewItem(
                    reviewId = 103,
                    staffId = "Clinician Omari",
                    staffTitle = "Maternity Health Officer",
                    query = "Thermostat froze below freezing point (-1.4°C for 2 hours). Can Hepatitis B vials still be administered safely?",
                    proposedAction = "Perform Shake Test immediately. Hepatitis B is sensitive to freezing. If sedimentation occurs within 15 minutes, decommission batch #HB-402.",
                    riskLevel = "CRITICAL ACTION",
                    status = "PENDING",
                    createdAt = "Today, 04:30 AM"
                ),
                HitlReviewItem(
                    reviewId = 101,
                    staffId = "Nurse Mariam",
                    staffTitle = "Dispensary Unit In-Charge",
                    query = "Scheduled maintenance power outage for 3 hours. Action plan for BCG vaccine vials.",
                    proposedAction = "Move BCG vials into pre-conditioned vaccine cold box with ice-packs. Chemical disposal is not warranted.",
                    riskLevel = "STANDARD MITIGATION",
                    status = "APPROVED",
                    createdAt = "Yesterday, 14:20 PM",
                    reviewerNotes = "Cold box transfer confirmed. Temperature maintained at +3.8°C."
                )
            )
        )
    }

    var selectedReviewForDetail by remember { mutableStateOf<HitlReviewItem?>(null) }
    var supervisorNotesInput by remember { mutableStateOf("") }

    val filteredList = remember(reviewQueue, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> reviewQueue.filter { it.status == "PENDING" }
            "APPROVED" -> reviewQueue.filter { it.status == "APPROVED" }
            "REJECTED" -> reviewQueue.filter { it.status == "REJECTED" }
            else -> reviewQueue
        }
    }

    // Hardware Back Handler for Review Detail View
    if (selectedReviewForDetail != null) {
        BackHandler {
            selectedReviewForDetail = null
        }
    }

    AnimatedVisibility(
        visible = selectedReviewForDetail != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedReviewForDetail?.let { item ->
            ReviewDetailView(
                item = item,
                notesInput = supervisorNotesInput,
                onNotesChange = { supervisorNotesInput = it },
                onBack = { selectedReviewForDetail = null },
                onAuthorize = {
                    reviewQueue = reviewQueue.map {
                        if (it.reviewId == item.reviewId) {
                            it.copy(
                                status = "APPROVED",
                                reviewerNotes = supervisorNotesInput.ifBlank { "Disposal authorized by Chief Medical Officer under WHO guidelines." }
                            )
                        } else it
                    }
                    selectedReviewForDetail = null
                },
                onReject = {
                    reviewQueue = reviewQueue.map {
                        if (it.reviewId == item.reviewId) {
                            it.copy(
                                status = "REJECTED",
                                reviewerNotes = supervisorNotesInput.ifBlank { "Action rejected. Proceed with physical vial stability re-testing." }
                            )
                        } else it
                    }
                    selectedReviewForDetail = null
                }
            )
        }
    }

    AnimatedVisibility(
        visible = selectedReviewForDetail == null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
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
                        text = "CLINICAL SUPERVISOR DESK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Action Authorizations",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryBlue.copy(alpha = 0.12f))
                        .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Auditor Active",
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
                    SkeletonCard(height = 140.dp)
                    SkeletonCard(height = 140.dp)
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
                        CleanHitlCard(
                            item = item,
                            onClick = {
                                selectedReviewForDetail = item
                                supervisorNotesInput = item.reviewerNotes ?: ""
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewDetailView(
    item: HitlReviewItem,
    notesInput: String,
    onNotesChange: (String) -> Unit,
    onBack: () -> Unit,
    onAuthorize: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation with Back Arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REQUEST #${item.reviewId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "Authorization Review",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            StatusBadge(status = item.status)
        }

        // Details Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "SUBMITTED BY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = "${item.staffId} (${item.staffTitle})", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "DATE & TIME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = item.createdAt, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }

                Divider(color = BorderSubtle, thickness = 1.dp)

                Text(text = "OBSERVED INCIDENT REPORT:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(text = item.query, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)

                Divider(color = BorderSubtle, thickness = 1.dp)

                Text(text = "PROPOSED CLINICAL ACTION:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(text = item.proposedAction, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                // Digital Verification Seal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Authentic",
                        tint = StatusNominal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Digital Verification Seal: Verified Authentic & Tamper-Proof",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        if (item.status == "PENDING") {
            OutlinedTextField(
                value = notesInput,
                onValueChange = onNotesChange,
                label = { Text("Supervisor Rationale & Authorizing Remarks") },
                placeholder = { Text("e.g., Confirmed thermal exposure. Disposal authorized.") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusCritical)
                ) {
                    Text("Reject Action")
                }

                Button(
                    onClick = onAuthorize,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Authorize & Sign")
                }
            }
        } else {
            if (!item.reviewerNotes.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "RECORDED SUPERVISOR DECISION:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = item.reviewerNotes, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CleanHitlCard(
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
                    text = "Review & Authorize ->",
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
