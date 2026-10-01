package com.example.ui.screens.privacy

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.PrivacyVaultManager
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WVioletAccent
import kotlinx.coroutines.launch

enum class PrivacyAddType(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    VIDEO(
        title = "Video",
        subtitle = "Private Video Safe Zone",
        icon = Icons.Filled.Videocam
    ),
    AUDIO(
        title = "Audio",
        subtitle = "Private Audio Safe Zone",
        icon = Icons.Filled.Audiotrack
    ),
    IMAGE(
        title = "Image",
        subtitle = "Private Image Safe Zone",
        icon = Icons.Filled.Image
    );

    val accentColor: Color
        @Composable
        get() = when (this) {
            VIDEO -> F2WCyanPrimary
            AUDIO -> F2WVioletAccent
            IMAGE -> Color(0xFF10B981)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAddSubScreen(
    type: PrivacyAddType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val vaultManager = remember { PrivacyVaultManager.getInstance(context) }

    var showAllDeviceFilesPicker by remember { mutableStateOf(false) }

    // SAF Local Storage Picker State
    var pendingSelectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showLocalStorageConfirmDialog by remember { mutableStateOf(false) }
    var isMovingLocalStorageFiles by remember { mutableStateOf(false) }

    val localStoragePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            pendingSelectedUris = uris
            showLocalStorageConfirmDialog = true
        }
    }

    if (showAllDeviceFilesPicker) {
        PrivacyFilePickerListScreen(
            type = type,
            onBack = { showAllDeviceFilesPicker = false },
            onFilesMovedSuccess = {
                showAllDeviceFilesPicker = false
                onBack()
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .statusBarsPadding()
            .testTag("privacy_add_${type.name.lowercase()}_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("privacy_subscreen_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Rudi Nyuma",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column {
                Text(
                    text = type.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = type.subtitle,
                    color = type.accentColor,
                    fontSize = 11.5.sp
                )
            }
        }

        HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Category Header Banner
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    type.accentColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, type.accentColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = type.icon,
                        contentDescription = type.title,
                        tint = type.accentColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Import ${type.title}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = F2WTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select a source below to move ${type.title.lowercase()} files into Privacy. Files will be removed from original storage and kept only in your private vault.",
                    color = F2WTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // EXACTLY TWO OPTIONS:
                // Option 1: All Video / All Audio / All Image
                PrivacyImportOptionCard(
                    title = "All ${type.title}",
                    subtitle = "Scan and select ${type.title.lowercase()} files detected on this device",
                    icon = when (type) {
                        PrivacyAddType.VIDEO -> Icons.Filled.VideoLibrary
                        PrivacyAddType.AUDIO -> Icons.Filled.Audiotrack
                        PrivacyAddType.IMAGE -> Icons.Filled.Image
                    },
                    accentColor = type.accentColor,
                    badgeText = "Auto Detect",
                    testTag = "option_all_${type.name.lowercase()}",
                    onClick = { showAllDeviceFilesPicker = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option 2: Local Storage
                PrivacyImportOptionCard(
                    title = "Local Storage",
                    subtitle = "Browse device folders and manually select ${type.title.lowercase()} files",
                    icon = Icons.Filled.Folder,
                    accentColor = F2WCyanPrimary,
                    badgeText = "File Browser",
                    testTag = "option_local_storage_${type.name.lowercase()}",
                    onClick = {
                        val mimeTypes = when (type) {
                            PrivacyAddType.VIDEO -> arrayOf("video/*")
                            PrivacyAddType.AUDIO -> arrayOf("audio/*")
                            PrivacyAddType.IMAGE -> arrayOf("image/*")
                        }
                        localStoragePickerLauncher.launch(mimeTypes)
                    }
                )
            }
        }
    }

    // Confirmation Dialog for Local Storage import
    if (showLocalStorageConfirmDialog && pendingSelectedUris.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { if (!isMovingLocalStorageFiles) showLocalStorageConfirmDialog = false },
            containerColor = F2WSurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = type.accentColor,
                    modifier = Modifier.size(30.dp)
                )
            },
            title = {
                Text(
                    text = "Move Selected Files to Privacy?",
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "${pendingSelectedUris.size} file(s) selected from Local Storage will be moved into Privacy ${type.title}.",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Once moved, the files will be removed from their original location and stored exclusively inside the encrypted vault.",
                        color = F2WTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    if (isMovingLocalStorageFiles) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = type.accentColor,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Moving files securely...", color = type.accentColor, fontSize = 12.5.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isMovingLocalStorageFiles) {
                            isMovingLocalStorageFiles = true
                            coroutineScope.launch {
                                val result = vaultManager.moveLocalUrisToVault(
                                    uris = pendingSelectedUris,
                                    mediaType = type.name
                                )
                                isMovingLocalStorageFiles = false
                                showLocalStorageConfirmDialog = false
                                pendingSelectedUris = emptyList()

                                if (result.successCount > 0) {
                                    Toast.makeText(
                                        context,
                                        "Moved ${result.successCount} file(s) to Privacy!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onBack()
                                } else if (result.failedCount > 0) {
                                    Toast.makeText(
                                        context,
                                        "Failed: ${result.errors.firstOrNull() ?: "Could not move files"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = type.accentColor),
                    enabled = !isMovingLocalStorageFiles,
                    modifier = Modifier.testTag("confirm_local_storage_move_btn")
                ) {
                    Text(
                        text = if (isMovingLocalStorageFiles) "Moving..." else "Move to Privacy",
                        color = Color(0xFF070B12),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                if (!isMovingLocalStorageFiles) {
                    TextButton(onClick = { showLocalStorageConfirmDialog = false }) {
                        Text("Cancel", color = F2WTextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun PrivacyImportOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161A26))
            .border(1.dp, F2WCardBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = accentColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    color = F2WTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
