package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
    val excursionHours: Double = 0.0
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
                    id = "FRIDGE-01",
                    name = "B Medical Systems VLS 024",
                    location = "Kibaha Central Vaccine Depot",
                    currentTempCelsius = 4.2,
                    powerSource = "Solar Direct Drive",
                    batteryPct = 94,
                    doorOpenCount = 3,
                    biologics = listOf("OPV", "BCG", "MEASLES"),
                    excursionHours = 0.0
                ),
                RefrigeratorTelemetry(
                    id = "FRIDGE-02",
                    name = "Dometic TCX 220 Sub-Unit",
                    location = "Dispensary Unit B",
                    currentTempCelsius = 18.6,
                    powerSource = "Battery Inverter",
                    batteryPct = 14,
                    doorOpenCount = 8,
                    biologics = listOf("OPV", "INSULIN"),
                    excursionHours = 5.5
                ),
                RefrigeratorTelemetry(
                    id = "FRIDGE-03",
                    name = "Vestfrost Solar Vaccine Chest",
                    location = "Maternity Ward Cold Storage",
                    currentTempCelsius = -1.4,
                    powerSource = "National Grid",
                    batteryPct = 100,
                    doorOpenCount = 1,
                    biologics = listOf("HEPB", "TETANUS"),
                    excursionHours = 2.0
                )
            )
        )
    }

    var showArrheniusDialog by remember { mutableStateOf(false) }
    var selectedBiologicForCalc by remember { mutableStateOf("OPV") }
    var calcTemp by remember { mutableStateOf("19.0") }
    var calcDuration by remember { mutableStateOf("6.0") }
    var calcResult by remember { mutableStateOf<ArrheniusCalculationResult?>(null) }

    var showManualLogDialog by remember { mutableStateOf(false) }
    var manualFridgeId by remember { mutableStateOf("FRIDGE-01") }
    var manualTempInput by remember { mutableStateOf("4.5") }
    var manualPowerInput by remember { mutableStateOf("Solar") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
    ) {
        // Institutional Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COLD-CHAIN SURVEILLANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "WHO PQS Temperature Guard",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = { showArrheniusDialog = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Arrhenius Calculator",
                    tint = PrimaryBlue
                )
            }
        }

        // Summary Quick Status Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(
                label = "WHO SAFE (+2C to +8C)",
                value = "1 Nominal",
                statusColor = StatusNominal,
                modifier = Modifier.weight(1f)
            )
            StatusPill(
                label = "THERMAL EXCURSION",
                value = "1 High Alert",
                statusColor = StatusWarning,
                modifier = Modifier.weight(1f)
            )
            StatusPill(
                label = "SUB-ZERO FREEZE",
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
                    RefrigeratorCard(
                        telemetry = fridge,
                        onAnalyzeExcursion = {
                            selectedBiologicForCalc = fridge.biologics.firstOrNull() ?: "OPV"
                            calcTemp = fridge.currentTempCelsius.toString()
                            calcDuration = max(1.0, fridge.excursionHours).toString()
                            calcResult = computeArrheniusDecay(
                                selectedBiologicForCalc,
                                fridge.currentTempCelsius,
                                max(1.0, fridge.excursionHours)
                            )
                            showArrheniusDialog = true
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
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Manual Log",
                            modifier = Modifier.size(18.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record Manual Telemetry Inspection",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    // Arrhenius Decay Modal
    if (showArrheniusDialog) {
        AlertDialog(
            onDismissRequest = { showArrheniusDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = calcTemp.toDoubleOrNull() ?: 18.0
                        val d = calcDuration.toDoubleOrNull() ?: 5.0
                        calcResult = computeArrheniusDecay(selectedBiologicForCalc, t, d)
                        if (calcResult?.status == "CRITICAL_EXCURSION") {
                            onLogExcursionAlert(selectedBiologicForCalc, t, d)
                        }
                    }
                ) {
                    Text("Compute Degradation", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArrheniusDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            title = {
                Text(
                    text = "Arrhenius Thermal Decay Model",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Kinetic formula: k = A exp(-Ea / RT). Evaluates potency retention against WHO PQS thresholds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    // Biologic Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("OPV", "MEASLES", "BCG", "INSULIN", "HEPB").forEach { bio ->
                            val isSel = selectedBiologicForCalc == bio
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) PrimaryBlue else SurfaceElevated)
                                    .clickable {
                                        selectedBiologicForCalc = bio
                                        val t = calcTemp.toDoubleOrNull() ?: 18.0
                                        val d = calcDuration.toDoubleOrNull() ?: 5.0
                                        calcResult = computeArrheniusDecay(bio, t, d)
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
                        label = { Text("Temperature (Celsius)") },
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
                        label = { Text("Excursion Duration (Hours)") },
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
                                    if (res.status == "CRITICAL_EXCURSION") StatusCritical.copy(alpha = 0.15f)
                                    else if (res.status == "WARNING") StatusWarning.copy(alpha = 0.15f)
                                    else StatusNominal.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (res.status == "CRITICAL_EXCURSION") StatusCritical
                                    else if (res.status == "WARNING") StatusWarning
                                    else StatusNominal,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "STATUS: ${res.status}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (res.status == "CRITICAL_EXCURSION") StatusCritical else if (res.status == "WARNING") StatusWarning else StatusNominal
                                )
                                Text(
                                    text = "Decay Index: ${res.decayIndex} (Threshold: 1.000)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Action: ${res.actionRequired}",
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

    // Manual Telemetry Input Modal
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
                                    powerSource = manualPowerInput,
                                    doorOpenCount = f.doorOpenCount + 1,
                                    excursionHours = if (tempVal > 8.0) 1.0 else 0.0
                                )
                            } else f
                        }
                        showManualLogDialog = false
                    }
                ) {
                    Text("Record Entry", color = PrimaryBlue)
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
                    text = "Manual Nurse Telemetry Inspection",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualFridgeId,
                        onValueChange = { manualFridgeId = it },
                        label = { Text("Appliance ID") },
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
fun RefrigeratorCard(
    telemetry: RefrigeratorTelemetry,
    onAnalyzeExcursion: () -> Unit = {}
) {
    val isNominal = telemetry.currentTempCelsius in 2.0..8.0
    val isFreezeRisk = telemetry.currentTempCelsius <= 0.0
    val statusColor = when {
        isFreezeRisk -> StatusCritical
        isNominal -> StatusNominal
        else -> StatusWarning
    }
    val statusLabel = when {
        isFreezeRisk -> "FREEZE CRITICAL"
        isNominal -> "WHO NOMINAL"
        else -> "EXCURSION BREACH"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
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

                // Temperature Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.1f°C", telemetry.currentTempCelsius),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            Divider(color = BorderSubtle, thickness = 1.dp)

            // Telemetry Metadata Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "POWER SOURCE", value = telemetry.powerSource)
                MetricItem(label = "BATTERY", value = "${telemetry.batteryPct}%")
                MetricItem(label = "DOOR CYCLES", value = "${telemetry.doorOpenCount} opens")
            }

            // Biologics stored tag chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STOCKS:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                telemetry.biologics.forEach { bio ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!isNominal) {
                    TextButton(
                        onClick = onAnalyzeExcursion,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Analyze Excursion",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary
        )
    }
}

@Composable
private fun StatusPill(
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
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

data class ArrheniusCalculationResult(
    val biologic: String,
    val decayIndex: Double,
    val status: String,
    val actionRequired: String
)

fun computeArrheniusDecay(biologic: String, temp: Double, hours: Double): ArrheniusCalculationResult {
    val factor = when (biologic.uppercase()) {
        "OPV" -> 1.50
        "MEASLES" -> 1.35
        "BCG" -> 1.15
        "INSULIN" -> 1.10
        "HEPB" -> 1.05
        else -> 1.02
    }
    val maxHours = when (biologic.uppercase()) {
        "OPV" -> 8.0
        "MEASLES" -> 12.0
        "BCG" -> 24.0
        "INSULIN" -> 48.0
        else -> 72.0
    }
    val excessTemp = max(0.0, temp - 8.0)
    val decayRate = exp(0.08 * excessTemp * factor)
    val effectiveExposure = hours * decayRate
    val decayIndex = (effectiveExposure / maxHours * 1000).toLong() / 1000.0

    val isCritical = decayIndex >= 1.0 || (temp <= 0.0 && (biologic == "HEPB" || biologic == "INSULIN" || biologic == "TETANUS"))
    val status = if (isCritical) "CRITICAL_EXCURSION" else if (excessTemp > 0) "WARNING" else "NOMINAL"
    val action = if (isCritical) "Emergency cold box deployment and supervisor discard review required"
                 else if (excessTemp > 0) "Monitor closely and inspect physical vaccine vial monitor (VVM)"
                 else "Nominal storage conditions verified"

    return ArrheniusCalculationResult(
        biologic = biologic,
        decayIndex = decayIndex,
        status = status,
        actionRequired = action
    )
}

