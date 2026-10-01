package com.example.ui.screens.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WVioletAccent

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
                    KeypadNumberButton(
                        text = digit,
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
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C2230))
                        .border(1.dp, F2WVioletAccent.copy(alpha = 0.5f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = F2WVioletAccent),
                            onClick = onFingerprintClick
                        )
                        .testTag("pin_fingerprint_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Use Fingerprint",
                        tint = F2WVioletAccent,
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }

            KeypadNumberButton(
                text = "0",
                onClick = { onDigitClick("0") }
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C2230))
                    .border(1.dp, F2WCardBorder.copy(alpha = 0.4f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = F2WCyanPrimary),
                        onClick = onDeleteClick
                    )
                    .testTag("pin_delete_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Futa Nambari",
                    tint = F2WTextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun KeypadNumberButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF181D28))
            .border(1.dp, F2WCardBorder.copy(alpha = 0.5f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = F2WCyanPrimary),
                onClick = onClick
            )
            .testTag("pin_digit_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = F2WTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
