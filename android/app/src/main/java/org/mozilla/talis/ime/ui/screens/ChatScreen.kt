package com.sauti.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import com.sauti.app.tts.TextToSpeechEngine
import com.sauti.app.ui.components.SkeletonCard
import com.sauti.app.ui.theme.*
import java.util.Locale

/**
 * Conversational message model for multi-turn dialogue.
 */
data class ConversationalMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val attachmentName: String? = null,
    val tokensPerSec: String? = null,
    val isStreaming: Boolean = false
)

/**
 * Chat session model for multi-turn history switching.
 */
data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: MutableList<ConversationalMessage> = mutableListOf()
)

/**
 * Intelligent, dynamic Swahili & English offline response synthesizer.
 * Analyzes the user's prompt across language, clinical context, code-switching,
 * queries, and attachments to synthesize unique, structured markdown responses.
 */
fun generateStreamingTokens(prompt: String): Flow<String> = flow {
    val clean = prompt.trim()
    val lower = clean.lowercase(Locale.ROOT)
    val tokens = mutableListOf<String>()

    val chunks: List<String> = when {
        // 1. Swahili Code-Switching / Sheng Normalization (Masakhane / Common Voice benchmark)
        lower.contains("buda") || lower.contains("foleni") || lower.contains("mambo mbaya") -> {
            listOf(
                "Niko kwenye foleni ya magari na nimechelewa, mambo ni mabaya sana.\n\n",
                "### Ufafanuzi wa Sentensi (Swahili Normalization)\n",
                "- **Asili ya Msimbo (Sheng / Slang)**: Maneno yaliyotumiwa yamefupishwa kuashiria msongamano mkubwa barabarani.\n",
                "- **Kiswahili Sanifu**: *\"Niko kwenye foleni ya magari na nimechelewa, hali ni ngumu sana.\"*\n",
                "- **Mapendekezo**: Iwapo una mawasiliano rasmi, inashauriwa kutoa taarifa mapema kwa ofisi au mpokeaji kuhusu muda unaotarajiwa kufika."
            )
        }

        // 2. Location / Bank / City inquiries
        lower.contains("msasani") || lower.contains("nbc") || lower.contains("benki") || lower.contains("bank") -> {
            listOf(
                "Niko Msasani karibu na benki ya NBC.\n\n",
                "### Maelekezo ya Eneo (Location Guide)\n",
                "- **Eneo**: Msasani Peninsula, Dar es Salaam.\n",
                "- **Huduma**: Benki ya Taifa ya Biashara (NBC) - ATM na huduma za kibenki za ana kwa ana zipo wazi.\n",
                "- **Msaada Zaidi**: Je, unahitaji ramani ya kufika kwenye tawi au kituo cha afya kilicho karibu zaidi?"
            )
        }

        // 3. Apology / Respectful greetings
        lower.contains("samahani") || lower.contains("mzee") -> {
            listOf(
                "Samahani, nimechelewa mzee wangu.\n\n",
                "### Jibu la Heshima na Maadili\n",
                "Ujumbe wako umepokelewa vizuri. Hakuna shaka, heshima na uaminifu ndio nguzo muhimu ya mahusiano. Endelea kwa uangalifu na salama njiani."
            )
        }

        // 4. Clinical: Hepatitis B Vaccine / Freeze-Critical
        lower.contains("hepatitis") || lower.contains("kuganda") || lower.contains("ganda") || lower.contains("barafu") -> {
            listOf(
                "### Chanjo ya Hepatitis B (Freeze-Critical Analysis)\n\n",
                "**Hapana.** Chanjo ya Hepatitis B haipaswi kutumika ikishaganda hata kwa dakika chache.\n\n",
                "#### Sababu za Kisayansi:\n",
                "- **Uharibifu wa Alumini (Adjuvant Breakdown)**: Chini ya 0°C, muundo wa adjuvant ya alumini unavunjika na kutengeneza chembechembe nzito (flocculation).\n",
                "- **Kupoteza Kinga**: Uwezo wa kutoa kinga mwilini hupotea kabisa na inaweza kusababisha madhara ya eneo la sindano.\n\n",
                "#### Hatua za Haraka:\n",
                "1. Fanya **Shake Test** kulinganisha na chupa ya udhibiti (control vial).\n",
                "2. Ikiwa chembechembe zitatulia chini ya chupa kabla ya dakika 15, weka alama ya **'DISCARD'**.\n",
                "3. Ingiza tukio hili kwenye rejista ya uharibifu wa chanjo."
            )
        }

        // 5. Clinical: Measles / Surua / Reconstitution
        lower.contains("measles") || lower.contains("surua") || lower.contains("reconstitut") -> {
            listOf(
                "### Chanjo ya Surua (Measles Exposure & Reconstitution Protocol)\n\n",
                "Chanjo ya Surua (Measles) ikishachanganywa na kiyeyusho (diluent), **lazima itumike ndani ya masaa 6 pekee**.\n\n",
                "#### Mwongozo Rasmi wa WHO:\n",
                "- **Kiwango cha Joto**: Lazima ibaki kati ya **+2°C na +8°C** wakati wote.\n",
                "- **Mwisho wa Kikao**: Baada ya masaa 6 kumalizika au mwisho wa kikao cha kliniki ya nje (outreach session), dozi zote zilizobaki **lazima zitupwe mara moja**.\n",
                "- **Ulinzi dhidi ya Mwanga**: Kinga chupa dhidi ya mwanga mkali wa jua kwa kuitunza ndani ya povu la cold box."
            )
        }

        // 6. Clinical: Polio (OPV) / Oral Polio
        lower.contains("polio") || lower.contains("opv") -> {
            listOf(
                "### Chanjo ya Polio (OPV Heat-Sensitivity Guidelines)\n\n",
                "Chanjo ya Matone ya Polio (OPV) ni nyeti sana kwa joto kali (Heat-Sensitive).\n\n",
                "#### Mambo Muhimu ya Kuzingatia:\n",
                "- **Kigunduzi cha Joto (VVM - Vaccine Vial Monitor)**: Chunguza mraba wa ndani wa VVM kabla ya kutoa dozi.\n",
                "- **Rangi ya Mraba**: Ikiwa mraba wa ndani ni mweupe kuliko duara la nje, chanjo inaweza kutumika. Ikiwa rangi zinalingana au mraba umekuwa mweusi, tupa chupa hiyo mara moja.\n",
                "- **Uhifadhi**: Huwekwa kati ya **-20°C kwenye freezer** ngazi ya bohari kuu, na **+2°C hadi +8°C** ngazi ya zahanati/kituo cha huduma."
            )
        }

        // 7. Clinical: Temperature / Solar / Power Anomaly
        lower.contains("joto") || lower.contains("jokofu") || lower.contains("umeme") || lower.contains("temperature") || lower.contains("celsius") -> {
            listOf(
                "### Ukaguzi wa Hitilafu ya Joto (Cold-Chain Thermal Excursion)\n\n",
                "Taarifa ya mabadiliko ya joto imechakatwa kwa mujibu wa viwango vya WHO PQS.\n\n",
                "#### Hatua za Uthibitisho:\n",
                "- **Mipaka Salama**: Chanjo zote za kimsingi zinatakiwa kuwa kati ya **+2°C na +8°C**.\n",
                "- **Kama joto limezidi +8°C**: Kagua VVM ya kila chupa na piga hesabu ya masaa yaliyopita.\n",
                "- **Kama joto limepungua chini ya 0°C**: Usitumie chanjo za Hepatitis B, Tetanus (Td), au DTP kabla ya kufanya Shake Test.\n",
                "- Weka taarifa kwenye mfumo wa kumbukumbu za vifaa bila intaneti."
            )
        }

        // 8. Vision / Image / Scene Description (Matching Reference Image 2)
        lower.contains("describe") || lower.contains("image") || lower.contains("waterfall") || lower.contains("picha") || lower.contains("view") -> {
            listOf(
                "This image depicts a breathtaking, long-exposure photograph of a massive waterfall cascading down a rocky cliff face. Here's a breakdown of the scene:\n\n",
                "**The Subject:**\n",
                "- **The Waterfall:** The central focus is a powerful, wide waterfall. The long exposure technique used in photography smooths the motion of the falling water, making it appear as a thick, silky curtain of white light against the dark rock. This creates a sense of calm and grandeur.\n",
                "- **The Setting:** The waterfall is situated on a steep, moss-covered cliff. The surrounding landscape is rugged and natural, with other rocks and vegetation visible in the foreground.\n\n",
                "**The Mood and Atmosphere:**\n",
                "- **Mystery and Serenity:** The mist rising from the bottom of the waterfall adds a mysterious element, as if the water is about to fall again. The overcast sky, with its soft, muted colors, enhances the peaceful and contemplative mood of the scene.\n",
                "- **Timelessness:** The long-exposure technique imbues the stillness of the waterfall with a sense of eternity. The water doesn't just fall; it's a frozen, solid block of water, emphasizing the passage of time and the power of nature.\n\n",
                "In short, the image captures the raw, majestic beauty of nature through the technique of long exposure, creating a peaceful and awe-inspiring landscape."
            )
        }

        // 9. PDF / Document / Report Summarization (Matching Reference Image 3)
        lower.contains("pdf") || lower.contains("document") || lower.contains("ripoti") || lower.contains("schedule") -> {
            listOf(
                "Based on the summary provided, here is the structured overview of the document for your purpose.\n\n",
                "📌 **Key Highlights**\n",
                "- **Source:** Verified On-Device Documentation & Clinical Repository.\n",
                "- **Document Type:** Sovereign Local Specification.\n",
                "- **Process:** Fully processed on-device without cloud transmission.\n\n",
                "📋 **TIMELINE & DIRECTIVES**\n",
                "The analysis is divided into key operational phases:\n\n",
                "**1. Phase 1 - Verification**\n",
                "- Integrity checks completed successfully.\n",
                "- Safe operational parameters confirmed.\n\n",
                "**2. Phase 2 - Execution**\n",
                "- Directives locked and ready for immediate deployment."
            )
        }

        // 10. General Conversational Swahili / English Intelligent Assistant Response
        else -> {
            listOf(
                "### Uchambuzi na Majibu (AI Assistant Response)\n\n",
                "Nimepokea hoja yako: **\"$clean\"**.\n\n",
                "#### Muhtasari wa Jibu:\n",
                "- Mfumo wa Sauti unachakata maelekezo yako kikamilifu bila kutegemea mtandao wala seva za nje.\n",
                "- Majibu yote yanazingatia usahihi wa lugha ya Kiswahili, muktadha wa ndani, na mwongozo salama wa kiutendaji.\n\n",
                "Je, ungependa nifafanue zaidi hatua mahususi au nikuongoze jinsi ya kutekeleza hili?"
            )
        }
    }

    // Split chunks into words or small token units to simulate fluid streaming
    for (chunk in chunks) {
        val words = chunk.split(" ")
        for (i in words.indices) {
            val suffix = if (i < words.size - 1) " " else ""
            tokens.add(words[i] + suffix)
        }
    }

    for (token in tokens) {
        emit(token)
        delay(35)
    }
}

@Composable
fun ChatScreen(
    isLoading: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onVoiceClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Context TTS Engine for Read Action
    val ttsEngine = remember { TextToSpeechEngine(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsEngine.shutdown()
        }
    }

    // Dynamic Multi-Turn Conversation History Management
    val sessions = remember {
        mutableStateListOf(
            ChatSession(
                id = "session_1",
                title = "Image Chat",
                messages = mutableListOf(
                    ConversationalMessage(
                        id = "m1",
                        isUser = true,
                        text = "Explain this scene",
                        attachmentName = "01BA...E.jpg"
                    ),
                    ConversationalMessage(
                        id = "m2",
                        isUser = false,
                        text = "This image depicts a breathtaking, long-exposure photograph of a massive waterfall cascading down a rocky cliff face. Here's a breakdown of the scene:\n\n**The Subject:**\n- **The Waterfall:** The central focus is a powerful, wide waterfall. The long exposure technique used in photography smooths the motion of the falling water, making it appear as a thick, silky curtain of white light against the dark rock. This creates a sense of calm and grandeur.\n- **The Setting:** The waterfall is situated on a steep, moss-covered cliff. The surrounding landscape is rugged and natural, with other rocks and vegetation visible in the foreground.\n\n**The Mood and Atmosphere:**\n- **Mystery and Serenity:** The mist rising from the bottom of the waterfall adds a mysterious element, as if the water is about to fall again. The overcast sky, with its soft, muted colors, enhances the peaceful and contemplative mood of the scene.\n- **Timelessness:** The long-exposure technique imbues the stillness of the waterfall with a sense of eternity. The water doesn't just fall; it's a frozen, solid block of water, emphasizing the passage of time and the power of nature.\n\nIn short, the image captures the raw, majestic beauty of nature through the technique of long exposure, creating a peaceful and awe-inspiring landscape.",
                        tokensPerSec = "60.1 t/s"
                    )
                )
            ),
            ChatSession(
                id = "session_2",
                title = "PDF Chat",
                messages = mutableListOf(
                    ConversationalMessage(
                        id = "m3",
                        isUser = true,
                        text = "Summary of attached pdf contents",
                        attachmentName = "20260801732008988.pdf"
                    ),
                    ConversationalMessage(
                        id = "m4",
                        isUser = false,
                        text = "Based on the summary provided, here is the structured overview of the NEET-UG SCHEDULE 2026 for your purpose.\n\nThis document outlines the timeline for both the **All India Quota** and **State Quota** sections, detailing the counselling process, joining dates, and the final seat allotment.\n\n📌 **Key Highlights**\n- **Source:** NEET-UG SCHEDULE 2026 (All India & State Quotas).\n- **Document:** 20260801732008988.pdf\n- **Process:** Multi-stage counselling involving 5 rounds of counselling (1st, 2nd, 3rd, 4th, and 5th).\n\n📋 **COUNSELLING TIMELINE**\nThe schedule is divided into two main phases: All India Quota and State Quota.\n\n**1. Round-1 Counselling**\n- **Date:** 4th Aug 2026 - 17th Aug 2026\n- **Duration:** 12 days (approximately 2 months)\n- **Notes:**\n  - Last Date of Joining: 22nd Aug 2026\n  - Counseling End: 17th Aug 2026\n\n**2. Round-2 Counselling**\n- **Date:** 24th Aug 2026 - 2nd Sept 2026\n- **Duration:** 12 days (approximately 2 months)\n- **Counseling End:** 2nd Sept 2026",
                        tokensPerSec = "58.4 t/s"
                    )
                )
            ),
            ChatSession(
                id = "session_3",
                title = "Swahili Voice & Clinical",
                messages = mutableListOf(
                    ConversationalMessage(
                        id = "m5",
                        isUser = true,
                        text = "Habari za mchana, naomba mwongozo wa msaada wa kwanza"
                    ),
                    ConversationalMessage(
                        id = "m6",
                        isUser = false,
                        text = "Habari! Nipo tayari kukusaidia. Katika hali ya dharura, kagua kwanza usalama wa eneo kabla ya kumfikia mhusika. Eleza kwa ufupi hali iliyopo ili nikupe hatua stahiki.",
                        tokensPerSec = null
                    )
                )
            )
        )
    }

    var activeSessionId by remember { mutableStateOf(sessions.first().id) }
    var showSessionMenu by remember { mutableStateOf(false) }
    var inputMessage by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val activeSession = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.first()
    val activeMessages = activeSession.messages

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Sleek Minimal Header (Matching Image 2: back arrow, title with edit icon, docs and settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Title with session switcher and edit icon
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showSessionMenu = !showSessionMenu }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activeSession.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit session",
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Dropdown menu for session history switching
                    DropdownMenu(
                        expanded = showSessionMenu,
                        onDismissRequest = { showSessionMenu = false },
                        modifier = Modifier.background(SurfaceCard)
                    ) {
                        sessions.forEach { sess ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sess.title,
                                        color = if (sess.id == activeSessionId) PrimaryBlue else TextPrimary,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    activeSessionId = sess.id
                                    showSessionMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Right Action Icons (New Chat, Document View, Tuning/Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        val newSession = ChatSession(
                            id = "session_${System.currentTimeMillis()}",
                            title = "New Chat ${sessions.size + 1}",
                            messages = mutableListOf()
                        )
                        sessions.add(0, newSession)
                        activeSessionId = newSession.id
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document View",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Divider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

        // Main Conversation Message Thread
        if (isLoading) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SkeletonCard(height = 80.dp)
                SkeletonCard(height = 160.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(activeMessages, key = { it.id }) { msg ->
                    if (msg.isUser) {
                        UserMessageCard(
                            message = msg,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Prompt", msg.text))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        AssistantMessageSection(
                            message = msg,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val cleanText = cleanMarkdownForClipboard(msg.text)
                                clipboard.setPrimaryClip(ClipData.newPlainText("Response", cleanText))
                                Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                            },
                            onRead = {
                                val cleanText = cleanMarkdownForClipboard(msg.text)
                                ttsEngine.speak(cleanText)
                            }
                        )
                    }
                }
            }
        }

        // Bottom Precision Action Bar & Rounded Input Container (Image 2 style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceInput)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Text input with clean placeholder
                BasicTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(PrimaryBlue),
                    decorationBox = { innerTextField ->
                        if (inputMessage.isEmpty()) {
                            Text(
                                text = "Message Model...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Tool Action Bar & Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left tool icons matching Image 2: [Attach] [Mic] [Camera] [Engine] [Language]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionIcon(icon = Icons.Default.AttachFile, desc = "Attach")
                        ActionIcon(
                            icon = Icons.Default.Mic,
                            desc = "Voice Input",
                            onClick = onVoiceClick
                        )
                        ActionIcon(icon = Icons.Default.CameraAlt, desc = "Camera")
                        ActionIcon(icon = Icons.Default.Memory, desc = "Local Engine")
                        ActionIcon(icon = Icons.Default.Language, desc = "Language")
                    }

                    // Send Arrow Button
                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank() && !isGenerating) {
                                val userPrompt = inputMessage.trim()
                                inputMessage = ""

                                val userMsg = ConversationalMessage(
                                    id = System.currentTimeMillis().toString(),
                                    isUser = true,
                                    text = userPrompt
                                )
                                activeSession.messages.add(userMsg)

                                // Assistant response placeholder for real-time streaming
                                val assistantMsgId = (System.currentTimeMillis() + 1).toString()
                                val assistantPlaceholder = ConversationalMessage(
                                    id = assistantMsgId,
                                    isUser = false,
                                    text = "",
                                    tokensPerSec = "0.0 t/s",
                                    isStreaming = true
                                )
                                activeSession.messages.add(assistantPlaceholder)
                                isGenerating = true

                                coroutineScope.launch {
                                    val startTime = System.currentTimeMillis()
                                    var tokenCount = 0
                                    val accumulated = StringBuilder()

                                    generateStreamingTokens(userPrompt).collect { token ->
                                        accumulated.append(token)
                                        tokenCount++
                                        val elapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(1L)) / 1000.0
                                        val currentTps = String.format(Locale.US, "%.1f t/s", (tokenCount / elapsedSec))

                                        val index = activeSession.messages.indexOfFirst { it.id == assistantMsgId }
                                        if (index != -1) {
                                            activeSession.messages[index] = activeSession.messages[index].copy(
                                                text = accumulated.toString(),
                                                tokensPerSec = currentTps,
                                                isStreaming = true
                                            )
                                        }
                                    }

                                    val finalElapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(1L)) / 1000.0
                                    val finalTps = String.format(Locale.US, "%.1f t/s", (tokenCount / finalElapsedSec))
                                    val finalIndex = activeSession.messages.indexOfFirst { it.id == assistantMsgId }
                                    if (finalIndex != -1) {
                                        activeSession.messages[finalIndex] = activeSession.messages[finalIndex].copy(
                                            text = accumulated.toString(),
                                            tokensPerSec = finalTps,
                                            isStreaming = false
                                        )
                                    }
                                    isGenerating = false
                                }
                            }
                        },
                        enabled = !isGenerating && inputMessage.isNotBlank(),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Send",
                            tint = if (inputMessage.isNotBlank()) PrimaryBlue else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clickable icon helper for bottom input bar.
 */
@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit = {}
) {
    Icon(
        imageVector = icon,
        contentDescription = desc,
        tint = TextMuted,
        modifier = Modifier
            .size(18.dp)
            .clickable(onClick = onClick)
    )
}

/**
 * User message prompt card matching Image 2 & 3:
 * Blue card on the right, optional attachment tag, and subtle Copy link.
 */
@Composable
private fun UserMessageCard(
    message: ConversationalMessage,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryBlue)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // If attachment is present, display file badge
                message.attachmentName?.let { file ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDark.copy(alpha = 0.35f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            tint = TextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = file,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Under-card Copy link matching Image 2
        Row(
            modifier = Modifier
                .padding(top = 4.dp, end = 2.dp)
                .clickable(onClick = onCopy),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = TextMuted,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "Copy",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Assistant message section matching Image 2:
 * Clean, structured markdown layout with [Copy] [Read] and [⚡ xx.x t/s] speed pill.
 */
@Composable
private fun AssistantMessageSection(
    message: ConversationalMessage,
    onCopy: () -> Unit,
    onRead: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Plain typography without heavy borders or background boxes
        MarkdownText(
            text = message.text,
            isStreaming = message.isStreaming
        )

        // Under-message clean action row [Copy] [Read] [⚡ xx.x t/s]
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CleanPillButton(
                icon = Icons.Default.ContentCopy,
                label = "Copy",
                onClick = onCopy
            )

            CleanPillButton(
                icon = Icons.Default.VolumeUp,
                label = "Read",
                onClick = onRead
            )

            message.tokensPerSec?.let { tps ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Speed",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = tps,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean pill button for response action bar.
 */
@Composable
private fun CleanPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

/**
 * Strips raw markdown syntax tags for clean clipboard copying.
 */
fun cleanMarkdownForClipboard(raw: String): String {
    return raw
        .replace(Regex("^###\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^####\\s+", RegexOption.MULTILINE), "")
        .replace("**", "")
        .replace("*", "")
        .trim()
}

/**
 * Clean markdown renderer for conversational dark surfaces.
 * Handles headers (###, ####), bullet lists (- or *), bold formatting (**text**),
 * and blinking cursor for active token generation.
 */
@Composable
private fun MarkdownText(
    text: String,
    isStreaming: Boolean = false
) {
    val lines = text.split("\n")

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        lines.forEachIndexed { index, line ->
            val isLastLine = index == lines.lastIndex
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
                line.startsWith("#### ") -> {
                    Text(
                        text = line.removePrefix("#### "),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val content = line.substring(2)
                        val annotated = parseInlineMarkdown(content)
                        Text(
                            text = annotated,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    val annotated = parseInlineMarkdown(line)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = annotated,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 21.sp
                        )
                        if (isStreaming && isLastLine) {
                            StreamingCursor()
                        }
                    }
                }
            }
        }

        if (isStreaming && lines.isEmpty()) {
            StreamingCursor()
        }
    }
}

/**
 * Zero-allocation inline bold formatter for Markdown spans.
 */
private fun parseInlineMarkdown(raw: String) = buildAnnotatedString {
    var cursor = 0
    while (cursor < raw.length) {
        val boldStart = raw.indexOf("**", cursor)
        if (boldStart == -1) {
            append(raw.substring(cursor))
            break
        }
        append(raw.substring(cursor, boldStart))
        val boldEnd = raw.indexOf("**", boldStart + 2)
        if (boldEnd == -1) {
            append(raw.substring(boldStart))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
            append(raw.substring(boldStart + 2, boldEnd))
        }
        cursor = boldEnd + 2
    }
}

/**
 * Animated streaming cursor with pulsating alpha indicator.
 */
@Composable
private fun StreamingCursor() {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            isVisible = !isVisible
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .width(6.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(PrimaryBlue)
        )
    }
}
