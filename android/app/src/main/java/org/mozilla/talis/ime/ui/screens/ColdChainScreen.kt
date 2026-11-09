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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.components.SkeletonCard
import com.sauti.app.ui.theme.*
import kotlin.math.exp
import kotlin.math.max

data class RefrigeratorTelemetry(
    val id: String,
    val name: String,
    val location: String,
    val currentTempCelsius: Double,
    val powerSource: String,
    val batteryPct: Int,
    val doorOpenCount: Int,
    val biologics: List<String>,
    val excursionHours: Double = 0.0,
    val lastSyncTime: String = "10 mins ago",
    val historyLogs: List<Pair<String, Double>> = listOf(
        "08:00" to 4.1,
        "10:00" to 4.3,
        "12:00" to 4.5,
        "14:00" to 4.2
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColdChainScreen(
    isLoading: Boolean = false,
    onNavigateToHitl: () -> Unit = {},
    onLogExcursionAlert: (String, Double, Double) -> Unit = { _, _, _ -> }
) {
    var refrigerators by remember {
        mutableStateOf(
            listOf(
                RefrigeratorTelemetry(
                    id = "UNIT-01",
                    name = "Central Vaccine Refrigerator",
                    location = "Main Vaccine Storage Room",
                    currentTempCelsius = 4.2,
                    powerSource = "Solar Power System",
                    batteryPct = 94,
                    doorOpenCount = 3,
                    biologics = listOf("Polio (OPV)", "BCG", "Measles"),
                    excursionHours = 0.0
                ),
                RefrigeratorTelemetry(
                    id = "UNIT-02",
                    name = "Dispensary Storage Fridge",
                    location = "Outpatient Clinic Room B",
                    currentTempCelsius = 18.6,
                    powerSource = "Backup Battery",
                    batteryPct = 14,
                    doorOpenCount = 8,
                    biologics = listOf("Polio (OPV)", "Insulin"),
                    excursionHours = 5.5
                ),
                RefrigeratorTelemetry(
                    id = "UNIT-03",
                    name = "Maternity Ward Vaccine Unit",
                    location = "Delivery & Newborn Suite",
                    currentTempCelsius = -1.4,
                    powerSource = "Main Electrical Grid",
                    batteryPct = 100,
                    doorOpenCount = 1,
                    biologics = listOf("Hepatitis B", "Tetanus"),
                    excursionHours = 2.0
                )
            )
        )
    }

    var selectedFridgeForDetail by remember { mutableStateOf<RefrigeratorTelemetry?>(null) }

    var showPotencyCalculator by remember { mutableStateOf(false) }
    var selectedVaccine by remember { mutableStateOf("Polio (OPV)") }
    var calcTemp by remember { mutableStateOf("18.5") }
    var calcDuration by remember { mutableStateOf("5.0") }
    var calcResult by remember { mutableStateOf<VaccineSafetyResult?>(null) }

    var showManualLogDialog by remember { mutableStateOf(false) }
    var manualFridgeId by remember { mutableStateOf("UNIT-01") }
    var manualTempInput by remember { mutableStateOf("4.5") }

    // Hardware Back Handler for Detail View
    if (selectedFridgeForDetail != null) {
        BackHandler {
            selectedFridgeForDetail = null
        }
    }

    AnimatedVisibility(
        visible = selectedFridgeForDetail != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedFridgeForDetail?.let { fridge ->
            RefrigeratorDetailView(
                fridge = fridge,
                onBack = { selectedFridgeForDetail = null },
                onCheckPotency = {
                    selectedVaccine = fridge.biologics.firstOrNull() ?: "Polio (OPV)"
                    calcTemp = fridge.currentTempCelsius.toString()
                    calcDuration = max(1.0, fridge.excursionHours).toString()
                    calcResult = evaluateVaccineSafety(selectedVaccine, fridge.currentTempCelsius, max(1.0, fridge.excursionHours))
                    showPotencyCalculator = true
                }
            )
        }
    }

    AnimatedVisibility(
        visible = selectedFridgeForDetail == null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundMatte)
        ) {
            // Clean Healthcare Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VACCINE REFRIGERATION SAFETY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Cold Storage Units",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = { showPotencyCalculator = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Vaccine Potency Calculator",
                        tint = PrimaryBlue
                    )
                }
            }

            // Simple Status Indicators (Eye friendly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SafePill(
                    label = "SAFE STORAGE",
                    value = "1 Unit",
                    statusColor = StatusNominal,
                    modifier = Modifier.weight(1f)
                )
                SafePill(
                    label = "HIGH TEMPERATURE",
                    value = "1 Warning",
                    statusColor = StatusWarning,
                    modifier = Modifier.weight(1f)
                )
                SafePill(
                    label = "FREEZING RISK",
                    value = "1 Critical",
                    statusColor = StatusCritical,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    items(refrigerators) { fridge ->
                        CleanRefrigeratorCard(
                            telemetry = fridge,
                            onClick = {
                                selectedFridgeForDetail = fridge
                            },
                            onAnalyze = {
                                selectedVaccine = fridge.biologics.firstOrNull() ?: "Polio (OPV)"
                                calcTemp = fridge.currentTempCelsius.toString()
                                calcDuration = max(1.0, fridge.excursionHours).toString()
                                calcResult = evaluateVaccineSafety(
                                    selectedVaccine,
                                    fridge.currentTempCelsius,
                                    max(1.0, fridge.excursionHours)
                                )
                                showPotencyCalculator = true
                            }
                        )
                    }

                    item {
                        Button(
                            onClick = { showManualLogDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceCard,
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "Manual Inspection",
                                modifier = Modifier.size(18.dp),
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Record Temperature Inspection",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    // Vaccine Potency Safety Calculator Dialog (WHO PQS Guideline)
    if (showPotencyCalculator) {
        AlertDialog(
            onDismissRequest = { showPotencyCalculator = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = calcTemp.toDoubleOrNull() ?: 18.0
                        val d = calcDuration.toDoubleOrNull() ?: 5.0
                        calcResult = evaluateVaccineSafety(selectedVaccine, t, d)
                        if (calcResult?.status == "CRITICAL_ACTION") {
                            onLogExcursionAlert(selectedVaccine, t, d)
                        }
                    }
                ) {
                    Text("Check Safety", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPotencyCalculator = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            title = {
                Text(
                    text = "Vaccine Storage Safety Checker",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Checks vaccine potency against WHO cold chain standards (+2°C to +8°C).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    // Vaccine Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Polio (OPV)", "Measles", "BCG", "Insulin", "Hepatitis B").forEach { bio ->
                            val isSel = selectedVaccine == bio
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) PrimaryBlue else SurfaceElevated)
                                    .clickable {
                                        selectedVaccine = bio
                                        val t = calcTemp.toDoubleOrNull() ?: 18.0
                                        val d = calcDuration.toDoubleOrNull() ?: 5.0
                                        calcResult = evaluateVaccineSafety(bio, t, d)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = calcTemp,
                        onValueChange = { calcTemp = it },
                        label = { Text("Fridge Temperature (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = calcDuration,
                        onValueChange = { calcDuration = it },
                        label = { Text("Hours Outside Safe Range") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    calcResult?.let { res ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (res.status == "CRITICAL_ACTION") StatusCritical.copy(alpha = 0.12f)
                                    else if (res.status == "WARNING") StatusWarning.copy(alpha = 0.12f)
                                    else StatusNominal.copy(alpha = 0.12f)
                                )
                                .border(
                                    1.dp,
                                    if (res.status == "CRITICAL_ACTION") StatusCritical
                                    else if (res.status == "WARNING") StatusWarning
                                    else StatusNominal,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = res.statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (res.status == "CRITICAL_ACTION") StatusCritical else if (res.status == "WARNING") StatusWarning else StatusNominal
                                )
                                Text(
                                    text = "Action: ${res.actionRequired}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = res.guidelineReference,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    // Manual Inspection Entry Dialog
    if (showManualLogDialog) {
        AlertDialog(
            onDismissRequest = { showManualLogDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tempVal = manualTempInput.toDoubleOrNull() ?: 4.0
                        refrigerators = refrigerators.map { f ->
                            if (f.id == manualFridgeId) {
                                f.copy(
                                    currentTempCelsius = tempVal,
                                    doorOpenCount = f.doorOpenCount + 1,
                                    excursionHours = if (tempVal > 8.0) 1.0 else 0.0,
                                    lastSyncTime = "Just now"
                                )
                            } else f
                        }
                        showManualLogDialog = false
                    }
                ) {
                    Text("Save Record", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLogDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            title = {
                Text(
                    text = "Log Temperature Inspection",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualFridgeId,
                        onValueChange = { manualFridgeId = it },
                        label = { Text("Fridge Unit ID") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = manualTempInput,
                        onValueChange = { manualTempInput = it },
                        label = { Text("Observed Temperature (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun RefrigeratorDetailView(
    fridge: RefrigeratorTelemetry,
    onBack: () -> Unit,
    onCheckPotency: () -> Unit
) {
    val isNominal = fridge.currentTempCelsius in 2.0..8.0
    val statusColor = if (fridge.currentTempCelsius <= 0.0) StatusCritical else if (isNominal) StatusNominal else StatusWarning
    val statusText = if (fridge.currentTempCelsius <= 0.0) "Freezing Risk" else if (isNominal) "Safe Storage (+2°C to +8°C)" else "Temperature Excursion Alert"

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
                    contentDescription = "Back to Overview",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = fridge.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = fridge.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }

        // Main Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "CURRENT TEMPERATURE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = String.format("%.1f°C", fridge.currentTempCelsius),
                            style = MaterialTheme.typography.displayMedium,
                            color = statusColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = statusText, style = MaterialTheme.typography.labelMedium, color = statusColor)
                    }
                }

                Divider(color = BorderSubtle, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "POWER STATUS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = fridge.powerSource, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Column {
                        Text(text = "BATTERY LEVEL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = "${fridge.batteryPct}% Remaining", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Column {
                        Text(text = "LAST CHECK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = fridge.lastSyncTime, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
        }

        // Stored Vaccines List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "STORED VACCINES & BIOLOGICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                fridge.biologics.forEach { bio ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryBlue))
                            Text(text = bio, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }

                        Text(
                            text = if (bio.contains("Hepatitis") || bio.contains("Insulin")) "Sensitive to Freezing" else "Heat Sensitive",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onCheckPotency,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(imageVector = Icons.Default.Calculate, contentDescription = "Check Safety", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Check Vaccine Potency Safety", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CleanRefrigeratorCard(
    telemetry: RefrigeratorTelemetry,
    onClick: () -> Unit,
    onAnalyze: () -> Unit
) {
    val isNominal = telemetry.currentTempCelsius in 2.0..8.0
    val isFreeze = telemetry.currentTempCelsius <= 0.0
    val statusColor = when {
        isFreeze -> StatusCritical
        isNominal -> StatusNominal
        else -> StatusWarning
    }
    val statusText = when {
        isFreeze -> "Freezing Risk"
        isNominal -> "Safe Range"
        else -> "Excursion Alert"
    }

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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = telemetry.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = telemetry.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = telemetry.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.1f°C", telemetry.currentTempCelsius),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            Divider(color = BorderSubtle, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Power: ${telemetry.powerSource}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Battery: ${telemetry.batteryPct}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "View Details ->",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun SafePill(
    label: String,
    value: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

data class VaccineSafetyResult(
    val vaccine: String,
    val status: String,
    val statusLabel: String,
    val actionRequired: String,
    val guidelineReference: String
)

fun evaluateVaccineSafety(vaccine: String, temp: Double, hours: Double): VaccineSafetyResult {
    val factor = if (vaccine.contains("Polio")) 1.50 else if (vaccine.contains("Measles")) 1.35 else 1.10
    val maxHours = if (vaccine.contains("Polio")) 8.0 else if (vaccine.contains("Measles")) 12.0 else 24.0

    val excessTemp = max(0.0, temp - 8.0)
    val decayRate = exp(0.08 * excessTemp * factor)
    val effectiveHours = hours * decayRate

    val isFreezeSensitive = vaccine.contains("Hepatitis") || vaccine.contains("Insulin") || vaccine.contains("Tetanus")
    val isFrozen = temp <= 0.0 && isFreezeSensitive
    val isThermalDamaged = effectiveHours >= maxHours

    return when {
        isFrozen -> VaccineSafetyResult(
            vaccine = vaccine,
            status = "CRITICAL_ACTION",
            statusLabel = "POTENCY COMPROMISED (FROZEN)",
            actionRequired = "Do not administer. Perform Shake Test immediately. Submit for supervisor disposal review.",
            guidelineReference = "WHO PQS Vaccine Excursion Protocol §3.1"
        )
        isThermalDamaged -> VaccineSafetyResult(
            vaccine = vaccine,
            status = "CRITICAL_ACTION",
            statusLabel = "THERMAL LIMIT EXCEEDED",
            actionRequired = "Move remaining stock to emergency cold box. Submit disposal request to clinical supervisor.",
            guidelineReference = "WHO Immunization Standards E003"
        )
        excessTemp > 0 -> VaccineSafetyResult(
            vaccine = vaccine,
            status = "WARNING",
            statusLabel = "ELEVATED TEMPERATURE WARNING",
            actionRequired = "Inspect physical Vaccine Vial Monitor (VVM) indicator and log hourly readings.",
            guidelineReference = "WHO Cold Chain Guidelines 2023"
        )
        else -> VaccineSafetyResult(
            vaccine = vaccine,
            status = "SAFE",
            statusLabel = "NOMINAL COLD CHAIN CONDITION",
            actionRequired = "Conditions are fully safe. Routine vaccination may proceed.",
            guidelineReference = "WHO PQS Temperature Guide (+2°C to +8°C)"
        )
    }
}
