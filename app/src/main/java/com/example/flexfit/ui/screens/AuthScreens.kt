package com.example.flexfit.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.flexfit.DataPill
import com.example.flexfit.FluxBackdrop
import com.example.flexfit.GlassPanel
import com.example.flexfit.GradientChip
import com.example.flexfit.HaloIcon
import com.example.flexfit.NeonActionButton
import com.example.flexfit.firebase.auth.GoogleAuthManager
import com.example.flexfit.firebase.models.UserProfile
import com.example.flexfit.ui.auth.AuthUiState
import com.example.flexfit.ui.theme.FluxAccent
import com.example.flexfit.ui.theme.FluxAurora
import com.example.flexfit.ui.theme.FluxBlue
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxHotPink
import com.example.flexfit.ui.theme.FluxSurface
import com.example.flexfit.ui.theme.FluxSurfaceSoft
import com.example.flexfit.ui.theme.FluxTextMuted
import com.example.flexfit.ui.theme.FluxTextPrimary
import com.example.flexfit.ui.theme.FluxTextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onOpenRegister: () -> Unit,
    onGoogleSignIn: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    val context = LocalContext.current
    val googleAuthManager = remember(context) { GoogleAuthManager(context) }
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_CANCELED) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            googleAuthManager.extractIdToken(result.data)
                .onSuccess(onGoogleSignIn)
                .onFailure { error ->
                    snackbarHostState.showSnackbar(error.message ?: "Google sign-in failed.")
                }
        }
    }

    MessageEffect(message = uiState.message, snackbarHostState = snackbarHostState, onShown = onDismissMessage)

    AuthScaffold(
        title = "Train beyond routine",
        subtitle = "AI-guided fitness command center with live sync, reminders, and premium analytics.",
        eyebrow = "FLUXFIT // ACCESS",
        snackbarHostState = snackbarHostState
    ) {
        AuthHero(
            title = "Neural fitness interface",
            body = "Securely restore your training profile and continue where your performance graph left off."
        )
        Spacer(modifier = Modifier.height(18.dp))
        AuthCard {
            SectionHeading("Welcome back", "Resume your personalized training environment.")
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = email, label = "Email", onValueChange = { email = it }, leadingIcon = Icons.Filled.Security)
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(
                value = password,
                label = "Password",
                onValueChange = { password = it },
                isPassword = true,
                leadingIcon = Icons.Filled.Shield
            )
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryAuthButton(
                label = if (uiState.isBusy) "Syncing..." else "Enter FluxFit",
                onClick = { onLogin(email, password) },
                enabled = !uiState.isBusy,
                loading = uiState.isBusy
            )
            Spacer(modifier = Modifier.height(12.dp))
            NeonActionButton(
                label = "Continue with Google",
                icon = Icons.Filled.AutoAwesome,
                accent = FluxGlow,
                modifier = Modifier.fillMaxWidth(),
                onClick = { googleLauncher.launch(googleAuthManager.signInIntent()) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenRegister) {
                    Text("Create account")
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (String, String, String) -> Unit,
    onBackToLogin: () -> Unit,
    onDismissMessage: () -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(message = uiState.message, snackbarHostState = snackbarHostState, onShown = onDismissMessage)

    AuthScaffold(
        title = "Build your athlete profile",
        subtitle = "Create a secure account, verify your email, and unlock the full FluxFit experience.",
        eyebrow = "FLUXFIT // ONBOARD",
        snackbarHostState = snackbarHostState
    ) {
        AuthHero(
            title = "Performance identity",
            body = "Your Firebase account powers live workout history, reminders, and wearable-ready progress signals."
        )
        Spacer(modifier = Modifier.height(18.dp))
        AuthCard {
            SectionHeading("Create account", "Start with a clean profile and premium motion-first onboarding.")
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = fullName, label = "Full name", onValueChange = { fullName = it }, leadingIcon = Icons.Filled.DirectionsRun)
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(value = email, label = "Email", onValueChange = { email = it }, leadingIcon = Icons.Filled.Security)
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(
                value = password,
                label = "Strong password",
                onValueChange = { password = it },
                isPassword = true,
                leadingIcon = Icons.Filled.Shield
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use 8+ characters with upper, lower, number, and symbol.",
                style = MaterialTheme.typography.bodySmall,
                color = FluxTextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryAuthButton(
                label = if (uiState.isBusy) "Provisioning..." else "Create account",
                onClick = { onRegister(fullName, email, password) },
                enabled = !uiState.isBusy,
                loading = uiState.isBusy
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.End)) {
                Text("Back to login")
            }
        }
    }
}

@Composable
fun EmailVerificationScreen(
    email: String,
    isBusy: Boolean,
    message: String?,
    onRefresh: () -> Unit,
    onResendVerification: () -> Unit,
    onSignOut: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(message = message, snackbarHostState = snackbarHostState, onShown = onDismissMessage)

    AuthScaffold(
        title = "Verify and activate",
        subtitle = "Your account exists, but the premium dashboard stays locked until email verification completes.",
        eyebrow = "FLUXFIT // VERIFY",
        snackbarHostState = snackbarHostState
    ) {
        AuthHero(
            title = "Transmission pending",
            body = "A verification pulse was sent to $email. Confirm it, then re-scan the session to continue."
        )
        Spacer(modifier = Modifier.height(18.dp))
        AuthCard {
            SectionHeading("Email checkpoint", "This protects your data and keeps your cross-device session trusted.")
            Spacer(modifier = Modifier.height(16.dp))
            NeonStatusCard(
                title = "Verification target",
                body = email,
                accent = FluxAurora,
                icon = Icons.Filled.Security
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryAuthButton(
                label = "I verified my email",
                onClick = onRefresh,
                enabled = !isBusy,
                loading = isBusy
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onResendVerification, modifier = Modifier.fillMaxWidth(), enabled = !isBusy) {
                Text("Resend verification email")
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.End)) {
                Text("Sign out")
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    isBusy: Boolean,
    existingProfile: UserProfile?,
    message: String?,
    onSave: (String, String, String, String, String) -> Unit,
    onSignOut: () -> Unit,
    onDismissMessage: () -> Unit
) {
    var fullName by rememberSaveable(existingProfile?.fullName) { mutableStateOf(existingProfile?.fullName.orEmpty()) }
    var age by rememberSaveable(existingProfile?.age) { mutableStateOf(existingProfile?.age?.toString().orEmpty()) }
    var height by rememberSaveable(existingProfile?.heightCm) { mutableStateOf(existingProfile?.heightCm?.toString().orEmpty()) }
    var weight by rememberSaveable(existingProfile?.weightKg) { mutableStateOf(existingProfile?.weightKg?.toString().orEmpty()) }
    var goal by rememberSaveable(existingProfile?.goal) { mutableStateOf(existingProfile?.goal.orEmpty()) }
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(message = message, snackbarHostState = snackbarHostState, onShown = onDismissMessage)

    AuthScaffold(
        title = "Tune your training DNA",
        subtitle = "Complete your profile to personalize insights, reminders, streak visuals, and dashboard targets.",
        eyebrow = "FLUXFIT // PROFILE",
        snackbarHostState = snackbarHostState
    ) {
        AuthHero(
            title = "Athlete calibration",
            body = "These values shape the interface only. Core session and Firestore behavior remain unchanged."
        )
        Spacer(modifier = Modifier.height(18.dp))
        AuthCard {
            SectionHeading("Profile setup", "Set the baseline that powers your visual experience.")
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = fullName, label = "Full name", onValueChange = { fullName = it }, leadingIcon = Icons.Filled.DirectionsRun)
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(
                value = age,
                label = "Age",
                onValueChange = { age = it },
                leadingIcon = Icons.Filled.Timeline,
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(
                value = height,
                label = "Height (cm)",
                onValueChange = { height = it },
                leadingIcon = Icons.Filled.Bolt,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(
                value = weight,
                label = "Weight (kg)",
                onValueChange = { weight = it },
                leadingIcon = Icons.Filled.AutoAwesome,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(value = goal, label = "Goal", onValueChange = { goal = it }, leadingIcon = Icons.Filled.KeyboardArrowRight)
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryAuthButton(
                label = if (isBusy) "Syncing profile..." else "Save profile",
                onClick = { onSave(fullName, age, height, weight, goal) },
                enabled = !isBusy,
                loading = isBusy
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.End)) {
                Text("Sign out")
            }
        }
    }
}

@Composable
internal fun AuthScaffold(
    title: String,
    subtitle: String,
    eyebrow: String,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    FluxBackdrop {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            GradientChip(text = eyebrow)
            Text(text = title, style = MaterialTheme.typography.headlineLarge, color = FluxTextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = FluxTextSecondary)
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(
                FluxSurface.copy(alpha = 0.96f),
                FluxSurfaceSoft.copy(alpha = 0.92f)
            )
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun AuthHero(
    title: String,
    body: String
) {
    val transition = rememberInfiniteTransition(label = "authHero")
    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroGlow"
    )

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        gradient = Brush.linearGradient(
            colors = listOf(
                FluxBlue.copy(alpha = 0.18f),
                FluxHotPink.copy(alpha = 0.16f),
                FluxSurfaceSoft.copy(alpha = 0.96f)
            )
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(FluxGlow.copy(alpha = glow), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = FluxTextPrimary
                        )
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FluxTextSecondary
                        )
                    }
                    HaloIcon(icon = Icons.Filled.AutoAwesome, accent = FluxGlow)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DataPill(label = "SYNC", value = "LIVE", accent = FluxAurora)
                    DataPill(label = "MODE", value = "PREMIUM", accent = FluxHotPink)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignalPoint(icon = Icons.Filled.Bolt, title = "Smart streaks")
                    SignalPoint(icon = Icons.Filled.Timeline, title = "Adaptive insights")
                    SignalPoint(icon = Icons.Filled.Security, title = "Secure session")
                }
            }
        }
    }
}

@Composable
private fun SignalPoint(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HaloIcon(icon = icon, accent = FluxCyan, size = 34.dp)
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = FluxTextPrimary)
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = FluxTextPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = FluxTextMuted)
    }
}

@Composable
private fun PrimaryAuthButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = FluxAccent,
            contentColor = FluxSurface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = FluxSurface
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
            Text(label)
        }
    }
}

@Composable
private fun NeonStatusCard(
    title: String,
    body: String,
    accent: Color,
    icon: ImageVector
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        gradient = Brush.horizontalGradient(
            colors = listOf(accent.copy(alpha = 0.14f), FluxSurface.copy(alpha = 0.94f))
        )
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HaloIcon(icon = icon, accent = accent)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = FluxTextPrimary)
                Text(text = body, style = MaterialTheme.typography.bodyMedium, color = FluxTextSecondary)
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            androidx.compose.material3.Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = FluxGlow
            )
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun MessageEffect(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit
) {
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            onShown()
        }
    }
}
