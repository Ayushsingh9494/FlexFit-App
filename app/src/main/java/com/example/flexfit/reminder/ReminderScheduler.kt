package com.example.flexfit.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun createExactAlarmSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun scheduleReminder(reminder: ReminderData) {
        reminder.selectedDays.forEach { day ->
            scheduleReminderDay(reminder, day)
        }
    }

    fun scheduleReminderDay(
        reminder: ReminderData,
        day: ReminderDay,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ) {
        val triggerAtMillis = nextTriggerMillis(
            day = day,
            hour24 = reminder.hour24,
            minute = reminder.minute,
            referenceTimeMillis = referenceTimeMillis
        )
        val pendingIntent = buildPendingIntent(reminder.id, day)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelReminder(reminder: ReminderData) {
        ReminderDay.entries.forEach { day ->
            cancelReminderDay(reminder.id, day)
        }
    }

    fun cancelReminderDay(reminderId: String, day: ReminderDay) {
        val pendingIntent = buildPendingIntent(reminderId, day)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    internal fun nextTriggerMillis(
        day: ReminderDay,
        hour24: Int,
        minute: Int,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        val zoneId = ZoneId.systemDefault()
        val referenceTime = Instant.ofEpochMilli(referenceTimeMillis).atZone(zoneId)
        var targetTime = referenceTime
            .with(TemporalAdjusters.nextOrSame(day.javaDay))
            .withHour(hour24)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (!targetTime.isAfter(referenceTime)) {
            targetTime = targetTime.plusWeeks(1)
        }

        return targetTime.toInstant().toEpochMilli()
    }

    private fun buildPendingIntent(reminderId: String, day: ReminderDay): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_DAY, day.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(reminderId, day),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(reminderId: String, day: ReminderDay): Int {
        return "${reminderId}_${day.name}".hashCode()
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.flexfit.reminder.ACTION_TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_DAY = "extra_reminder_day"
    }
}
