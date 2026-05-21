package com.example.flexfit.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfit.firebase.auth.AuthManager
import com.example.flexfit.firebase.auth.AuthState
import com.example.flexfit.firebase.auth.GoogleAuthManager
import com.example.flexfit.firebase.auth.SessionManager
import com.example.flexfit.firebase.firestore.UserRepository
import com.example.flexfit.firebase.models.UserProfile
import com.example.flexfit.sensor.StepCounterServiceController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val authState: AuthState = AuthState.Loading,
    val profile: UserProfile? = null,
    val isBusy: Boolean = false,
    val message: String? = null
) {
    val requiresProfileSetup: Boolean
        get() = authState is AuthState.Authenticated && (
            profile == null ||
                profile.fullName.isBlank() ||
                profile.age == null ||
                profile.heightCm == null ||
                profile.weightKg == null ||
                profile.goal.isBlank()
            )
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager()
    private val userRepository = UserRepository()
    private val authManager = AuthManager(userRepository = userRepository)
    private val googleAuthManager = GoogleAuthManager(application)
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.authState.collectLatest { authState ->
                _uiState.update { it.copy(authState = authState, profile = null) }
                profileJob?.cancel()
                if (authState is AuthState.Authenticated) {
                    StepCounterServiceController.start(getApplication())
                    profileJob = launch {
                        userRepository.observeProfile(authState.uid).collectLatest { profile ->
                            _uiState.update { current -> current.copy(profile = profile) }
                        }
                    }
                } else {
                    StepCounterServiceController.stop(getApplication())
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        execute("Signed in.") {
            authManager.signInWithEmail(email, password).getOrThrow()
        }
    }

    fun registerWithEmail(fullName: String, email: String, password: String) {
        execute("Verification email sent. Confirm it before logging in.") {
            authManager.registerWithEmail(fullName, email, password).getOrThrow()
        }
    }

    fun signInWithGoogle(idToken: String) {
        execute("Google account connected.") {
            authManager.signInWithGoogle(idToken).getOrThrow()
        }
    }

    fun resendVerificationEmail() {
        execute("Verification email sent again.") {
            authManager.resendVerificationEmail().getOrThrow()
        }
    }

    fun refreshVerificationStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching { sessionManager.refreshCurrentUser() }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Error(throwable.message ?: "Unable to refresh verification status."),
                            isBusy = false,
                            message = throwable.message
                        )
                    }
                }
                .onSuccess { state ->
                    _uiState.update { it.copy(authState = state, isBusy = false) }
                }
        }
    }

    fun saveProfile(
        fullName: String,
        age: String,
        heightCm: String,
        weightKg: String,
        goal: String
    ) {
        execute("Profile synced to Firestore.") {
            val authState = _uiState.value.authState as? AuthState.Authenticated
                ?: error("No authenticated user.")
            val ageValue = age.toIntOrNull() ?: error("Enter a valid age.")
            val heightValue = heightCm.toDoubleOrNull() ?: error("Enter a valid height.")
            val weightValue = weightKg.toDoubleOrNull() ?: error("Enter a valid weight.")
            if (fullName.trim().length < 2) error("Enter your full name.")
            if (goal.isBlank()) error("Choose a goal.")

            val existing = _uiState.value.profile
            userRepository.upsertProfile(
                UserProfile(
                    uid = authState.uid,
                    fullName = fullName.trim(),
                    email = authState.email.orEmpty(),
                    age = ageValue,
                    heightCm = heightValue,
                    weightKg = weightValue,
                    goal = goal.trim(),
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    profileImageUrl = existing?.profileImageUrl,
                    streakCount = existing?.streakCount ?: 0
                )
            )
        }
    }

    fun signOut() {
        execute("Signed out.") {
            StepCounterServiceController.stop(getApplication())
            authManager.signOut(googleAuthManager).getOrThrow()
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun execute(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching { action() }
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false, message = successMessage) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            authState = if (it.authState is AuthState.Loading) {
                                AuthState.Error(throwable.message ?: "Something went wrong.")
                            } else {
                                it.authState
                            },
                            isBusy = false,
                            message = throwable.message ?: "Something went wrong."
                        )
                    }
                }
        }
    }
}
