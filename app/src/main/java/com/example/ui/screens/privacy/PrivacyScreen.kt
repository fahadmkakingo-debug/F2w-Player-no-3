package com.example.ui.screens.privacy

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.security.PrivacySecurityManager
import com.example.ui.theme.F2WSurface

@Composable
fun PrivacyScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val securityManager = remember { PrivacySecurityManager.getInstance(context) }

    var isPinConfigured by remember { mutableStateOf(securityManager.isPinConfigured()) }
    var isVaultUnlocked by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("privacy_screen_container")
    ) {
        AnimatedContent(
            targetState = when {
                !isPinConfigured -> "SETUP"
                !isVaultUnlocked -> "LOCKED"
                else -> "UNLOCKED"
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "privacy_state_transition"
        ) { state ->
            when (state) {
                "SETUP" -> {
                    PrivacySetupScreen(
                        securityManager = securityManager,
                        onSetupComplete = {
                            isPinConfigured = true
                            isVaultUnlocked = true
                        }
                    )
                }

                "LOCKED" -> {
                    PrivacyLockScreen(
                        securityManager = securityManager,
                        onUnlockSuccess = {
                            isVaultUnlocked = true
                        }
                    )
                }

                "UNLOCKED" -> {
                    PrivacyVaultContentScreen(
                        securityManager = securityManager,
                        onLockVault = {
                            isVaultUnlocked = false
                        }
                    )
                }
            }
        }
    }
}
