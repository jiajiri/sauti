package com.sauti.app.sauti

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Sauti Native Asset Registration & Engine Loader.
 * Manages zero-copy loading for Sauti Voice Recognition and Sauti Core.
 */
object SautiEngineLoader {

    private const val TAG = "SautiEngineLoader"

    // Asset Directory Paths
    const val ASSET_SAUTI_VOICE = "sauti/voice.bin"
    const val ASSET_SAUTI_CORE = "sauti/core.bin"

    private var sautiVoicePath: String? = null
    private var sautiCorePath: String? = null

    /**
     * Extracts or links asset files into private application storage for zero-copy mmap.
     */
    fun ensureAssetsRegistered(context: Context): Pair<String, String> {
        val sautiDir = File(context.filesDir, "sauti")
        if (!sautiDir.exists()) {
            sautiDir.mkdirs()
        }

        val voiceFile = File(sautiDir, "voice.bin")
        if (!voiceFile.exists() || voiceFile.length() == 0L) {
            copyAssetToFile(context, ASSET_SAUTI_VOICE, voiceFile)
        }

        val coreFile = File(sautiDir, "core.bin")
        if (!coreFile.exists() || coreFile.length() == 0L) {
            copyAssetToFile(context, ASSET_SAUTI_CORE, coreFile)
        }

        sautiVoicePath = voiceFile.absolutePath
        sautiCorePath = coreFile.absolutePath

        return Pair(sautiVoicePath!!, sautiCorePath!!)
    }

    private fun copyAssetToFile(context: Context, assetPath: String, destFile: File) {
        try {
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
            Log.w(TAG, "Asset $assetPath could not be copied directly: ${e.message}")
        }
    }

    fun getSautiVoicePath(): String? = sautiVoicePath
    fun getSautiCorePath(): String? = sautiCorePath
}
