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
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/**
 * Resilient background worker that downloads model weights with HTTP Range resumption,
 * live background progress tracking, and atomic finalization.
 *
 * If internet drops or is disconnected, already downloaded bytes are preserved (.part file),
 * and the worker resumes from the exact byte offset when reconnected.
 */
class ModelDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "ModelDownloadWorker"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
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

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = SautiNotificationManager.createDownloadProgressNotification(
            context = context,
            modelName = "Msaidizi",
            progressPercent = 0,
            downloadedMb = 0,
            totalMb = 0
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

        // Verify if models already exist locally in private storage
        val voiceFile = File(sautiDir, "voice_lite.bin")
        val coreFile = File(sautiDir, "core_pro.bin")
        val coreLiteFile = File(sautiDir, "core_lite.bin")

        val hasVoice = voiceFile.exists() && voiceFile.length() > 1024 * 1024
        val hasCore = (coreFile.exists() && coreFile.length() > 1024 * 1024) ||
                      (coreLiteFile.exists() && coreLiteFile.length() > 1024 * 1024)

        if (hasVoice && hasCore) {
            Log.i(TAG, "Local models verified present. No download necessary.")
            return Result.success()
        }

        try {
            // Set as foreground service to ensure Android does not kill during background downloads
            try {
                setForeground(getForegroundInfo())
            } catch (e: Exception) {
                Log.w(TAG, "Foreground service notification skipped: ${e.message}")
            }

            // Fetch remote manifest
            val manifestRequest = Request.Builder().url(MANIFEST_URL).build()
            client.newCall(manifestRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Remote manifest unavailable (HTTP ${response.code}). Will retry when network permits.")
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
                        val targetFile = File(sautiDir, filename)

                        if (!targetFile.exists() || targetFile.length() < 1024 * 1024) {
                            Log.i(TAG, "Starting resilient download for '$id' from: $remoteUrl")
                            val success = downloadWithResumption(remoteUrl, targetFile, id)
                            if (!success) {
                                Log.w(TAG, "Download interrupted for '$id', saved progress for next retry.")
                                return Result.retry()
                            }
                        }
                    }
                }
            }

            // Remove download notification when completed
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(SautiNotificationManager.DOWNLOAD_NOTIFICATION_ID)

            // Notify user that Assistant is ready offline
            SautiNotificationManager.notifyTaskComplete(
                context = context,
                title = "Sauti Msaidizi / Assistant",
                previewText = "Mifano imepakuliwa kikamilifu. Sasa unaweza kutumia Sauti bila mtandao!"
            )

            Log.i(TAG, "Background model sync completed successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Background sync paused or network disconnected: ${e.message}")
            return Result.retry()
        }
    }

    /**
     * Downloads file in chunks with HTTP Range support.
     * Retains partial file (targetFile.part) on network failure so downloads resume smoothly.
     */
    private suspend fun downloadWithResumption(
        url: String,
        targetFile: File,
        displayName: String
    ): Boolean {
        val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
        val existingBytes = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(url)
        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            Log.i(TAG, "Resuming download from byte offset: $existingBytes for ${targetFile.name}")
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

                            // Update WorkManager progress state
                            setProgress(workDataOf(
                                "progress" to percent,
                                "downloaded_mb" to downloadedMb,
                                "total_mb" to totalMb,
                                "model_name" to displayName
                            ))

                            // Update persistent notification
                            val notification = SautiNotificationManager.createDownloadProgressNotification(
                                context = context,
                                modelName = displayName,
                                progressPercent = percent,
                                downloadedMb = downloadedMb,
                                totalMb = totalMb
                            )
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(SautiNotificationManager.DOWNLOAD_NOTIFICATION_ID, notification)
                        }
                    }
                    fos.flush()
                }
                fos.close()

                // Download completed: atomically rename .part to final target
                if (partFile.exists() && partFile.length() > 1024 * 1024) {
                    if (targetFile.exists()) targetFile.delete()
                    partFile.renameTo(targetFile)
                    Log.i(TAG, "Download and verification complete: ${targetFile.name} (${targetFile.length()} bytes)")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Download stream interrupted for ${targetFile.name}: ${e.message}")
        }
        return false
    }
}
