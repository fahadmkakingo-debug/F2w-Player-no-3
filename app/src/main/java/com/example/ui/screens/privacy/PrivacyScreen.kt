package com.example.ui.screens.privacy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.security.PrivacySecurityManager
import com.example.ui.theme.F2WSurface
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun PrivacyScreen(
    onBackToHome: () -> Unit = {},
    onVaultUnlockedChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val securityManager = remember { PrivacySecurityManager.getInstance(context) }

    var isPinConfigured by remember { mutableStateOf(securityManager.isPinConfigured()) }
    var isVaultUnlocked by remember { mutableStateOf(false) }

    // Notify parent when unlock status changes (to hide/show bottom navigation bar)
    LaunchedEffect(isVaultUnlocked) {
        onVaultUnlockedChanged(isVaultUnlocked)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("privacy_screen_container")
    ) {
        if (!isPinConfigured) {
            PrivacySetupScreen(
                securityManager = securityManager,
                onSetupComplete = {
                    isPinConfigured = true
                    isVaultUnlocked = true
                }
            )
        } else {
            PageCurlUnveilBox(
                isUnlocked = isVaultUnlocked,
                lockContent = {
                    PrivacyLockScreen(
                        securityManager = securityManager,
                        onUnlockSuccess = {
                            isVaultUnlocked = true
                        }
                    )
                },
                unlockedContent = {
                    PrivacyVaultContentScreen(
                        securityManager = securityManager,
                        onLockVault = {
                            isVaultUnlocked = false
                        },
                        onBackToHome = {
                            isVaultUnlocked = false
                            onBackToHome()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun PageCurlUnveilBox(
    isUnlocked: Boolean,
    lockContent: @Composable () -> Unit,
    unlockedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnimRunning by remember { mutableStateOf(false) }
    val curlAnim = remember { Animatable(0f) }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            isAnimRunning = true
            curlAnim.snapTo(0f)
            curlAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
            isAnimRunning = false
        } else {
            curlAnim.snapTo(0f)
            isAnimRunning = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isUnlocked) {
            // 1. Vault Content rendered underneath
            Box(modifier = Modifier.fillMaxSize()) {
                unlockedContent()
            }

            // 2. Page Curl Peeling Overlay (Active during unlock animation)
            if (isAnimRunning || curlAnim.value < 1f) {
                val progress = curlAnim.value

                // Lock screen container with curl transformation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Translate up-left as paper curls away
                            translationX = -progress * size.width * 0.5f
                            translationY = -progress * size.height * 0.7f
                            rotationZ = -progress * 22f
                            alpha = (1f - progress * 0.7f).coerceIn(0f, 1f)
                        }
                ) {
                    lockContent()
                }

                // Page Curl Paper Flap & Shadow Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val maxDiag = hypot(w, h)
                    val peelDist = progress * maxDiag * 1.35f

                    val angleRad = Math.toRadians(38.0)
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()

                    val foldX = w - peelDist * cosA
                    val foldY = h - peelDist * sinA

                    if (progress > 0.01f && progress < 0.99f) {
                        // Drop Shadow under paper fold
                        val shadowPath = Path().apply {
                            moveTo(foldX, foldY)
                            lineTo(w, foldY - (w - foldX) * (cosA / sinA))
                            lineTo(foldX - (h - foldY) * (sinA / cosA), h)
                            close()
                        }

                        drawPath(
                            path = shadowPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                start = Offset(foldX, foldY),
                                end = Offset(foldX - 60f, foldY - 60f)
                            )
                        )

                        // Curled Paper Flap Backside
                        val flapPath = Path().apply {
                            moveTo(foldX, foldY)
                            lineTo(foldX - 140f * cosA, foldY - 140f * sinA)
                            lineTo(w, h)
                            close()
                        }

                        drawPath(
                            path = flapPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFE5E7EB),
                                    Color(0xFF9CA3AF)
                                ),
                                start = Offset(foldX, foldY),
                                end = Offset(w, h)
                            )
                        )
                    }
                }
            }
        } else {
            // Locked State
            lockContent()
        }
    }
}
