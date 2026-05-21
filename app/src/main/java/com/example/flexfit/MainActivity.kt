package com.example.flexfit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexfit.firebase.auth.AuthState
import com.example.flexfit.reminder.EXTRA_FOCUS_REMINDER_ID
import com.example.flexfit.reminder.EXTRA_OPEN_REMINDER_STUDIO
import com.example.flexfit.ui.auth.AuthViewModel
import com.example.flexfit.ui.screens.DashboardScreen
import com.example.flexfit.ui.screens.EmailVerificationScreen
import com.example.flexfit.ui.screens.LoadingScreen
import com.example.flexfit.ui.screens.LoginScreen
import com.example.flexfit.ui.screens.ProfileSetupScreen
import com.example.flexfit.ui.screens.RegisterScreen
import com.example.flexfit.ui.theme.FluxFitTheme

class MainActivity : ComponentActivity() {
    private var openReminderStudio by mutableStateOf(false)
    private var focusReminderId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncReminderRoute(intent)
        setContent {
            FluxFitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FluxFitApp(
                        initialOpenReminderStudio = openReminderStudio,
                        focusReminderId = focusReminderId,
                        onReminderRouteConsumed = {
                            openReminderStudio = false
                            focusReminderId = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        syncReminderRoute(intent)
    }

    private fun syncReminderRoute(intent: Intent?) {
        openReminderStudio = intent?.getBooleanExtra(EXTRA_OPEN_REMINDER_STUDIO, false) == true
        focusReminderId = intent?.getStringExtra(EXTRA_FOCUS_REMINDER_ID)
    }
}

private enum class AuthDestination {
    Login,
    Register
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FluxFitApp(
    initialOpenReminderStudio: Boolean = false,
    focusReminderId: String? = null,
    onReminderRouteConsumed: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var authDestination by rememberSaveable { mutableStateOf(AuthDestination.Login) }

    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.Unauthenticated) {
            authDestination = AuthDestination.Login
        }
    }

    AnimatedContent(
        targetState = Triple(uiState.authState, uiState.requiresProfileSetup, authDestination),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "appState"
    ) { (authState, requiresProfileSetup, destination) ->
        when (authState) {
            AuthState.Loading -> LoadingScreen(message = "Restoring your secure session...")
            is AuthState.Unauthenticated,
            is AuthState.Error -> {
                if (destination == AuthDestination.Login) {
                    LoginScreen(
                        uiState = uiState,
                        onLogin = authViewModel::signInWithEmail,
                        onOpenRegister = { authDestination = AuthDestination.Register },
                        onGoogleSignIn = authViewModel::signInWithGoogle,
                        onDismissMessage = authViewModel::dismissMessage
                    )
                } else {
                    RegisterScreen(
                        uiState = uiState,
                        onRegister = authViewModel::registerWithEmail,
                        onBackToLogin = { authDestination = AuthDestination.Login },
                        onDismissMessage = authViewModel::dismissMessage
                    )
                }
            }

            is AuthState.EmailNotVerified -> EmailVerificationScreen(
                email = authState.email.orEmpty(),
                isBusy = uiState.isBusy,
                message = uiState.message,
                onRefresh = authViewModel::refreshVerificationStatus,
                onResendVerification = authViewModel::resendVerificationEmail,
                onSignOut = authViewModel::signOut,
                onDismissMessage = authViewModel::dismissMessage
            )

            is AuthState.Authenticated -> {
                if (requiresProfileSetup) {
                    ProfileSetupScreen(
                        isBusy = uiState.isBusy,
                        existingProfile = uiState.profile,
                        message = uiState.message,
                        onSave = authViewModel::saveProfile,
                        onSignOut = authViewModel::signOut,
                        onDismissMessage = authViewModel::dismissMessage
                    )
                } else {
                    DashboardScreen(
                        profile = uiState.profile,
                        onSignOut = authViewModel::signOut,
                        initialOpenReminderStudio = initialOpenReminderStudio,
                        focusReminderId = focusReminderId,
                        onReminderRouteConsumed = onReminderRouteConsumed
                    )
                }
            }
        }
    }
}
