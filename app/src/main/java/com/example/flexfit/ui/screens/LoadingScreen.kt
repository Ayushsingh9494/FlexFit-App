package com.example.flexfit.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flexfit.FluxBackdrop
import com.example.flexfit.GlassPanel
import com.example.flexfit.GradientChip
import com.example.flexfit.HaloIcon
import com.example.flexfit.ui.theme.FluxAurora
import com.example.flexfit.ui.theme.FluxCyan
import com.example.flexfit.ui.theme.FluxGlow
import com.example.flexfit.ui.theme.FluxTextPrimary
import com.example.flexfit.ui.theme.FluxTextSecondary

@Composable
fun LoadingScreen(message: String) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pulse = rememberInfiniteTransition(label = "loading")
    val glow by pulse.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    FluxBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassPanel(
                shape = RoundedCornerShape(34.dp),
                gradient = Brush.verticalGradient(
                    colors = listOf(
                        FluxCyan.copy(alpha = 0.12f),
                        FluxAurora.copy(alpha = 0.08f),
                        Color(0xCC0B1023)
                    )
                )
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(FluxGlow.copy(alpha = glow), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(34.dp)
                            )
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GradientChip(text = "SESSION RESTORE")
                        HaloIcon(icon = Icons.Filled.AutoAwesome, accent = FluxGlow, size = 64.dp)
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = FluxCyan,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.titleLarge,
                            color = FluxTextPrimary
                        )
                        Text(
                            text = "Checking Firebase Authentication and aligning your premium dashboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FluxTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
