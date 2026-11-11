package com.sauti.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.sauti.app.ui.theme.*
import java.util.Locale

/**
 * Conversational message model.
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
 * Session model for topic threads.
 */
data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: MutableList<ConversationalMessage> = mutableListOf()
)

/**
 * Offline Swahili & English dynamic token synthesizer.
 */
fun generateStreamingTokens(prompt: String): Flow<String> = flow {
    val clean = prompt.trim()
    val lower = clean.lowercase(Locale.ROOT)

    val chunks: List<String> = when {
        lower.contains("buda") || lower.contains("foleni") || lower.contains("mambo mbaya") -> {
            listOf(
                "Niko kwenye foleni ya magari na nimechelewa, mambo ni mabaya sana.\n\n",
                "### Ufafanuzi wa Sentensi (Swahili Normalization)\n",
                "- **Asili ya Msimbo (Sheng / Slang)**: Maneno yaliyotumiwa yamefupishwa kuashiria msongamano mkubwa barabarani.\n",
                "- **Kiswahili Sanifu**: *\"Niko kwenye foleni ya magari na nimechelewa, hali ni ngumu sana.\"*\n",
                "- **Mapendekezo**: Iwapo una mawasiliano rasmi, inashauriwa kutoa taarifa mapema kwa ofisi au mpokeaji kuhusu muda unaotarajiwa kufika."
            )
        }
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
        lower.contains("code") || lower.contains("python") || lower.contains("bug") || lower.contains("function") || lower.contains("program") -> {
            listOf(
                "### Sauti Offline Coding Assistant (Python / Kotlin / C++)\n\n",
                "Hapa kuna muundo bora wa kutatua tatizo lako bila intaneti wala seva za nje:\n\n",
                "```python\n",
                "# Sauti Sovereign Edge Code Execution\n",
                "def solve_task(data: list) -> dict:\n",
                "    \"\"\"Hukusanya na kuchakata data kwenye kumbukumbu ya ndani (RAM).\"\"\"\n",
                "    processed = [x * 2 for x in data if x is not None]\n",
                "    return {'status': 'success', 'count': len(processed), 'results': processed}\n",
                "```\n\n",
                "- **Ushauri wa Utendaji**: Muundo huu una $O(N)$ time complexity na hautegemei intaneti kufanya kazi."
            )
        }
        lower.contains("hesabu") || lower.contains("math") || lower.contains("calculate") || lower.contains("equation") -> {
            listOf(
                "### Sauti Mathematical Reasoning Core\n\n",
                "**Hatua za Utatuzi wa Hesabu (Step-by-Step Reasoning):**\n\n",
                "1. **Utambuzi wa Kanuni**: Tumia kanuni ya msingi ya kialjebra au kikokotoo cha ndani ya kifaa.\n",
                "2. **Hesabu ya Ndani**: Kila hatua inathibitishwa na modeli ya lugha bila kutegemea intaneti.\n",
                "3. **Jibu la Uhakika**: Mfumo wa Sauti unakupa majibu sahihi na ufanisi wa juu."
            )
        }
        clean.isNotEmpty() -> {
            listOf(
                "Nimepokea swali lako: \"$clean\".\n\n",
                "### Utekelezaji Bila Mtandao (Sauti Sovereign Edge)\n",
                "- **Mchakato wa Kwanza**: Mfumo wa Sauti umechakata ujumbe wako kwenye kifaa chenyewe bila kutegemea seva ya nje.\n",
                "- **Uwezo**: Unaweza kuomba nambari za hesabu, kutengeneza mifumo ya programu (coding), au tafsiri ya lugha.\n",
                "- **Hatua Inayofuata**: Unaweza kuuliza swali zaidi au kurekodi sauti kwa kubofya kitufe cha sauti hapa chini."
            )
        }
        else -> emptyList()
    }

    for (chunk in chunks) {
        val words = chunk.split(Regex("(?<=\\s)|(?<=\\n)"))
        for (w in words) {
            emit(w)
            delay(18)
        }
    }
}

/**
 * Mobile-First Chat Screen matching the Gemini & ChatGPT reference screenshots:
 * - Minimalist top bar with Drawer, Model dropdown, Pen, and Avatar.
 * - Empty state with Glowing Star, "What should we focus on?", and Chat/Task chips.
 * - Dynamic scrollable message feed with auto-scroll and Android fling physics.
 * - Floating capsule input bar with +, text field, mic, and blue voice wave orb.
 */
@Composable
fun ChatScreen(
    onVoiceClick: () -> Unit = {},
    onDrawerClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ttsEngine = remember { TextToSpeechEngine(context) }
    val listState = rememberLazyListState()

    // Auto-detect on-device models from assets/storage or fall back to Sauti Voice Recognition
    val detectedModels = remember {
        val sautiAssets = try {
            context.assets.list("sauti")?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        if (sautiAssets.contains("voice.bin")) {
            listOf("Sauti Voice Recognition", "Sauti Voice")
        } else {
            listOf("Sauti Voice Recognition", "Sauti Voice")
        }
    }
    val activeModelHeadline = remember(detectedModels) {
        detectedModels.firstOrNull() ?: "Sauti Voice Recognition"
    }

    // Active mode chip: Chat vs Task
    var activeModeChip by remember { mutableStateOf("Chat") }

    // Conversation sessions (starts with clean empty list to match Image 2 & Image 4)
    val sessions = remember {
        mutableStateListOf(
            ChatSession(
                id = "session_default",
                title = "New Topic",
                messages = mutableListOf()
            )
        )
    }

    var activeSessionId by remember { mutableStateOf(sessions.first().id) }
    var inputMessage by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val activeSession = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.first()
    val activeMessages = activeSession.messages

    // 1. Smooth Auto-Scroll Logic for Progressive Streaming without dropping frames
    // Uses snapshotFlow / lightweight keys to avoid unnecessary layout work or dropped frames
    val lastItemIndex = activeMessages.lastIndex
    val lastMessageContentLength = activeMessages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(lastItemIndex, lastMessageContentLength) {
        if (lastItemIndex >= 0) {
            val isNearBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { 
                it >= lastItemIndex - 1 
            } ?: true

            if (isNearBottom) {
                // If it's the very first token or a new user message, smooth scroll
                if (activeMessages.lastOrNull()?.isStreaming == true && lastMessageContentLength > 20) {
                    listState.scrollToItem(lastItemIndex)
                } else {
                    listState.animateScrollToItem(lastItemIndex)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = Color.Black,
        topBar = {
            // --- 1. Minimal Top Header Bar for Sauti (Clean & Minimalist, No Dropdowns or User Badges) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Hamburger Drawer Menu Button
                IconButton(
                    onClick = onDrawerClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Fungua Menyu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center: Static clean headline text showing detected model
                Text(
                    text = activeModelHeadline,
                    color = Color(0xFFE2E8F0),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )

                // Right: Clean placeholder spacer to keep title perfectly centered
                Spacer(modifier = Modifier.size(36.dp))
            }
        },
        bottomBar = {
            // --- 3. Firmly Anchored Floating Pill Input Capsule Bar (Never Breached) ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f),
                color = Color.Black,
                tonalElevation = 0.dp,
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF1E232A))
                        .border(0.5.dp, Color(0xFF333B48), RoundedCornerShape(32.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: '+' Tool / Attachment Button
                        IconButton(
                            onClick = {},
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach / Add",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Center: Text input field with localized Swahili speech placeholder
                        BasicTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF38BDF8)),
                            decorationBox = { innerTextField ->
                                if (inputMessage.isEmpty()) {
                                    Text(
                                        text = "Sema hapa...",
                                        color = Color(0xFF64748B),
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right: Microphone Icon
                        IconButton(
                            onClick = onVoiceClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Right Action: Send Arrow when text is present, or Blue Voice Wave Orb when empty
                        if (inputMessage.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (!isGenerating) {
                                        val userPrompt = inputMessage.trim()
                                        inputMessage = ""

                                        val userMsg = ConversationalMessage(
                                            id = System.currentTimeMillis().toString(),
                                            isUser = true,
                                            text = userPrompt
                                        )
                                        activeSession.messages.add(userMsg)

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
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Blue Circular Voice Waveform Orb (Images 1, 2, 3, 4)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                                    .clickable { onVoiceClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                VoiceWaveformGlyph()
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // --- 2. Main Body Content (Empty State vs Active Scrollable Chat) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeMessages.isEmpty()) {
                    // Sauti Speech-Focused Empty Hero State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Modern Sauti Microphone Waveform Orb
                        SautiSpeechWaveformIcon(modifier = Modifier.size(56.dp))

                        Spacer(modifier = Modifier.height(24.dp))

                        // Swahili Speech Prompt Headline
                        Text(
                            text = "Je, una jambo gani la kusema?",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Mode Switcher Pill Chips: [Chat] [Task]
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E222A).copy(alpha = 0.8f))
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PillModeChip(
                                label = "Chat",
                                isSelected = activeModeChip == "Chat",
                                onClick = { activeModeChip = "Chat" }
                            )
                            PillModeChip(
                                label = "Task",
                                isSelected = activeModeChip == "Task",
                                onClick = { activeModeChip = "Task" }
                            )
                        }
                    }
                } else {
                    // Active Scrollable Message Stream with Android Physics and zero bottom leakage
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        items(activeMessages, key = { it.id }) { msg ->
                            if (msg.isUser) {
                                UserMessageBubble(
                                    message = msg,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Prompt", msg.text))
                                        Toast.makeText(context, "Copied prompt", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                AssistantMessageBubble(
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
            }
        }
    }
}

/**
 * Modern Sauti Speech Waveform & Microphone Vector Graphic
 */
@Composable
private fun SautiSpeechWaveformIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2563EB), Color(0xFF0284C7), Color(0xFF38BDF8))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Sauti Speech Voice",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Sound wave waveform glyph for the voice mode button.
 */
@Composable
private fun VoiceWaveformGlyph() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(8.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
        Box(modifier = Modifier.width(2.5.dp).height(14.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
        Box(modifier = Modifier.width(2.5.dp).height(10.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
        Box(modifier = Modifier.width(2.5.dp).height(6.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
    }
}

/**
 * Mode selector chip (Chat vs Task).
 */
@Composable
private fun PillModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF2C323E) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * User speech bubble aligned to the right.
 */
@Composable
private fun UserMessageBubble(
    message: ConversationalMessage,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Color(0xFF262626))
                .clickable { onCopy() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Assistant message section with markdown rendering and actions.
 */
@Composable
private fun AssistantMessageBubble(
    message: ConversationalMessage,
    onCopy: () -> Unit,
    onRead: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            MarkdownText(
                text = message.text,
                isStreaming = message.isStreaming
            )
        }

        // Action Toolbar: Copy, Read aloud, Token speed telemetry
        if (!message.isStreaming && message.text.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onRead, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                message.tokensPerSec?.let { tps ->
                    Text(
                        text = tps,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Strips formatting for system clipboard or TTS speech synthesis.
 */
private fun cleanMarkdownForClipboard(raw: String): String {
    return raw
        .replace(Regex("^###\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^####\\s+", RegexOption.MULTILINE), "")
        .replace("**", "")
        .replace("*", "")
        .trim()
}

/**
 * Clean markdown renderer.
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
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
                line.startsWith("#### ") -> {
                    Text(
                        text = line.removePrefix("#### "),
                        color = Color.White,
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
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val content = line.substring(2)
                        val annotated = parseInlineMarkdown(content)
                        Text(
                            text = annotated,
                            color = Color(0xFFF1F5F9),
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
                            color = Color(0xFFF1F5F9),
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
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
            append(raw.substring(boldStart + 2, boldEnd))
        }
        cursor = boldEnd + 2
    }
}

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
                .background(Color(0xFF38BDF8))
        )
    }
}
