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

        // 2. Schedule background model sync (will safely succeed or be no-op if offline)
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
                Log.i(TAG, "Initializing native voice engine with: ${voiceFile.absolutePath} (${voiceFile.length()} bytes)")
                nativeInitVoiceEngine(sautiVoicePath!!)
            } else {
                Log.w(TAG, "Voice model not yet ready at ${voiceFile.absolutePath}")
            }
            if (coreFile.exists() && coreFile.length() > 1024 * 1024) {
                Log.i(TAG, "Initializing native core engine with: ${coreFile.absolutePath} (${coreFile.length()} bytes)")
                nativeInitCoreEngine(sautiCorePath!!)
            } else {
                Log.w(TAG, "Core language model not yet ready at ${coreFile.absolutePath}")
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
     * Streams tokens from on-device model using Gemma turn-based template formatting, strict language mirroring, and RAG context injection:
     * <start_of_turn>user\n[system instructions + context + user prompt]<end_of_turn>\n<start_of_turn>model\n
     */
    fun generateStreamingResponse(userPrompt: String): Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        val clean = userPrompt.trim()

        // Retrieve local domain context whenever available from Sovereign Knowledge Store
        val retrievedContext = if (knowledgeDatabase != null) {
            val chunks = knowledgeDatabase!!.searchKnowledge(clean, maxResults = 2)
            if (chunks.isNotEmpty()) {
                "\n[MUKTADHA WA NDANI / VERIFIED LOCAL CONTEXT]\n" + chunks.joinToString("\n") + "\n"
            } else ""
        } else ""

        val systemLanguageInstruction = """
            You are Sauti Assistant (Sauti Msaidizi), an authentic sovereign bilingual assistant fluent in Tanzanian Kiswahili and English.
            Always mirror the language of the user's message:
            - If the user writes in Kiswahili, reply strictly in fluent, natural Kiswahili as Sauti Msaidizi.
            - If the user writes in English, reply strictly in natural, concise English as Sauti Assistant.
            - If the user writes a code-switching mix, reply in authentic Tanzanian Kiswahili while respecting the technical terms.
            Never refer to yourself as AI. Refer to yourself as Assistant (English) or Msaidizi (Kiswahili).
            Provide direct, helpful, and accurate answers.
        """.trimIndent()

        // Gemma turn template: system prompt is placed directly within the first user turn
        val formattedPrompt = buildString {
            append("<start_of_turn>user\n")
            append(systemLanguageInstruction)
            if (retrievedContext.isNotBlank()) {
                append("\nBase your answer strictly on the provided verified context:\n")
                append(retrievedContext)
            }
            if (customUserInstructions.isNotBlank()) {
                append("\nUser Preferences: ").append(customUserInstructions)
            }
            append("\n\nUser Question:\n")
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

        // If native inference was unavailable or failed to produce tokens (e.g. weights loading/downloading), notify user cleanly
        if (!tokensEmitted) {
            val coreFile = sautiCorePath?.let { File(it) }
            val hasCoreModel = coreFile != null && coreFile.exists() && coreFile.length() > 1024 * 1024
            if (!hasCoreModel) {
                trySend("Uzito wa modeli ya Sauti unapakuliwa au kupakiwa kwenye kifaa. Tafadhali subiri kidogo kisha ujaribu tena.")
            } else {
                trySend("Inapakia modeli kwenye kumbukumbu ya kifaa, tafadhali subiri...")
            }
        }

        close()
        awaitClose { /* Cleanup when flow collection cancelled */ }
    }.flowOn(Dispatchers.IO)

    fun getSautiVoicePath(): String? = sautiVoicePath
    fun getSautiCorePath(): String? = sautiCorePath
}
