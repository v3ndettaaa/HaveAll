package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val ring by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "alpha")
    val scale by animateFloatAsState(
        if (visible) 1f else 0.75f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    LaunchedEffect(Unit) { visible = true }

    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier.fillMaxSize().background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = ring)
                        .background(
                            Brush.sweepGradient(listOf(primary.copy(0f), primary, primary.copy(0f))),
                            CircleShape
                        )
                        .padding(3.dp)
                        .background(bg, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = glow * 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Hub, null, tint = primary, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "HaveAll",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "همه برای تو",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = primary,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(48.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(120.dp).height(2.dp).clip(CircleShape),
                color = primary,
                trackColor = primary.copy(alpha = 0.15f)
            )
        }
    }
}
