package com.example.flexfit.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfit.firebase.firestore.ActivityRepository
import com.example.flexfit.firebase.firestore.ReminderRepository
import com.example.flexfit.firebase.firestore.StepRepository
import com.example.flexfit.firebase.firestore.StepSyncCoordinator
import com.example.flexfit.firebase.models.ActivityLog
import com.example.flexfit.firebase.models.StepData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val todaySteps: StepData? = null,
    val weeklyHistory: List<StepData> = emptyList(),
    val recentActivities: List<ActivityLog> = emptyList(),
    val reminderCount: Int = 0,
    val isSaving: Boolean = false,
    val message: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val stepRepository = StepRepository()
    private val reminderRepository = ReminderRepository()
    private val activityRepository = ActivityRepository()
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                stepRepository.observeTodaySteps(userId).collectLatest { stepData ->
                    _uiState.update { it.copy(todaySteps = stepData) }
                }
            }
            viewModelScope.launch {
                stepRepository.observeWeeklyHistory(userId).collectLatest { history ->
                    _uiState.update { it.copy(weeklyHistory = history) }
                }
            }
            viewModelScope.launch {
                reminderRepository.observeReminders(userId).collectLatest { reminders ->
                    _uiState.update { it.copy(reminderCount = reminders.count { reminder -> reminder.enabled }) }
                }
            }
            viewModelScope.launch {
                activityRepository.observeRecentActivities(userId).collectLatest { logs ->
                    _uiState.update { it.copy(recentActivities = logs) }
                }
            }
        }
    }

    fun syncTodaySteps(stepCount: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            StepSyncCoordinator.syncImmediate(
                context = getApplication(),
                userId = userId,
                stepCount = stepCount,
                repository = stepRepository
            )
        }
    }

    fun addActivityLog(
        workoutName: String,
        durationMinutes: String,
        caloriesBurned: String,
        notes: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                val duration = durationMinutes.toIntOrNull() ?: error("Enter workout duration in minutes.")
                val calories = caloriesBurned.toIntOrNull() ?: error("Enter calories burned.")
                if (workoutName.isBlank()) error("Enter a workout name.")
                activityRepository.addActivity(
                    userId = userId,
                    activityLog = ActivityLog(
                        id = UUID.randomUUID().toString(),
                        workoutCompleted = workoutName.trim(),
                        durationMinutes = duration,
                        caloriesBurned = calories,
                        notes = notes.trim()
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, message = "Workout synced to Firestore.") }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = throwable.message ?: "Unable to save activity."
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
