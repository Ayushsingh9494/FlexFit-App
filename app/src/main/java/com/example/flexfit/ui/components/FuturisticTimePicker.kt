package com.example.flexfit.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfit.reminder.ReminderDay
import com.example.flexfit.ui.theme.FluxAccent
import com.example.flexfit.ui.theme.FluxBackground
import com.example.flexfit.ui.theme.FluxBlue
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxSurface
import com.example.flexfit.ui.theme.FluxSurfaceElevated
import com.example.flexfit.ui.theme.FluxSurfaceSoft
import com.example.flexfit.ui.theme.FluxTextMuted
import com.example.flexfit.ui.theme.FluxTextPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class TimeDialMode {
    HOUR,
    MINUTE
}

@Composable
fun FuturisticTimePicker(
    hour24: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dialMode by rememberSaveable { mutableStateOf(TimeDialMode.HOUR) }
    val isAm = hour24 < 12
    val displayHour = ((hour24 + 11) % 12) + 1
    val displayTime = String.format("%02d:%02d %s", displayHour, minute, if (isAm) "AM" else "PM")
    val animatedRing = rememberInfiniteTransition(label = "timeDial")
    val sweepRotation by animatedRing.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeSweepRotation"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DialModePill(
                label = "Hours",
                selected = dialMode == TimeDialMode.HOUR,
                modifier = Modifier.weight(1f)
            ) { dialMode = TimeDialMode.HOUR }
            DialModePill(
                label = "Minutes",
                selected = dialMode == TimeDialMode.MINUTE,
                modifier = Modifier.weight(1f)
            ) { dialMode = TimeDialMode.MINUTE }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            FluxSurfaceElevated.copy(alpha = 0.95f),
                            FluxSurface.copy(alpha = 0.96f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(FluxGlow.copy(alpha = 0.82f), Color.White.copy(alpha = 0.08f))
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(20.dp)
        ) {
            var dialSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
            val dialRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { 122.dp.toPx() }
            val labelRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { 110.dp.toPx() }
            val selectionAngle = if (dialMode == TimeDialMode.HOUR) {
                (displayHour % 12) * 30f
            } else {
                minute * 6f
            }
            val centerOffset = Offset(dialSize.width / 2f, dialSize.height / 2f)

            fun commitSelection(position: Offset) {
                if (dialSize.width == 0 || dialSize.height == 0) return
                val angle = normalizedAngle(position, centerOffset)
                if (dialMode == TimeDialMode.HOUR) {
                    val selectedHour = ((angle / 30f).roundToInt()).positiveMod(12).let {
                        if (it == 0) 12 else it
                    }
                    val adjustedHour = if (isAm) {
                        if (selectedHour == 12) 0 else selectedHour
                    } else {
                        if (selectedHour == 12) 12 else selectedHour + 12
                    }
                    onTimeChange(adjustedHour, minute)
                } else {
                    val adjustedMinute = ((angle / 6f).roundToInt()).positiveMod(60)
                    onTimeChange(hour24, adjustedMinute)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { dialSize = it }
                    .pointerInput(dialMode, hour24, minute) {
                        detectTapGestures { offset ->
                            commitSelection(offset)
                        }
                    }
                    .pointerInput(dialMode, hour24, minute) {
                        detectDragGestures(
                            onDragStart = { offset -> commitSelection(offset) },
                            onDrag = { change, _ ->
                                change.consume()
                                commitSelection(change.position)
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(10.dp)
                ) {
                    val radius = size.minDimension * 0.4f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(FluxAccent.copy(alpha = 0.18f), Color.Transparent)
                        ),
                        radius = radius * 1.2f
                    )
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                FluxGlow.copy(alpha = 0.6f),
                                FluxAccent.copy(alpha = 0.85f),
                                FluxBlue.copy(alpha = 0.6f),
                                FluxGlow.copy(alpha = 0.6f)
                            ),
                            center = center
                        ),
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 18f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, FluxGlow.copy(alpha = 0.82f), Color.Transparent),
                            center = center
                        ),
                        startAngle = sweepRotation,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 22f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.cornerPathEffect(12f)
                        )
                    )
                    drawCircle(
                        color = FluxSurface.copy(alpha = 0.96f),
                        radius = radius - 22f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(FluxBlue.copy(alpha = 0.22f), Color.Transparent)
                        ),
                        radius = radius * 0.82f,
                        blendMode = BlendMode.Screen
                    )

                    val markerCount = if (dialMode == TimeDialMode.HOUR) 12 else 60
                    repeat(markerCount) { index ->
                        val angle = Math.toRadians(index * (360.0 / markerCount) - 90.0)
                        val innerRadius = if (dialMode == TimeDialMode.MINUTE && index % 5 != 0) radius - 10f else radius - 18f
                        val outerRadius = radius + if (dialMode == TimeDialMode.MINUTE && index % 5 != 0) 4f else 10f
                        val start = Offset(
                            x = center.x + cos(angle).toFloat() * innerRadius,
                            y = center.y + sin(angle).toFloat() * innerRadius
                        )
                        val end = Offset(
                            x = center.x + cos(angle).toFloat() * outerRadius,
                            y = center.y + sin(angle).toFloat() * outerRadius
                        )
                        drawLine(
                            color = if (index == ((selectionAngle / (360f / markerCount)).roundToInt()).positiveMod(markerCount)) {
                                FluxGlow
                            } else {
                                FluxCyan.copy(alpha = if (dialMode == TimeDialMode.MINUTE && index % 5 != 0) 0.18f else 0.42f)
                            },
                            start = start,
                            end = end,
                            strokeWidth = if (dialMode == TimeDialMode.MINUTE && index % 5 != 0) 2f else 4f,
                            cap = StrokeCap.Round
                        )
                    }

                    val selectorAngle = Math.toRadians(selectionAngle.toDouble() - 90.0)
                    val handEnd = Offset(
                        x = center.x + cos(selectorAngle).toFloat() * (radius - 34f),
                        y = center.y + sin(selectorAngle).toFloat() * (radius - 34f)
                    )
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(FluxGlow, FluxAccent.copy(alpha = 0.92f))
                        ),
                        start = center,
                        end = handEnd,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = FluxGlow,
                        radius = 12f,
                        center = handEnd
                    )
                    drawCircle(
                        color = FluxBackground,
                        radius = 6f,
                        center = handEnd
                    )
                    drawCircle(
                        color = FluxGlow.copy(alpha = 0.9f),
                        radius = 8f
                    )
                }

                TimeDialLabels(
                    dialMode = dialMode,
                    displayHour = displayHour,
                    minute = minute,
                    center = centerOffset,
                    radiusPx = labelRadiusPx,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    FluxSurfaceSoft.copy(alpha = 0.88f),
                                    FluxSurface.copy(alpha = 0.94f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(FluxGlow.copy(alpha = 0.76f), Color.White.copy(alpha = 0.08f))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (dialMode == TimeDialMode.HOUR) "Hour Matrix" else "Minute Matrix",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        ),
                        color = FluxGlow
                    )
                    AnimatedContent(
                        targetState = displayTime,
                        label = "dialTimeText"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = FluxTextPrimary
                        )
                    }
                    Text(
                        text = "Drag the dial to tune precision",
                        style = MaterialTheme.typography.bodySmall,
                        color = FluxTextMuted
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AmPmToggle(
                label = "AM",
                selected = isAm,
                modifier = Modifier.weight(1f)
            ) {
                if (!isAm) {
                    onTimeChange((hour24 + 12).positiveMod(24), minute)
                }
            }
            AmPmToggle(
                label = "PM",
                selected = !isAm,
                modifier = Modifier.weight(1f)
            ) {
                if (isAm) {
                    onTimeChange((hour24 + 12).positiveMod(24), minute)
                }
            }
        }
    }
}

@Composable
private fun TimeDialLabels(
    dialMode: TimeDialMode,
    displayHour: Int,
    minute: Int,
    center: Offset,
    radiusPx: Float,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labels = if (dialMode == TimeDialMode.HOUR) {
        (1..12).map { value -> value to value.toString().padStart(2, '0') }
    } else {
        (0 until 12).map { value ->
            val minuteValue = value * 5
            minuteValue to minuteValue.toString().padStart(2, '0')
        }
    }

    Box(modifier = modifier) {
        labels.forEach { (value, label) ->
            val angleDegrees = if (dialMode == TimeDialMode.HOUR) {
                value * 30f
            } else {
                value * 30f
            }
            val angleRadians = Math.toRadians(angleDegrees.toDouble() - 90.0)
            val isSelected = if (dialMode == TimeDialMode.HOUR) {
                value == displayHour
            } else {
                minute == value
            }
            val x = center.x + cos(angleRadians).toFloat() * radiusPx
            val y = center.y + sin(angleRadians).toFloat() * radiusPx
            val offsetX = with(density) { (x - 18.dp.toPx()).roundToInt() }
            val offsetY = with(density) { (y - 12.dp.toPx()).roundToInt() }
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1f,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "dialLabelScale$label"
            )

            Text(
                text = label,
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .scale(scale),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) FluxGlow else FluxTextPrimary.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun DialModePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.97f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "modePill$label"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (selected) {
                        listOf(FluxAccent.copy(alpha = 0.28f), FluxSurfaceSoft.copy(alpha = 0.92f))
                    } else {
                        listOf(FluxSurfaceSoft.copy(alpha = 0.86f), FluxSurface.copy(alpha = 0.94f))
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (selected) {
                        listOf(FluxGlow.copy(alpha = 0.92f), FluxBlue.copy(alpha = 0.45f))
                    } else {
                        listOf(FluxCyan.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
                    }
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (selected) FluxGlow else FluxTextPrimary
        )
    }
}

@Composable
private fun AmPmToggle(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (selected) {
                        listOf(FluxGlow.copy(alpha = 0.26f), FluxSurfaceSoft.copy(alpha = 0.92f))
                    } else {
                        listOf(FluxSurfaceSoft.copy(alpha = 0.84f), FluxSurface.copy(alpha = 0.94f))
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (selected) {
                        listOf(FluxGlow.copy(alpha = 0.9f), FluxBlue.copy(alpha = 0.42f))
                    } else {
                        listOf(FluxCyan.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = if (selected) FluxGlow else FluxTextPrimary
        )
    }
}

private fun normalizedAngle(position: Offset, center: Offset): Float {
    val angle = Math.toDegrees(
        atan2(
            (position.y - center.y).toDouble(),
            (position.x - center.x).toDouble()
        )
    ) + 90.0
    return ((angle + 360.0) % 360.0).toFloat()
}

private fun Int.positiveMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor
