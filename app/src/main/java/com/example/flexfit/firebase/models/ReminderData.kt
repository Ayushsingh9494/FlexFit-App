package com.example.flexfit.firebase.models

data class ReminderData(
    val id: String = "",
    val title: String = "",
    val selectedDays: List<String> = emptyList(),
    val reminderTime: String = "",
    val ringtoneUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val enabled: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
