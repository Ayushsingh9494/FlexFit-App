package com.example.flexfit.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.flexfit.MainActivity
import com.example.flexfit.R

class ReminderNotificationManager(context: Context) {

    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)

    fun recreateReminderChannel(reminder: ReminderData) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        removeReminderChannel(reminder.id)

        val channel = NotificationChannel(
            reminderChannelId(reminder.id),
            "FluxFit • ${reminder.workoutName}",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Workout reminder channel for ${reminder.workoutName}"
            enableVibration(reminder.vibrationEnabled)
            vibrationPattern = if (reminder.vibrationEnabled) {
                longArrayOf(0L, 180L, 90L, 220L)
            } else {
                longArrayOf(0L)
            }
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                reminder.ringtoneUri?.let(Uri::parse)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        notificationManager.createNotificationChannel(channel)
    }

    fun removeReminderChannel(reminderId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(reminderChannelId(reminderId))
        }
    }

    fun showReminderNotification(reminder: ReminderData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        recreateReminderChannel(reminder)

        val contentIntent = PendingIntent.getActivity(
            appContext,
            reminder.id.hashCode(),
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_REMINDER_STUDIO, true)
                putExtra(EXTRA_FOCUS_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, reminderChannelId(reminder.id))
            .setSmallIcon(R.drawable.ic_notification_fluxfit)
            .setColor(0xFF5DE2E7.toInt())
            .setContentTitle(reminder.workoutName)
            .setContentText(reminder.resolvedNote())
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.resolvedNote()))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val soundUri = reminder.ringtoneUri?.let(Uri::parse)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
            if (reminder.vibrationEnabled) {
                builder.setVibrate(longArrayOf(0L, 180L, 90L, 220L))
            }
        }

        NotificationManagerCompat.from(appContext)
            .notify(reminder.id.hashCode(), builder.build())
    }
}
