package com.example.ui.screens.privacy

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.PrivacySecurityManager
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary

@Composable
fun PrivacyForgotPinDialog(
    securityManager: PrivacySecurityManager,
    onDismissRequest: () -> Unit,
    onPinResetSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isAnswerVerified by remember { mutableStateOf(false) }
    var answerInput by remember { mutableStateOf("") }
    var answerError by remember { mutableStateOf(false) }

    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var pinMismatchError by remember { mutableStateOf(false) }

    val question = remember { securityManager.getSecurityQuestion() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = F2WSurfaceElevated,
        icon = {
            Icon(
                imageVector = if (!isAnswerVerified) Icons.Filled.HelpOutline else Icons.Filled.LockReset,
                contentDescription = null,
                tint = F2WCyanPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (!isAnswerVerified) "Recover Privacy PIN" else "Set New 4-Digit PIN",
                color = F2WTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isAnswerVerified) {
                    Text(
                        text = "Answer your security recovery question to reset your Privacy PIN.",
                        color = F2WTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Question Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E2433))
                            .border(1.dp, F2WCardBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "QUESTION:",
                                color = F2WCyanPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = question,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = {
                            answerInput = it
                            answerError = false
                        },
                        label = { Text("Your Secret Answer") },
                        placeholder = { Text("Enter secret answer") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = answerError,
                        supportingText = if (answerError) {
                            { Text("Incorrect answer. Please try again.", color = Color(0xFFEF4444)) }
                        } else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = F2WCyanPrimary,
                            unfocusedBorderColor = F2WCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = F2WCyanPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_answer_input")
                    )
                } else {
                    Text(
                        text = "Your identity has been verified. Enter a new 4-digit PIN.",
                        color = F2WTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPin = it },
                        label = { Text("New 4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = F2WCyanPrimary,
                            unfocusedBorderColor = F2WCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = F2WCyanPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_pin_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmNewPin = it
                            pinMismatchError = false
                        },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinMismatchError,
                        supportingText = if (pinMismatchError) {
                            { Text("PINs do not match.", color = Color(0xFFEF4444)) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = F2WCyanPrimary,
                            unfocusedBorderColor = F2WCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = F2WCyanPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_new_pin_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isAnswerVerified) {
                        if (securityManager.verifySecurityAnswer(answerInput)) {
                            isAnswerVerified = true
                            answerError = false
                        } else {
                            answerError = true
                        }
                    } else {
                        if (newPin.length == 4 && newPin == confirmNewPin) {
                            securityManager.resetPin(newPin)
                            Toast.makeText(context, "PIN successfully reset!", Toast.LENGTH_SHORT).show()
                            onPinResetSuccess()
                        } else {
                            pinMismatchError = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("recovery_action_btn")
            ) {
                Text(
                    text = if (!isAnswerVerified) "Verify Answer" else "Save New PIN",
                    color = Color(0xFF070B12),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = F2WTextSecondary)
            }
        }
    )
}
