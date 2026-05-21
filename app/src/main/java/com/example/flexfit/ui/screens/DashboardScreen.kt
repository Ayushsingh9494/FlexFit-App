package com.example.flexfit.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RunCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexfit.DataPill
import com.example.flexfit.FluxBackdrop
import com.example.flexfit.GlassPanel
import com.example.flexfit.GradientChip
import com.example.flexfit.HaloIcon
import com.example.flexfit.NeonActionButton
import com.example.flexfit.firebase.models.ActivityLog
import com.example.flexfit.firebase.models.UserProfile
import com.example.flexfit.sensor.StepCounterServiceController
import com.example.flexfit.sensor.StepCounterManager
import com.example.flexfit.ui.dashboard.DashboardUiState
import com.example.flexfit.ui.dashboard.DashboardViewModel
import com.example.flexfit.ui.theme.FluxAccent
import com.example.flexfit.ui.theme.FluxAurora
import com.example.flexfit.ui.theme.FluxBlue
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxDanger
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxHotPink
import com.example.flexfit.ui.theme.FluxSuccess
import com.example.flexfit.ui.theme.FluxSurface
import com.example.flexfit.ui.theme.FluxSurfaceSoft
import com.example.flexfit.ui.theme.FluxTextMuted
import com.example.flexfit.ui.theme.FluxTextPrimary
import com.example.flexfit.ui.theme.FluxTextSecondary
import kotlin.math.max
import kotlin.math.roundToInt
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
private enum class DashboardTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Activity("Workout", Icons.Filled.RunCircle),
    Reminder("Reminder", Icons.Filled.NotificationsActive),
    Analytics("Analytics", Icons.Filled.Insights),
    Profile("Profile", Icons.Filled.Person)
}

private data class AchievementBadge(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun DashboardScreen(
    profile: UserProfile?,
    onSignOut: () -> Unit,
    initialOpenReminderStudio: Boolean,
    focusReminderId: String?,
    onReminderRouteConsumed: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val stepCounterManager = remember(context) { StepCounterManager(context) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                stepCounterManager.startListening()
                StepCounterServiceController.start(context)
            }
        }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable {
        mutableStateOf(if (initialOpenReminderStudio) DashboardTab.Reminder else DashboardTab.Home)
    }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }



    DisposableEffect(Unit) {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            stepCounterManager.startListening()
            StepCounterServiceController.start(context)

        } else {

            permissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        }

        onDispose {
            stepCounterManager.stopListening()
        }
    }

    LaunchedEffect(stepCounterManager.steps.intValue) {
        viewModel.syncTodaySteps(stepCounterManager.steps.intValue)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(initialOpenReminderStudio, focusReminderId) {
        if (initialOpenReminderStudio || !focusReminderId.isNullOrBlank()) {
            selectedTab = DashboardTab.Reminder
            onReminderRouteConsumed()
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FluxDanger)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Leave FluxFit?") },
            text = { Text("Your authenticated session will be signed out on this device.") }
        )
    }

    FluxBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedBottomBar(
                    selectedTab = selectedTab,
                    onSelected = { selectedTab = it }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    label = "dashboardTab"
                ) { tab ->
                    when (tab) {
                        DashboardTab.Home -> HomeTab(
                            profile = profile,
                            uiState = uiState,
                            onOpenReminders = {
                                selectedTab = DashboardTab.Reminder
                            }
                        )

                        DashboardTab.Activity -> ActivityTab(
                            uiState = uiState,
                            onSaveLog = viewModel::addActivityLog
                        )

                        DashboardTab.Reminder -> ReminderScreen(
                            onBack = { selectedTab = DashboardTab.Home },
                            highlightedReminderId = focusReminderId
                        )

                        DashboardTab.Analytics -> AnalyticsTab(
                            profile = profile,
                            uiState = uiState,
                            onOpenReminders = { selectedTab = DashboardTab.Reminder }
                        )

                        DashboardTab.Profile -> ProfileTab(
                            profile = profile,
                            uiState = uiState,
                            onOpenReminders = { selectedTab = DashboardTab.Reminder },
                            onSignOut = { showLogoutDialog = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBottomBar(
    selectedTab: DashboardTab,
    onSelected: (DashboardTab) -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(28.dp),
        gradient = Brush.horizontalGradient(
            colors = listOf(
                FluxSurface.copy(alpha = 0.96f),
                FluxSurfaceSoft.copy(alpha = 0.92f)
            )
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardTab.entries.forEach { tab ->
                val selected = selectedTab == tab   
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.96f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "navScale${tab.label}"
                )
                val accent by animateColorAsState(
                    targetValue = when (tab) {
                        DashboardTab.Home -> FluxAccent
                        DashboardTab.Activity -> FluxHotPink
                        DashboardTab.Reminder -> FluxAurora
                        DashboardTab.Analytics -> FluxCyan
                        DashboardTab.Profile -> FluxGlow
                    },
                    label = "navAccent${tab.label}"
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (selected) {
                                Brush.horizontalGradient(
                                    colors = listOf(accent.copy(alpha = 0.18f), FluxSurfaceSoft.copy(alpha = 0.96f))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent)
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { onSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = if (selected) accent else FluxTextMuted)
                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = tab.label,
//                        style = MaterialTheme.typography.labelLarge,
//                        color = if (selected) FluxTextPrimary else FluxTextSecondary
//                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTab(
    profile: UserProfile?,
    uiState: DashboardUiState,
    onOpenReminders: () -> Unit
) {
    val stepCount = uiState.todaySteps?.dailyStepCount ?: 0
    val weeklyHistory = uiState.weeklyHistory.map { it.dailyStepCount }
    val achievements = remember(stepCount, uiState.reminderCount, profile?.streakCount, uiState.recentActivities.size) {
        buildAchievements(
            stepCount = stepCount,
            reminderCount = uiState.reminderCount,
            streakCount = profile?.streakCount ?: 0,
            activityCount = uiState.recentActivities.size
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsCard(
                weeklyHistory = weeklyHistory,
                recentActivities = uiState.recentActivities
            )
        }
        item {
            SectionTitle(
                title = "Achievement grid",
                subtitle = "A cleaner view of your streaks, steps, reminders, and workout cadence."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                achievements.take(2).forEach { badge ->
                    AchievementCard(badge = badge, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                achievements.drop(2).take(2).forEach { badge ->
                    AchievementCard(badge = badge, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle(
                        title = "Reminder access",
                        subtitle = "Jump into the existing reminder studio from analytics without changing its logic."
                    )
                    NeonActionButton(
                        label = "Open reminder studio",
                        icon = Icons.Filled.NotificationsActive,
                        accent = FluxAccent,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenReminders
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTab(
    profile: UserProfile?,
    uiState: DashboardUiState,
    onOpenReminders: () -> Unit
) {
    val stepCount = uiState.todaySteps?.dailyStepCount ?: 0
    val calories = uiState.todaySteps?.caloriesBurned ?: 0.0
    val distance = uiState.todaySteps?.distanceKm ?: 0.0
    val weeklyHistory = uiState.weeklyHistory.map { it.dailyStepCount }
    val stepGoal = 10000
    val progress = (stepCount / stepGoal.toFloat()).coerceIn(0f, 1f)
    val achievements = remember(stepCount, uiState.reminderCount, profile?.streakCount, uiState.recentActivities.size) {
        buildAchievements(
            stepCount = stepCount,
            reminderCount = uiState.reminderCount,
            streakCount = profile?.streakCount ?: 0,
            activityCount = uiState.recentActivities.size
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardHeroCard(
                profile = profile,
                stepCount = stepCount,
                progress = progress,
                reminderCount = uiState.reminderCount
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Calories",
                    value = "${calories.roundToInt()}",
                    subtitle = "Burn estimate",
                    accent = FluxHotPink,
                    icon = Icons.Filled.Bolt,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Distance",
                    value = String.format("%.2f km", distance),
                    subtitle = "Movement radius",
                    accent = FluxAurora,
                    icon = Icons.Filled.DirectionsRun,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Streak",
                    value = "${profile?.streakCount ?: 0}d",
                    subtitle = "Consistency chain",
                    accent = FluxSuccess,
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Reminders",
                    value = uiState.reminderCount.toString(),
                    subtitle = "Active nudges",
                    accent = FluxAccent,
                    icon = Icons.Filled.NotificationsActive,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            AnalyticsCard(
                weeklyHistory = weeklyHistory,
                recentActivities = uiState.recentActivities
            )
        }
        item {
            SectionTitle(
                title = "Achievement grid",
                subtitle = "Derived from your current synced data and recent activity."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                achievements.take(2).forEach { badge ->
                    AchievementCard(badge = badge, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                achievements.drop(2).take(2).forEach { badge ->
                    AchievementCard(badge = badge, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                gradient = Brush.linearGradient(
                    colors = listOf(
                        FluxAccent.copy(alpha = 0.14f),
                        FluxSurfaceSoft.copy(alpha = 0.92f),
                        FluxSurface.copy(alpha = 0.95f)
                    )
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle(
                        title = "Reminder matrix",
                        subtitle = "Keep your training cadence alive with futuristic nudges and custom sound design."
                    )
                    NeonActionButton(
                        label = "Open reminder studio",
                        icon = Icons.Filled.NotificationsActive,
                        accent = FluxAccent,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenReminders
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(
    profile: UserProfile?,
    stepCount: Int,
    progress: Float,
    reminderCount: Int
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        gradient = Brush.linearGradient(
            colors = listOf(
                FluxAccent.copy(alpha = 0.16f),
                FluxCyan.copy(alpha = 0.08f),
                FluxSurface.copy(alpha = 0.96f)
            )
        ),
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepProgressRing(progress = progress, stepCount = stepCount, modifier = Modifier.size(132.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GradientChip(text = "DAILY OVERVIEW")
                Text(
                    text = "Welcome, ${profile?.fullName?.ifBlank { "Athlete" } ?: "Athlete"}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = FluxTextPrimary
                )
                Text(
                    text = "Your live training dashboard is syncing steps, reminders, and workout intelligence in real time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluxTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DataPill(label = "STEPS", value = stepCount.toString(), accent = FluxAccent)
                    DataPill(label = "REM", value = reminderCount.toString(), accent = FluxAurora)
                }
            }
        }
    }
}

@Composable
private fun StepProgressRing(
    progress: Float,
    stepCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                style = Stroke(width = 18.dp.toPx())
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(FluxAccent, FluxAurora, FluxCyan, FluxAccent)
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${(animatedProgress * 100).roundToInt()}%", style = MaterialTheme.typography.headlineSmall, color = FluxTextPrimary)
            Text(text = "$stepCount steps", style = MaterialTheme.typography.bodySmall, color = FluxTextSecondary)
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(accent.copy(alpha = 0.14f), FluxSurface.copy(alpha = 0.95f))
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HaloIcon(icon = icon, accent = accent)
            Text(title, style = MaterialTheme.typography.labelLarge, color = FluxTextSecondary)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = FluxTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FluxTextMuted)
        }
    }
}

@Composable
private fun AnalyticsCard(
    weeklyHistory: List<Int>,
    recentActivities: List<ActivityLog>
) {
    val maxSteps = max(weeklyHistory.maxOrNull() ?: 0, 1000)
    val totalMinutes = recentActivities.sumOf { it.durationMinutes }
    val totalCalories = recentActivities.sumOf { it.caloriesBurned }
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(
                FluxSurface.copy(alpha = 0.96f),
                FluxSurfaceSoft.copy(alpha = 0.92f)
            )
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(
                title = "Workout analytics",
                subtitle = "Weekly pace plus workload extracted from synced steps and recent logs."
            )
            PremiumLineChart(values = weeklyHistory.ifEmpty { listOf(1200, 2200, 1800, 3600, 2800, 4200, 3000) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DataPill(label = "PEAK", value = "$maxSteps", accent = FluxGlow)
                DataPill(label = "MIN", value = "$totalMinutes", accent = FluxAurora)
                DataPill(label = "KCAL", value = "$totalCalories", accent = FluxCyan)
            }
        }
    }
}

@Composable
private fun PremiumLineChart(values: List<Int>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FluxAccent.copy(alpha = 0.08f),
                        FluxCyan.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxValue = max(values.maxOrNull() ?: 1, 1).toFloat()
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)
            for (i in 0 until 4) {
                val y = size.height * (i / 3f)
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }

            val path = Path()
            values.forEachIndexed { index, value ->
                val x = stepX * index
                val y = size.height - (value / maxValue) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(
                    color = FluxAccent,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(FluxAccent, FluxAurora)),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun buildAchievements(
    stepCount: Int,
    reminderCount: Int,
    streakCount: Int,
    activityCount: Int
): List<AchievementBadge> {
    return listOf(
        AchievementBadge(
            title = if (stepCount >= 10000) "Goal Cleared" else "Velocity Rising",
            subtitle = if (stepCount >= 10000) "Daily step goal cleared." else "$stepCount steps live today.",
            icon = Icons.Filled.DirectionsRun,
            accent = FluxAccent
        ),
        AchievementBadge(
            title = if (streakCount >= 7) "Heat Streak" else "Streak Engine",
            subtitle = if (streakCount >= 7) "$streakCount day momentum locked." else "Current streak: $streakCount days.",
            icon = Icons.Filled.LocalFireDepartment,
            accent = FluxSuccess
        ),
        AchievementBadge(
            title = if (activityCount >= 3) "Log Architect" else "Workout Drafting",
            subtitle = if (activityCount >= 3) "$activityCount recent workouts archived." else "Add more logs to unlock depth.",
            icon = Icons.Filled.AutoGraph,
            accent = FluxCyan
        ),
        AchievementBadge(
            title = if (reminderCount > 0) "Reminder Matrix" else "Alert Offline",
            subtitle = if (reminderCount > 0) "$reminderCount reminders armed." else "No active reminder pulses yet.",
            icon = Icons.Filled.EmojiEvents,
            accent = FluxAurora
        )
    )
}

@Composable
private fun AchievementCard(
    badge: AchievementBadge,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(badge.accent.copy(alpha = 0.18f), FluxSurface.copy(alpha = 0.94f))
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HaloIcon(icon = badge.icon, accent = badge.accent)
            Text(text = badge.title, style = MaterialTheme.typography.titleMedium, color = FluxTextPrimary)
            Text(text = badge.subtitle, style = MaterialTheme.typography.bodySmall, color = FluxTextSecondary)
        }
    }
}

@Composable
private fun ActivityTab(
    uiState: DashboardUiState,
    onSaveLog: (String, String, String, String) -> Unit
) {
    var workoutName by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                gradient = Brush.verticalGradient(
                    colors = listOf(
                        FluxHotPink.copy(alpha = 0.14f),
                        FluxBlue.copy(alpha = 0.12f),
                        FluxSurface.copy(alpha = 0.96f)
                    )
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GradientChip(text = "WORKOUT LOGGER")
                    SectionTitle(
                        title = "Capture a new session",
                        subtitle = "Push a workout into the activity stream without changing any underlying sync logic."
                    )
                    DashboardTextField(
                        value = workoutName,
                        label = "Workout completed",
                        onValueChange = { workoutName = it },
                        icon = Icons.Filled.Token
                    )
                    DashboardTextField(
                        value = duration,
                        label = "Duration (minutes)",
                        onValueChange = { duration = it },
                        icon = Icons.Filled.Timeline,
                        keyboardType = KeyboardType.Number
                    )
                    DashboardTextField(
                        value = calories,
                        label = "Calories burned",
                        onValueChange = { calories = it },
                        icon = Icons.Filled.Bolt,
                        keyboardType = KeyboardType.Number
                    )
                    DashboardTextField(
                        value = notes,
                        label = "Notes",
                        onValueChange = { notes = it },
                        icon = Icons.Filled.Psychology
                    )
                    Button(
                        onClick = {
                            onSaveLog(workoutName, duration, calories, notes)
                            workoutName = ""
                            duration = ""
                            calories = ""
                            notes = ""
                        },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FluxAccent, contentColor = FluxSurface)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isSaving) "Saving..." else "Sync workout")
                    }
                }
            }
        }
        item {
            SectionTitle(
                title = "Recent workout feed",
                subtitle = "Your latest activity logs rendered as a premium timeline."
            )
        }
        if (uiState.recentActivities.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No workouts logged yet",
                    body = "Your next saved session will appear here with calories, duration, and notes."
                )
            }
        } else {
            items(uiState.recentActivities, key = { it.id }) { activity ->
                ActivityTimelineCard(activity = activity)
            }
        }
    }
}

@Composable
private fun ActivityTimelineCard(activity: ActivityLog) {
    val pulse = rememberInfiniteTransition(label = "activityPulse${activity.id}")
    val alpha by pulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.54f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activityAlpha${activity.id}"
    )

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        gradient = Brush.horizontalGradient(
            colors = listOf(
                FluxBlue.copy(alpha = 0.12f),
                FluxSurface.copy(alpha = 0.95f)
            )
        )
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(FluxGlow.copy(alpha = alpha))
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activity.workoutCompleted, style = MaterialTheme.typography.titleMedium, color = FluxTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DataPill(label = "MIN", value = activity.durationMinutes.toString(), accent = FluxAurora)
                    DataPill(label = "KCAL", value = activity.caloriesBurned.toString(), accent = FluxHotPink)
                }
                if (activity.notes.isNotBlank()) {
                    Text(activity.notes, style = MaterialTheme.typography.bodySmall, color = FluxTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(
    profile: UserProfile?,
    uiState: DashboardUiState,
    onOpenReminders: () -> Unit,
    onSignOut: () -> Unit
) {
    val totalCalories = uiState.recentActivities.sumOf { it.caloriesBurned }
    val totalMinutes = uiState.recentActivities.sumOf { it.durationMinutes }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(34.dp),
                gradient = Brush.linearGradient(
                    colors = listOf(
                        FluxAurora.copy(alpha = 0.18f),
                        FluxBlue.copy(alpha = 0.14f),
                        FluxSurface.copy(alpha = 0.96f)
                    )
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientChip(text = "PROFILE CORE")
                            Text(profile?.fullName.orEmpty().ifBlank { "Flux Athlete" }, style = MaterialTheme.typography.headlineSmall, color = FluxTextPrimary)
                            Text(profile?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = FluxTextSecondary)
                        }
                        HaloIcon(icon = Icons.Filled.Person, accent = FluxAurora, size = 60.dp)
                    }
                    Text(
                        text = "Goal: ${profile?.goal.orEmpty().ifBlank { "No goal set yet" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FluxTextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DataPill(label = "AGE", value = profile?.age?.toString() ?: "-", accent = FluxGlow)
                        DataPill(label = "HT", value = profile?.heightCm?.toString() ?: "-", accent = FluxBlue)
                        DataPill(label = "WT", value = profile?.weightKg?.toString() ?: "-", accent = FluxHotPink)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Current streak",
                    value = "${profile?.streakCount ?: 0} days",
                    subtitle = "Consistency engine",
                    accent = FluxSuccess,
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Workload",
                    value = "$totalMinutes min",
                    subtitle = "Tracked sessions",
                    accent = FluxGlow,
                    icon = Icons.Filled.Insights,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Energy output",
                    value = "$totalCalories kcal",
                    subtitle = "Recent burn",
                    accent = FluxHotPink,
                    icon = Icons.Filled.AutoGraph,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Workout count",
                    value = uiState.recentActivities.size.toString(),
                    subtitle = "Recent archive",
                    accent = FluxAurora,
                    icon = Icons.Filled.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle(
                        title = "Profile actions",
                        subtitle = "Quick controls for reminders and session state."
                    )
                    NeonActionButton(
                        label = "Manage reminders",
                        icon = Icons.Filled.NotificationsActive,
                        accent = FluxGlow,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenReminders
                    )
                    NeonActionButton(
                        label = "Logout",
                        icon = Icons.Filled.Logout,
                        accent = FluxDanger,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSignOut
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = FluxTextPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = FluxTextMuted)
    }
}

@Composable
private fun DashboardTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = FluxGlow) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun EmptyStateCard(
    title: String,
    body: String
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = FluxTextPrimary)
            Text(body, style = MaterialTheme.typography.bodySmall, color = FluxTextSecondary)
        }
    }
}
