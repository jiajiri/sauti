package com.sauti.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.annotation.ShowkaseComposable
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

    val focusRequester = remember { FocusRequester() }
    var streamingMessage by remember { mutableStateOf<ConversationalMessage?>(null) }
    var inputTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isGenerating by remember { mutableStateOf(false) }

    // Gate voice input on Whisper model readiness
    val isVoiceReady by produceState(initialValue = false) {
        val voicePath = com.sauti.app.sauti.SautiEngineLoader.getSautiVoicePath()
        val voiceFile = voicePath?.let { java.io.File(it) } ?: java.io.File(context.filesDir, "sauti/voice_lite.bin")
        value = com.sauti.app.sync.ModelDownloadWorker.isValidGgml(voiceFile)
    }

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
            inputTextFieldValue = TextFieldValue("")

            val currentSId = viewModel?.getOrCreateActiveSession() ?: "default"
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

                // Dispatch task completion notification for background awareness
                try {
                    com.sauti.app.notifications.SautiNotificationManager.notifyTaskComplete(
                        context = context,
                        title = "Sauti Msaidizi / Assistant",
                        previewText = finalResponse
                    )
                } catch (_: Exception) {}

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    streamingMessage = null
                    isGenerating = false
                }
            }
        }
    }

    val pendingPrompt by viewModel?.pendingPrompt?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(pendingPrompt) {
        pendingPrompt?.let { prompt ->
            if (prompt.isNotBlank()) {
                viewModel?.consumePendingPrompt()
                submitPrompt(prompt)
            }
        }
    }

    val colors = MaterialTheme.colorScheme

    // Observe WorkManager download progress for the live banner
    val workInfoList by produceState<List<WorkInfo>>(initialValue = emptyList(), context) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(com.sauti.app.sync.ModelDownloadWorker.WORK_NAME)
            .collect { value = it }
    }

    val downloadProgress: Triple<Int, Long, Long>? = remember(workInfoList) {
        workInfoList.firstOrNull()?.let { info: WorkInfo ->
            if (info.state == WorkInfo.State.RUNNING) {
                val pct = info.progress.getInt("progress", -1)
                val dlMb = info.progress.getLong("downloaded_mb", 0L)
                val totMb = info.progress.getLong("total_mb", 0L)
                if (pct >= 0) Triple(pct, dlMb, totMb) else null
            } else null
        }
    }

    val systemLang = remember { com.sauti.app.notifications.SautiNotificationManager.resolveSystemLanguage(context) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = colors.background,
        topBar = {
            // Top App Bar: Circular menu button, pill badge, circular new chat action
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
                        .background(colors.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu",
                        tint = colors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center subtle pill badge ("Sauti")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceVariant,
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (displayMessages.isEmpty()) "Sauti" else activeSessionTitle,
                            color = colors.onSurface,
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
                        .background(colors.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "New Chat",
                        tint = colors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            // Docked Pill Composer Bar
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
                    color = colors.surfaceVariant,
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
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { /* Attachment trigger */ }
                        )

                        // Center Input Field
                        BasicTextField(
                            value = inputTextFieldValue,
                            onValueChange = { inputTextFieldValue = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                color = colors.onSurface,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Default
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = { submitPrompt(inputTextFieldValue.text) }
                            ),
                            decorationBox = { innerTextField ->
                                if (inputTextFieldValue.text.isEmpty()) {
                                    Text(
                                        text = "Ask Sauti...",
                                        color = colors.onSurfaceVariant,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Dictation Microphone Icon (disabled if Whisper model not yet ready)
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = if (isVoiceReady) "Speak" else "Voice downloading...",
                            tint = if (isVoiceReady) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(enabled = isVoiceReady) { onVoiceClick() }
                        )
                    }
                }

                // Far-Right Circular Blue Send Arrow Button
                IconButton(
                    onClick = {
                        if (inputTextFieldValue.text.isNotBlank()) {
                            submitPrompt(inputTextFieldValue.text)
                        } else if (isVoiceReady) {
                            onVoiceClick()
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Send message",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Localized Download Progress Banner (auto-hides when complete) ───
            if (downloadProgress != null) {
                val (pct, dlMb, totMb) = downloadProgress
                val isSw = systemLang == "sw"
                val bannerTitle = if (isSw) {
                    "Inapakua Msaidizi... ($pct%)"
                } else {
                    "Downloading Assistant... ($pct%)"
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bannerTitle,
                                color = colors.onPrimaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (totMb > 0) "$dlMb MB / $totMb MB" else "$dlMb MB",
                                color = colors.onPrimaryContainer.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colors.primary,
                            trackColor = colors.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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
                            color = colors.onBackground,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "What can I help you explore today?",
                            color = colors.onSurfaceVariant,
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

                        // Centered Pill CTA Button
                        Button(
                            onClick = { submitPrompt("Niambie jinsi unavyoweza kunisaidia leo kama msaidizi wa kibinafsi.") },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceVariant,
                                contentColor = colors.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Explore tasks",
                                color = colors.onSurface,
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
                                onSpeakClick = { ttsEngine.speak(msg.text) },
                                onEditClick = {
                                    inputTextFieldValue = TextFieldValue(
                                        text = msg.text,
                                        selection = TextRange(msg.text.length)
                                    )
                                    coroutineScope.launch {
                                        try {
                                            focusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                        }
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
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
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
                tint = colors.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = colors.onSurface,
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

// ── Markdown & Code Block Parsing Models ────────────────────────────────────

sealed interface ContentBlock {
    data class TextBlock(val content: String) : ContentBlock
    data class CodeBlock(val language: String, val code: String) : ContentBlock
}

/**
 * Normalizes raw/malformed model artifacts (<br/>, &lt;br/&gt;, etc.) and splits
 * markdown into structured TextBlock and CodeBlock segments.
 */
internal fun parseMessageBlocks(raw: String): List<ContentBlock> {
    if (raw.isBlank()) return emptyList()

    // Normalize raw HTML breaks and entities emitted by LLM tokens
    val sanitized = raw
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)&lt;br\\s*/?&gt;"), "\n")
        .replace(Regex("(?i)&amp;"), "&")
        .replace(Regex("(?i)&lt;"), "<")
        .replace(Regex("(?i)&gt;"), ">")
        .replace(Regex("(?i)&quot;"), "\"")

    val blocks = mutableListOf<ContentBlock>()
    val codeFenceRegex = Regex("(?s)```([a-zA-Z0-9_-]*)[\\r\\n]+(.*?)```")
    var lastIndex = 0

    for (match in codeFenceRegex.findAll(sanitized)) {
        val start = match.range.first
        val end = match.range.last + 1

        if (start > lastIndex) {
            val textBefore = sanitized.substring(lastIndex, start).trim()
            if (textBefore.isNotEmpty()) {
                blocks.add(ContentBlock.TextBlock(textBefore))
            }
        }

        val lang = match.groupValues[1].trim().ifEmpty { "code" }
        val code = match.groupValues[2].trimEnd()
        blocks.add(ContentBlock.CodeBlock(language = lang, code = code))
        lastIndex = end
    }

    if (lastIndex < sanitized.length) {
        val remainingText = sanitized.substring(lastIndex).trim()
        if (remainingText.isNotEmpty()) {
            blocks.add(ContentBlock.TextBlock(remainingText))
        }
    }

    return if (blocks.isEmpty()) listOf(ContentBlock.TextBlock(sanitized.trim())) else blocks
}

/**
 * Renders an inline/block formatted text paragraph supporting headers (#, ##),
 * bold (**text**), italics (*text*), and inline code (`code`).
 */
@Composable
private fun FormattedMarkdownText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                return@forEach
            }

            // Headers
            val isH1 = trimmed.startsWith("# ")
            val isH2 = trimmed.startsWith("## ")
            val isH3 = trimmed.startsWith("### ")
            val cleanLine = when {
                isH1 -> trimmed.removePrefix("# ")
                isH2 -> trimmed.removePrefix("## ")
                isH3 -> trimmed.removePrefix("### ")
                else -> trimmed
            }

            val fontSize = when {
                isH1 -> 18.sp
                isH2 -> 16.sp
                isH3 -> 15.sp
                else -> 14.5.sp
            }
            val fontWeight = when {
                isH1 || isH2 -> FontWeight.Bold
                isH3 -> FontWeight.SemiBold
                else -> FontWeight.Normal
            }

            val annotated = buildAnnotatedString {
                // Parse bold (**...**) and inline code (`...`)
                val tokenRegex = Regex("(\\*\\*.*?\\*\\*|`.*?`)")
                var curIdx = 0
                for (match in tokenRegex.findAll(cleanLine)) {
                    val mStart = match.range.first
                    val mEnd = match.range.last + 1
                    if (mStart > curIdx) {
                        append(cleanLine.substring(curIdx, mStart))
                    }
                    val matchedVal = match.value
                    if (matchedVal.startsWith("**") && matchedVal.endsWith("**") && matchedVal.length >= 4) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(matchedVal.substring(2, matchedVal.length - 2))
                        }
                    } else if (matchedVal.startsWith("`") && matchedVal.endsWith("`") && matchedVal.length >= 2) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33888888),
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(" " + matchedVal.substring(1, matchedVal.length - 1) + " ")
                        }
                    }
                    curIdx = mEnd
                }
                if (curIdx < cleanLine.length) {
                    append(cleanLine.substring(curIdx))
                }
            }

            Text(
                text = annotated,
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                lineHeight = if (isH1 || isH2) 24.sp else 21.sp
            )
        }
    }
}

/**
 * High-contrast production code block container with a clean header, language tag,
 * and one-click copy button.
 */
@Composable
private fun CodeBlockView(
    language: String,
    code: String,
    onCopyCode: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF121214),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.lowercase(Locale.ROOT),
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onCopyCode()
                            copied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) Color(0xFF22C55E) else Color(0xFFA1A1AA),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        color = if (copied) Color(0xFF22C55E) else Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Code Content Container with Horizontal Scroll
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = code,
                        color = Color(0xFFE4E4E7),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun MessageBubbleRow(
    message: ConversationalMessage,
    onSpeakClick: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val isLight = colors.background.red > 0.5f

    val bubbleBg = if (message.isUser) {
        if (isLight) Color(0xFFE2E8F0) else Color(0xFF262626)
    } else {
        if (isLight) Color(0xFFFFFFFF) else Color(0xFF171717)
    }

    val blocks = remember(message.text) { parseMessageBlocks(message.text) }

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
            color = bubbleBg,
            shadowElevation = if (isLight && !message.isUser) 1.dp else 0.dp,
            modifier = Modifier.widthIn(max = if (message.isUser) 320.dp else 360.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Content Rendering
                if (message.isUser) {
                    Text(
                        text = message.text,
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                } else if (message.text.isBlank() && message.isStreaming) {
                    // ── Active Background Generating State Bubble ──────────────────
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 850),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    val isSw = com.sauti.app.notifications.SautiNotificationManager.resolveSystemLanguage(context) == "sw"
                    val generatingLabel = if (isSw) "Inatengeneza jibu..." else "Generating response..."

                    Text(
                        text = generatingLabel,
                        color = colors.onSurfaceVariant.copy(alpha = pulseAlpha),
                        fontSize = 14.5.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                } else {
                    blocks.forEachIndexed { index, block ->
                        if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                        when (block) {
                            is ContentBlock.TextBlock -> {
                                FormattedMarkdownText(
                                    text = block.content,
                                    textColor = colors.onSurface
                                )
                            }
                            is ContentBlock.CodeBlock -> {
                                CodeBlockView(
                                    language = block.language,
                                    code = block.code,
                                    onCopyCode = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = ClipData.newPlainText("Code", block.code)
                                        clipboard?.setPrimaryClip(clip)
                                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                // Action Bar (Copy, TTS, Edit)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isUser) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isUser) {
                        // User message: Edit prompt button
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit prompt",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // User message: Copy prompt
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Prompt", message.text)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Prompt copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy message",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else if (message.text.isNotBlank()) {
                        // Assistant message: Copy full message
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val cleanText = message.text
                                    .replace(Regex("(?i)<br\\s*/?>"), "\n")
                                    .replace(Regex("(?i)&lt;br\\s*/?&gt;"), "\n")
                                val clip = ClipData.newPlainText("Response", cleanText)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Response copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy response",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Assistant message: Read aloud
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@ShowkaseComposable(name = "UserBubble", group = "ChatBubbles")
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewUserBubble() {
    MessageBubbleRow(
        message = ConversationalMessage(
            id = "preview_1",
            isUser = true,
            text = "Habari za asubuhi! Naomba unisaidie kueleza CAP Theorem kwa Kiswahili."
        ),
        onSpeakClick = {}
    )
}

@ShowkaseComposable(name = "AssistantBubble", group = "ChatBubbles")
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewAssistantBubble() {
    MessageBubbleRow(
        message = ConversationalMessage(
            id = "preview_2",
            isUser = false,
            text = "Nadharia ya CAP (Consistency, Availability, Partition tolerance) inasema mfumo wa kompyuta hauwezi kutoa vyote vitatu kwa pamoja.",
            tokensPerSec = "24.5 t/s"
        ),
        onSpeakClick = {}
    )
}

