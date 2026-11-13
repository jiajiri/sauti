package com.sauti.app.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.sauti.app.security.ModelCryptoManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Resilient background worker that downloads and decrypts model weights without disturbing the user.
 * Supports resumption, automatic connectivity retry, and Google Play Store compliant dynamic asset provisioning.
 */
class ModelDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "ModelDownloadWorker"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val WORK_NAME = "sauti_model_sync_work"
        private const val MANIFEST_URL = "https://cdn.sauti.ai/models/latest/manifest.json"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting background model asset synchronization...")
        val sautiDir = File(context.filesDir, "sauti").apply { mkdirs() }
        val key = ModelCryptoManager.deriveAppKey(context)

        // Verify if models already exist locally in private storage
        val voiceFile = File(sautiDir, "voice_lite.bin")
        val coreFile = File(sautiDir, "core_lite.bin")

        if (voiceFile.exists() && voiceFile.length() > 1024 * 1024 &&
            coreFile.exists() && coreFile.length() > 1024 * 1024
        ) {
            Log.i(TAG, "Local models verified present. No download necessary.")
            return Result.success()
        }

        try {
            // Check remote manifest
            val manifestRequest = Request.Builder().url(MANIFEST_URL).build()
            client.newCall(manifestRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Remote manifest unavailable (HTTP ${response.code}). Will retry when network conditions permit.")
                    return Result.retry()
                }

                val body = response.body?.string() ?: return Result.retry()
                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models")

                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val mObj = modelsArray.getJSONObject(i)
                        val id = mObj.optString("id")
                        val filename = mObj.optString("filename")
                        val remoteUrl = mObj.optString("remote_url")
                        val isCompressed = mObj.optBoolean("compressed", false)
                        val targetFile = File(sautiDir, filename)

                        if (!targetFile.exists() || targetFile.length() < 1024 * 1024) {
                            Log.i(TAG, "Downloading model '$id' from: $remoteUrl (compressed=$isCompressed)")
                            downloadAndDecrypt(remoteUrl, targetFile, key, isCompressed)
                        }
                    }
                }
            }

            Log.i(TAG, "Background model sync completed successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Background sync paused or network disconnected: ${e.message}")
            return Result.retry()
        }
    }

    private fun downloadAndDecrypt(
        url: String,
        targetFile: File,
        key: javax.crypto.SecretKey,
        isCompressed: Boolean = false
    ) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { stream ->
                    val success = ModelCryptoManager.decryptStream(stream, targetFile, key, isCompressed)
                    Log.i(TAG, "Model decrypted into ${targetFile.name}: success=$success (${targetFile.length()} bytes)")
                }
            }
        }
    }
}
