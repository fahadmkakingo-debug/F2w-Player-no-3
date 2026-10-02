package com.example.ui.screens.privacy

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WVioletAccent
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PrivacyPinDots(
    pinLength: Int,
    maxDigits: Int = 4,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDigits) {
            val isFilled = i < pinLength
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> Color(0xFFEF4444)
                            isFilled -> F2WCyanPrimary
                            else -> Color(0xFF1E2330)
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isError -> Color(0xFFEF4444)
                            isFilled -> F2WCyanPrimary
                            else -> F2WCardBorder
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun PrivacyKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    showFingerprint: Boolean = false,
    onFingerprintClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (digit in row) {
                    Privacy3DWetGlassKeypadButton(
                        text = digit,
                        testTag = "pin_digit_$digit",
                        onClick = { onDigitClick(digit) }
                    )
                }
            }
        }

        // Bottom Row: [Fingerprint / Empty] [ 0 ] [ Backspace ]
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showFingerprint) {
                Privacy3DWetGlassKeypadButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Use Fingerprint",
                            tint = F2WVioletAccent,
                            modifier = Modifier.size(34.dp)
                        )
                    },
                    testTag = "pin_fingerprint_btn",
                    seedOffset = 11,
                    accentGlow = F2WVioletAccent,
                    onClick = onFingerprintClick
                )
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }

            Privacy3DWetGlassKeypadButton(
                text = "0",
                testTag = "pin_digit_0",
                seedOffset = 0,
                onClick = { onDigitClick("0") }
            )

            Privacy3DWetGlassKeypadButton(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Futa Nambari",
                        tint = Color(0xFFE2F4FA),
                        modifier = Modifier.size(26.dp)
                    )
                },
                testTag = "pin_delete_btn",
                seedOffset = 12,
                onClick = onDeleteClick
            )
        }
    }
}

/**
 * 3D Wet Glass Keypad Button with glistening water droplets (dew drops)
 * and interactive "hand plunged into water" liquid splash ripple animation.
 */
@Composable
private fun Privacy3DWetGlassKeypadButton(
    text: String? = null,
    icon: (@Composable () -> Unit)? = null,
    testTag: String,
    seedOffset: Int = text?.toIntOrNull() ?: 5,
    accentGlow: Color = F2WCyanPrimary,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val coroutineScope = rememberCoroutineScope()

    // Interactive Hand-In-Water Splash Ripple Animation
    val splashProgress = remember { Animatable(0f) }

    val handleButtonClick = {
        coroutineScope.launch {
            splashProgress.snapTo(0f)
            splashProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
        onClick()
    }

    // Tactile 3D press scale
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1.0f,
        label = "button_scale_$testTag"
    )

    // Independent Water Droplets & Wet Sheen Animation
    val transition = rememberInfiniteTransition(label = "glass_wet_$testTag")

    // Dew droplets glistening sparkle pulse
    val dropletGleam by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700 + (seedOffset * 220), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "droplet_gleam_$testTag"
    )

    // Soft wet sheen sweep across the glass
    val sheenProgress by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600 + (seedOffset * 350), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_$testTag"
    )

    // 3D Bevel Border (light reflection at top-left, shadow depth at bottom-right)
    val bevelBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f),
            accentGlow.copy(alpha = 0.65f),
            Color(0x33102A45),
            Color.Black.copy(alpha = 0.85f)
        ),
        start = Offset(0f, 0f),
        end = Offset(200f, 200f)
    )

    // Translucent Frosted Wet Glass Gradient Fill
    val glassBaseBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xCC183B59), // translucent glossy wet glass tint
            Color(0xEE0E2438), // mid crystal body
            Color(0xFA071625)  // deep 3D shadow bottom
        ),
        center = Offset(100f, 60f),
        radius = 180f
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(pressScale)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                spotColor = accentGlow.copy(alpha = 0.70f),
                ambientColor = Color(0x55000000)
            )
            .clip(CircleShape)
            .background(glassBaseBrush)
            .border(width = 1.4.dp, brush = bevelBorderBrush, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentGlow),
                onClick = handleButtonClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // 💧 Layer 1: Wet Glass Surface with Dew Droplets & Interactive Splash Animation
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)

            if (width <= 0 || height <= 0) return@Canvas

            // 1. Wet glass soft condensation radial mist
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentGlow.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.4f, height * 0.35f),
                    radius = width * 0.6f
                ),
                radius = width * 0.5f,
                center = center
            )

            // 2. Dew Water Droplets on glass surface (Unique per button seed)
            val dropConfigs = listOf(
                listOf(0.22f, 0.28f, 3.2f, 0.95f),
                listOf(0.74f, 0.25f, 2.8f, 0.85f),
                listOf(0.20f, 0.70f, 3.0f, 0.90f),
                listOf(0.78f, 0.68f, 2.4f, 0.75f),
                listOf(0.48f, 0.18f, 2.0f, 0.80f),
                listOf(0.50f, 0.82f, 2.6f, 0.85f),
                listOf(0.85f, 0.45f, 1.8f, 0.70f),
                listOf(0.14f, 0.48f, 1.9f, 0.70f)
            )

            dropConfigs.forEachIndexed { idx, drop ->
                val xFrac = (drop[0] + (seedOffset * 0.08f)) % 0.82f + 0.09f
                val yFrac = (drop[1] + (seedOffset * 0.06f)) % 0.82f + 0.09f
                val baseRadius = drop[2]
                val individualFactor = drop[3]

                val cx = width * xFrac
                val cy = height * yFrac

                // Droplet shadow on glass
                drawCircle(
                    color = Color.Black.copy(alpha = 0.42f),
                    radius = baseRadius + 0.4f,
                    center = Offset(cx + 0.6f, cy + 0.9f)
                )

                // Translucent water droplet body
                drawCircle(
                    color = Color(0x6000F0FF),
                    radius = baseRadius,
                    center = Offset(cx, cy)
                )

                // Bottom internal refraction glow
                drawCircle(
                    color = Color.White.copy(alpha = 0.40f * dropletGleam),
                    radius = baseRadius * 0.55f,
                    center = Offset(cx + 0.3f, cy + (baseRadius * 0.35f))
                )

                // Glistening highlight spark on top-left of droplet
                val highAlpha = (0.75f + 0.25f * sin(dropletGleam * PI.toFloat() * individualFactor)).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = highAlpha),
                    radius = (baseRadius * 0.4f).coerceAtLeast(0.8f),
                    center = Offset(cx - (baseRadius * 0.35f), cy - (baseRadius * 0.35f))
                )
            }

            // 3. Gentle wet sheen light sweep
            val sheenX = sheenProgress * (width + height)
            val sheenStart = Offset(sheenX - 30f, 0f)
            val sheenEnd = Offset(sheenX + 30f, height)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = sheenStart,
                    end = sheenEnd
                )
            )

            // 🌊 4. INTERACTIVE HAND-IN-WATER SPLASH RIPPLES (When pressed)
            val p = splashProgress.value
            if (p > 0f && p < 1f) {
                // Flash of water entry disturbance
                val flashAlpha = ((1f - p) * 0.55f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = flashAlpha),
                            accentGlow.copy(alpha = flashAlpha * 0.7f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = width * 0.55f * p.coerceAtLeast(0.1f)
                    ),
                    radius = width * 0.55f * p.coerceAtLeast(0.1f),
                    center = center
                )

                // Primary expanding liquid wave ring 1
                val r1 = (width * 0.65f) * p
                val alpha1 = ((1f - p) * 0.85f).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = alpha1),
                    radius = r1,
                    center = center,
                    style = Stroke(width = (4f * (1f - p)).coerceAtLeast(1f))
                )
                drawCircle(
                    color = accentGlow.copy(alpha = alpha1 * 0.6f),
                    radius = (r1 - 2.5f).coerceAtLeast(0f),
                    center = center,
                    style = Stroke(width = (2.5f * (1f - p)).coerceAtLeast(0.8f))
                )

                // Secondary delayed liquid wave ring 2
                if (p > 0.18f) {
                    val p2 = (p - 0.18f) / 0.82f
                    val r2 = (width * 0.55f) * p2
                    val alpha2 = ((1f - p2) * 0.70f).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = alpha2),
                        radius = r2,
                        center = center,
                        style = Stroke(width = (3f * (1f - p2)).coerceAtLeast(0.8f))
                    )
                }

                // Liquid splash droplet particles radiating outwards
                val splashParticleCount = 8
                for (i in 0 until splashParticleCount) {
                    val angle = (i.toFloat() / splashParticleCount) * 2f * PI.toFloat() + (seedOffset * 0.3f)
                    val particleDist = (width * 0.48f) * (p * 0.95f)
                    val px = center.x + (cos(angle) * particleDist)
                    val py = center.y + (sin(angle) * particleDist)
                    val particleAlpha = ((1f - p) * 0.85f).coerceIn(0f, 1f)
                    val particleRadius = (2.5f * (1f - p * 0.5f)).coerceAtLeast(0.8f)

                    drawCircle(
                        color = Color.White.copy(alpha = particleAlpha),
                        radius = particleRadius,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Layer 2: Top 3D Specular Glass Glare Overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.36f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 40f
                    )
                )
        )

        // Layer 3: Button Text / Icon
        if (text != null) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            icon?.invoke()
        }
    }
}

