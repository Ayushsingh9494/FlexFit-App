package com.example.flexfit.firebase.models

data class StepData(
    val id: String = "",
    val dateKey: String = "",
    val dailyStepCount: Int = 0,
    val caloriesBurned: Double = 0.0,
    val distanceKm: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
