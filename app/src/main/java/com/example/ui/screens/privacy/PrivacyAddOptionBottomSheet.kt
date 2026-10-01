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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAddOptionBottomSheet(
    onDismissRequest: () -> Unit,
    onOptionSelect: (PrivacyAddType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = F2WSurfaceElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF384055))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 6.dp)
                .testTag("privacy_add_options_sheet")
        ) {
            Text(
                text = "Add to Privacy Vault",
                color = F2WTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Select what you would like to secure in your private vault:",
                color = F2WTextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Option 1: Video
            PrivacyAddOptionItem(
                type = PrivacyAddType.VIDEO,
                onClick = {
                    onDismissRequest()
                    onOptionSelect(PrivacyAddType.VIDEO)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Audio
            PrivacyAddOptionItem(
                type = PrivacyAddType.AUDIO,
                onClick = {
                    onDismissRequest()
                    onOptionSelect(PrivacyAddType.AUDIO)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 3: Image
            PrivacyAddOptionItem(
                type = PrivacyAddType.IMAGE,
                onClick = {
                    onDismissRequest()
                    onOptionSelect(PrivacyAddType.IMAGE)
                }
            )
        }
    }
}

@Composable
private fun PrivacyAddOptionItem(
    type: PrivacyAddType,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1B202E))
            .border(1.dp, F2WCardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = type.accentColor),
                onClick = onClick
            )
            .padding(14.dp)
            .testTag("privacy_option_${type.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(type.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, type.accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = type.title,
                    tint = type.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (type) {
                        PrivacyAddType.VIDEO -> "Add private videos to secure zone"
                        PrivacyAddType.AUDIO -> "Add private music & audio files"
                        PrivacyAddType.IMAGE -> "Add private pictures & photos"
                    },
                    color = F2WTextSecondary,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = type.accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
