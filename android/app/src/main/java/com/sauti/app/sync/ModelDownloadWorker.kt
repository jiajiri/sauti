package com.sauti.app.sync

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.*
import com.sauti.app.notifications.SautiNotificationManager
import com.sauti.app.security.ModelCryptoManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Resilient background worker that downloads, decrypts, decompresses, and verifies
 * model weights with HTTP Range resumption, live progress tracking, and atomic finalization.
 *
 * Pipeline per model:
 *   1. HTTP Range download → filename.part (resumable across network interruptions)
 *   2. Rename .part → filename.enc (raw encrypted+compressed blob)
 *   3. AES-256-GCM decrypt + GZIP decompress → plaintext GGUF
 *   4. SHA-256 integrity verification against manifest hash
 *   5. Atomic rename → final filename
 */
class ModelDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "ModelDownloadWorker"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    companion object {
        const val WORK_NAME = "sauti_model_sync_work"
        private const val MANIFEST_URL = com.sauti.app.SautiConfig.MODEL_MANIFEST_URL

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            // REPLACE: cancels any stale or failed prior job so a fresh clean run always starts
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        /**
         * Checks first 4 bytes for GGUF magic header (0x47 0x47 0x55 0x46 = "GGUF").
         * Used for core language models (Gemma, Qwen, etc.).
         */
        fun isValidGguf(file: File): Boolean {
            if (!file.exists() || file.length() < 1024 * 1024) return false
            return try {
                file.inputStream().use { s ->
                    val magic = ByteArray(4)
                    s.read(magic) == 4 &&
                        magic[0] == 'G'.code.toByte() &&
                        magic[1] == 'G'.code.toByte() &&
                        magic[2] == 'U'.code.toByte() &&
                        magic[3] == 'F'.code.toByte()
                }
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Checks first 4 bytes for GGML magic header (0x67 0x67 0x6d 0x6c = "ggml" little-endian "lmgg").
         * Whisper .bin files use GGML format, NOT GGUF.
         * Python hexdump confirmed: voice_lite.bin first bytes = 6c 6d 67 67 ("lmgg").
         */
        fun isValidGgml(file: File): Boolean {
            if (!file.exists() || file.length() < 1024 * 1024) return false
            return try {
                file.inputStream().use { s ->
                    val magic = ByteArray(4)
                    s.read(magic) == 4 &&
                        magic[0] == 0x6c.toByte() &&  // 'l'
                        magic[1] == 0x6d.toByte() &&  // 'm'
                        magic[2] == 0x67.toByte() &&  // 'g'
                        magic[3] == 0x67.toByte()     // 'g'
                }
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Returns true if the model file is valid based on its type.
         * Voice models (Whisper) use GGML format; core models use GGUF format.
         */
        fun isModelReady(file: File, isVoiceModel: Boolean = false): Boolean =
            if (isVoiceModel) isValidGgml(file) else isValidGguf(file)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val lang = SautiNotificationManager.resolveSystemLanguage(context)
        val defaultModelName = if (lang == "sw") "Msaidizi" else "Assistant"
        val notification = SautiNotificationManager.createDownloadProgressNotification(
            context = context,
            modelName = defaultModelName,
            progressPercent = 0,
            downloadedMb = 0,
            totalMb = 0,
            lang = lang
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                SautiNotificationManager.DOWNLOAD_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                SautiNotificationManager.DOWNLOAD_NOTIFICATION_ID,
                notification
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting resilient background model asset synchronization...")
        val sautiDir = File(context.filesDir, "sauti").apply { mkdirs() }
        val key = ModelCryptoManager.deriveAppKey(context)

        val voiceFile = File(sautiDir, "voice_lite.bin")
        val coreFile = File(sautiDir, "core_pro.bin")
        val coreLiteFile = File(sautiDir, "core_lite.bin")

        // Purge stale corrupt files — voice uses GGML, cores use GGUF
        purgeCorruptIfNeeded(coreFile, isVoiceModel = false)
        purgeCorruptIfNeeded(coreLiteFile, isVoiceModel = false)
        purgeCorruptIfNeeded(voiceFile, isVoiceModel = true)

        val hasVoice = isValidGgml(voiceFile)
        val hasCore = isValidGguf(coreFile) || isValidGguf(coreLiteFile)

        if (hasVoice && hasCore) {
            Log.i(TAG, "Local models verified as valid GGUF. No download necessary.")
            return Result.success()
        }

        try {
            // Promote to foreground so Android does not kill during large downloads
            try {
                setForeground(getForegroundInfo())
            } catch (e: Exception) {
                Log.w(TAG, "Foreground service notification skipped: ${e.message}")
            }

            // Fetch remote manifest
            val manifestRequest = Request.Builder().url(MANIFEST_URL).build()
            client.newCall(manifestRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Remote manifest unavailable (HTTP ${response.code}). Will retry.")
                    return Result.retry()
                }

                val body = response.body?.string() ?: return Result.retry()
                val json = JSONObject(body)

                // OPTION A: Single-request unified bundle (both Whisper Multilingual + Gemma 3 1B)
                val bundleObj = json.optJSONObject("bundle")
                if (bundleObj != null) {
                    val bundleUrl = bundleObj.optString("remote_url")
                    val isCompressed = bundleObj.optBoolean("compressed", true)
                    val bundleEncFile = File(sautiDir, "bundle.bin.enc")

                    Log.i(TAG, "Executing single-request unified model bundle download from $bundleUrl...")
                    val success = downloadWithResumption(bundleUrl, bundleEncFile, "Unified Models Bundle")
                    if (!success) {
                        Log.w(TAG, "Bundle download interrupted, progress saved for resumption.")
                        return Result.retry()
                    }

                    Log.i(TAG, "Decrypting and extracting unified model archive with 1MB high-speed I/O...")
                    setProgress(workDataOf(
                        "progress" to 95,
                        "status" to "extracting",
                        "model_name" to "Extracting model weights..."
                    ))

                    val extracted = bundleEncFile.inputStream().use { stream ->
                        ModelCryptoManager.decryptAndExtractZipStream(
                            encryptedStream = stream,
                            targetDir = sautiDir,
                            key = key,
                            isGzipCompressed = isCompressed
                        )
                    }
                    bundleEncFile.delete()

                    setProgress(workDataOf(
                        "progress" to 100,
                        "status" to "extracted",
                        "model_name" to "Model ready"
                    ))

                    if (extracted.isEmpty() || !hasVoiceAndCore(sautiDir)) {
                        Log.e(TAG, "Unified bundle extraction failed or models invalid.")
                        return Result.retry()
                    }

                    // Auto-trigger native model initialization immediately upon successful extraction
                    try {
                        val coreFile = File(sautiDir, "core_pro.bin")
                        val voiceFile = File(sautiDir, "voice_lite.bin")
                        if (isValidGguf(coreFile)) {
                            com.sauti.app.sauti.SautiEngineLoader.ensureAssetsRegistered(context)
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Immediate engine pre-warm on worker finished: ${e.message}")
                    }

                    Log.i(TAG, "Unified bundle successfully extracted and verified: ${extracted.keys}")
                } else {
                    // OPTION B: Fallback to individual model entries
                    val modelsArray = json.optJSONArray("models") ?: return Result.retry()
                    for (i in 0 until modelsArray.length()) {
                        val mObj = modelsArray.getJSONObject(i)
                        val id           = mObj.optString("id")
                        val filename     = mObj.optString("filename")
                        val remoteUrl    = mObj.optString("remote_url")
                        val expectedSha  = mObj.optString("sha256")
                        val isCompressed = mObj.optBoolean("compressed", false)
                        val targetFile   = File(sautiDir, filename)

                        val isVoiceModel = id.startsWith("voice")
                        if (isModelReady(targetFile, isVoiceModel)) {
                            Log.i(TAG, "Model '$id' already valid — skipping download.")
                            continue
                        }

                        Log.i(TAG, "Starting download for '$id' → $filename from $remoteUrl")
                        val encFile = File(sautiDir, "$filename.enc")
                        val success = downloadWithResumption(remoteUrl, encFile, id)
                        if (!success) return Result.retry()

                        val decryptOk = encFile.inputStream().use { stream ->
                            ModelCryptoManager.decryptStream(
                                encryptedStream = stream,
                                targetFile = targetFile,
                                key = key,
                                isGzipCompressed = isCompressed
                            )
                        }
                        encFile.delete()
                        if (!decryptOk) return Result.retry()

                        if (expectedSha.isNotBlank()) {
                            val actualSha = ModelCryptoManager.computeSha256(targetFile)
                            if (actualSha != expectedSha) {
                                targetFile.delete()
                                return Result.retry()
                            }
                        }

                        if (!isModelReady(targetFile, isVoiceModel)) {
                            targetFile.delete()
                            return Result.retry()
                        }
                    }
                }
            }

            // All models ready: cancel the download progress notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(SautiNotificationManager.DOWNLOAD_NOTIFICATION_ID)

            // Notify user that the Assistant is fully ready offline
            SautiNotificationManager.notifyTaskComplete(
                context = context,
                title = "Sauti Msaidizi tayari / Assistant Ready",
                previewText = "Mifano imepakuliwa kikamilifu. Sasa unaweza kutumia Sauti bila mtandao!"
            )

            Log.i(TAG, "Background model sync completed and verified successfully.")
            return Result.success()

        } catch (e: Exception) {
            Log.w(TAG, "Background sync paused or network disconnected: ${e.message}")
            return Result.retry()
        }
    }

    private fun hasVoiceAndCore(sautiDir: File): Boolean {
        val voiceFile = File(sautiDir, "voice_lite.bin")
        val voiceEnFile = File(sautiDir, "voice_en.bin")
        val coreFile = File(sautiDir, "core_pro.bin")
        val coreLiteFile = File(sautiDir, "core_lite.bin")
        val hasVoice = isValidGgml(voiceFile) || isValidGgml(voiceEnFile)
        val hasCore = isValidGguf(coreFile) || isValidGguf(coreLiteFile)
        return hasVoice && hasCore
    }

    /**
     * If a file exists but is NOT a valid model (GGUF for cores, GGML for voice), delete it
     * and any associated .part file so the correct decrypt pipeline runs fresh.
     */
    private fun purgeCorruptIfNeeded(file: File, isVoiceModel: Boolean = false) {
        if (file.exists() && file.length() > 1024 * 1024 && !isModelReady(file, isVoiceModel)) {
            Log.w(TAG, "Stale corrupt model at ${file.name} — purging for clean re-download")
            file.delete()
            File(file.parentFile, "${file.name}.enc").delete()
            File(file.parentFile, "${file.name}.enc.part").delete()
            File(file.parentFile, "${file.name}.part").delete()
            File(file.parentFile, "${file.name}.tmp").delete()
        }
    }

    /**
     * Downloads the encrypted blob in chunks with HTTP Range support.
     * Uses a .enc.part temporary file so partial progress is preserved on network failure.
     */
    private suspend fun downloadWithResumption(
        url: String,
        targetEncFile: File,
        displayName: String
    ): Boolean {
        val partFile = File(targetEncFile.parentFile, "${targetEncFile.name}.part")
        val existingBytes = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(url)
        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            Log.i(TAG, "Resuming download from byte $existingBytes for ${targetEncFile.name}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val code = response.code
                if (code != 200 && code != 206) {
                    Log.w(TAG, "Server responded with HTTP $code for $url")
                    return false
                }

                val responseBody = response.body ?: return false
                val contentLength = responseBody.contentLength()
                val totalBytes = if (code == 206) existingBytes + contentLength else contentLength

                val isAppend = (code == 206 && existingBytes > 0)
                val fos = FileOutputStream(partFile, isAppend)

                responseBody.byteStream().use { inputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = if (isAppend) existingBytes else 0L
                    var lastNotifiedPercent = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val percent = if (totalBytes > 0) {
                            ((totalDownloaded * 100) / totalBytes).toInt()
                        } else 0

                        if (percent != lastNotifiedPercent) {
                            lastNotifiedPercent = percent
                            val downloadedMb = totalDownloaded / (1024 * 1024)
                            val totalMb = totalBytes / (1024 * 1024)

                            // Publish live progress to WorkManager observers (ChatScreen banner)
                            setProgress(workDataOf(
                                "progress" to percent,
                                "downloaded_mb" to downloadedMb,
                                "total_mb" to totalMb,
                                "model_name" to displayName
                            ))

                            // Update persistent status bar notification with active language
                            val lang = SautiNotificationManager.resolveSystemLanguage(context)
                            val notification = SautiNotificationManager.createDownloadProgressNotification(
                                context = context,
                                modelName = displayName,
                                progressPercent = percent,
                                downloadedMb = downloadedMb,
                                totalMb = totalMb,
                                lang = lang
                            )
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(SautiNotificationManager.DOWNLOAD_NOTIFICATION_ID, notification)
                        }
                    }
                    fos.flush()
                }
                fos.close()

                // Rename .part to final encrypted blob file
                if (partFile.exists() && partFile.length() > 1024 * 1024) {
                    if (targetEncFile.exists()) targetEncFile.delete()
                    partFile.renameTo(targetEncFile)
                    Log.i(TAG, "Download complete: ${targetEncFile.name} (${targetEncFile.length()} bytes)")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Download stream interrupted for ${targetEncFile.name}: ${e.message}")
        }
        return false
    }
}
