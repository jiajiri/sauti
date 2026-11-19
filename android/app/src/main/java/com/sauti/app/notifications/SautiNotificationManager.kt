package com.sauti.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sauti.app.MainActivity
import com.sauti.app.R

/**
 * Enterprise Notification Engine for Sauti Assistant / Sauti Msaidizi.
 * Manages foreground download progress notifications and background task completion alerts.
 */
object SautiNotificationManager {

    const val CHANNEL_DOWNLOAD_ID = "sauti_model_download"
    const val CHANNEL_COMPLETION_ID = "sauti_task_completion"
    const val DOWNLOAD_NOTIFICATION_ID = 2001
    const val COMPLETION_NOTIFICATION_ID = 2002

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Model download progress channel (DEFAULT importance ensures visibility in notification drawer)
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_ID,
                "Model Downloads / Upakuaji wa Mifano",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live download progress for offline models"
                setShowBadge(true)
            }

            // 2. Task completion channel
            val completionChannel = NotificationChannel(
                CHANNEL_COMPLETION_ID,
                "Assistant Updates / Taarifa za Msaidizi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when background requests and reasoning finish"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(downloadChannel)
            notificationManager.createNotificationChannel(completionChannel)
        }
    }

    /**
     * Builds a progress notification for the ongoing model download.
     */
    fun createDownloadProgressNotification(
        context: Context,
        modelName: String,
        progressPercent: Int,
        downloadedMb: Long,
        totalMb: Long
    ): android.app.Notification {
        ensureNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (progressPercent in 0..99) {
            "Sauti Msaidizi: Inapakua $modelName ($progressPercent%)"
        } else {
            "Sauti Msaidizi: Inakamilisha ufungaji..."
        }

        val content = if (totalMb > 0) {
            "$downloadedMb MB / $totalMb MB ($progressPercent%)"
        } else {
            "$downloadedMb MB imepakuliwa..."
        }

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(R.drawable.ic_sauti_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(100, progressPercent.coerceIn(0, 100), totalMb <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Dispatches a notification when background question/task processing finishes.
     */
    fun notifyTaskComplete(
        context: Context,
        title: String,
        previewText: String
    ) {
        ensureNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val cleanPreview = previewText
            .replace(Regex("(?i)<br\\s*/?>"), " ")
            .replace(Regex("(?i)&lt;br\\s*/?&gt;"), " ")
            .trim()
            .take(120)

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION_ID)
            .setSmallIcon(R.drawable.ic_sauti_notification)
            .setContentTitle(title)
            .setContentText(cleanPreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanPreview))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }
}
