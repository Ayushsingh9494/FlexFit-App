package com.example.flexfit.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_TRIGGER_REMINDER) return

        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
        val dayName = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_DAY) ?: return
        val reminderDay = ReminderDay.fromStorageValue(dayName) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ReminderRepository(context)
                val notificationManager = ReminderNotificationManager(context)
                val scheduler = ReminderScheduler(context)
                val reminder = repository.getReminderById(reminderId)

                if (reminder != null && reminder.enabled && reminderDay in reminder.selectedDays) {
                    notificationManager.showReminderNotification(reminder)
                    scheduler.scheduleReminderDay(reminder, reminderDay)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
