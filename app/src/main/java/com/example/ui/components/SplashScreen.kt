package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(
    darkMode: Boolean,
    onFinished: () -> Unit
) {
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
        kotlinx.coroutines.delay(2200)
        onFinished()
    }

    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(700),
        label = "alpha"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    val bg = if (darkMode) Color(0xFF0A0F1D) else Color(0xFFF3F6FD)
    val accent = if (darkMode) Color(0xFF00E5FF) else Color(0xFF2979FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(
                scaleX = scale * pulseScale, scaleY = scale * pulseScale, alpha = alpha
            )
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation)
                        .border(3.dp, Brush.sweepGradient(listOf(accent, Color(0xFF9061F9), accent)), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Cloud, "Logo", tint = accent, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("HAVEALL", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = if (darkMode) Color.White else Color(0xFF0A0F1D))
            Spacer(modifier = Modifier.height(4.dp))
            Text("همه برای تو", fontSize = 14.sp, color = (if (darkMode) Color.White else Color(0xFF0A0F1D)).copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(modifier = Modifier.size(30.dp), color = accent, strokeWidth = 2.5.dp)
        }
    }
}
