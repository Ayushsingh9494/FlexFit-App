package com.example.flexfit.firebase.models

data class ActivityLog(
    val id: String = "",
    val workoutCompleted: String = "",
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
