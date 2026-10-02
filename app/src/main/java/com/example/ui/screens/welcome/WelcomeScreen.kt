package com.example.ui.screens.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.F2WCyanPrimary
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLaunched by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Auto-advance after 2.3 seconds
    LaunchedEffect(Unit) {
        isLaunched = true
        delay(2300)
        onContinue()
    }

    // Dynamic scale and entrance animation
    val logoScale by animateFloatAsState(
        targetValue = if (isLaunched) 1.0f else 0.4f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "logo_scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isLaunched) 1.0f else 0f,
        animationSpec = tween(durationMillis = 1100, delayMillis = 300),
        label = "content_alpha"
    )

    // Breathing spotlight pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_value"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onContinue
            )
            .testTag("welcome_screen_root")
    ) {
        // Dramatic Spotlight Overhead Beam Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val logoCenterY = canvasHeight * 0.44f

            // 1. Conical Downward Light Beam from the very top (y = 0) down to the logo
            val lampWidth = 24.dp.toPx()
            val beamBottomWidth = (canvasWidth * 0.52f) * pulse
            val beamBottomY = logoCenterY + 110.dp.toPx()

            val beamPath = Path().apply {
                moveTo(centerX - lampWidth / 2f, 0f)
                lineTo(centerX + lampWidth / 2f, 0f)
                lineTo(centerX + beamBottomWidth / 2f, beamBottomY)
                lineTo(centerX - beamBottomWidth / 2f, beamBottomY)
                close()
            }

            // Downward gradient light
            val beamBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.52f * pulse),
                    Color(0xFFE0F7FA).copy(alpha = 0.30f * pulse),
                    Color(0xFF80DEEA).copy(alpha = 0.14f * pulse),
                    Color(0x00000000)
                ),
                start = Offset(centerX, 0f),
                end = Offset(centerX, beamBottomY)
            )

            drawPath(path = beamPath, brush = beamBrush)

            // 2. Focused Radial Spotlight Pool directly onto and around the logo
            val poolRadius = 160.dp.toPx() * pulse
            val radialPoolBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.45f * pulse),
                    Color(0xFFB2EBF2).copy(alpha = 0.28f * pulse),
                    Color(0xFF80DEEA).copy(alpha = 0.12f * pulse),
                    Color(0x00000000)
                ),
                center = Offset(centerX, logoCenterY),
                radius = poolRadius
            )

            drawCircle(
                brush = radialPoolBrush,
                radius = poolRadius,
                center = Offset(centerX, logoCenterY)
            )

            // 3. Top Lamp Fixture lens glow at the very top (source of light)
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = 14.dp.toPx(),
                center = Offset(centerX, 0f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.85f),
                        Color(0xFF80DEEA).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, 0f),
                    radius = 42.dp.toPx()
                ),
                radius = 42.dp.toPx(),
                center = Offset(centerX, 0f)
            )
        }

        // Content positioned vertically: Logo in the illuminated spotlight center
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.38f))

            // The Illuminated Logo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(logoScale),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_f2w_logo),
                    contentDescription = "F2W Player Logo",
                    modifier = Modifier.size(146.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(contentAlpha)
            ) {
                Text(
                    text = "F2W Player",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Universal Media Player",
                    color = F2WCyanPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.45f))

            // Bottom loading / tap hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .scale(contentAlpha)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .size(width = 120.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = F2WCyanPrimary,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Gusa popote kuendelea",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
