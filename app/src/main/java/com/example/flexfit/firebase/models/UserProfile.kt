package com.example.flexfit.firebase.models

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val goal: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val profileImageUrl: String? = null,
    val streakCount: Int = 0
)
