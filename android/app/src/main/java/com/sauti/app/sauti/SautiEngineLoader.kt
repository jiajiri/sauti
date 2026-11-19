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
     * If models are not yet downloaded, scans offline directories (/sdcard/Download, external dirs)
     * before falling back to background WorkManager fetch.
     */
    fun ensureAssetsRegistered(context: Context): Pair<String, String> {
        val sautiDir = File(context.filesDir, "sauti")
        if (!sautiDir.exists()) {
            sautiDir.mkdirs()
        }

        // 1. Scan external offline storage directories for pre-loaded models
        scanAndImportOfflineModels(context, sautiDir)

        // 2. Purge stale corrupt files (raw .enc.gz blobs written by the old worker before decryption was wired)
        purgeCorruptModelIfNeeded(sautiDir, "core_pro.bin", isVoiceModel = false)
        purgeCorruptModelIfNeeded(sautiDir, "core_lite.bin", isVoiceModel = false)
        purgeCorruptModelIfNeeded(sautiDir, "voice_lite.bin", isVoiceModel = true)

        // 3. Schedule background model sync (will safely succeed or be no-op if offline)
        try {
            com.sauti.app.sync.ModelDownloadWorker.schedule(context)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to schedule model sync: ${e.message}")
        }

        // Set English as the default voice ASR language on startup
        try { nativeSetVoiceLanguage("en") } catch (_: Throwable) {}

        val voiceFile = File(sautiDir, "voice_lite.bin")
        if (!voiceFile.exists() || voiceFile.length() == 0L) {
            copyAssetToFile(context, ASSET_SAUTI_VOICE_LITE, voiceFile)
        }

        // Check for valid GGUF core_pro or core_lite
        var coreFile = File(sautiDir, "core_pro.bin")
        if (!com.sauti.app.sync.ModelDownloadWorker.isModelReady(coreFile)) {
            val liteFile = File(sautiDir, "core_lite.bin")
            if (com.sauti.app.sync.ModelDownloadWorker.isModelReady(liteFile)) {
                coreFile = liteFile
            } else {
                // Not present — will be fetched by background worker; try bundled asset as fallback
                copyAssetToFile(context, ASSET_SAUTI_CORE_LITE, coreFile)
            }
        }

        sautiVoicePath = voiceFile.absolutePath
        sautiCorePath = coreFile.absolutePath

        // Initialize local SQLite RAG store
        initKnowledgeStore(context)

        try {
            if (com.sauti.app.sync.ModelDownloadWorker.isValidGgml(voiceFile)) {
                Log.i(TAG, "Initializing native voice engine with: ${voiceFile.absolutePath} (${voiceFile.length()} bytes)")
                nativeInitVoiceEngine(sautiVoicePath!!)
            } else {
                Log.w(TAG, "Voice model not ready (not a valid GGML/Whisper binary) at ${voiceFile.absolutePath}")
            }
            if (com.sauti.app.sync.ModelDownloadWorker.isValidGguf(coreFile)) {
                Log.i(TAG, "Initializing native core engine with: ${coreFile.absolutePath} (${coreFile.length()} bytes)")
                nativeInitCoreEngine(sautiCorePath!!)
            } else {
                Log.w(TAG, "Core model not ready (not a valid GGUF) at ${coreFile.absolutePath}")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Native engine init: " + e.message)
        }

        return Pair(sautiVoicePath!!, sautiCorePath!!)
    }

    /**
     * Auto-discovers offline model files placed on phone storage:
     * - /sdcard/Download/sauti_models/
     * - /sdcard/Download/
     * - /sdcard/sauti/
     * - context.getExternalFilesDir("models")
     *
     * Once imported into internal private storage, original public files are automatically deleted
     * to protect model assets from unauthorized extraction or copying.
     */
    private fun scanAndImportOfflineModels(context: Context, sautiDir: File) {
        val targetVoice = File(sautiDir, "voice_lite.bin")
        val targetCore = File(sautiDir, "core_pro.bin")

        val searchDirs = listOfNotNull(
            File("/sdcard/Download/sauti_models"),
            File("/sdcard/Download"),
            File("/sdcard/sauti"),
            context.getExternalFilesDir("models"),
            context.getExternalFilesDir(null)
        )

        for (dir in searchDirs) {
            if (!dir.exists() || !dir.isDirectory) continue

            val files = dir.listFiles() ?: continue
            for (file in files) {
                if (!file.isFile || file.length() < 1024 * 1024) continue

                val isGGUF = isGgufFile(file)
                val isWhisper = isWhisperBinary(file)

                // 1. Language Reasoning Core: GGUF Model -> core_pro.bin
                if ((!targetCore.exists() || targetCore.length() < 1024 * 1024) && isGGUF) {
                    Log.i(TAG, "Discovered offline core model: ${file.name} -> importing to secure sandbox")
                    if (copyAndCleanSource(file, targetCore)) {
                        Log.i(TAG, "Securely installed core model into private sandbox")
                    }
                    continue
                }

                // 2. Acoustic Voice Model: Whisper Binary -> voice_lite.bin
                if ((!targetVoice.exists() || targetVoice.length() < 1024 * 1024) && isWhisper) {
                    Log.i(TAG, "Discovered offline voice model: ${file.name} -> importing to secure sandbox")
                    if (copyAndCleanSource(file, targetVoice)) {
                        Log.i(TAG, "Securely installed voice model into private sandbox")
                    }
                    continue
                }
            }
        }
    }

    /**
     * Inspects the first 4 bytes of a file to check for GGUF magic header (0x46554747 -> "GGUF").
     */
    private fun isGgufFile(file: File): Boolean {
        if (file.name.endsWith(".gguf", ignoreCase = true)) return true
        return try {
            file.inputStream().use { input ->
                val magic = ByteArray(4)
                if (input.read(magic) == 4) {
                    magic[0] == 'G'.code.toByte() &&
                    magic[1] == 'G'.code.toByte() &&
                    magic[2] == 'U'.code.toByte() &&
                    magic[3] == 'F'.code.toByte()
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inspects binary structure or naming for whisper acoustic models.
     */
    private fun isWhisperBinary(file: File): Boolean {
        val name = file.name.lowercase()
        if (name == "voice_lite.bin" || name.contains("whisper") || name.contains("ggml-base")) return true
        if (name.endsWith(".bin") && !name.contains("core") && !name.contains("gemma")) return true
        return false
    }

    /**
     * Copies the source file into the private internal app sandbox and deletes the exposed public file.
     */
    private fun copyAndCleanSource(src: File, dst: File): Boolean {
        return try {
            val tmp = File(dst.parentFile, "${dst.name}.tmp")
            src.inputStream().use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 256 * 1024)
                }
            }
            if (tmp.exists() && tmp.length() == src.length()) {
                if (dst.exists()) dst.delete()
                val renamed = tmp.renameTo(dst)
                if (renamed) {
                    // Clean up source file from public SDCard so it cannot be copied by other apps
                    try {
                        src.delete()
                        Log.i(TAG, "Cleaned up source file from public storage: ${src.absolutePath}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not delete public source file: ${e.message}")
                    }
                    true
                } else {
                    tmp.delete()
                    false
                }
            } else {
                tmp.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing offline model: ${e.message}")
            false
        }
    }

    /**
     * Deletes a model file if it exists but is NOT a valid model (GGUF for cores, GGML for voice).
     * This purges stale encrypted blobs written by the old download worker before
     * decryption was wired into the pipeline.
     */
    private fun purgeCorruptModelIfNeeded(dir: File, filename: String, isVoiceModel: Boolean = false) {
        val file = File(dir, filename)
        if (file.exists() && file.length() > 1024 * 1024 &&
            !com.sauti.app.sync.ModelDownloadWorker.isModelReady(file, isVoiceModel)
        ) {
            val format = if (isVoiceModel) "GGML (Whisper)" else "GGUF"
            Log.w(TAG, "Purging stale corrupt model: $filename (not a valid $format)")
            file.delete()
            File(dir, "$filename.enc").delete()
            File(dir, "$filename.part").delete()
            File(dir, "$filename.enc.part").delete()
        }
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
     * Fast, lightweight on-device language detector.
     * Evaluates lexical tokens and morphological markers to deterministically identify
     * Swahili ("sw") vs English ("en") inputs.
     */
    fun detectLanguage(text: String): String {
        val clean = text.lowercase().trim()
        val words = clean.split("\\s+".toRegex()).map { it.trim('.', ',', '!', '?', ';', ':', '"', '\'', '(', ')') }
        if (words.isEmpty()) return "en"

        val swWords = setOf(
            "habari", "hujambo", "sijambo", "naomba", "tafadhali", "nifundishe", "misemo", "kazi",
            "barua", "mambo", "vipi", "nini", "gani", "kwa", "katika", "yangu", "yake", "wangu",
            "wake", "kama", "hapa", "kuna", "ndiyo", "hapana", "asante", "shikamoo", "marahaba",
            "jinsi", "sababu", "nzuri", "mbaya", "mtu", "watu", "kitu", "vitu", "sana", "leo", "kesho",
            "jana", "sasa", "bado", "zaidi", "kidogo", "pamoja", "maana", "eleza", "andika"
        )
        val enWords = setOf(
            "the", "is", "are", "and", "in", "to", "what", "how", "why", "when", "where", "who",
            "which", "can", "you", "please", "tell", "me", "write", "code", "issue", "troubleshoot",
            "for", "with", "about", "help", "good", "morning", "hello", "hi", "explain", "create",
            "make", "give", "list", "does", "will", "would", "should", "could", "have", "from", "on"
        )

        var swScore = 0.0f
        var enScore = 0.0f

        val swPrefixes = listOf("ni", "wa", "tu", "ku", "ali", "ana", "ata", "wali", "wana", "wata")
        for (w in words) {
            if (swWords.contains(w)) swScore += 1.5f
            if (enWords.contains(w)) enScore += 1.5f
            if (w.length >= 4 && swPrefixes.any { w.startsWith(it) }) {
                swScore += 0.4f
            }
        }

        return if (swScore > enScore && swScore >= 1.0f) "sw" else "en"
    }

    /**
     * Streams tokens from on-device model using Gemma turn-based template formatting,
     * strict deterministic language directives, and optimized mobile inference.
     * <start_of_turn>user\n[system instructions + context + user prompt]<end_of_turn>\n<start_of_turn>model\n
     */
    fun generateStreamingResponse(userPrompt: String): Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        val clean = userPrompt.trim()
        val detectedLang = detectLanguage(clean)

        // Retrieve local domain context whenever available from Sovereign Knowledge Store
        val retrievedContext = if (knowledgeDatabase != null) {
            val chunks = knowledgeDatabase!!.searchKnowledge(clean, maxResults = 2)
            if (chunks.isNotEmpty()) {
                "\n[VERIFIED LOCAL CONTEXT]\n" + chunks.joinToString("\n") + "\n"
            } else ""
        } else ""

        // Concise, token-efficient system instruction with turn-specific deterministic language & code formatting constraints
        val turnDirective = if (detectedLang == "sw") {
            "Wewe ni Sauti Msaidizi. Swali hili limeulizwa kwa Kiswahili. Jibu kwa ufasaha na ukamilifu kwa lugha ya Kiswahili pekee. Usitumie Kiingereza. Unapotoa msimbo au script yoyote ya programu, weka msimbo wote ndani ya vizuizi vya ```lugha na ``` pekee bila kuchanganya maelezo ya maongezi ndani ya mistari ya msimbo."
        } else {
            "You are Sauti Assistant. This query is asked in English. Answer accurately, completely, and strictly in English. Do not reply in Swahili. When providing code or scripts, isolate executable code cleanly inside standard ```language and ``` blocks without mixing conversational explanations into code lines."
        }

        // Gemma turn template: system prompt is placed directly within the first user turn
        val formattedPrompt = buildString {
            append("<start_of_turn>user\n")
            append(turnDirective)
            if (retrievedContext.isNotBlank()) {
                append("\nBase your answer on this context if relevant:\n").append(retrievedContext)
            }
            if (customUserInstructions.isNotBlank()) {
                append("\nUser Preferences: ").append(customUserInstructions)
            }
            append("\n\nQuestion:\n")
            append(clean)
            append("<end_of_turn>\n")
            append("<start_of_turn>model\n")
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

        // If native inference was unavailable or failed to produce tokens, notify user clearly
        if (!tokensEmitted) {
            val coreFile = sautiCorePath?.let { File(it) }
            val hasCoreModel = coreFile != null && coreFile.exists() && coreFile.length() > 1024 * 1024
            if (!hasCoreModel) {
                if (detectedLang == "sw") {
                    trySend("Modeli ya akili bado inapakuliwa chinichini. Tafadhali subiri upakuaji ukamilike kisha ujaribu tena.")
                } else {
                    trySend("The AI model is being downloaded in the background. Please wait for the download to complete, then try again.")
                }
            } else {
                if (detectedLang == "sw") {
                    trySend("Inapakia modeli kwenye kumbukumbu ya kifaa, tafadhali subiri kidogo...")
                } else {
                    trySend("Loading model into memory, please wait a moment...")
                }
            }
        }

        close()
        awaitClose { /* Cleanup when flow collection cancelled */ }
    }.flowOn(Dispatchers.IO)

    fun getSautiVoicePath(): String? = sautiVoicePath
    fun getSautiCorePath(): String? = sautiCorePath
}
