package com.example.flexfit.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexfit.GlassPanel
import com.example.flexfit.reminder.ReminderData
import com.example.flexfit.reminder.ReminderDay
import com.example.flexfit.ui.theme.FluxAccent
import com.example.flexfit.ui.theme.FluxAurora
import com.example.flexfit.ui.theme.FluxBlue
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxDanger
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxSurface
import com.example.flexfit.ui.theme.FluxSurfaceElevated
import com.example.flexfit.ui.theme.FluxSurfaceSoft
import com.example.flexfit.ui.theme.FluxTextMuted
import com.example.flexfit.ui.theme.FluxTextPrimary
import com.example.flexfit.ui.theme.FluxTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: ReminderData,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "reminderPulse${reminder.id}")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = if (reminder.enabled) 0.08f else 0.02f,
        targetValue = if (reminder.enabled) 0.18f else 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reminderGlow${reminder.id}"
    )
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.32f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            DismissBackdrop(
                state = dismissState,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        modifier = modifier
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .clickable { onExpandToggle() },
            shape = RoundedCornerShape(28.dp),
            gradient = Brush.verticalGradient(
                colors = listOf(
                    FluxSurfaceElevated.copy(alpha = 0.97f),
                    FluxSurface.copy(alpha = 0.95f)
                )
            ),
            contentPadding = PaddingValues(18.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    FluxGlow.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                )

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReminderStatusPill(reminder = reminder)
                            BasicText(
                                text = reminder.workoutName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FluxTextPrimary
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DetailBadge(
                                    icon = Icons.Filled.Alarm,
                                    text = reminder.displayTime(),
                                    accent = FluxAccent
                                )
                                DetailBadge(
                                    icon = if (reminder.enabled) {
                                        Icons.Filled.NotificationsActive
                                    } else {
                                        Icons.Filled.NotificationsOff
                                    },
                                    text = reminder.displayDaySummary(),
                                    accent = if (reminder.enabled) FluxAccent else FluxTextMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = FluxTextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderDay.ordered.forEach { day ->
                            val active = day in reminder.selectedDays
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (active) FluxAccent.copy(alpha = 0.14f) else FluxSurfaceSoft.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (active) FluxAccent.copy(alpha = 0.34f) else FluxCyan.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                BasicText(
                                    text = day.shortLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (active) FluxAurora else FluxTextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(260)),
                        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(220))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            DetailLine(
                                title = "AI note",
                                body = reminder.resolvedNote()
                            )
                            DetailLine(
                                title = "Ringtone",
                                body = reminder.ringtoneName
                            )
                            DetailLine(
                                title = "Vibration",
                                body = if (reminder.vibrationEnabled) "Neural pulse enabled" else "Silent haptic lane"
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MiniActionButton(
                                    label = if (reminder.enabled) "Disarm" else "Arm",
                                    icon = if (reminder.enabled) Icons.Filled.NotificationsOff else Icons.Filled.NotificationsActive,
                                    accent = if (reminder.enabled) FluxDanger else FluxAccent,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onEnabledChange(!reminder.enabled)
                                }
                                MiniActionButton(
                                    label = "Edit",
                                    icon = Icons.Filled.Edit,
                                    accent = FluxBlue,
                                    modifier = Modifier.weight(1f),
                                    onClick = onEdit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderStatusPill(reminder: ReminderData) {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (reminder.enabled) {
                        listOf(FluxAccent.copy(alpha = 0.14f), FluxSurfaceSoft.copy(alpha = 0.92f))
                    } else {
                        listOf(FluxDanger.copy(alpha = 0.16f), FluxSurfaceSoft.copy(alpha = 0.8f))
                    }
                ),
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = if (reminder.enabled) FluxAccent.copy(alpha = 0.42f) else FluxDanger.copy(alpha = 0.55f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        BasicText(
            text = if (reminder.enabled) "ACTIVE REMINDER" else "PAUSED REMINDER",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (reminder.enabled) FluxAurora else FluxDanger,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun DetailBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        BasicText(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(color = accent)
        )
    }
}

@Composable
private fun DetailLine(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = FluxTextMuted,
                fontFamily = FontFamily.Monospace
            )
        )
        BasicText(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(color = FluxTextSecondary)
        )
    }
}

@Composable
private fun MiniActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(200),
        label = "miniButton$label"
    )

    Row(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(accent.copy(alpha = 0.18f), FluxSurfaceSoft.copy(alpha = 0.86f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.48f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
        BasicText(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                color = FluxTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackdrop(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier
) {
    val progress = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        state.progress
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        FluxSurface.copy(alpha = 0.9f),
                        FluxDanger.copy(alpha = 0.15f + (progress * 0.35f))
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                color = FluxDanger.copy(alpha = 0.4f + progress * 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicText(
                text = "Delete",
                style = MaterialTheme.typography.titleMedium.copy(color = FluxTextPrimary)
            )
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = FluxDanger
            )
        }
    }
}
