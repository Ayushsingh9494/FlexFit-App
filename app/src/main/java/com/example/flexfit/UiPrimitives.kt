package com.example.flexfit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.flexfit.ui.theme.FluxAurora
import com.example.flexfit.ui.theme.FluxBlue
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxAccent
import com.example.flexfit.ui.theme.FluxSurface
import com.example.flexfit.ui.theme.FluxSurfaceElevated
import com.example.flexfit.ui.theme.FluxSurfaceSoft
import com.example.flexfit.ui.theme.FluxTextMuted
import com.example.flexfit.ui.theme.FluxTextPrimary

@Composable
fun FluxBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val ambientMotion = rememberInfiniteTransition(label = "backdrop")
    val drift by ambientMotion.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050505),
                        Color(0xFF0C0C0C),
                        FluxSurface
                    )
                )
            )
    ) {
        GlowOrb(
            color = FluxAccent.copy(alpha = 0.09f),
            size = 260.dp,
            alignment = Alignment.TopStart,
            offsetX = (-60).dp,
            offsetY = (drift / 2f).dp
        )
        GlowOrb(
            color = FluxCyan.copy(alpha = 0.07f),
            size = 320.dp,
            alignment = Alignment.CenterEnd,
            offsetX = 110.dp,
            offsetY = (-120).dp
        )
        GlowOrb(
            color = FluxAurora.copy(alpha = 0.05f),
            size = 200.dp,
            alignment = Alignment.BottomStart,
            offsetX = (-40).dp,
            offsetY = 80.dp
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent,
                            FluxAccent.copy(alpha = 0.03f)
                        )
                    )
                )
        )
        content()
    }
}

@Composable
private fun GlowOrb(
    color: Color,
    size: Dp,
    alignment: Alignment,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX, y = offsetY)
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color, Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    gradient: Brush = Brush.linearGradient(
        colors = listOf(FluxSurfaceElevated.copy(alpha = 0.98f), FluxSurface.copy(alpha = 0.95f))
    ),
    borderAlpha: Float = 1f,
    backgroundAlpha: Float = 1f,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.4f)),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, FluxSurfaceSoft.copy(alpha = 0.9f * borderAlpha))
    ) {
        Box(
            modifier = Modifier
                .background(brush = gradient, alpha = backgroundAlpha)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = shape
                )
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun GradientChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                1.dp,
                FluxAccent.copy(alpha = 0.45f),
                RoundedCornerShape(999.dp)
            )
            .background(
                Brush.horizontalGradient(
                    colors = listOf(FluxAccent.copy(alpha = 0.18f), FluxSurfaceSoft.copy(alpha = 0.96f))
                ),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = FluxAurora
        )
    }
}

@Composable
fun NeonActionButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(accent.copy(alpha = 0.18f), FluxSurfaceSoft.copy(alpha = 0.96f))
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.34f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FluxTextPrimary
            )
        }
    }
}

@Composable
fun DataPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FluxTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = FluxTextPrimary
        )
    }
}

@Composable
fun HaloIcon(
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.16f), FluxSurfaceSoft.copy(alpha = 0.94f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent)
    }
}
