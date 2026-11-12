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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    val tokensPerSec: String = "",
    val isStreaming: Boolean = false
)

/**
 * Session model for topic threads.
 */
data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: androidx.compose.runtime.snapshots.SnapshotStateList<ConversationalMessage> = androidx.compose.runtime.mutableStateListOf()
)

/**
 * Parsed markdown content block (Text vs Code Block).
 */
sealed class ContentBlock {
    data class Text(val content: String) : ContentBlock()
    data class Code(val language: String, val code: String) : ContentBlock()
}

/**
 * Parses markdown string into structured text blocks and code blocks.
 */
fun parseMarkdownContent(raw: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val lines = raw.split("\n")
    var inCodeBlock = false
    var currentLanguage = "CODE"
    val codeBuilder = StringBuilder()
    val textBuilder = StringBuilder()

    for (line in lines) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // Closing code block
                blocks.add(ContentBlock.Code(currentLanguage, codeBuilder.toString().trimEnd()))
                codeBuilder.clear()
                inCodeBlock = false
            } else {
                // Opening code block
                if (textBuilder.isNotEmpty()) {
                    blocks.add(ContentBlock.Text(textBuilder.toString().trimEnd()))
                    textBuilder.clear()
                }
                val langTag = trimmed.removePrefix("```").trim().ifEmpty { "CODE" }.uppercase()
                currentLanguage = langTag
                inCodeBlock = true
            }
        } else {
            if (inCodeBlock) {
                if (codeBuilder.isNotEmpty()) codeBuilder.append("\n")
                codeBuilder.append(line)
            } else {
                if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                textBuilder.append(line)
            }
        }
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        blocks.add(ContentBlock.Code(currentLanguage, codeBuilder.toString().trimEnd()))
    } else if (textBuilder.isNotEmpty()) {
        blocks.add(ContentBlock.Text(textBuilder.toString().trimEnd()))
    }

    return blocks
}

/**
 * Offline Intelligent Agent Response Synthesizer (Sauti Engine).
 * Covers Coding, General Reasoning, Problem Solving, and Conversational Q&A.
 */
fun generateStreamingTokens(prompt: String): Flow<String> {
    return com.sauti.app.sauti.SautiEngineLoader.generateStreamingResponse(prompt)
}
@Composable
fun ChatScreen(
    onVoiceClick: () -> Unit = {},
    onDrawerClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val ttsEngine = remember { TextToSpeechEngine(context) }

    // Session list container
    val sessions = remember {
        mutableStateListOf(
            ChatSession(
                id = "session_default",
                title = "New Topic",
                messages = androidx.compose.runtime.mutableStateListOf()
            )
        )
    }

    var activeSessionId by remember { mutableStateOf(sessions.first().id) }
    var inputMessage by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val activeSession = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.first()
    val activeMessages = activeSession.messages

    val lastItemIndex = activeMessages.lastIndex
    val lastMessageContentLength = activeMessages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(lastItemIndex, lastMessageContentLength) {
        if (lastItemIndex >= 0) {
            val isNearBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let {
                it >= lastItemIndex - 1
            } ?: true

            if (isNearBottom) {
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
            // Clean Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDrawerClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Sauti",
                    color = Color(0xFFE2E8F0),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )

                IconButton(
                    onClick = {
                        val newSessionId = "session_${System.currentTimeMillis()}"
                        sessions.add(
                            0,
                            ChatSession(
                                id = newSessionId,
                                title = "New Topic",
                                messages = mutableStateListOf()
                            )
                        )
                        activeSessionId = newSessionId
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "New Session",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            // Anchored Floating Input Capsule
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f),
                color = Color.Black,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF1E232A))
                        .border(0.5.dp, Color(0xFF333B48), RoundedCornerShape(32.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Input Field
                        BasicTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Default
                            ),
                            cursorBrush = SolidColor(Color(0xFF38BDF8)),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (!isGenerating && inputMessage.isNotBlank()) {
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
                                                    val index = activeSession.messages.indexOfFirst { it.id == assistantMsgId }
                                                    if (index != -1) {
                                                        activeSession.messages[index] = activeSession.messages[index].copy(
                                                            text = accumulated.toString(),
                                                            tokensPerSec = currentTps,
                                                            isStreaming = true
                                                        )
                                                    }
                                                }
                                            }

                                            val finalElapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(1L)) / 1000.0
                                            val finalTps = String.format(Locale.US, "%.1f t/s", (tokenCount / finalElapsedSec))
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                val finalIndex = activeSession.messages.indexOfFirst { it.id == assistantMsgId }
                                                if (finalIndex != -1) {
                                                    activeSession.messages[finalIndex] = activeSession.messages[finalIndex].copy(
                                                        tokensPerSec = finalTps,
                                                        isStreaming = false
                                                    )
                                                }
                                                isGenerating = false
                                            }
                                        }
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (inputMessage.isEmpty()) {
                                    Text(
                                        text = "Andika swali au omba code hapa...",
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

                        // Mic Button
                        IconButton(
                            onClick = onVoiceClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "Voice Input",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send Arrow
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
                                            isStreaming = true
                                        )
                                        activeSession.messages.add(assistantPlaceholder)
                                        isGenerating = true

                                        coroutineScope.launch {
                                            val accumulated = StringBuilder()

                                            generateStreamingTokens(userPrompt).collect { token ->
                                                accumulated.append(token)
                                                val index = activeSession.messages.indexOfFirst { it.id == assistantMsgId }
                                                if (index != -1) {
                                                    activeSession.messages[index] = activeSession.messages[index].copy(
                                                        text = accumulated.toString(),
                                                        isStreaming = true
                                                    )
                                                }
                                            }

                                            val finalIndex = activeSession.messages.indexOfFirst { it.id == assistantMsgId }
                                            if (finalIndex != -1) {
                                                activeSession.messages[finalIndex] = activeSession.messages[finalIndex].copy(
                                                    text = accumulated.toString(),
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
                                    .background(Color(0xFF0284C7))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
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
                    // Minimalist Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF161A22))
                                .border(1.dp, Color(0xFF232A38), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = "Sauti Agent",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Sauti Offline Agent",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Code generation, general reasoning & voice assistant",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Interactive Prompt Chips
                        val starterPrompts = listOf(
                            "Andika function ya Python ya...",
                            "Kotlin Coroutines StateFlow mfano",
                            "Tengeneza SQL query ya...",
                            "Nielezee jinsi ya kuunda API..."
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            starterPrompts.forEach { prompt ->
                                Surface(
                                    onClick = { inputMessage = prompt },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF141720),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF222838)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Terminal,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = prompt,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        items(activeMessages, key = { it.id }) { msg ->
                            if (msg.isUser) {
                                UserMessageBubble(
                                    message = msg,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Prompt", msg.text))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
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
 * User message bubble aligned to the right.
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
                .background(Color(0xFF1E2430))
                .border(0.5.dp, Color(0xFF2C3547), RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
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
 * Assistant message bubble with code block rendering, text copy, and voice reading.
 */
@Composable
private fun AssistantMessageBubble(
    message: ConversationalMessage,
    onCopy: () -> Unit,
    onRead: () -> Unit
) {
    val context = LocalContext.current
    val contentBlocks = remember(message.text) {
        parseMarkdownContent(message.text)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        contentBlocks.forEach { block ->
            when (block) {
                is ContentBlock.Text -> {
                    MarkdownText(text = block.content, isStreaming = message.isStreaming)
                }
                is ContentBlock.Code -> {
                    CodeSnippetCard(
                        language = block.language,
                        code = block.code,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", block.code))
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // Action Toolbar: Copy & Read aloud
        if (!message.isStreaming && message.text.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onRead, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "Offline Sauti Agent",
                    color = Color(0xFF475569),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Modern High-Precision Code Block Card.
 */
@Composable
fun CodeSnippetCard(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isCopied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F131A))
            .border(1.dp, Color(0xFF222838), RoundedCornerShape(10.dp))
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF171B24))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = "Code",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = language,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = {
                        onCopy()
                        isCopied = true
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = if (isCopied) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "Copied" else "Copy",
                        color = if (isCopied) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            // Code Content with Horizontal Scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(14.dp)
            ) {
                Text(
                    text = code,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 19.sp
                )
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
        .replace(Regex("```[a-zA-Z]*\\n", RegexOption.MULTILINE), "")
        .replace("```", "")
        .replace("**", "")
        .replace("*", "")
        .trim()
}

/**
 * Clean markdown renderer for regular text, headings, and bullet points.
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
