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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
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
    val userProfile by viewModel?.userProfile?.collectAsState() ?: remember { mutableStateOf(com.sauti.app.data.model.UserProfile("User")) }

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

                // Far-Right Circular Blue Send Arrow Button (Reference: user request)
                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            submitPrompt(inputMessage)
                        } else {
                            onVoiceClick()
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Send message",
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
                // ── Warm Personalized Welcome Centerpiece ─────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val firstName = userProfile.fullName.trim().split("\\s+".toRegex()).firstOrNull { it.isNotEmpty() } ?: "there"
                    Text(
                        text = "Hello, $firstName",
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "What can I help you explore today?",
                        color = TextMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Row 1: Smooth Infinite Scrolling Chips (Drifting Left)
                    AutoScrollingChipRow(
                        items = listOf(
                            Triple(Icons.Outlined.Translate, "Learn Swahili", "Hujambo Sauti, nifundishe misemo ya kawaida ya Kiswahili kwa urahisi."),
                            Triple(Icons.Outlined.Create, "Create poem", "Nitungie shairi zuri lenye vina kuhusu amani na upendo."),
                            Triple(Icons.Outlined.Lightbulb, "Brainstorm ideas", "Nipe mawazo mapya ya kuanzisha mradi mdogo wa kidijitali."),
                            Triple(Icons.Outlined.MusicNote, "Stories & folklore", "Niambie hadithi fupi ya kusisimua ya utamaduni wa pwani.")
                        ),
                        scrollDirection = 1, // Drifting left
                        onItemClick = { submitPrompt(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Smooth Infinite Scrolling Chips (Drifting Right)
                    AutoScrollingChipRow(
                        items = listOf(
                            Triple(Icons.Outlined.Code, "Write code", "Andika function ya Python inayopanga data haraka."),
                            Triple(Icons.Outlined.Description, "Draft document", "Andika barua rasmi ya kuomba kazi kwa lugha ya kitaalamu."),
                            Triple(Icons.Outlined.Summarize, "Summarize notes", "Fafanua kwa ufupi mambo makuu ya kuzingatia unapoanza biashara."),
                            Triple(Icons.Outlined.Psychology, "Quick advice", "Nipe ushauri mzuri wa kupanga muda wangu wa siku.")
                        ),
                        scrollDirection = -1, // Drifting right
                        onItemClick = { submitPrompt(it) }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Centered High-Contrast White Pill CTA Button
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
                            text = "Explore tasks",
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

/**
 * Infinite looping chip row that slowly and continuously drifts horizontally,
 * while allowing individual items to remain clickable.
 */
@Composable
private fun AutoScrollingChipRow(
    items: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String>>,
    scrollDirection: Int, // 1 for drifting left, -1 for drifting right
    onItemClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    // Infinite gentle continuous drifting animation loop
    LaunchedEffect(scrollDirection) {
        val initialScroll = if (scrollDirection < 0) 3000 else 0
        if (scrollDirection < 0) {
            scrollState.scrollTo(initialScroll)
        }
        while (true) {
            val target = if (scrollDirection > 0) {
                (scrollState.value + 400).coerceAtMost(scrollState.maxValue)
            } else {
                (scrollState.value - 400).coerceAtLeast(0)
            }

            if (scrollDirection > 0 && scrollState.value >= scrollState.maxValue - 10) {
                scrollState.scrollTo(0)
            } else if (scrollDirection < 0 && scrollState.value <= 10) {
                scrollState.scrollTo(scrollState.maxValue)
            } else {
                scrollState.animateScrollTo(
                    value = target,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 3500,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                )
            }
            kotlinx.coroutines.delay(10)
        }
    }

    // Multiply items so the row has plenty of repeating content to loop smoothly
    val repeatedItems = remember(items) { items + items + items + items }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeatedItems.forEach { (icon, label, prompt) ->
            SuggestionChipItem(
                icon = icon,
                label = label,
                onClick = { onItemClick(prompt) }
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
                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
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
