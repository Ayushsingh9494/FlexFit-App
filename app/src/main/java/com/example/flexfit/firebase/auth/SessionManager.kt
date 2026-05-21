package com.example.flexfit.firebase.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class SessionManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser.toAuthState())
        }
        trySend(auth.currentUser.toAuthState())
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged()

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun refreshCurrentUser(): AuthState {
        auth.currentUser?.reload()?.await()
        return auth.currentUser.toAuthState()
    }

    private fun FirebaseUser?.toAuthState(): AuthState {
        if (this == null) return AuthState.Unauthenticated
        val isGoogleAccount = providerData.any { it.providerId == "google.com" }
        return if (isEmailVerified || isGoogleAccount) {
            AuthState.Authenticated(
                uid = uid,
                email = email,
                displayName = displayName,
                isEmailVerified = isEmailVerified || isGoogleAccount
            )
        } else {
            AuthState.EmailNotVerified(email = email)
        }
    }
}
