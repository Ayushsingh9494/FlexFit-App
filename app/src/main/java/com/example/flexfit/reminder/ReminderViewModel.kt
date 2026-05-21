package com.example.flexfit.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

data class ReminderFormState(
    val editingReminderId: String? = null,
    val workoutName: String = "",
    val selectedDays: Set<ReminderDay> = setOf(ReminderDay.MONDAY, ReminderDay.WEDNESDAY, ReminderDay.FRIDAY),
    val hour24: Int = 18,
    val minute: Int = 30,
    val motivationalNote: String = "",
    val ringtoneUri: String? = null,
    val ringtoneName: String = DEFAULT_RINGTONE_NAME,
    val vibrationEnabled: Boolean = true,
    val enabled: Boolean = true,
    val feedbackMessage: String? = null,
    val successVisible: Boolean = false
) {
    val isEditing: Boolean
        get() = editingReminderId != null

    val previewMessage: String
        get() = motivationalNote.ifBlank { buildAiMotivationalMessage(workoutName) }
}

data class ReminderStudioUiState(
    val reminders: List<ReminderData> = emptyList(),
    val form: ReminderFormState = ReminderFormState()
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository(application)
    private val _uiState = MutableStateFlow(ReminderStudioUiState())
    val uiState: StateFlow<ReminderStudioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.reminders.collectLatest { reminders ->
                _uiState.update { current ->
                    current.copy(reminders = reminders)
                }
            }
        }
    }

    fun updateWorkoutName(value: String) {
        _uiState.updateForm {
            copy(workoutName = value, feedbackMessage = null)
        }
    }

    fun toggleDay(day: ReminderDay) {
        _uiState.updateForm {
            val updatedDays = selectedDays.toMutableSet().apply {
                if (day in this) remove(day) else add(day)
            }
            copy(selectedDays = updatedDays, feedbackMessage = null)
        }
    }

    fun setTime(hour24: Int, minute: Int) {
        _uiState.updateForm {
            copy(hour24 = hour24, minute = minute, feedbackMessage = null)
        }
    }

    fun updateMotivationalNote(value: String) {
        _uiState.updateForm {
            copy(motivationalNote = value, feedbackMessage = null)
        }
    }

    fun useAiMessage() {
        _uiState.updateForm {
            copy(motivationalNote = previewMessage, feedbackMessage = null)
        }
    }

    fun setRingtone(uriString: String?, ringtoneName: String) {
        _uiState.updateForm {
            copy(
                ringtoneUri = uriString,
                ringtoneName = ringtoneName,
                feedbackMessage = null
            )
        }
    }

    fun toggleVibration() {
        _uiState.updateForm {
            copy(vibrationEnabled = !vibrationEnabled, feedbackMessage = null)
        }
    }

    fun toggleArmedState() {
        _uiState.updateForm {
            copy(enabled = !enabled, feedbackMessage = null)
        }
    }

    fun beginEditing(reminder: ReminderData) {
        _uiState.update {
            it.copy(
                form = ReminderFormState(
                    editingReminderId = reminder.id,
                    workoutName = reminder.workoutName,
                    selectedDays = reminder.selectedDays,
                    hour24 = reminder.hour24,
                    minute = reminder.minute,
                    motivationalNote = reminder.motivationalNote,
                    ringtoneUri = reminder.ringtoneUri,
                    ringtoneName = reminder.ringtoneName,
                    vibrationEnabled = reminder.vibrationEnabled,
                    enabled = reminder.enabled
                )
            )
        }
    }

    fun resetComposer() {
        _uiState.update {
            it.copy(form = defaultFormState())
        }
    }

    fun dismissFeedback() {
        _uiState.updateForm {
            copy(feedbackMessage = null, successVisible = false)
        }
    }

    fun saveReminder() {
        val form = _uiState.value.form
        if (form.workoutName.isBlank()) {
            _uiState.updateForm { copy(feedbackMessage = "Name the workout before saving.") }
            return
        }
        if (form.selectedDays.isEmpty()) {
            _uiState.updateForm { copy(feedbackMessage = "Select at least one training day.") }
            return
        }

        viewModelScope.launch {
            val reminder = ReminderData(
                id = form.editingReminderId ?: UUID.randomUUID().toString(),
                workoutName = form.workoutName,
                selectedDays = form.selectedDays,
                hour24 = form.hour24,
                minute = form.minute,
                motivationalNote = form.motivationalNote,
                ringtoneUri = form.ringtoneUri,
                ringtoneName = form.ringtoneName,
                vibrationEnabled = form.vibrationEnabled,
                enabled = form.enabled
            )

            repository.upsertReminder(reminder)

            _uiState.update {
                it.copy(
                    form = defaultFormState().copy(
                        successVisible = true,
                        feedbackMessage = "Reminder synced into the neural schedule."
                    )
                )
            }

            delay(1600)
            _uiState.updateForm { copy(successVisible = false, feedbackMessage = null) }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
            if (_uiState.value.form.editingReminderId == reminderId) {
                _uiState.update {
                    it.copy(form = defaultFormState())
                }
            }
        }
    }

    fun setReminderEnabled(reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(reminderId, enabled)
        }
    }

    private fun defaultFormState(): ReminderFormState {
        val upcoming = LocalTime.now().plusMinutes(30)
        return ReminderFormState(
            hour24 = upcoming.hour,
            minute = if (upcoming.minute < 30) 30 else 0
        )
    }

    private fun MutableStateFlow<ReminderStudioUiState>.updateForm(
        transform: ReminderFormState.() -> ReminderFormState
    ) {
        update { state -> state.copy(form = state.form.transform()) }
    }
}
