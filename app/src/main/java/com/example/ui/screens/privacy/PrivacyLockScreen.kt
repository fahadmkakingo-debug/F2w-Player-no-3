package com.example.ui.screens.privacy

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.PrivacySecurityManager
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WVioletAccent

@Composable
fun PrivacyLockScreen(
    securityManager: PrivacySecurityManager,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    val isFingerprintEnabled = remember { securityManager.isFingerprintEnabled() }
    val isBiometricSupported = remember { securityManager.isDeviceBiometricSupported(context) }

    val triggerBiometrics = {
        if (isFingerprintEnabled && isBiometricSupported) {
            securityManager.authenticateWithBiometrics(
                context = context,
                onSuccess = {
                    Toast.makeText(context, "Biometric verified!", Toast.LENGTH_SHORT).show()
                    onUnlockSuccess()
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    // User opted for PIN input
                }
            )
        }
    }

    // Attempt biometric prompt on initial entry if enabled
    LaunchedEffect(Unit) {
        if (isFingerprintEnabled && isBiometricSupported) {
            triggerBiometrics()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        val isExistingVaultDetected = remember { com.example.data.security.PrivacyVaultManager.getInstance(context).isExistingVaultDetected() }

        if (isExistingVaultDetected) {
            androidx.compose.material3.Surface(
                color = F2WCyanPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, F2WCyanPrimary),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Lock,
                        contentDescription = null,
                        tint = F2WCyanPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Existing F2W Private Vault found on device",
                        color = F2WCyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Glowing Vault Emblem
        Box(
            modifier = Modifier
                .size(94.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            F2WVioletAccent.copy(alpha = 0.35f),
                            F2WCyanPrimary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(F2WVioletAccent, F2WCyanPrimary)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = F2WCyanPrimary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Privacy Vault Locked",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = F2WTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isPinError) "Incorrect PIN. Try again." else "Enter your 4-digit PIN or use fingerprint.",
            color = if (isPinError) Color(0xFFEF4444) else F2WTextSecondary,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        PrivacyPinDots(
            pinLength = enteredPin.length,
            isError = isPinError,
            modifier = Modifier.testTag("pin_dots_display")
        )

        Spacer(modifier = Modifier.height(32.dp))

        PrivacyKeypad(
            onDigitClick = { digit ->
                if (enteredPin.length < 4) {
                    enteredPin += digit
                    isPinError = false
                    if (enteredPin.length == 4) {
                        if (securityManager.verifyPin(enteredPin)) {
                            onUnlockSuccess()
                        } else {
                            isPinError = true
                            enteredPin = ""
                        }
                    }
                }
            },
            onDeleteClick = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                    isPinError = false
                }
            },
            showFingerprint = isFingerprintEnabled && isBiometricSupported,
            onFingerprintClick = triggerBiometrics
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { showForgotPinDialog = true },
            modifier = Modifier.testTag("forgot_pin_btn")
        ) {
            Text(
                text = "Forgot PIN?",
                color = F2WCyanPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showForgotPinDialog) {
        PrivacyForgotPinDialog(
            securityManager = securityManager,
            onDismissRequest = { showForgotPinDialog = false },
            onPinResetSuccess = {
                showForgotPinDialog = false
                onUnlockSuccess()
            }
        )
    }
}
