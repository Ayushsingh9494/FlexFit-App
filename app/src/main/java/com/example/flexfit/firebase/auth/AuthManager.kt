package com.example.flexfit.firebase.auth

import android.util.Patterns
import com.example.flexfit.firebase.firestore.UserRepository
import com.example.flexfit.firebase.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {

    suspend fun registerWithEmail(
        fullName: String,
        email: String,
        password: String
    ): Result<Unit> = runCatching {
        validateRegistration(fullName, email, password)
        val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = authResult.user ?: error("Unable to create your account.")
        user.sendEmailVerification().await()
        userRepository.ensureProfileSeed(
            UserProfile(
                uid = user.uid,
                fullName = fullName.trim(),
                email = user.email.orEmpty()
            )
        )
    }.mapError()

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        validateEmail(email)
        if (password.isBlank()) error("Enter your password.")
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = auth.currentUser ?: error("Unable to restore your account.")
        if (!user.isEmailVerified) {
            error("Verify your email before signing in.")
        }
    }.mapError()

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user ?: error("Unable to complete Google sign-in.")
        userRepository.ensureProfileSeed(
            UserProfile(
                uid = user.uid,
                fullName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                profileImageUrl = user.photoUrl?.toString()
            )
        )
    }.mapError()

    suspend fun resendVerificationEmail(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No pending account to verify.")
        user.reload().await()
        user.sendEmailVerification().await()
        Unit
    }.mapError()

    suspend fun signOut(googleAuthManager: GoogleAuthManager? = null): Result<Unit> = runCatching {
        auth.signOut()
        googleAuthManager?.signOut()
        Unit
    }.mapError()

    private fun validateRegistration(fullName: String, email: String, password: String) {
        if (fullName.trim().length < 2) {
            error("Enter your full name.")
        }
        validateEmail(email)
        validatePassword(password)
    }

    private fun validateEmail(email: String) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            error("Enter a valid email address.")
        }
    }

    private fun validatePassword(password: String) {
        val hasUpper = password.any(Char::isUpperCase)
        val hasLower = password.any(Char::isLowerCase)
        val hasDigit = password.any(Char::isDigit)
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        if (password.length < 8 || !hasUpper || !hasLower || !hasDigit || !hasSymbol) {
            error("Password must be 8+ characters with upper, lower, number, and symbol.")
        }
    }

    private fun <T> Result<T>.mapError(): Result<T> {
        return recoverCatching { throwable ->
            throw IllegalStateException(mapFirebaseError(throwable))
        }
    }

    private fun mapFirebaseError(throwable: Throwable): String {
        return when ((throwable as? FirebaseAuthException)?.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "That email is already registered."
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_WEAK_PASSWORD" -> "Use a stronger password."
            "ERROR_USER_NOT_FOUND",
            "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
            "ERROR_WRONG_PASSWORD" -> "Incorrect email or password."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again shortly."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and retry."
            else -> throwable.message ?: "Authentication failed."
        }
    }
}
