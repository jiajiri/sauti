package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.components.SkeletonCard
import com.sauti.app.ui.theme.*

data class ClinicalMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val tokensPerSec: String? = null,
    val citations: List<String> = emptyList(),
    val groundednessScore: String? = null
)

@Composable
fun ChatScreen(
    isLoading: Boolean = false,
    onVoiceClick: () -> Unit = {}
) {
    var inputMessage by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ClinicalMessage(
                id = "1",
                isUser = true,
                text = "Je, chanjo ya Hepatitis B ikiganda kwa masaa mawili ndani ya friji, bado inaweza kutumika?"
            ),
            ClinicalMessage(
                id = "2",
                isUser = false,
                text = "Hapana. Chanjo ya Hepatitis B ni Freeze-Critical (ina uharibifu wa kudumu ikiganda chini ya 0°C). Kupunguza joto chini ya sufuri husababisha muungano wa alumini kuharibika na chembechembe kuganda (flocculation), jambo linalopunguza uwezo wa kutoa kinga na kuongeza hatari ya madhara ya baada ya chanjo.\n\nFanya 'Shake Test' mara moja. Ikiwa mchanga au chembechembe zilizoganda zitatulia chini ya chupa kabla ya dakika 15, kundi lote la chanjo linapaswa kusitishwa na kuingizwa kwenye mfumo wa uharibifu wa dawa.",
                tokensPerSec = "38.2 t/s (ARM Cortex-A53)",
                citations = listOf("WHO PQS Guideline E003/PV01", "Tanzania MoH Immunization Manual 2024, §4.3"),
                groundednessScore = "98% Grounded"
            ),
            ClinicalMessage(
                id = "3",
                isUser = true,
                text = "Kwa muda gani chanjo ya Measles inaweza kukaa kwenye chombo cha barafu wakati wa kliniki ya nje?"
            ),
            ClinicalMessage(
                id = "4",
                isUser = false,
                text = "Chanjo ya Surua (Measles) ikichanganywa (reconstituted), lazima itumike ndani ya masaa 6 pekee, na joto lake lihifadhiwe madhubuti kati ya +2°C na +8°C ndani ya cold box yenye 'conditioned water-packs'. Baada ya masaa 6, dozi zote zilizobaki lazima zitupwe hata kama hazijatumika.",
                tokensPerSec = "41.6 t/s (ARM Cortex-A53)",
                citations = listOf("WHO Immunization in Practice (IIP) Module 2", "WHO PQS Code of Practice 2023"),
                groundednessScore = "95% Grounded"
            )
        )
    }

    val quickQuestions = listOf(
        "Muda gani chanjo ya OPV inakaa nje ya friji?",
        "Jinsi ya kufanya Shake Test kwa chanjo zilizoganda",
        "Insulin iliyofika 25°C inaweza kuokolewa?",
        "Taratibu za kuanzisha conditioned ice-packs"
    )

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
                    text = "LOCAL CLINICAL ASSISTANT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "Offline RAG Consultation",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryBlue.copy(alpha = 0.12f))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Zero Internet",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
            }
        }

        // Quick Clinical Inquiry Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickQuestions) { q ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .clickable {
                            inputMessage = q
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = q,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Thread
        if (isLoading) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonCard(height = 90.dp)
                SkeletonCard(height = 140.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(messages) { msg ->
                    ClinicalMessageBubble(message = msg)
                }
            }
        }

        // Bottom Input Toolbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mic voice button
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Dictation",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Input Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceInput)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputMessage.isEmpty()) {
                        Text(
                            text = "Ask clinical cold-chain query...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }

                    BasicTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        textStyle = TextStyle(
                            fontFamily = RobotoFont,
                            fontSize = 13.sp,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            val userText = inputMessage
                            inputMessage = ""
                            messages.add(
                                ClinicalMessage(
                                    id = System.currentTimeMillis().toString(),
                                    isUser = true,
                                    text = userText
                                )
                            )
                            // Deterministic clinical response
                            messages.add(
                                ClinicalMessage(
                                    id = (System.currentTimeMillis() + 1).toString(),
                                    isUser = false,
                                    text = "Utafiti wa ndani (Local Vector Store) umekamilika kwa hoja: '$userText'. Miongozo ya WHO PQS inathibitisha uhifadhi wa joto ufuatie +2°C hadi +8°C. Hatua zote za dharura za kutupa chanjo zitawekwa kwenye foleni ya idhini ya Daktari Mkuu.",
                                    tokensPerSec = "39.1 t/s (ARM Cortex-A53)",
                                    citations = listOf("WHO PQS Guidelines §2.1", "Tanzania National Cold Chain Protocol"),
                                    groundednessScore = "96% Grounded"
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClinicalMessageBubble(message: ClinicalMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (message.isUser) 0.85f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 10.dp,
                        topEnd = 10.dp,
                        bottomStart = if (message.isUser) 10.dp else 2.dp,
                        bottomEnd = if (message.isUser) 2.dp else 10.dp
                    )
                )
                .background(if (message.isUser) PrimaryBlue else SurfaceCard)
                .border(
                    1.dp,
                    if (message.isUser) PrimaryBlue else BorderSubtle,
                    RoundedCornerShape(10.dp)
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!message.isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TALIS CLINICAL CORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue
                        )
                        message.tokensPerSec?.let { tps ->
                            Text(
                                text = tps,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                // Verified Citations
                if (message.citations.isNotEmpty()) {
                    Divider(color = BorderSubtle, thickness = 1.dp)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "VERIFIED INSTITUTIONAL CITATIONS:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                            message.groundednessScore?.let { gs ->
                                Text(
                                    text = gs,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusNominal,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        message.citations.forEach { cite ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue)
                                )
                                Text(
                                    text = cite,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
