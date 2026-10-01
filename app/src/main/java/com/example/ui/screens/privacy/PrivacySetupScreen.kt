package com.example.ui.screens.privacy

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.PrivacySecurityManager
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent

private enum class SetupStep {
    CREATE_PIN,
    CONFIRM_PIN,
    SECURITY_QUESTION,
    BIOMETRIC_OPTION
}

@Composable
fun PrivacySetupScreen(
    securityManager: PrivacySecurityManager,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentStep by remember { mutableStateOf(SetupStep.CREATE_PIN) }
    var createdPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Security question options
    val defaultQuestions = listOf(
        "What was the name of your first school or pet?",
        "What is your mother's maiden name?",
        "What is your favorite movie or book?",
        "What city were you born in?",
        "What was the model of your first phone?"
    )
    var selectedQuestion by remember { mutableStateOf(defaultQuestions[0]) }
    var isQuestionDropdownExpanded by remember { mutableStateOf(false) }
    var securityAnswerInput by remember { mutableStateOf("") }

    // Biometric detection
    val isBiometricSupported = remember { securityManager.isDeviceBiometricSupported(context) }
    var enableFingerprint by remember { mutableStateOf(isBiometricSupported) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Glowing Shield / Lock Header
        Box(
            modifier = Modifier
                .size(90.dp)
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
                imageVector = when (currentStep) {
                    SetupStep.CREATE_PIN, SetupStep.CONFIRM_PIN -> Icons.Filled.Lock
                    SetupStep.SECURITY_QUESTION -> Icons.Filled.HelpOutline
                    SetupStep.BIOMETRIC_OPTION -> Icons.Filled.Fingerprint
                },
                contentDescription = null,
                tint = F2WCyanPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "setup_step_animation"
        ) { step ->
            when (step) {
                SetupStep.CREATE_PIN -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Create Privacy PIN",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = F2WTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter a 4-digit master security code to lock your private files.",
                            color = F2WTextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        PrivacyPinDots(pinLength = createdPin.length, isError = pinError)

                        Spacer(modifier = Modifier.height(32.dp))

                        PrivacyKeypad(
                            onDigitClick = { digit ->
                                if (createdPin.length < 4) {
                                    createdPin += digit
                                    pinError = false
                                    if (createdPin.length == 4) {
                                        currentStep = SetupStep.CONFIRM_PIN
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (createdPin.isNotEmpty()) {
                                    createdPin = createdPin.dropLast(1)
                                }
                            }
                        )
                    }
                }

                SetupStep.CONFIRM_PIN -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Confirm Privacy PIN",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = F2WTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "Re-enter your 4-digit PIN to confirm.",
                            color = if (pinError) Color(0xFFEF4444) else F2WTextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        PrivacyPinDots(pinLength = confirmPin.length, isError = pinError)

                        Spacer(modifier = Modifier.height(32.dp))

                        PrivacyKeypad(
                            onDigitClick = { digit ->
                                if (confirmPin.length < 4) {
                                    confirmPin += digit
                                    if (confirmPin.length == 4) {
                                        if (confirmPin == createdPin) {
                                            pinError = false
                                            errorMessage = null
                                            currentStep = SetupStep.SECURITY_QUESTION
                                        } else {
                                            pinError = true
                                            errorMessage = "PINs do not match. Please try again."
                                            confirmPin = ""
                                        }
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (confirmPin.isNotEmpty()) {
                                    confirmPin = confirmPin.dropLast(1)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(
                            onClick = {
                                createdPin = ""
                                confirmPin = ""
                                pinError = false
                                errorMessage = null
                                currentStep = SetupStep.CREATE_PIN
                            }
                        ) {
                            Text("Start Over", color = F2WCyanPrimary)
                        }
                    }
                }

                SetupStep.SECURITY_QUESTION -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Security Recovery",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = F2WTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create a security question & answer to recover your PIN if forgotten. The answer will be encrypted securely.",
                            color = F2WTextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Question selector
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B202D))
                                .border(1.dp, F2WCardBorder, RoundedCornerShape(12.dp))
                                .clickable { isQuestionDropdownExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "SECURITY QUESTION",
                                    color = F2WCyanPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedQuestion,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }

                            DropdownMenu(
                                expanded = isQuestionDropdownExpanded,
                                onDismissRequest = { isQuestionDropdownExpanded = false }
                            ) {
                                defaultQuestions.forEach { question ->
                                    DropdownMenuItem(
                                        text = { Text(question) },
                                        onClick = {
                                            selectedQuestion = question
                                            isQuestionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Answer input field (never displayed anywhere after saving)
                        OutlinedTextField(
                            value = securityAnswerInput,
                            onValueChange = { securityAnswerInput = it },
                            label = { Text("Your Secret Answer") },
                            placeholder = { Text("Enter answer") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = F2WCyanPrimary,
                                unfocusedBorderColor = F2WCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = F2WCyanPrimary,
                                unfocusedLabelColor = F2WTextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_answer_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (securityAnswerInput.trim().length < 2) {
                                    Toast.makeText(context, "Please enter a valid answer", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (isBiometricSupported) {
                                        currentStep = SetupStep.BIOMETRIC_OPTION
                                    } else {
                                        // Save PIN, security, and finish
                                        securityManager.setupPinAndSecurity(
                                            pin = createdPin,
                                            question = selectedQuestion,
                                            answer = securityAnswerInput,
                                            fingerprintEnabled = false
                                        )
                                        Toast.makeText(context, "Privacy Vault Protected!", Toast.LENGTH_SHORT).show()
                                        onSetupComplete()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_security_question_btn")
                        ) {
                            Text(
                                text = if (isBiometricSupported) "Next: Biometrics" else "Finish Vault Protection",
                                color = Color(0xFF070B12),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                SetupStep.BIOMETRIC_OPTION -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add Fingerprint",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = F2WTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your phone supports fingerprint authentication. Enable it for quick one-touch unlock instead of entering your PIN.",
                            color = F2WTextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Toggle Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B202D))
                                .border(1.dp, F2WVioletAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = null,
                                        tint = F2WVioletAccent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Use Phone's Fingerprint",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Unlock vault using already enrolled fingerprint",
                                            color = F2WTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = enableFingerprint,
                                    onCheckedChange = { enableFingerprint = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = F2WVioletAccent,
                                        uncheckedThumbColor = F2WTextSecondary,
                                        uncheckedTrackColor = Color(0xFF282F40)
                                    ),
                                    modifier = Modifier.testTag("fingerprint_switch")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                securityManager.setupPinAndSecurity(
                                    pin = createdPin,
                                    question = selectedQuestion,
                                    answer = securityAnswerInput,
                                    fingerprintEnabled = enableFingerprint
                                )
                                Toast.makeText(context, "Privacy Vault Protected!", Toast.LENGTH_SHORT).show()
                                onSetupComplete()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("complete_vault_setup_btn")
                        ) {
                            Text(
                                text = "Complete Setup & Open Vault",
                                color = Color(0xFF070B12),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
