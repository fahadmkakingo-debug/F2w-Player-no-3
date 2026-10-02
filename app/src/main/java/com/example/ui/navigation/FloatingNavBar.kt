package com.example.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WNavBackground

/**
 * Modern floating pill navigation bar with transparent glassmorphism styling
 * and an animated liquid water wave shimmer effect inside the curved capsule.
 * The curved pill card protrudes upward so its top half floats freely over content,
 * while only the bottom half has the docking background behind it.
 */
@Composable
fun FloatingNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("floating_bottom_navigation_bar"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Bottom-Half Background Layer (Only covers bottom half behind the card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            F2WNavBackground.copy(alpha = 0.65f),
                            F2WNavBackground.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 0.6.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            F2WCardBorder.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    shape = androidx.compose.ui.graphics.RectangleShape
                )
                .align(Alignment.BottomCenter)
        )

        // 2. The Protruding Curved Capsule Pill Card (Carve card ya buttons imetokeza kwa juu)
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 5.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(percent = 50),
                    spotColor = F2WCyanPrimary.copy(alpha = 0.50f),
                    ambientColor = Color.Black.copy(alpha = 0.40f)
                )
                .clip(RoundedCornerShape(percent = 50))
                // Glass Base Layer (Translucent Frosted Tint)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x990F2438), // Transparent glassy deep navy-cyan
                            Color(0xB3061422)  // Transparent bottom glass tint
                        )
                    )
                )
                // Specular Glass Border Highlight
                .border(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f), // Glass top reflection
                            F2WCyanPrimary.copy(alpha = 0.70f), // Cyan glass glow
                            Color.White.copy(alpha = 0.15f),
                            Color(0x400088FF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 100f)
                    ),
                    shape = RoundedCornerShape(percent = 50)
                )
        ) {
            // 🌊 Animated Liquid Water Waves & Fluid Sheen Layer
            GlassmorphismLiquidBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(percent = 50))
            )

            // Top Glass Sheen Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Navigation Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab

                    NavPillItem(
                        tab = tab,
                        isSelected = isSelected,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing animated fluid water waves and light caustic shimmer
 * to achieve a living, liquid glassmorphism effect.
 */
@Composable
private fun GlassmorphismLiquidBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_water_transition")

    // Wave 1: Gentle continuous wave roll
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    // Wave 2: Faster crossing liquid wave
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    // Shimmer: Fluid light sheen sweep across the glass
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // 1. Back fluid wave (Deep translucent luminous cyan wave)
        val path1 = Path().apply {
            moveTo(0f, height)
            val baseWaterLevel = height * 0.50f
            val waveAmplitude = height * 0.14f
            var x = 0f
            while (x <= width) {
                val y = baseWaterLevel + waveAmplitude * sin((x / width * 2 * Math.PI + wavePhase1).toDouble()).toFloat()
                lineTo(x, y)
                x += 8f
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = path1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x4400E5FF), // Translucent cyan wave crest
                    Color(0x300077D4), // Soft ocean body
                    Color(0x18002844)
                ),
                startY = height * 0.30f,
                endY = height
            )
        )

        // 2. Front fluid wave (Bright glistening liquid wave)
        val path2 = Path().apply {
            moveTo(0f, height)
            val baseWaterLevel = height * 0.58f
            val waveAmplitude = height * 0.11f
            var x = 0f
            while (x <= width) {
                val y = baseWaterLevel + waveAmplitude * sin((x / width * 2 * Math.PI + wavePhase2 + 1.4).toDouble()).toFloat()
                lineTo(x, y)
                x += 8f
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = path2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x5500F0FF), // Vivid luminous liquid crest
                    Color(0x350088EA),
                    Color(0x20041828)
                ),
                startY = height * 0.40f,
                endY = height
            )
        )

        // 3. Diagonal light caustic shimmer sweep
        val shimmerStartX = width * shimmerOffset
        val shimmerEndX = shimmerStartX + width * 0.35f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.04f),
                    Color(0x3300E5FF),
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                start = Offset(shimmerStartX, 0f),
                end = Offset(shimmerEndX, height)
            ),
            blendMode = BlendMode.Screen
        )
    }
}

@Composable
private fun NavPillItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Spring animation for active bubble entrance
    val bubbleScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bubble_scale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )

    val unselectedColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF8BA5B8),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "unselected_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color(0xFF00A2FF), bounded = false, radius = 28.dp),
                onClick = onClick
            )
            .testTag(tab.testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Elevated Vibrant Circular Button (matching the "Cart" circle in the reference image)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(bubbleScale)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFF0088EA),
                        ambientColor = Color(0x660088EA)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00A0FA), // Vibrant electric sky blue
                                Color(0xFF0072D4)  // Rich vivid azure
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = tab.selectedIcon,
                        contentDescription = tab.title,
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(iconScale)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.title,
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                }
            }
        } else {
            // Unselected Item (clean outline icon + title in soft blue-slate)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = tab.unselectedIcon,
                    contentDescription = tab.title,
                    tint = unselectedColor,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = tab.title,
                    color = unselectedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}
