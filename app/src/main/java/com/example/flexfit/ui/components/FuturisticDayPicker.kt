package com.example.flexfit.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexfit.reminder.ReminderDay
import com.example.flexfit.ui.theme.FluxAccent
import com.example.flexfit.ui.theme.FluxBlue
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxSurface
import com.example.flexfit.ui.theme.FluxSurfaceSoft
import com.example.flexfit.ui.theme.FluxTextMuted
import com.example.flexfit.ui.theme.FluxTextPrimary
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun FuturisticDayPicker(
    selectedDays: Set<ReminderDay>,
    onDayToggle: (ReminderDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(ReminderDay.ordered, key = { it.name }) { day ->
            val selected = day in selectedDays
            val scale = animateFloatAsState(
                targetValue = if (selected) 1f else 0.95f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
                label = "dayScale${day.name}"
            )
            val glowTransition = rememberInfiniteTransition(label = "dayGlow${day.name}")
            val glowAlpha = glowTransition.animateFloat(
                initialValue = if (selected) 0.1f else 0.03f,
                targetValue = if (selected) 0.22f else 0.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dayGlowAlpha${day.name}"
            )

            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (selected) {
                                listOf(FluxGlow.copy(alpha = 0.95f), FluxBlue.copy(alpha = 0.55f))
                            } else {
                                listOf(FluxCyan.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
                            }
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (selected) {
                                listOf(
                                    FluxAccent.copy(alpha = 0.22f),
                                    FluxSurfaceSoft.copy(alpha = 0.94f)
                                )
                            } else {
                                listOf(
                                    FluxSurfaceSoft.copy(alpha = 0.9f),
                                    FluxSurface.copy(alpha = 0.92f)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDayToggle(day)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    FluxGlow.copy(alpha = glowAlpha.value),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = if (selected) {
                                        listOf(FluxGlow.copy(alpha = 0.95f), FluxBlue.copy(alpha = 0.45f))
                                    } else {
                                        listOf(FluxCyan.copy(alpha = 0.2f), Color.Transparent)
                                    }
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) FluxGlow.copy(alpha = 0.95f) else FluxCyan.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.shortDisplayLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = FluxTextPrimary
                        )
                    }

                    Column {
                        Text(
                            text = day.shortLabel.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = FluxTextPrimary
                        )
                        Text(
                            text = if (selected) "Synced" else "Idle",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) FluxGlow else FluxTextMuted
                        )
                    }
                }
            }
        }
    }
}
