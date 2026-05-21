package com.example.flexfit.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexfit.GlassPanel
import com.example.flexfit.GradientChip
import com.example.flexfit.reminder.ReminderDay
import com.example.flexfit.reminder.ReminderScheduler
import com.example.flexfit.reminder.ReminderViewModel
import com.example.flexfit.reminder.resolveRingtoneTitle
import com.example.flexfit.ui.components.FuturisticDayPicker
import com.example.flexfit.ui.components.FuturisticTimePicker
import com.example.flexfit.ui.components.ReminderCard
import com.example.flexfit.ui.theme.FluxAccent
import com.example.flexfit.ui.theme.FluxAurora
import com.example.flexfit.ui.theme.FluxBackground
import com.example.flexfit.ui.theme.FluxBlue
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxSuccess
import com.example.flexfit.ui.theme.FluxStroke
import com.example.flexfit.ui.theme.FluxSurface
import com.example.flexfit.ui.theme.FluxSurfaceElevated
import com.example.flexfit.ui.theme.FluxSurfaceSoft
import com.example.flexfit.ui.theme.FluxTextMuted
import com.example.flexfit.ui.theme.FluxTextPrimary
import com.example.flexfit.ui.theme.FluxTextSecondary

@Composable
fun ReminderScreen(
    onBack: () -> Unit,
    highlightedReminderId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ReminderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scheduler = remember(context) { ReminderScheduler(context) }
    var expandedReminderId by rememberSaveable { mutableStateOf(highlightedReminderId) }
    var notificationGranted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var exactAlarmGranted by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    var previewRingtone by remember { mutableStateOf<Ringtone?>(null) }
    val previewPulse = rememberInfiniteTransition(label = "savePulse")
    val composerGlow by previewPulse.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "composerGlow"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri = result.data?.pickedRingtoneUri()
            viewModel.setRingtone(
                uriString = pickedUri?.toString(),
                ringtoneName = resolveRingtoneTitle(context, pickedUri?.toString())
            )
        }
    }

    LaunchedEffect(highlightedReminderId) {
        if (!highlightedReminderId.isNullOrBlank()) {
            expandedReminderId = highlightedReminderId
        }
    }

    DisposableEffect(previewRingtone) {
        onDispose {
            previewRingtone?.stop()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = context.hasNotificationPermission()
                exactAlarmGranted = scheduler.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                ReminderStudioHeader(onBack = onBack)
            }

            if (!notificationGranted) {
                item {
                    SystemActionBanner(
                        title = "Notification permission offline",
                        body = "Android 13+ blocks alerts until POST_NOTIFICATIONS is granted. Arm this first or reminders will stay silent.",
                        icon = Icons.Filled.NotificationsActive,
                        accent = FluxGlow,
                        actionLabel = "Grant access"
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            }

            if (!exactAlarmGranted) {
                item {
                    SystemActionBanner(
                        title = "Exact alarm access recommended",
                        body = "FluxFit can fall back to inexact delivery, but exact alarm access keeps weekly reminders on the precise minute.",
                        icon = Icons.Filled.Security,
                        accent = FluxAccent,
                        actionLabel = "Open settings"
                    ) {
                        scheduler.createExactAlarmSettingsIntent()?.let(context::startActivity)
                    }
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(34.dp),
                    gradient = Brush.verticalGradient(
                        colors = listOf(
                            FluxSurfaceElevated.copy(alpha = 0.98f),
                            FluxSurface.copy(alpha = 0.96f)
                        )
                    ),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            FluxAccent.copy(alpha = composerGlow),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(34.dp)
                                )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GradientChip(text = "WORKOUT REMINDER STUDIO")
                                    AnimatedContent(
                                        targetState = uiState.form.isEditing,
                                        label = "composerTitle"
                                    ) { editing ->
                                        androidx.compose.material3.Text(
                                            text = if (editing) "Edit Neural Reminder" else "Create Neural Reminder",
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            color = FluxTextPrimary
                                        )
                                    }
                                    androidx.compose.material3.Text(
                                        text = "Design a weekly training pulse with custom sound, precise timing, and AI-generated motivation.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FluxTextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(FluxAccent.copy(alpha = 0.16f), FluxSurfaceSoft.copy(alpha = 0.96f))
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(FluxAccent.copy(alpha = 0.32f), Color.White.copy(alpha = 0.06f))
                                            ),
                                            shape = RoundedCornerShape(18.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FluxAurora)
                                }
                            }

                            NeonTextField(
                                label = "Workout name",
                                value = uiState.form.workoutName,
                                placeholder = "Upper body strength, sprint protocol, mobility reset...",
                                onValueChange = viewModel::updateWorkoutName
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FieldCaption(title = "Training days", subtitle = "Multi-select your weekly cadence")
                                FuturisticDayPicker(
                                    selectedDays = uiState.form.selectedDays,
                                    onDayToggle = viewModel::toggleDay
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FieldCaption(title = "Trigger time", subtitle = "Radial selection with live AM/PM control")
                                FuturisticTimePicker(
                                    hour24 = uiState.form.hour24,
                                    minute = uiState.form.minute,
                                    onTimeChange = viewModel::setTime
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FieldCaption(title = "AI motivational note", subtitle = "Optional message shown inside the notification")
                                NeonTextField(
                                    label = "Motivational note",
                                    value = uiState.form.motivationalNote,
                                    placeholder = "Leave blank to let FluxFit generate a dynamic AI line.",
                                    minLines = 3,
                                    onValueChange = viewModel::updateMotivationalNote
                                )
                                GlassInlineMessage(
                                    icon = Icons.Filled.GraphicEq,
                                    accent = FluxAccent,
                                    title = "AI preview",
                                    body = uiState.form.previewMessage,
                                    actionLabel = "Use AI Copy"
                                ) {
                                    viewModel.useAiMessage()
                                }
                            }

                            RingtoneDock(
                                currentLabel = uiState.form.ringtoneName,
                                onChoose = {
                                    ringtonePickerLauncher.launch(buildRingtonePickerIntent(uiState.form.ringtoneUri))
                                },
                                onPreview = {
                                    previewRingtone?.stop()
                                    val targetUri = uiState.form.ringtoneUri?.let(Uri::parse)
                                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                    previewRingtone = runCatching {
                                        RingtoneManager.getRingtone(context, targetUri)
                                    }.getOrNull()?.also { it.play() }
                                }
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BinaryStateCard(
                                    title = "Vibration",
                                    subtitle = if (uiState.form.vibrationEnabled) "Pulse on impact" else "Silent body",
                                    selected = uiState.form.vibrationEnabled,
                                    icon = Icons.Filled.Vibration,
                                    accent = FluxGlow,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.toggleVibration()
                                }
                                BinaryStateCard(
                                    title = "Armed",
                                    subtitle = if (uiState.form.enabled) "Notification live" else "Saved but paused",
                                    selected = uiState.form.enabled,
                                    icon = Icons.Filled.Alarm,
                                    accent = FluxSuccess,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.toggleArmedState()
                                }
                            }

                            uiState.form.feedbackMessage?.let { message ->
                                GlassInlineMessage(
                                    icon = if (uiState.form.successVisible) Icons.Filled.NotificationsActive else Icons.Filled.Refresh,
                                    accent = if (uiState.form.successVisible) FluxSuccess else FluxAccent,
                                    title = if (uiState.form.successVisible) "Reminder synced" else "Composer status",
                                    body = message,
                                    actionLabel = if (uiState.form.successVisible) null else "Dismiss"
                                ) {
                                    viewModel.dismissFeedback()
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                NeonActionButton(
                                    label = if (uiState.form.isEditing) "Reset" else "Clear",
                                    icon = Icons.Filled.Refresh,
                                    accent = FluxBlue,
                                    modifier = Modifier.weight(0.38f)
                                ) {
                                    viewModel.resetComposer()
                                }
                                NeonActionButton(
                                    label = if (uiState.form.isEditing) "Update Reminder" else "Save Reminder",
                                    icon = Icons.Filled.NotificationsActive,
                                    accent = FluxGlow,
                                    modifier = Modifier.weight(0.62f)
                                ) {
                                    viewModel.saveReminder()
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.Text(
                        text = "Scheduled Reminders",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = FluxTextPrimary
                    )
                    androidx.compose.material3.Text(
                        text = "Swipe any card left to delete it. Expand cards to edit, disarm, or inspect sound and note details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FluxTextSecondary
                    )
                }
            }

            if (uiState.reminders.isEmpty()) {
                item {
                    EmptyReminderState()
                }
            } else {
                items(uiState.reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        expanded = expandedReminderId == reminder.id,
                        onExpandToggle = {
                            expandedReminderId = if (expandedReminderId == reminder.id) null else reminder.id
                        },
                        onEdit = {
                            expandedReminderId = reminder.id
                            viewModel.beginEditing(reminder)
                        },
                        onDelete = {
                            if (expandedReminderId == reminder.id) {
                                expandedReminderId = null
                            }
                            viewModel.deleteReminder(reminder.id)
                        },
                        onEnabledChange = { enabled ->
                            viewModel.setReminderEnabled(reminder.id, enabled)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.form.successVisible,
            enter = fadeIn(animationSpec = tween(180)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            SuccessPulseCard()
        }
    }
}

@Composable
private fun ReminderStudioHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(FluxSurfaceSoft.copy(alpha = 0.9f), FluxSurface.copy(alpha = 0.96f))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(FluxAccent.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FluxTextPrimary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GradientChip(text = "REMINDER STUDIO")
                androidx.compose.material3.Text(
                    text = "Workout Reminder",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = FluxTextPrimary
                )
                androidx.compose.material3.Text(
                    text = "Premium scheduling with real alarms, per-reminder ringtone channels, and weekly persistence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluxTextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(FluxAccent.copy(alpha = 0.12f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = FluxAurora,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun FieldCaption(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.material3.Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FluxTextPrimary
        )
        androidx.compose.material3.Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = FluxTextMuted
        )
    }
}

@Composable
private fun NeonTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            ),
            color = FluxTextMuted
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(FluxSurfaceSoft.copy(alpha = 0.92f), FluxSurface.copy(alpha = 0.96f))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(FluxStroke.copy(alpha = 0.92f), Color.White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = FluxTextPrimary,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                ),
                minLines = minLines,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(FluxAccent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        androidx.compose.material3.Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FluxTextMuted,
                            modifier = Modifier.alpha(0.85f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun RingtoneDock(
    currentLabel: String,
    onChoose: () -> Unit,
    onPreview: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        gradient = Brush.horizontalGradient(
            colors = listOf(FluxSurfaceElevated.copy(alpha = 0.98f), FluxSurface.copy(alpha = 0.96f))
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.Text(
                        text = "Custom ringtone",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FluxTextPrimary
                    )
                    androidx.compose.material3.Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FluxTextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FluxSurfaceSoft.copy(alpha = 0.9f))
                        .border(1.dp, FluxAccent.copy(alpha = 0.24f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = FluxAurora)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonActionButton(
                    label = "Choose Tone",
                    icon = Icons.Filled.MusicNote,
                    accent = FluxAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onChoose
                )
                NeonActionButton(
                    label = "Preview",
                    icon = Icons.Filled.PlayArrow,
                    accent = FluxBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onPreview
                )
            }
        }
    }
}

@Composable
private fun BinaryStateCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "binaryState$title"
    )

    GlassPanel(
        modifier = modifier
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(26.dp),
        gradient = Brush.verticalGradient(
            colors = if (selected) {
                listOf(accent.copy(alpha = 0.22f), FluxSurface.copy(alpha = 0.94f))
            } else {
                listOf(FluxSurfaceSoft.copy(alpha = 0.9f), FluxSurface.copy(alpha = 0.95f))
            }
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.36f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (selected) accent.copy(alpha = 0.18f) else FluxSurfaceSoft.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) accent.copy(alpha = 0.6f) else FluxTextMuted.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = if (selected) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (selected) accent else FluxTextMuted
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FluxTextPrimary
                )
                androidx.compose.material3.Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FluxTextSecondary
                )
            }
        }
    }
}

@Composable
private fun NeonActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), FluxSurfaceSoft.copy(alpha = 0.88f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.8f), Color.White.copy(alpha = 0.06f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))
            androidx.compose.material3.Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FluxTextPrimary
            )
        }
    }
}

@Composable
private fun SystemActionBanner(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    actionLabel: String,
    onAction: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        gradient = Brush.horizontalGradient(
            colors = listOf(accent.copy(alpha = 0.14f), FluxSurface.copy(alpha = 0.96f))
        ),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FluxTextPrimary
                    )
                    androidx.compose.material3.Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = FluxTextSecondary
                    )
                }
            }
            NeonActionButton(
                label = actionLabel,
                icon = icon,
                accent = accent,
                modifier = Modifier.fillMaxWidth(),
                onClick = onAction
            )
        }
    }
}

@Composable
private fun GlassInlineMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(accent.copy(alpha = 0.14f), FluxSurfaceSoft.copy(alpha = 0.88f))
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.34f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = FluxTextPrimary
            )
            androidx.compose.material3.Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = FluxTextSecondary
            )
        }
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.36f), RoundedCornerShape(16.dp))
                    .clickable { onAction() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                androidx.compose.material3.Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun EmptyReminderState() {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        gradient = Brush.verticalGradient(
            colors = listOf(FluxSurfaceElevated.copy(alpha = 0.94f), FluxSurface.copy(alpha = 0.96f))
        ),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FluxAccent.copy(alpha = 0.14f))
                    .border(1.dp, FluxAccent.copy(alpha = 0.28f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = FluxAurora)
            }
            androidx.compose.material3.Text(
                text = "No reminders armed yet",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FluxTextPrimary
            )
            androidx.compose.material3.Text(
                text = "Compose your first AI workout alert above. Once saved, FluxFit will schedule it with AlarmManager and restore it after reboot.",
                style = MaterialTheme.typography.bodyMedium,
                color = FluxTextSecondary
            )
        }
    }
}

@Composable
private fun SuccessPulseCard() {
    val transition = rememberInfiniteTransition(label = "successCard")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successGlow"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        FluxSuccess.copy(alpha = 0.16f),
                        FluxSurfaceSoft.copy(alpha = 0.94f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = FluxSuccess.copy(alpha = 0.72f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(FluxSuccess.copy(alpha = glowAlpha), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = FluxSuccess)
            androidx.compose.material3.Text(
                text = "Reminder synced to the weekly grid",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FluxTextPrimary
            )
        }
    }
}

private fun Context.hasNotificationPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

private fun buildRingtonePickerIntent(currentUri: String?): Intent {
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select reminder tone")
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            currentUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )
    }
}

private fun Intent.pickedRingtoneUri(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
}
