package com.sauti.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.tts.TextToSpeechEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale

// ── Obsidian Pure Black AMOLED Tokens ────────────────────────────────────────
private val CanvasBlack  = Color(0xFF000000)
private val ChipDarkFill = Color(0xFF1E1E1E)
private val PillInputBg  = Color(0xFF1E1E1E)
private val AccentBlue   = Color(0xFF2563EB)
private val TextWhite    = Color(0xFFFFFFFF)
private val TextMuted    = Color(0xFF9E9E9E)
private val BtnWhite     = Color(0xFFFFFFFF)

data class ConversationalMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val tokensPerSec: String = "",
    val isStreaming: Boolean = false
)

fun generateStreamingTokens(prompt: String): Flow<String> {
    return com.sauti.app.sauti.SautiEngineLoader.generateStreamingResponse(prompt)
}

/**
 * Main Chat & Home Screen strictly modeled after Reference 6.
 * Features the "Put Sauti to work" centerpiece with two staggered chip rows,
 * white CTA button, and docked pill composer with live-waveform button.
 */
@Composable
fun ChatScreen(
    onVoiceClick: () -> Unit = {},
    onDrawerClick: () -> Unit = {},
    viewModel: com.sauti.app.viewmodel.SautiNavigationViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val ttsEngine = remember { TextToSpeechEngine(context) }

    val activeSessionId by viewModel?.activeSessionId?.collectAsState() ?: remember { mutableStateOf(null) }
    val activeSessionTitle by viewModel?.activeSessionTitle?.collectAsState() ?: remember { mutableStateOf("New Chat") }
    val persistedMessages by viewModel?.activeMessages?.collectAsState() ?: remember { mutableStateOf(emptyList<com.sauti.app.data.local.ChatMessage>()) }

    var streamingMessage by remember { mutableStateOf<ConversationalMessage?>(null) }
    var inputMessage by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val displayMessages = remember(persistedMessages, streamingMessage) {
        val list = persistedMessages.map { dbMsg ->
            ConversationalMessage(
                id = dbMsg.id,
                isUser = dbMsg.isUser,
                text = dbMsg.text,
                tokensPerSec = dbMsg.tokensPerSec,
                isStreaming = false
            )
        }.toMutableList()

        if (streamingMessage != null) {
            list.add(streamingMessage!!)
        }
        list
    }

    val lastItemIndex = displayMessages.lastIndex
    val lastMessageContentLength = displayMessages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(lastItemIndex, lastMessageContentLength) {
        if (lastItemIndex >= 0) {
            listState.animateScrollToItem(lastItemIndex)
        }
    }

    fun submitPrompt(promptText: String) {
        if (!isGenerating && promptText.isNotBlank()) {
            val userPrompt = promptText.trim()
            inputMessage = ""

            val currentSId = activeSessionId ?: "default"
            viewModel?.updateSessionTitleIfDefault(currentSId, userPrompt)
            viewModel?.saveMessage(currentSId, isUser = true, text = userPrompt)

            val assistantMsgId = (System.currentTimeMillis() + 1).toString()
            streamingMessage = ConversationalMessage(
                id = assistantMsgId,
                isUser = false,
                text = "",
                tokensPerSec = "0.0 t/s",
                isStreaming = true
            )
            isGenerating = true

            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                var tokenCount = 0
                val accumulated = StringBuilder()

                generateStreamingTokens(userPrompt).collect { token ->
                    accumulated.append(token)
                    tokenCount++
                    val elapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(1L)) / 1000.0
                    val currentTps = String.format(Locale.US, "%.1f t/s", (tokenCount / elapsedSec))

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        streamingMessage = streamingMessage?.copy(
                            text = accumulated.toString(),
                            tokensPerSec = currentTps,
                            isStreaming = true
                        )
                    }
                }

                val finalElapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(1L)) / 1000.0
                val finalTps = String.format(Locale.US, "%.1f t/s", (tokenCount / finalElapsedSec))
                val finalResponse = accumulated.toString()

                viewModel?.saveMessage(currentSId, isUser = false, text = finalResponse, tokensPerSec = finalTps)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    streamingMessage = null
                    isGenerating = false
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
        containerColor = CanvasBlack,
        topBar = {
            // Top App Bar (Reference 6): Circular menu button, pill badge, circular new chat action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular menu button
                IconButton(
                    onClick = onDrawerClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ChipDarkFill)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center subtle pill badge ("Sauti")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ChipDarkFill,
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (displayMessages.isEmpty()) "Sauti" else activeSessionTitle,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Circular new chat action button
                IconButton(
                    onClick = { viewModel?.createNewSession() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ChipDarkFill)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "New Chat",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            // Docked Pill Composer Bar (Reference 6)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pill Container
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = PillInputBg,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Leading '+' attachment button
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Attach",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { /* Attachment trigger */ }
                        )

                        // Center Input Field
                        BasicTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Default
                            ),
                            cursorBrush = SolidColor(AccentBlue),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = { submitPrompt(inputMessage) }
                            ),
                            decorationBox = { innerTextField ->
                                if (inputMessage.isEmpty()) {
                                    Text(
                                        text = "Ask Sauti...",
                                        color = TextMuted,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Dictation Microphone Icon
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Speak",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onVoiceClick() }
                        )
                    }
                }

                // Far-Right Circular Blue Live-Waveform Button (Reference 6)
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = "Live Voice Mode",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (displayMessages.isEmpty()) {
                // ── Empty State Centerpiece (Reference 6) ─────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Put Sauti to work",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Row 1: Staggered horizontal scrolling chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SuggestionChipItem(
                            icon = Icons.Outlined.Translate,
                            label = "Practice Swahili",
                            onClick = { submitPrompt("Hujambo Sauti, tuongee kwa Kiswahili fasaha kuhusu siku yangu.") }
                        )
                        SuggestionChipItem(
                            icon = Icons.Outlined.Place,
                            label = "Plan my weekend",
                            onClick = { submitPrompt("Nipe mpango mzuri wa kutumia wikendi yangu kwa utulivu.") }
                        )
                        SuggestionChipItem(
                            icon = Icons.Outlined.MusicNote,
                            label = "Stories & culture",
                            onClick = { submitPrompt("Niambie hadithi ya kusisimua kuhusu utamaduni wa pwani.") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Staggered horizontal scrolling chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SuggestionChipItem(
                            icon = Icons.Outlined.Description,
                            label = "Draft a document",
                            onClick = { submitPrompt("Andika barua rasmi ya kuomba kazi kwa lugha ya kitaalamu.") }
                        )
                        SuggestionChipItem(
                            icon = Icons.Outlined.Summarize,
                            label = "Summarize text",
                            onClick = { submitPrompt("Fafanua kwa ufupi mambo makuu ya kuzingatia unapoanza biashara.") }
                        )
                        SuggestionChipItem(
                            icon = Icons.Outlined.Code,
                            label = "Write code",
                            onClick = { submitPrompt("Andika function ya Python inayopanga data haraka.") }
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Centered High-Contrast White Pill CTA Button (Reference 6)
                    Button(
                        onClick = { submitPrompt("Niambie jinsi unavyoweza kunisaidia leo kama msaidizi wa kibinafsi.") },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BtnWhite,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Pick a task",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // ── Conversational Message Feed ───────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                ) {
                    items(displayMessages, key = { it.id }) { msg ->
                        MessageBubbleRow(
                            message = msg,
                            onSpeakClick = { ttsEngine.speak(msg.text) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChipItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ChipDarkFill,
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MessageBubbleRow(
    message: ConversationalMessage,
    onSpeakClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            color = if (message.isUser) Color(0xFF262626) else Color(0xFF171717),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    color = TextWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                if (!message.isUser && message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.tokensPerSec.isNotBlank()) {
                            Text(
                                text = message.tokensPerSec,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
