package com.example.ui.components.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent
import com.example.util.permission.MediaPermissionType

/**
 * An inline banner that appears at the top of media screens when storage access has not been granted.
 */
@Composable
fun MediaPermissionBanner(
    permissionState: MediaPermissionState,
    modifier: Modifier = Modifier,
    title: String = "Storage Access Required",
    description: String = "Grant access to scan and play video and audio files from your device storage."
) {
    AnimatedVisibility(
        visible = !permissionState.hasAccess || permissionState.isPartialAccess,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            F2WCyanPrimary.copy(alpha = 0.15f),
                            F2WVioletAccent.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            F2WCyanPrimary.copy(alpha = 0.5f),
                            F2WVioletAccent.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
                .testTag("media_permission_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(F2WCyanPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (permissionState.isPartialAccess) Icons.Filled.FolderOpen else Icons.Filled.PermMedia,
                        contentDescription = "Permission Icon",
                        tint = F2WCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (permissionState.isPartialAccess) "Partial Media Access" else title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = F2WTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (permissionState.isPartialAccess) {
                            "Only selected media is visible. Grant full access to show all device videos."
                        } else {
                            description
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = F2WTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                }

                Button(
                    onClick = {
                        if (permissionState.hasRequestedOnce && !permissionState.hasAccess) {
                            permissionState.openSettings()
                        } else {
                            permissionState.requestPermissions()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = F2WCyanPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("grant_permission_btn")
                ) {
                    Text(
                        text = if (permissionState.hasRequestedOnce && !permissionState.hasAccess) "Settings" else "Grant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * A comprehensive permission dialog with security details, permission explanation, and actions.
 */
@Composable
fun MediaPermissionRationaleDialog(
    permissionState: MediaPermissionState,
    onDismiss: () -> Unit = { permissionState.dismissRationale() }
) {
    if (!permissionState.showRationaleDialog) return

    val isVideo = permissionState.permissionType == MediaPermissionType.VIDEO
    val isAudio = permissionState.permissionType == MediaPermissionType.AUDIO

    val typeLabel = when {
        isVideo -> "Video Library"
        isAudio -> "Audio & Music Files"
        else -> "Device Media & Storage"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("media_permission_dialog"),
        containerColor = F2WSurfaceElevated,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(F2WCyanPrimary, F2WVioletAccent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Security & Privacy",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Storage Permission",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = F2WTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Local Media Discovery",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = F2WCyanPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "F2W Player requires permission to read media files stored on your device in order to scan, organize, and play your $typeLabel.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = F2WTextSecondary,
                        lineHeight = 20.sp
                    )
                )

                // Privacy guarantee box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(F2WSurface)
                        .border(1.dp, F2WCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Privacy Shield",
                            tint = F2WCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "100% Offline & Private: Your media never leaves your device or gets uploaded to any external server.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = F2WTextTertiary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (permissionState.hasRequestedOnce) {
                        permissionState.openSettings()
                    } else {
                        permissionState.requestPermissions()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = F2WCyanPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("dialog_grant_btn")
            ) {
                if (permissionState.hasRequestedOnce) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = "Open Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Settings", fontWeight = FontWeight.Bold)
                } else {
                    Text("Allow Access", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Not Now",
                    color = F2WTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

/**
 * Full card component used in empty states to request media permissions.
 */
@Composable
fun MediaPermissionCard(
    permissionState: MediaPermissionState,
    modifier: Modifier = Modifier,
    title: String = "Permission Required to Access Media",
    description: String = "To list your device videos and audio files, please allow media storage access."
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(F2WSurfaceElevated)
            .border(1.dp, F2WCardBorder, RoundedCornerShape(20.dp))
            .padding(24.dp)
            .testTag("media_permission_card"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                F2WCyanPrimary.copy(alpha = 0.2f),
                                F2WVioletAccent.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(F2WCyanPrimary, F2WVioletAccent)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = "Media Permission",
                    tint = F2WCyanPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = F2WTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (permissionState.hasRequestedOnce && !permissionState.hasAccess) {
                            permissionState.openSettings()
                        } else {
                            permissionState.requestPermissions()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = F2WCyanPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("card_grant_permission_btn")
                ) {
                    Text(
                        text = if (permissionState.hasRequestedOnce && !permissionState.hasAccess) "Open App Settings" else "Grant Storage Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
