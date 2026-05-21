package com.example.flexfit.reminder

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

const val EXTRA_OPEN_REMINDER_STUDIO = "extra_open_reminder_studio"
const val EXTRA_FOCUS_REMINDER_ID = "extra_focus_reminder_id"

@Immutable
enum class ReminderDay(
    val shortLabel: String,
    val shortDisplayLabel: String,
    val javaDay: DayOfWeek
) {
    MONDAY("Mon", "M", DayOfWeek.MONDAY),
    TUESDAY("Tue", "T", DayOfWeek.TUESDAY),
    WEDNESDAY("Wed", "W", DayOfWeek.WEDNESDAY),
    THURSDAY("Thu", "T", DayOfWeek.THURSDAY),
    FRIDAY("Fri", "F", DayOfWeek.FRIDAY),
    SATURDAY("Sat", "S", DayOfWeek.SATURDAY),
    SUNDAY("Sun", "S", DayOfWeek.SUNDAY);

    companion object {
        val ordered: List<ReminderDay> = entries.toList()

        fun fromStorageValue(value: String?): ReminderDay? {
            return entries.firstOrNull { it.name == value }
        }
    }
}

@Immutable
data class ReminderData(
    val id: String = UUID.randomUUID().toString(),
    val workoutName: String,
    val selectedDays: Set<ReminderDay>,
    val hour24: Int,
    val minute: Int,
    val motivationalNote: String = "",
    val ringtoneUri: String? = null,
    val ringtoneName: String = DEFAULT_RINGTONE_NAME,
    val vibrationEnabled: Boolean = true,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun displayTime(): String {
        return LocalTime.of(hour24, minute).format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    fun displayDaySummary(): String {
        return when {
            selectedDays.size == ReminderDay.entries.size -> "Daily"
            else -> ReminderDay.ordered
                .filter { it in selectedDays }
                .joinToString(" • ") { it.shortLabel }
        }
    }

    fun resolvedNote(): String {
        return motivationalNote.ifBlank { buildAiMotivationalMessage(workoutName) }
    }
}

const val DEFAULT_RINGTONE_NAME = "Default Pulse"

fun reminderChannelId(reminderId: String): String = "fluxfit_reminder_$reminderId"

fun resolveRingtoneTitle(context: Context, uriString: String?): String {
    if (uriString.isNullOrBlank()) return DEFAULT_RINGTONE_NAME
    val ringtone = runCatching {
        RingtoneManager.getRingtone(context, android.net.Uri.parse(uriString))
    }.getOrNull()
    return ringtone?.getTitle(context).orEmpty().ifBlank { DEFAULT_RINGTONE_NAME }
}

fun buildAiMotivationalMessage(workoutName: String): String {
    val normalizedName = workoutName.trim().ifBlank { "workout block" }
    val templates = listOf(
        "AI pulse locked. $normalizedName starts soon, so step in sharp and own the set.",
        "Your $normalizedName window is opening. Show up before motivation has to catch up.",
        "Recovery metrics are green. Push into $normalizedName and let consistency compound.",
        "Neural coach check-in: $normalizedName is queued. Start strong and finish cleaner.",
        "Discipline protocol armed. Hit $normalizedName on schedule and keep the streak alive."
    )
    val signature = normalizedName.lowercase(Locale.getDefault()).sumOf { it.code }
    return templates[signature % templates.size]
}
