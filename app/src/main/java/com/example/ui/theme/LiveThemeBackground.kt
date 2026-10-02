package com.example.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Global Live Theme Background Engine for Jetpack Compose.
 * Renders high-performance, GPU-accelerated 60 FPS live animated backdrops
 * for all live animated themes with independent color palettes:
 * 1. LIVE_GLASS_RAIN: White frosted crystal glass with water droplets & falling rain streaks.
 * 2. LIVE_ADVENTURE: Lush nature forest with trees, flowers, floating leaves & fireflies.
 * 3. LIVE_METROPOLIS: City skyline with illuminated skyscrapers, blinking windows & searchlights.
 * 4. LIVE_HIGHWAY_DRIVE: Cruising car on a scenic sunset highway with passing streetlights & trees.
 * 5. LIVE_GOLDEN_FIELDS: Rolling meadow hills, waving harvest wheat, drifting clouds & dandelion fluff.
 * 6. LIVE_WILDLIFE_SAFARI: Sunset savanna with grazing/walking deer, birds in flight & butterflies.
 * 7. LIVE_AURORA_WAVE: Undulating glowing Northern Lights (Aurora Borealis) & cosmic matrix.
 */
@Composable
fun LiveThemeBackground(
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    when (theme) {
        AppTheme.LIVE_GLASS_RAIN -> LiveWhiteGlassRainCanvas(modifier = modifier)
        AppTheme.LIVE_ADVENTURE -> LiveAdventureNatureCanvas(modifier = modifier)
        AppTheme.LIVE_METROPOLIS -> LiveMetropolisCanvas(modifier = modifier)
        AppTheme.LIVE_HIGHWAY_DRIVE -> LiveHighwayDriveCanvas(modifier = modifier)
        AppTheme.LIVE_GOLDEN_FIELDS -> LiveGoldenFieldsCanvas(modifier = modifier)
        AppTheme.LIVE_WILDLIFE_SAFARI -> LiveWildlifeSafariCanvas(modifier = modifier)
        AppTheme.LIVE_AURORA_WAVE -> LiveAuroraWaveCanvas(modifier = modifier)
        else -> Box(modifier = modifier)
    }
}

// -------------------------------------------------------------
// 1. WHITE FROSTED GLASS RAIN (Water droplets on white glass)
// -------------------------------------------------------------
private data class WhiteRainDrop(
    val xNorm: Float,
    val speedNorm: Float,
    val lengthNorm: Float,
    val widthPx: Float,
    val alpha: Float,
    val phaseOffset: Float,
    val isHeavy: Boolean
)

private data class WhiteGlassDewDrop(
    val xNorm: Float,
    val yNorm: Float,
    val radiusPx: Float,
    val pulsePhase: Float
)

@Composable
fun LiveWhiteGlassRainCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "white_rain_anim")

    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_progress"
    )

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dew_shimmer"
    )

    val rainParticles = remember {
        List(50) { index ->
            val randA = ((index * 37 + 13) % 100) / 100f
            val randB = ((index * 59 + 71) % 100) / 100f
            val randC = ((index * 83 + 29) % 100) / 100f
            val isHeavy = index % 4 == 0
            WhiteRainDrop(
                xNorm = randA,
                speedNorm = 0.75f + randB * 0.9f,
                lengthNorm = if (isHeavy) 0.08f + randC * 0.05f else 0.04f + randC * 0.03f,
                widthPx = if (isHeavy) 2.4f else 1.4f,
                alpha = if (isHeavy) 0.75f else 0.45f + randB * 0.35f,
                phaseOffset = (index.toFloat() / 50f) + randC * 0.2f,
                isHeavy = isHeavy
            )
        }
    }

    val dewDrops = remember {
        List(30) { index ->
            val rx = ((index * 43 + 17) % 100) / 100f
            val ry = ((index * 79 + 31) % 100) / 100f
            val rad = 2.8f + (((index * 31) % 10) / 10f) * 3.5f
            WhiteGlassDewDrop(
                xNorm = rx,
                yNorm = ry,
                radiusPx = rad,
                pulsePhase = (index * 0.5f) % 6.28f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // White Frosted Crystal Glass Backdrop
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0),
                    Color(0xFFCBD5E1)
                )
            )
        )

        // Glass specular glare diagonal bands
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h * 0.7f)
            )
        )

        // Draw stationary glistening condensation dew droplets
        dewDrops.forEach { dew ->
            val cx = dew.xNorm * w
            val cy = dew.yNorm * h
            val pulse = (sin(shimmerProgress + dew.pulsePhase) + 1f) / 2f
            val radius = dew.radiusPx

            // Subtle dark shadow for 3D depth
            drawCircle(
                color = Color(0x33000000),
                radius = radius * 1.15f,
                center = Offset(cx, cy + radius * 0.35f)
            )

            // Refractive water dome
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x99E0F2FE),
                        Color(0x660284C7),
                        Color(0x330369A1)
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            // Specular reflection glint
            drawCircle(
                color = Color.White.copy(alpha = 0.85f + pulse * 0.15f),
                radius = radius * 0.40f,
                center = Offset(cx - radius * 0.3f, cy - radius * 0.3f)
            )
        }

        // Draw falling rain streaks from top to bottom
        rainParticles.forEach { drop ->
            val particleTime = (rainProgress * drop.speedNorm + drop.phaseOffset) % 1f
            val startY = (particleTime * (h + 150f)) - 100f
            val streakLen = drop.lengthNorm * h
            val endY = startY + streakLen
            val startX = drop.xNorm * w
            val endX = startX - (streakLen * 0.06f)

            if (endY > -20f && startY < h + 20f) {
                // Streak gradient on white glass
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x880284C7).copy(alpha = drop.alpha * 0.6f),
                            Color(0xFF0369A1).copy(alpha = drop.alpha)
                        ),
                        startY = startY,
                        endY = endY
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = drop.widthPx,
                    cap = StrokeCap.Round
                )

                // Droplet head
                drawCircle(
                    color = Color(0xFF0284C7).copy(alpha = drop.alpha),
                    radius = drop.widthPx * 1.15f,
                    center = Offset(endX, endY)
                )

                // Expanding ripple rings
                if (drop.isHeavy && endY > h * 0.65f) {
                    val ripplePhase = ((endY - (h * 0.65f)) / (h * 0.35f)).coerceIn(0f, 1f)
                    val rippleRadius = ripplePhase * 22f
                    val rippleAlpha = (1f - ripplePhase) * 0.4f
                    if (rippleAlpha > 0.02f) {
                        drawOval(
                            color = Color(0xFF0284C7).copy(alpha = rippleAlpha),
                            topLeft = Offset(endX - rippleRadius, endY - (rippleRadius * 0.35f)),
                            size = Size(rippleRadius * 2f, rippleRadius * 0.7f),
                            style = Stroke(width = 1.2f)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. LUSH ADVENTURE NATURE (Trees, blooming flowers, falling leaves)
// -------------------------------------------------------------
private data class FallingLeaf(
    val startXNorm: Float,
    val speedNorm: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val sizePx: Float,
    val color: Color,
    val phase: Float
)

@Composable
fun LiveAdventureNatureCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "nature_anim")

    val masterTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f * 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nature_time"
    )

    val leaves = remember {
        List(30) { index ->
            val colors = listOf(
                Color(0xFF10B981), // Emerald Leaf
                Color(0xFF34D399), // Mint Green
                Color(0xFFFFB703), // Autumn Amber
                Color(0xFFF43F5E), // Sakura Petal Pink
                Color(0xFFFB7185)  // Rose Blossom
            )
            FallingLeaf(
                startXNorm = ((index * 37 + 19) % 100) / 100f,
                speedNorm = 0.08f + ((index % 6) / 6f) * 0.10f,
                swayAmp = 15f + (index % 4) * 8f,
                swayFreq = 1.8f + (index % 3) * 0.6f,
                sizePx = 4f + (index % 4) * 2f,
                color = colors[index % colors.size],
                phase = (index * 0.8f)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep lush forest gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF041A11),
                    Color(0xFF082E1E),
                    Color(0xFF0D3F2B),
                    Color(0xFF051D14)
                )
            )
        )

        // Draw background silhouette forest trees
        val treePath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.70f)
            // Tree 1
            lineTo(w * 0.15f, h * 0.58f)
            lineTo(w * 0.28f, h * 0.72f)
            // Tree 2
            lineTo(w * 0.45f, h * 0.52f)
            lineTo(w * 0.62f, h * 0.74f)
            // Tree 3
            lineTo(w * 0.80f, h * 0.56f)
            lineTo(w * 0.92f, h * 0.68f)
            lineTo(w, h * 0.60f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = treePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x55064E3B), Color(0x99022C22), Color(0xFF021B14)),
                startY = h * 0.50f,
                endY = h
            )
        )

        // Blooming flowers and waving grass along bottom
        for (i in 0 until 18) {
            val fx = (i / 17f) * w
            val sway = sin(masterTime * 2f + (i * 0.7f)) * 5f
            val stemHeight = 35f + (i % 5) * 8f
            val fy = h - stemHeight

            // Stem
            drawLine(
                color = Color(0xFF10B981),
                start = Offset(fx, h),
                end = Offset(fx + sway, fy),
                strokeWidth = 2.5f
            )

            // Blossom Flower Head
            val flowerColors = listOf(Color(0xFFFFB703), Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF38BDF8))
            val petalColor = flowerColors[i % flowerColors.size]
            drawCircle(
                color = petalColor,
                radius = 5.5f,
                center = Offset(fx + sway, fy)
            )
            drawCircle(
                color = Color.White,
                radius = 2.2f,
                center = Offset(fx + sway, fy)
            )
        }

        // Falling leaves & drifting petals
        leaves.forEach { leaf ->
            val timeOffset = (masterTime * leaf.speedNorm + leaf.phase)
            val currYNorm = timeOffset % 1f
            val currY = currYNorm * (h + 40f) - 20f
            val swayX = sin(masterTime * leaf.swayFreq + leaf.phase) * leaf.swayAmp
            val currX = (leaf.startXNorm * w) + swayX
            val alpha = (sin(currYNorm * 3.14159f)).coerceIn(0.2f, 1f)

            drawOval(
                color = leaf.color.copy(alpha = alpha * 0.85f),
                topLeft = Offset(currX - leaf.sizePx, currY - (leaf.sizePx * 0.6f)),
                size = Size(leaf.sizePx * 2f, leaf.sizePx * 1.2f)
            )
        }

        // Glowing fireflies rising in the forest
        for (i in 0 until 15) {
            val fx = ((i * 47 + 13) % 100) / 100f * w
            val fy = ((1f - ((masterTime * 0.06f + (i * 0.15f)) % 1f)) * h)
            val glow = (sin(masterTime * 3f + (i * 1.2f)) + 1f) / 2f
            drawCircle(
                color = Color(0xFFFFB703).copy(alpha = glow * 0.8f),
                radius = 2.5f + glow * 1.5f,
                center = Offset(fx + sin(masterTime + i) * 10f, fy)
            )
        }
    }
}

// -------------------------------------------------------------
// 3. CITY SKYLINE METROPOLIS (Illuminated skyscrapers & searchlights)
// -------------------------------------------------------------
@Composable
fun LiveMetropolisCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "city_anim")

    val cityTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f * 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "city_time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Midnight skyline gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF070B14),
                    Color(0xFF0F172A),
                    Color(0xFF1E293B),
                    Color(0xFF0A0F1D)
                )
            )
        )

        // Stars in upper sky
        for (i in 0 until 35) {
            val sx = ((i * 41 + 17) % 100) / 100f * w
            val sy = ((i * 53 + 23) % 45) / 100f * h
            val twinkle = (sin(cityTime * 2f + i) + 1f) / 2f
            drawCircle(
                color = Color.White.copy(alpha = 0.2f + twinkle * 0.7f),
                radius = 1.0f + twinkle * 0.8f,
                center = Offset(sx, sy)
            )
        }

        // Sweeping Searchlight beams
        val sweepAngle = sin(cityTime * 0.8f) * 0.45f
        val beamOrigin = Offset(w * 0.75f, h * 0.65f)
        val beamLen = h * 0.6f
        val beamEnd = Offset(
            beamOrigin.x + sin(sweepAngle) * beamLen,
            beamOrigin.y - cos(sweepAngle) * beamLen
        )
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0x6600E5FF), Color(0x2200E5FF), Color.Transparent),
                start = beamOrigin,
                end = beamEnd
            ),
            start = beamOrigin,
            end = beamEnd,
            strokeWidth = 24f,
            cap = StrokeCap.Round
        )

        // Skyscraper building silhouettes
        val buildings = listOf(
            Triple(0.02f, 0.15f, 0.42f),
            Triple(0.18f, 0.16f, 0.55f),
            Triple(0.35f, 0.14f, 0.38f),
            Triple(0.50f, 0.20f, 0.62f),
            Triple(0.71f, 0.15f, 0.48f),
            Triple(0.87f, 0.12f, 0.58f)
        )

        buildings.forEachIndexed { bIndex, (xNorm, widthNorm, heightNorm) ->
            val bx = xNorm * w
            val bw = widthNorm * w
            val bh = heightNorm * h
            val by = h - bh

            // Building body
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF050914)),
                    startY = by,
                    endY = h
                ),
                topLeft = Offset(bx, by),
                size = Size(bw, bh)
            )

            // Antenna beacon on top
            val antennaX = bx + bw / 2f
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(antennaX, by),
                end = Offset(antennaX, by - 22f),
                strokeWidth = 2f
            )
            val beaconFlash = (sin(cityTime * 4f + bIndex) + 1f) / 2f
            drawCircle(
                color = Color(0xFFFF0055).copy(alpha = beaconFlash),
                radius = 3f,
                center = Offset(antennaX, by - 22f)
            )

            // Illuminated windows in building
            val cols = (bw / 12f).toInt().coerceAtLeast(2)
            val rows = (bh / 16f).toInt().coerceAtLeast(4)
            for (r in 1 until rows - 1) {
                for (c in 1 until cols) {
                    val wx = bx + (c.toFloat() / cols) * bw
                    val wy = by + (r.toFloat() / rows) * bh
                    val isLit = ((bIndex * 19 + r * 7 + c * 13) % 5) != 0
                    val flicker = sin(cityTime + (r * c)) > 0.85f

                    if (isLit && !flicker) {
                        val windowColor = if ((r + c) % 3 == 0) Color(0xFFFFC107) else Color(0xFF38BDF8)
                        drawRect(
                            color = windowColor.copy(alpha = 0.75f),
                            topLeft = Offset(wx - 2f, wy - 3f),
                            size = Size(4f, 6f)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. SUNSET HIGHWAY DRIVE (Cruising car on animated road)
// -------------------------------------------------------------
@Composable
fun LiveHighwayDriveCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "highway_anim")

    val driveTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drive_time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Sunset sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF1E0826),
                    Color(0xFF4A154B),
                    Color(0xFF9D174D),
                    Color(0xFFFF5E00),
                    Color(0xFFFFB703),
                    Color(0xFF270E36)
                ),
                startY = 0f,
                endY = h * 0.65f
            )
        )

        // Sunset sun glowing disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFFB703), Color(0xFFFF5E00), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = 80f
            ),
            radius = 80f,
            center = Offset(w * 0.5f, h * 0.42f)
        )

        // Road Surface Perspective
        val horizonY = h * 0.56f
        val roadPath = Path().apply {
            moveTo(w * 0.42f, horizonY)
            lineTo(w * 0.58f, horizonY)
            lineTo(w * 0.95f, h)
            lineTo(w * 0.05f, h)
            close()
        }
        drawPath(
            path = roadPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2D1240), Color(0xFF1B0B26), Color(0xFF0F0617)),
                startY = horizonY,
                endY = h
            )
        )

        // Moving animated road dashed center lines
        val lineCount = 7
        for (i in 0 until lineCount) {
            val prog = (driveTime + (i.toFloat() / lineCount)) % 1f
            val ly = horizonY + (prog * prog) * (h - horizonY)
            val lw = 2f + prog * 10f
            val lh = 4f + prog * 28f
            drawRect(
                color = Color(0xFFFFB703).copy(alpha = prog * 0.9f),
                topLeft = Offset((w / 2f) - (lw / 2f), ly),
                size = Size(lw, lh)
            )
        }

        // Passing roadside trees and light posts
        for (i in 0 until 5) {
            val prog = (driveTime + (i.toFloat() / 5f)) % 1f
            val py = horizonY + (prog * prog) * (h - horizonY)
            val lx = (w * 0.40f) - (prog * w * 0.38f)
            val rx = (w * 0.60f) + (prog * w * 0.38f)
            val sizeP = 6f + prog * 30f

            // Left tree silhouette
            drawCircle(
                color = Color(0xFF12051E).copy(alpha = prog),
                radius = sizeP,
                center = Offset(lx, py)
            )
            // Right streetlight
            drawCircle(
                color = Color(0xFFFFE066).copy(alpha = prog * 0.8f),
                radius = 3f + prog * 4f,
                center = Offset(rx, py - sizeP)
            )
        }

        // Cruising sleek modern sports car on the road
        val carW = 105f
        val carH = 38f
        val carX = (w / 2f) - (carW / 2f)
        val carY = h * 0.78f

        // Car shadow
        drawOval(
            color = Color(0x99000000),
            topLeft = Offset(carX - 10f, carY + carH - 4f),
            size = Size(carW + 20f, 16f)
        )

        // Car body
        val carBodyPath = Path().apply {
            moveTo(carX + 10f, carY + carH)
            lineTo(carX + 5f, carY + carH * 0.65f)
            lineTo(carX + 22f, carY + carH * 0.35f)
            lineTo(carX + 45f, carY + carH * 0.10f)
            lineTo(carX + 75f, carY + carH * 0.10f)
            lineTo(carX + 92f, carY + carH * 0.45f)
            lineTo(carX + carW, carY + carH * 0.70f)
            lineTo(carX + carW - 5f, carY + carH)
            close()
        }
        drawPath(
            path = carBodyPath,
            brush = Brush.horizontalGradient(
                listOf(Color(0xFFFF0055), Color(0xFFFF5E00), Color(0xFFFF0055)),
                startX = carX,
                endX = carX + carW
            )
        )

        // Car glowing red tail lights
        drawCircle(
            color = Color(0xFFFF0055),
            radius = 5f,
            center = Offset(carX + 8f, carY + carH * 0.65f)
        )
        drawCircle(
            color = Color(0xFFFF0055),
            radius = 5f,
            center = Offset(carX + carW - 8f, carY + carH * 0.65f)
        )
    }
}

// -------------------------------------------------------------
// 5. GOLDEN MEADOW FIELDS (Rolling hills, waving wheat, clouds)
// -------------------------------------------------------------
@Composable
fun LiveGoldenFieldsCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fields_anim")

    val fieldTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f * 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "field_time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Sunny Meadow Sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF0F1A0B),
                    Color(0xFF1B2D13),
                    Color(0xFF385E23),
                    Color(0xFFDDA15E),
                    Color(0xFF283618)
                )
            )
        )

        // Warm Sun & Rays
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEFAE0), Color(0xFFDDA15E), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.25f),
                radius = 90f
            ),
            radius = 90f,
            center = Offset(w * 0.8f, h * 0.25f)
        )

        // Rolling Hill 1 (Far background)
        val hill1 = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.58f)
            quadraticTo(w * 0.45f, h * 0.50f, w, h * 0.62f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = hill1,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF4C6B22), Color(0xFF283618)),
                startY = h * 0.50f,
                endY = h
            )
        )

        // Rolling Hill 2 (Foreground Golden Wheat Field)
        val hill2 = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.72f)
            quadraticTo(w * 0.55f, h * 0.64f, w, h * 0.76f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = hill2,
            brush = Brush.verticalGradient(
                listOf(Color(0xFFDDA15E), Color(0xFFBC6C25), Color(0xFF1B2B11)),
                startY = h * 0.64f,
                endY = h
            )
        )

        // Waving golden wheat stalks
        for (i in 0 until 35) {
            val wx = (i / 34f) * w
            val sway = sin(fieldTime * 2.2f + (i * 0.45f)) * 8f
            val baseH = 45f + (i % 6) * 6f
            val wy = h - baseH

            // Stalk
            drawLine(
                color = Color(0xFFE9C46A),
                start = Offset(wx, h),
                end = Offset(wx + sway, wy),
                strokeWidth = 2f
            )

            // Wheat Grain Head
            drawOval(
                color = Color(0xFFFEFAE0),
                topLeft = Offset(wx + sway - 3f, wy - 8f),
                size = Size(6f, 14f)
            )
        }

        // Floating dandelion fluff drifting horizontally
        for (i in 0 until 18) {
            val dx = ((fieldTime * 25f + (i * 45f)) % (w + 40f)) - 20f
            val dy = h * 0.35f + ((i * 37) % 40) / 100f * (h * 0.45f) + sin(fieldTime + i) * 12f
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2.2f,
                center = Offset(dx, dy)
            )
        }
    }
}

// -------------------------------------------------------------
// 6. WILDLIFE SAFARI (Grazing/walking deer, birds in flight)
// -------------------------------------------------------------
@Composable
fun LiveWildlifeSafariCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wildlife_anim")

    val safariTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f * 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "safari_time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Savanna Sunset Gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF1A0A05),
                    Color(0xFF4A1C0E),
                    Color(0xFF9E3D1B),
                    Color(0xFFF4A261),
                    Color(0xFFE76F51),
                    Color(0xFF1E0E06)
                )
            )
        )

        // Savanna Sunset Sun
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFF4A261), Color.Transparent),
                center = Offset(w * 0.3f, h * 0.35f),
                radius = 75f
            ),
            radius = 75f,
            center = Offset(w * 0.3f, h * 0.35f)
        )

        // Acacia tree silhouette on right
        val acaciaX = w * 0.82f
        val acaciaY = h * 0.68f
        drawLine(
            color = Color(0xFF120803),
            start = Offset(acaciaX, h),
            end = Offset(acaciaX, acaciaY),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        drawOval(
            color = Color(0xFF120803),
            topLeft = Offset(acaciaX - 60f, acaciaY - 18f),
            size = Size(120f, 26f)
        )
        drawOval(
            color = Color(0xFF120803),
            topLeft = Offset(acaciaX - 40f, acaciaY - 32f),
            size = Size(80f, 20f)
        )

        // Birds in flight gliding across sunset sky
        for (i in 0 until 5) {
            val bx = ((safariTime * 28f + (i * 60f)) % (w + 100f)) - 50f
            val by = h * 0.20f + (i * 18f) + sin(safariTime * 2f + i) * 8f
            val wingFlap = sin(safariTime * 8f + i) * 6f

            val birdPath = Path().apply {
                moveTo(bx - 12f, by - wingFlap)
                quadraticTo(bx - 6f, by, bx, by + 2f)
                quadraticTo(bx + 6f, by, bx + 12f, by - wingFlap)
            }
            drawPath(
                path = birdPath,
                color = Color(0xFF1E0E06),
                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
            )
        }

        // Savanna Ground Silhouette
        val groundPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.74f)
            quadraticTo(w * 0.4f, h * 0.70f, w, h * 0.75f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = groundPath,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF2C150B), Color(0xFF150702)),
                startY = h * 0.70f,
                endY = h
            )
        )

        // Animated Deer walking/grazing silhouette
        val deerProgress = (safariTime * 0.15f) % 1f
        val deerX = w * 0.15f + (deerProgress * w * 0.45f)
        val deerY = h * 0.73f

        // Deer Body
        drawOval(
            color = Color(0xFF150702),
            topLeft = Offset(deerX, deerY - 24f),
            size = Size(42f, 22f)
        )
        // Deer Neck & Head
        val neckPath = Path().apply {
            moveTo(deerX + 32f, deerY - 14f)
            lineTo(deerX + 42f, deerY - 38f)
            lineTo(deerX + 48f, deerY - 32f)
            lineTo(deerX + 38f, deerY - 10f)
            close()
        }
        drawPath(path = neckPath, color = Color(0xFF150702))
        // Antlers
        drawLine(
            color = Color(0xFF150702),
            start = Offset(deerX + 42f, deerY - 38f),
            end = Offset(deerX + 40f, deerY - 48f),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFF150702),
            start = Offset(deerX + 42f, deerY - 38f),
            end = Offset(deerX + 46f, deerY - 46f),
            strokeWidth = 2f
        )

        // Deer Legs (Walking motion)
        val legStep = sin(safariTime * 6f) * 6f
        drawLine(
            color = Color(0xFF150702),
            start = Offset(deerX + 8f, deerY - 4f),
            end = Offset(deerX + 6f + legStep, deerY + 22f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF150702),
            start = Offset(deerX + 16f, deerY - 4f),
            end = Offset(deerX + 18f - legStep, deerY + 22f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF150702),
            start = Offset(deerX + 28f, deerY - 4f),
            end = Offset(deerX + 26f - legStep, deerY + 22f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF150702),
            start = Offset(deerX + 34f, deerY - 4f),
            end = Offset(deerX + 36f + legStep, deerY + 22f),
            strokeWidth = 3f
        )

        // Glowing fluttering butterflies
        for (i in 0 until 4) {
            val bfx = deerX - 25f + (i * 28f) + sin(safariTime * 4f + i) * 14f
            val bfy = deerY - 35f + cos(safariTime * 3f + i) * 10f
            drawCircle(
                color = Color(0xFFF4A261).copy(alpha = 0.85f),
                radius = 3.5f,
                center = Offset(bfx, bfy)
            )
        }
    }
}

// -------------------------------------------------------------
// 7. NEON AURORA WAVE (Northern Lights & cosmic stardust)
// -------------------------------------------------------------
@Composable
fun LiveAuroraWaveCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_live_anim")

    val waveTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f * 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_wave_time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF020912),
                    Color(0xFF041824),
                    Color(0xFF081228),
                    Color(0xFF030710)
                )
            )
        )

        for (i in 0 until 40) {
            val px = ((i * 59 + 23) % 100) / 100f * w
            val py = ((i * 73 + 37) % 100) / 100f * h
            val shimmer = (sin(waveTime * 1.5f + i * 0.8f) + 1f) / 2f
            drawCircle(
                color = if (i % 3 == 0) Color(0xFF00F5D4).copy(alpha = 0.25f + shimmer * 0.5f) else Color.White.copy(alpha = 0.15f + shimmer * 0.45f),
                radius = 0.9f + shimmer * 1.3f,
                center = Offset(px, py)
            )
        }

        drawAuroraCurtain(
            w = w,
            h = h,
            baseY = h * 0.28f,
            amplitude = 42f,
            time = waveTime,
            frequency = 1.8f,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x5500F5D4),
                    Color(0x9900F5D4),
                    Color(0x447B2CBF),
                    Color.Transparent
                ),
                startY = h * 0.10f,
                endY = h * 0.52f
            )
        )

        drawAuroraCurtain(
            w = w,
            h = h,
            baseY = h * 0.42f,
            amplitude = 55f,
            time = waveTime + 1.6f,
            frequency = 1.3f,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x6639FF14),
                    Color(0x8800B4D8),
                    Color(0x557928CA),
                    Color.Transparent
                ),
                startY = h * 0.22f,
                endY = h * 0.68f
            )
        )
    }
}

private fun DrawScope.drawAuroraCurtain(
    w: Float,
    h: Float,
    baseY: Float,
    amplitude: Float,
    time: Float,
    frequency: Float,
    brush: Brush
) {
    val path = Path()
    val steps = 24
    val stepX = w / steps

    path.moveTo(0f, baseY)
    for (i in 0..steps) {
        val x = i * stepX
        val normX = i.toFloat() / steps
        val wave1 = sin(normX * 3.14159f * frequency + time) * amplitude
        val wave2 = cos(normX * 6.283185f * 0.7f + time * 0.6f) * (amplitude * 0.4f)
        val y = baseY + wave1 + wave2
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.lineTo(w, baseY + 180f)
    path.lineTo(0f, baseY + 180f)
    path.close()

    drawPath(
        path = path,
        brush = brush,
        style = Fill
    )
}
