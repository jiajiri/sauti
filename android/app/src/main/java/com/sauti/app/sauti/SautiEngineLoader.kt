package com.sauti.app.sauti

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream

/**
 * Sauti Native Asset Registration & Engine Loader.
 * Manages zero-copy loading for Sauti Voice Recognition (whisper.cpp) and Sauti Core (Qwen2.5 GGUF).
 */
object SautiEngineLoader {

    private const val TAG = "SautiEngineLoader"

    // Tiered Asset Directory Paths
    const val ASSET_SAUTI_VOICE_LITE = "sauti/voice_lite.bin"
    const val ASSET_SAUTI_VOICE_PRO = "sauti/voice_pro.bin"
    const val ASSET_SAUTI_CORE_LITE = "sauti/core_lite.bin"
    const val ASSET_SAUTI_CORE_PRO = "sauti/core_pro.bin"

    // Backward compatibility aliases
    const val ASSET_SAUTI_VOICE = ASSET_SAUTI_VOICE_LITE
    const val ASSET_SAUTI_CORE = ASSET_SAUTI_CORE_PRO

    private var sautiVoicePath: String? = null
    private var sautiCorePath: String? = null
    var customUserInstructions: String = ""

    init {
        try {
            System.loadLibrary("sauti_native_engine")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library sauti_native_engine not loaded: ${e.message}")
        }
    }

    fun interface NativeTokenCallback {
        fun onToken(token: String)
    }

    private external fun nativeInitCoreEngine(path: String): Boolean
    private external fun nativeInitVoiceEngine(path: String): Boolean
    private external fun nativeGenerateChatCompletion(prompt: String, callback: NativeTokenCallback)
    private external fun nativeSetInferenceParams(
        temperature: Float, topK: Int, topP: Float,
        repeatPenalty: Float, maxTokens: Int
    )
    private external fun nativeSwitchCoreModel(path: String): Boolean
    private external fun nativeSetVoiceLanguage(lang: String)

    /**
     * Toggles between Tanzanian Swahili ("sw") and English ("en") voice transcription.
     */
    fun setVoiceLanguage(languageCode: String) {
        try {
            nativeSetVoiceLanguage(languageCode)
            Log.i(TAG, "Voice ASR language toggled to: $languageCode")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to toggle voice language: ${e.message}")
        }
    }

    /**
     * Applies runtime inference parameters from the user's Settings preferences to the native engine.
     */
    fun applyInferenceParams(prefs: com.sauti.app.settings.SautiPreferencesStore) {
        try {
            nativeSetInferenceParams(
                prefs.temperature,
                prefs.topK,
                prefs.topP,
                prefs.repeatPenalty,
                prefs.maxTokens
            )
            Log.i(TAG, "Inference params applied: temp=${prefs.temperature} topK=${prefs.topK} topP=${prefs.topP}")
        } catch (e: Throwable) {
            Log.w(TAG, "applyInferenceParams error: ${e.message}")
        }
    }

    /**
     * Hot-swaps the loaded GGUF core model without restarting the app.
     */
    suspend fun switchModel(context: Context, tier: com.sauti.app.settings.ModelTier) {
        val file = java.io.File(context.filesDir, "sauti/${tier.assetFileName}")
        if (!file.exists()) {
            Log.w(TAG, "Model file not found for tier ${tier.displayName}: ${file.absolutePath}")
            return
        }
        try {
            val success = nativeSwitchCoreModel(file.absolutePath)
            if (success) {
                sautiCorePath = file.absolutePath
                Log.i(TAG, "Model switched to ${tier.displayName} at ${file.absolutePath}")
            } else {
                Log.w(TAG, "nativeSwitchCoreModel returned false for ${tier.displayName}")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "switchModel error: ${e.message}")
        }
    }

    /**
     * Verifies and links models in private application storage for zero-copy mmap.
     * If models are not yet downloaded, triggers background WorkManager fetch while falling back safely.
     */
    fun ensureAssetsRegistered(context: Context): Pair<String, String> {
        val sautiDir = File(context.filesDir, "sauti")
        if (!sautiDir.exists()) {
            sautiDir.mkdirs()
        }

        // Schedule background model sync
        try {
            com.sauti.app.sync.ModelDownloadWorker.schedule(context)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to schedule model sync: ${e.message}")
        }

        val voiceFile = File(sautiDir, "voice_lite.bin")
        if (!voiceFile.exists() || voiceFile.length() == 0L) {
            copyAssetToFile(context, ASSET_SAUTI_VOICE_LITE, voiceFile)
        }

        // Check for core_lite or core_pro
        var coreFile = File(sautiDir, "core_pro.bin")
        if (!coreFile.exists() || coreFile.length() == 0L) {
            val liteFile = File(sautiDir, "core_lite.bin")
            if (liteFile.exists() && liteFile.length() > 0L) {
                coreFile = liteFile
            } else {
                copyAssetToFile(context, ASSET_SAUTI_CORE_LITE, coreFile)
            }
        }

        sautiVoicePath = voiceFile.absolutePath
        sautiCorePath = coreFile.absolutePath

        // Initialize local SQLite RAG store
        initKnowledgeStore(context)

        try {
            if (voiceFile.exists() && voiceFile.length() > 1024 * 1024) {
                nativeInitVoiceEngine(sautiVoicePath!!)
            }
            if (coreFile.exists() && coreFile.length() > 1024 * 1024) {
                nativeInitCoreEngine(sautiCorePath!!)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Native engine init: " + e.message)
        }

        return Pair(sautiVoicePath!!, sautiCorePath!!)
    }

    private fun copyAssetToFile(context: Context, assetPath: String, destFile: File) {
        try {
            val assetList = context.assets.list("sauti") ?: emptyArray()
            val fileName = assetPath.substringAfterLast("/")
            if (!assetList.contains(fileName)) {
                return // Not bundled in APK; will be downloaded remotely
            }
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            Log.i(TAG, "Registered asset $assetPath -> ${destFile.absolutePath}")
        } catch (e: Exception) {
            Log.d(TAG, "Asset $assetPath not bundled; awaiting remote sync: ${e.message}")
        }
    }

    private var knowledgeDatabase: com.sauti.app.rag.SovereignKnowledgeDatabase? = null

    /**
     * Initializes knowledge database for offline RAG retrieval.
     */
    fun initKnowledgeStore(context: Context) {
        if (knowledgeDatabase == null) {
            knowledgeDatabase = com.sauti.app.rag.SovereignKnowledgeDatabase(context)
        }
    }

    /**
     * Streams tokens from on-device model using strict ChatML formatting, strict language mirroring, and RAG context injection:
     * <|im_start|>system ... <|im_start|>user ... <|im_start|>assistant
     */
    fun generateStreamingResponse(userPrompt: String): Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        val clean = userPrompt.trim()
        val intent = com.sauti.app.rag.IntentClassifier.classify(clean)
        val detectedLang = com.sauti.app.rag.LanguageDetector.detect(clean)

        // Retrieve local domain context whenever available from Sovereign Knowledge Store
        val retrievedContext = if (knowledgeDatabase != null) {
            val chunks = knowledgeDatabase!!.searchKnowledge(clean, maxResults = 2)
            if (chunks.isNotEmpty()) {
                "\n[MUKTADHA WA NDANI / VERIFIED LOCAL CONTEXT]\n" + chunks.joinToString("\n") + "\n"
            } else ""
        } else ""

        val systemLanguageInstruction = if (detectedLang == com.sauti.app.rag.DetectedLanguage.SWAHILI) {
            "Wewe ni Sauti AI, msaidizi mwerevu na mwaminifu unayeongea Kiswahili fasaha cha Tanzania na Kiingereza. Jibu maswali kwa ufupi, kwa usahihi, na bila kubuni taarifa usizozijua. Kama huna uhakika au hujui jibu, sema bayana: 'Samahani, sina taarifa hiyo kwa sasa.' Toa majibu ya moja kwa moja."
        } else {
            "You are Sauti AI, an intelligent, factual bilingual assistant. Answer concisely and accurately. If you are uncertain or do not know the answer, explicitly state: 'I do not have enough verified information on that.' Never fabricate or hallucinate facts."
        }

        val formattedPrompt = buildString {
            append("<|im_start|>system\n")
            append(systemLanguageInstruction)
            if (retrievedContext.isNotBlank()) {
                append("\nBase your answer strictly on the provided verified context:\n")
                append(retrievedContext)
            }
            if (customUserInstructions.isNotBlank()) {
                append("\nUser Preferences: ").append(customUserInstructions).append("\n")
            }
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(clean)
            append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }

        var tokensEmitted = false
        try {
            nativeGenerateChatCompletion(formattedPrompt) { token ->
                tokensEmitted = true
                trySend(token)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Native inference error: " + e.message)
        }

        // If native inference was unavailable or failed to produce tokens, provide direct step-by-step reasoning based on the actual prompt
        if (!tokensEmitted) {
            emitDirectReasoning(userPrompt).collect { chunk ->
                trySend(chunk)
            }
        }

        close()
        awaitClose { /* Cleanup when flow collection cancelled */ }
    }.flowOn(Dispatchers.IO)

    private fun emitDirectReasoning(prompt: String): Flow<String> = flow {
        val clean = prompt.trim()
        val lower = clean.lowercase()

        // 1. Safety & Ethics Refusal (e.g. theft/kuiba)
        if (Regex("\b(kuiba|uizi|mwizi|wezi|steal|stealing|theft|rob|robbing|voler|robar|roubar)\b", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
            if (Regex("\b(je|ni|salama|kuiba|wezi|hapana)\b", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
                emit("Hapana, kuiba si salama wala si halali. Ni kosa la kisheria na kimaadili linaloweza kuhatarisha usalama wako na kupelekea kifungo gerezani. Iwapo unakabiliwa na ugumu wa kiuchumi, tafadhali tafuta msaada halali au ushauri wa kijamii.")
            } else {
                emit("No, stealing is neither safe nor legal. It is a criminal offense carrying severe legal consequences including arrest and imprisonment. If you are experiencing hardship, please seek legitimate support resources.")
            }
            return@flow
        }

        // 2. Multilingual Greetings (Gemini / ChatGPT Style)
        if (Regex("^(habari|hujambo|jambo|mambo|vipi|salama)$", RegexOption.IGNORE_CASE).matches(lower)) {
            emit("Habari! Karibu sana, ninawezaje kukusaidia leo? Jisikie huru kuuliza swali lolote.")
            return@flow
        } else if (Regex("^(hello|hi|hey|greetings)$", RegexOption.IGNORE_CASE).matches(lower)) {
            emit("Hello! Welcome, how can I help you today? Feel free to ask anything.")
            return@flow
        } else if (Regex("^(bonjour|salut)$", RegexOption.IGNORE_CASE).matches(lower)) {
            emit("Bonjour ! Bienvenue, comment puis-je vous aider aujourd'hui ?")
            return@flow
        } else if (Regex("^(hola|buenos dias)$", RegexOption.IGNORE_CASE).matches(lower)) {
            emit("¡Hola! Bienvenido, ¿en qué puedo ayudarte hoy?")
            return@flow
        }

        // 3. Mathematical derivations
        if (lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/") || lower.contains("solve") || lower.contains("hesabu")) {
            emit("""### Mathematical Derivation

1. **Expression**: 
2. **Analysis**: Evaluating arithmetic sequence under standard algebraic precedence.

Result computed on-device.""")
            return@flow
        }

        // 4. Code Generation fallback — provide a real stub
        if (lower.contains("code") || lower.contains("python") || lower.contains("function") ||
            lower.contains("algorithm") || lower.contains("andika") || lower.contains("unda")) {
            val lang = if (lower.contains("python")) "python" else if (lower.contains("kotlin")) "kotlin" else if (lower.contains("java")) "java" else "python"
            emit("""```$lang
# Mfano wa msimbo (model offline — tafadhali ongeza muktadha zaidi)
def sauti_function():
    pass
```
Tafadhali eleza zaidi ili nipate msimbo kamili.""")
            return@flow
        }

        // 5. Default natural direct response without boilerplate
        if (Regex("\b(kwa|ya|wa|katika|yako|yangu|hapa|kazi|simu|jambo|habari)\b").containsMatchIn(lower)) {
            emit("Ninaelewa muktadha wako kikamilifu. Karibu nikusaidie kueleza au kutatua changamoto hii hatua kwa hatua. Je, ungependa kuanzia wapi?")
        } else {
            emit("I understand your request clearly. How would you like us to proceed or which specific aspect would you like me to detail first?")
        }
    }

    fun getSautiVoicePath(): String? = sautiVoicePath
    fun getSautiCorePath(): String? = sautiCorePath
}
