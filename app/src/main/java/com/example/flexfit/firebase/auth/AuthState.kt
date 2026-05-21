package com.example.flexfit.firebase.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val isEmailVerified: Boolean
    ) : AuthState
    data class EmailNotVerified(val email: String?) : AuthState
    data class Error(val message: String) : AuthState
}
