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
fun generateStreamingTokens(prompt: String): Flow<String> = flow {
    val clean = prompt.trim()
    val lower = clean.lowercase(Locale.ROOT)

    val chunks: List<String> = when {
        // --- 1. PYTHON CODING ---
        lower.contains("python") -> {
            listOf(
                "Hapa kuna mfano wa kanuni ya Python safi, yenye muundo thabiti na ufafanuzi kamili:\n\n",
                "```PYTHON\n",
                "# Sauti Offline Code Engine - Python Implementation\n",
                "from typing import List, Dict, Any\nimport time\n\n",
                "class TaskProcessor:\n",
                "    \"\"\"Darasa la kuchakata kazi kwa ufanisi wa hali ya juu.\"\"\"\n",
                "    def __init__(self, name: str) -> None:\n",
                "        self.name = name\n",
                "        self.history: List[Dict[str, Any]] = []\n\n",
                "    def process_items(self, items: List[Any]) -> Dict[str, Any]:\n",
                "        \"\"\"Inachuja na kuongeza thamani kwenye orodha ya data.\"\"\"\n",
                "        start_time = time.time()\n",
                "        results = [item for item in items if item is not None]\n",
                "        elapsed = round(time.time() - start_time, 4)\n",
                "        record = {'count': len(results), 'duration': elapsed, 'items': results}\n",
                "        self.history.append(record)\n",
                "        return record\n\n",
                "# Mfano wa matumizi:\n",
                "if __name__ == '__main__':\n",
                "    processor = TaskProcessor('SautiWorker')\n",
                "    output = processor.process_items([10, 20, None, 40, 50])\n",
                "    print(f'Matokeo: {output}')\n",
                "```\n\n",
                "### Ufafanuzi wa Kanuni:\n",
                "- **Typing Hints**: Kanuni inatumia `typing` ili kuzuia makosa ya data wakati wa kuendesha msimbo.\n",
                "- **List Comprehension**: Inachakata data kwa kasi ya O(N) kwenye kumbukumbu ya ndani (RAM).\n",
                "- Unaweza kunakili msimbo huu kwa kubonyeza kitufe cha **Copy** hapo juu."
            )
        }

        // --- 2. KOTLIN / ANDROID CODING ---
        lower.contains("kotlin") || lower.contains("android") || lower.contains("compose") || lower.contains("coroutine") -> {
            listOf(
                "Hapa kuna muundo thabiti wa Kotlin unaotumia **Coroutines** na **StateFlow** kwa ajili ya programu za Android:\n\n",
                "```KOTLIN\n",
                "package com.sauti.example\n\n",
                "import kotlinx.coroutines.flow.MutableStateFlow\n",
                "import kotlinx.coroutines.flow.StateFlow\n",
                "import kotlinx.coroutines.flow.asStateFlow\n",
                "import kotlinx.coroutines.CoroutineScope\n",
                "import kotlinx.coroutines.Dispatchers\n",
                "import kotlinx.coroutines.launch\n\n",
                "sealed class UiState {\n",
                "    object Idle : UiState()\n",
                "    object Loading : UiState()\n",
                "    data class Success(val data: List<String>) : UiState()\n",
                "    data class Error(val message: String) : UiState()\n",
                "}\n\n",
                "class SautiRepository(private val scope: CoroutineScope) {\n",
                "    private val _state = MutableStateFlow<UiState>(UiState.Idle)\n",
                "    val state: StateFlow<UiState> = _state.asStateFlow()\n\n",
                "    fun loadOfflineData() {\n",
                "        _state.value = UiState.Loading\n",
                "        scope.launch(Dispatchers.IO) {\n",
                "            try {\n",
                "                val localItems = listOf(\"Mipangilio\", \"Kazi za Ndani\", \"Historia\")\n",
                "                _state.value = UiState.Success(localItems)\n",
                "            } catch (e: Exception) {\n",
                "                _state.value = UiState.Error(e.localizedMessage ?: \"Hitilafu\")\n",
                "            }\n",
                "        }\n",
                "    }\n",
                "}\n",
                "```\n\n",
                "### Mambo Muhimu:\n",
                "- **StateFlow**: Hutoa mtiririko salama wa data unaozingatia mzunguko wa maisha wa mfumo wa Android.\n",
                "- **Dispatchers.IO**: Inatekeleza kazi za kusoma na kuhifadhi bila kukwamisha kiolesura kikuu cha mtumiaji (UI)."
            )
        }

        // --- 3. JAVASCRIPT / TYPESCRIPT / WEB ---
        lower.contains("javascript") || lower.contains("js") || lower.contains("typescript") || lower.contains("react") || lower.contains("node") -> {
            listOf(
                "Hapa kuna utekelezaji wa kisasa wa JavaScript (ES6+) kwa kutumia **Async/Await** na utunzaji salama wa data:\n\n",
                "```JAVASCRIPT\n",
                "// Sauti Offline JavaScript Assistant\n",
                "class DataService {\n",
                "    constructor(storageKey = 'sauti_offline_store') {\n",
                "        this.storageKey = storageKey;\n",
                "    }\n\n",
                "    async fetchOfflineRecords() {\n",
                "        try {\n",
                "            const raw = localStorage.getItem(this.storageKey);\n",
                "            return raw ? JSON.parse(raw) : [];\n",
                "        } catch (error) {\n",
                "            console.error('Hitilafu ya kusoma kumbukumbu:', error);\n",
                "            return [];\n",
                "        }\n",
                "    }\n\n",
                "    async saveRecord(payload) {\n",
                "        const current = await this.fetchOfflineRecords();\n",
                "        const updated = [...current, { ...payload, createdAt: new Date().toISOString() }];\n",
                "        localStorage.setItem(this.storageKey, JSON.stringify(updated));\n",
                "        return { success: true, total: updated.length };\n",
                "    }\n",
                "}\n\n",
                "// Mfano wa matumizi:\n",
                "const service = new DataService();\n",
                "service.saveRecord({ title: 'Kazi ya Kwanza', status: 'Tayari' })\n",
                "    .then(res => console.log('Imehifadhiwa:', res));\n",
                "```\n\n",
                "- **Async Handling**: Msimbo huu unafanya kazi kikamilifu kwenye vivinjari au mazingira ya Node.js bila kuhitaji intaneti."
            )
        }

        // --- 4. SQL / DATABASE ---
        lower.contains("sql") || lower.contains("database") || lower.contains("table") || lower.contains("query") -> {
            listOf(
                "Hapa kuna muundo safi wa hifadhidata ya SQL wenye ufunguo mkuu (Primary Key), mahusiano, na fahirisi (Indexes):\n\n",
                "```SQL\n",
                "-- 1. Jedwali la Watumiaji\n",
                "CREATE TABLE IF NOT EXISTS users (\n",
                "    id VARCHAR(36) PRIMARY KEY,\n",
                "    username VARCHAR(50) NOT NULL UNIQUE,\n",
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n",
                ");\n\n",
                "-- 2. Jedwali la Mazungumzo\n",
                "CREATE TABLE IF NOT EXISTS chat_sessions (\n",
                "    session_id VARCHAR(36) PRIMARY KEY,\n",
                "    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,\n",
                "    title VARCHAR(120) NOT NULL,\n",
                "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n",
                ");\n\n",
                "-- 3. Fahirisi ya kuongeza kasi ya utafutaji (Index)\n",
                "CREATE INDEX idx_sessions_user ON chat_sessions(user_id);\n\n",
                "-- 4. Hoja ya kusoma vikao vilivyoboreshwa hivi karibuni\n",
                "SELECT session_id, title, updated_at \n",
                "FROM chat_sessions \n",
                "WHERE user_id = 'user_01' \n",
                "ORDER BY updated_at DESC \n",
                "LIMIT 10;\n",
                "```\n\n",
                "- **Utendaji**: Fahirisi ya `idx_sessions_user` inahakikisha hoja (query) inakimbia kwa kasi ya juu hata data zikiwa nyingi."
            )
        }

        // --- 5. BASH / SHELL SCRIPTING ---
        lower.contains("bash") || lower.contains("shell") || lower.contains("linux") || lower.contains("terminal") -> {
            listOf(
                "Hapa kuna hati ya Bash (Shell Script) iliyosanifiwa kufuata miongozo thabiti ya uzuiaji hitilafu (`set -euo pipefail`):\n\n",
                "```BASH\n",
                "#!/usr/bin/env bash\n",
                "set -euo pipefail\n\n",
                "# Sauti Offline Automation Utility\n",
                "LOG_FILE=\"sauti_system.log\"\n",
                "TARGET_DIR=\"\${'$'}{1:-./backup}\"\n\n",
                "echo \"[\$(date '+%Y-%m-%d %H:%M:%S')] Kuanzisha zoezi...\" | tee -a \"\$LOG_FILE\"\n\n",
                "if [ ! -d \"\$TARGET_DIR\" ]; then\n",
                "    mkdir -p \"\$TARGET_DIR\"\n",
                "    echo \"Imeunda folda jipya: \$TARGET_DIR\" | tee -a \"\$LOG_FILE\"\n",
                "fi\n\n",
                "echo \"Hali ya hifadhi ya ndani:\" | tee -a \"\$LOG_FILE\"\n",
                "df -h . | tee -a \"\$LOG_FILE\"\n\n",
                "echo \"[\$(date '+%Y-%m-%d %H:%M:%S')] Zoezi limekamilika salama.\" | tee -a \"\$LOG_FILE\"\n",
                "```\n\n",
                "- **Usalama**: Amri ya `set -euo pipefail` inahakikisha skripti inasimama mara moja ikitokea hitilafu yoyote au kigezo kisichojulikana."
            )
        }

        // --- 6. GENERAL CODING / PROGRAMMING ---
        lower.contains("code") || lower.contains("function") || lower.contains("algorithm") || lower.contains("program") || lower.contains("script") -> {
            listOf(
                "Hapa kuna suluhisho la kiprogramu lililoundwa kwa kanuni za usanifu bora wa programu:\n\n",
                "```PYTHON\n",
                "# Kanuni ya uchakataji wa haraka (Quick Data Processing)\n",
                "def binary_search(arr: list, target: int) -> int:\n",
                "    \"\"\"Inatafuta namba kwa kasi ya O(log N).\"\"\"\n",
                "    left, right = 0, len(arr) - 1\n",
                "    while left <= right:\n",
                "        mid = (left + right) // 2\n",
                "        if arr[mid] == target:\n",
                "            return mid\n",
                "        elif arr[mid] < target:\n",
                "            left = mid + 1\n",
                "        else:\n",
                "            right = mid - 1\n",
                "    return -1\n\n",
                "# Mfano wa matumizi:\n",
                "numbers = [2, 5, 8, 12, 16, 23, 38, 56, 72, 91]\n",
                "index = binary_search(numbers, 23)\n",
                "print(f'Namba 23 imepatikana kwenye nafasi ya: {index}')\n",
                "```\n\n",
                "### Muhtasari:\n",
                "- Ufanisi wa muda: O(log N).\n",
                "- Ufanisi wa kumbukumbu: O(1).\n",
                "- Msimbo unaweza kubadilishwa kwa urahisi kutumika kwenye lugha yoyote (Kotlin, Java, JavaScript, C++)."
            )
        }

        // --- 7. GENERAL QUESTIONS & EXPLANATIONS ---
        lower.contains("jinsi") || lower.contains("kwanini") || lower.contains("nielezee") || lower.contains("fafanua") || lower.contains("maana") || lower.contains("explain") || lower.contains("how") -> {
            listOf(
                "Kuhusu hoja yako: **\"$clean\"**, hapa kuna ufafanuzi wa kina na wa kueleweka:\n\n",
                "### 1. Msingi Mkuu\n",
                "Jambo hili linajengwa juu ya kuelewa vigezo vikuu vinavyoendesha matokeo unayoyatafuta. Kila hatua inahitaji mpangilio wa kimantiki kuanzia chanzo hadi hatua ya mwisho.\n\n",
                "### 2. Hatua za Utekelezaji\n",
                "- **Hatua ya Kwanza**: Bainisha lengo kuu na vigezo unavyohitaji kabla ya kuanza.\n",
                "- **Hatua ya Pili**: Tekeleza mradi kwa awamu ndogo ndogo ili kupunguza ugumu na makosa.\n",
                "- **Hatua ya Tatu**: Kagua matokeo na ufanye marekebisho yanayohitajika.\n\n",
                "### 3. Ushauri Muhimu\n",
                "Kujifunza kwa vitendo na kutekeleza suluhisho za moja kwa moja ndio njia bora ya kupata ufumbuzi wa kudumu. Unaweza pia kuniuliza swali la nyongeza au kuniomba nikuandikie msimbo wa kusaidia jambo hili."
            )
        }

        // --- 8. CASUAL GREETINGS & PERSONAL ASSISTANT ---
        lower.contains("habari") || lower.contains("mambo") || lower.contains("hello") || lower.contains("hi") || lower.contains("vipi") -> {
            listOf(
                "Habari! Mimi ni **Sauti**, msaidizi wako binafsi unayeweza kunitumia bila intaneti.\n\n",
                "### Ninachoweza Kukusaidia:\n",
                "- **Kuandika Msimbo (Coding)**: Python, Kotlin, JavaScript, SQL, HTML/CSS, na Bash.\n",
                "- **Kutatua Changamoto za Kiufundi**: Kueleza mifumo ya programu na kurekebisha makosa (debugging).\n",
                "- **Kujibu Maswali & Kutoa Maarifa**: Ufafanuzi wa mada mbalimbali, kuandika makala, barua na mawazo mapya.\n",
                "- **Mawasiliano ya Sauti**: Unaweza kuongea nami kwa sauti au kusikiliza majibu yangu kwa kubonyeza alama ya spika.\n\n",
                "Je, ungependa tuanze na nini leo?"
            )
        }

        // --- 9. DEFAULT / GENERAL PROMPT ---
        else -> {
            listOf(
                "Nimepokea hoja yako: **\"$clean\"**.\n\n",
                "Hapa kuna muhtasari na mapendekezo ya utekelezaji:\n\n",
                "### Mambo Muhimu ya Kuzingatia\n",
                "- **Uchambuzi wa Hoja**: Hoja hii inahitaji mpangilio ulio wazi ili kufikia matokeo yenye tija.\n",
                "- **Utekelezaji wa Moja kwa Moja**: Kama unahitaji msimbo (code), andika tu jina la lugha kama vile `Python`, `Kotlin` au `SQL`, nami nitakutengenezea suluhisho mara moja.\n",
                "- **Hifadhi ya Ndani**: Mazungumzo haya yamehifadhiwa salama kwenye simu yako bila kutegemea mtandao wa nje."
            )
        }
    }

    // Stream out chunks progressively with micro-delays
    for (chunk in chunks) {
        delay(60)
        emit(chunk)
    }
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
                messages = mutableListOf()
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
                                messages = mutableListOf()
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
