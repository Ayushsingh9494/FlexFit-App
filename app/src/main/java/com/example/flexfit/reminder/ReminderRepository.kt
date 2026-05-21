package com.example.flexfit.reminder

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flexfit.firebase.firestore.ReminderRepository as FirestoreReminderRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private val Context.reminderDataStore by preferencesDataStore(name = "fluxfit_reminders")

class ReminderRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scheduler = ReminderScheduler(appContext)
    private val notificationManager = ReminderNotificationManager(appContext)
    private val mutex = Mutex()
    private val auth = FirebaseAuth.getInstance()
    private val firestoreRepository = FirestoreReminderRepository()
    private val cachedReminders: Flow<List<ReminderData>> = appContext.reminderDataStore.data.map { preferences ->
        decodeReminders(preferences[REMINDERS_KEY]).sortedWith(
            compareBy<ReminderData> { it.hour24 }
                .thenBy { it.minute }
                .thenBy { it.workoutName.lowercase(Locale.getDefault()) }
        )
    }

    val reminders: Flow<List<ReminderData>> = flow {
        emit(cachedReminders.first())
        val userId = auth.currentUser?.uid
        if (userId == null) {
            emit(cachedReminders.first())
            return@flow
        }
        firestoreRepository.observeReminders(userId).collect { remote ->
            val localReminders = remote.map { it.toLocalReminder() }.sortedWith(
                compareBy<ReminderData> { it.hour24 }
                    .thenBy { it.minute }
                    .thenBy { it.workoutName.lowercase(Locale.getDefault()) }
            )
            persist(localReminders)
            emit(localReminders)
        }
    }

    suspend fun getReminderById(reminderId: String): ReminderData? {
        return reminders.first().firstOrNull { it.id == reminderId }
    }

    suspend fun upsertReminder(reminder: ReminderData): ReminderData = mutex.withLock {
        val currentReminders = reminders.first()
        currentReminders.firstOrNull { it.id == reminder.id }?.let { existing ->
            scheduler.cancelReminder(existing)
            notificationManager.removeReminderChannel(existing.id)
        }

        val normalizedReminder = reminder.copy(
            workoutName = reminder.workoutName.trim(),
            motivationalNote = reminder.motivationalNote.trim(),
            ringtoneName = reminder.ringtoneName.ifBlank {
                resolveRingtoneTitle(appContext, reminder.ringtoneUri)
            }
        )

        val updatedReminders = (currentReminders.filterNot { it.id == normalizedReminder.id } + normalizedReminder)
            .sortedWith(
                compareBy<ReminderData> { it.hour24 }
                    .thenBy { it.minute }
                    .thenBy { it.workoutName.lowercase(Locale.getDefault()) }
            )

        persist(updatedReminders)

        if (normalizedReminder.enabled) {
            notificationManager.recreateReminderChannel(normalizedReminder)
            scheduler.scheduleReminder(normalizedReminder)
        }

        auth.currentUser?.uid?.let { userId ->
            firestoreRepository.upsertReminder(userId, normalizedReminder.toFirestoreReminder())
        }

        normalizedReminder
    }

    suspend fun deleteReminder(reminderId: String) = mutex.withLock {
        val currentReminders = reminders.first()
        currentReminders.firstOrNull { it.id == reminderId }?.let { reminder ->
            scheduler.cancelReminder(reminder)
            notificationManager.removeReminderChannel(reminder.id)
        }
        persist(currentReminders.filterNot { it.id == reminderId })
        auth.currentUser?.uid?.let { userId ->
            firestoreRepository.deleteReminder(userId, reminderId)
        }
    }

    suspend fun setReminderEnabled(reminderId: String, enabled: Boolean) = mutex.withLock {
        val currentReminders = reminders.first()
        val reminder = currentReminders.firstOrNull { it.id == reminderId } ?: return@withLock
        val updatedReminder = reminder.copy(enabled = enabled)

        scheduler.cancelReminder(reminder)
        notificationManager.removeReminderChannel(reminder.id)

        val updatedReminders = currentReminders.map {
            if (it.id == reminderId) updatedReminder else it
        }
        persist(updatedReminders)

        if (enabled) {
            notificationManager.recreateReminderChannel(updatedReminder)
            scheduler.scheduleReminder(updatedReminder)
        }

        auth.currentUser?.uid?.let { userId ->
            firestoreRepository.upsertReminder(userId, updatedReminder.toFirestoreReminder())
        }
    }

    suspend fun rescheduleEnabledReminders() = mutex.withLock {
        val currentReminders = reminders.first()
        currentReminders.forEach { reminder ->
            scheduler.cancelReminder(reminder)
            notificationManager.removeReminderChannel(reminder.id)
            if (reminder.enabled) {
                notificationManager.recreateReminderChannel(reminder)
                scheduler.scheduleReminder(reminder)
            }
        }
    }

    private suspend fun persist(reminders: List<ReminderData>) {
        appContext.reminderDataStore.edit { preferences ->
            preferences[REMINDERS_KEY] = encodeReminders(reminders)
        }
    }

    private fun encodeReminders(reminders: List<ReminderData>): String {
        return JSONArray().apply {
            reminders.forEach { reminder ->
                put(
                    JSONObject().apply {
                        put("id", reminder.id)
                        put("workoutName", reminder.workoutName)
                        put("selectedDays", JSONArray().apply {
                            reminder.selectedDays.forEach { day -> put(day.name) }
                        })
                        put("hour24", reminder.hour24)
                        put("minute", reminder.minute)
                        put("motivationalNote", reminder.motivationalNote)
                        put("ringtoneUri", reminder.ringtoneUri)
                        put("ringtoneName", reminder.ringtoneName)
                        put("vibrationEnabled", reminder.vibrationEnabled)
                        put("enabled", reminder.enabled)
                        put("createdAt", reminder.createdAt)
                    }
                )
            }
        }.toString()
    }

    private fun decodeReminders(payload: String?): List<ReminderData> {
        if (payload.isNullOrBlank()) return emptyList()

        return runCatching {
            val remindersArray = JSONArray(payload)
            buildList {
                for (index in 0 until remindersArray.length()) {
                    val item = remindersArray.optJSONObject(index) ?: continue
                    val dayArray = item.optJSONArray("selectedDays") ?: JSONArray()
                    val selectedDays = buildSet {
                        for (dayIndex in 0 until dayArray.length()) {
                            ReminderDay.fromStorageValue(dayArray.optString(dayIndex))?.let(::add)
                        }
                    }
                    add(
                        ReminderData(
                            id = item.optString("id"),
                            workoutName = item.optString("workoutName"),
                            selectedDays = selectedDays,
                            hour24 = item.optInt("hour24"),
                            minute = item.optInt("minute"),
                            motivationalNote = item.optString("motivationalNote"),
                            ringtoneUri = item.optString("ringtoneUri").ifBlank { null },
                            ringtoneName = item.optString("ringtoneName").ifBlank { DEFAULT_RINGTONE_NAME },
                            vibrationEnabled = item.optBoolean("vibrationEnabled", true),
                            enabled = item.optBoolean("enabled", true),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }.filter { it.workoutName.isNotBlank() && it.selectedDays.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    companion object {
        private val REMINDERS_KEY = stringPreferencesKey("reminders_payload")
    }
}

private fun ReminderData.toFirestoreReminder(): com.example.flexfit.firebase.models.ReminderData {
    return com.example.flexfit.firebase.models.ReminderData(
        id = id,
        title = workoutName,
        selectedDays = selectedDays.map { it.name },
        reminderTime = "%02d:%02d".format(hour24, minute),
        ringtoneUri = ringtoneUri,
        vibrationEnabled = vibrationEnabled,
        enabled = enabled,
        notes = motivationalNote,
        createdAt = createdAt
    )
}

private fun com.example.flexfit.firebase.models.ReminderData.toLocalReminder(): ReminderData {
    val parts = reminderTime.split(":")
    val parsedHour = parts.getOrNull(0)?.toIntOrNull() ?: 18
    val parsedMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return ReminderData(
        id = id,
        workoutName = title,
        selectedDays = selectedDays.mapNotNull(ReminderDay::fromStorageValue).toSet(),
        hour24 = parsedHour,
        minute = parsedMinute,
        motivationalNote = notes,
        ringtoneUri = ringtoneUri,
        ringtoneName = DEFAULT_RINGTONE_NAME,
        vibrationEnabled = vibrationEnabled,
        enabled = enabled,
        createdAt = createdAt
    )
}
