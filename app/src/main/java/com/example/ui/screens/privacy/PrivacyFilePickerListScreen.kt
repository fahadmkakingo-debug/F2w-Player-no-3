package com.example.ui.screens.privacy

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.security.DeviceMediaFile
import com.example.data.security.PrivacySecurityManager
import com.example.data.security.PrivacyVaultManager
import com.example.ui.components.F2WEmptyState
import com.example.ui.components.permission.rememberMediaPermissionState
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.util.permission.MediaPermissionType
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyFilePickerListScreen(
    type: PrivacyAddType,
    onBack: () -> Unit,
    onFilesMovedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val vaultManager = remember { PrivacyVaultManager.getInstance(context) }

    var deviceFiles by remember { mutableStateOf<List<DeviceMediaFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedFiles = remember { mutableStateListOf<DeviceMediaFile>() }
    var isListView by remember { mutableStateOf(false) }

    var showConfirmMoveDialog by remember { mutableStateOf(false) }
    var isMoving by remember { mutableStateOf(false) }
    var moveProgressText by remember { mutableStateOf("") }

    val securityManager = remember { PrivacySecurityManager.getInstance(context) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }
    var inputPinForUnlock by remember { mutableStateOf("") }
    var pinUnlockError by remember { mutableStateOf(false) }

    val permissionType = when (type) {
        PrivacyAddType.VIDEO -> MediaPermissionType.VIDEO
        PrivacyAddType.AUDIO -> MediaPermissionType.AUDIO
        PrivacyAddType.IMAGE -> MediaPermissionType.IMAGES
    }

    val loadFiles = {
        coroutineScope.launch {
            isLoading = true
            deviceFiles = vaultManager.scanDeviceFiles(type.name)
            isLoading = false
        }
    }

    val mediaPermissionState = rememberMediaPermissionState(
        type = permissionType,
        onPermissionGranted = {
            loadFiles()
        }
    )

    LaunchedEffect(mediaPermissionState.hasAccess) {
        if (mediaPermissionState.hasAccess) {
            loadFiles()
        } else {
            isLoading = false
        }
    }

    val filteredFiles = remember(deviceFiles, searchQuery) {
        if (searchQuery.isBlank()) {
            deviceFiles
        } else {
            deviceFiles.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    val columnsCount = if (isListView) 1 else 2

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .statusBarsPadding()
            .testTag("privacy_picker_${type.name.lowercase()}_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("privacy_picker_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = "All ${type.title}",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedFiles.size} selected of ${filteredFiles.size} files",
                            color = if (selectedFiles.isNotEmpty()) type.accentColor else F2WTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // View Mode Toggle (List vs Grid)
                    IconButton(
                        onClick = { isListView = !isListView },
                        modifier = Modifier.testTag("privacy_picker_toggle_view_btn")
                    ) {
                        Icon(
                            imageVector = if (isListView) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (isListView) "Switch to Grid View" else "Switch to List View",
                            tint = type.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Select All
                    IconButton(
                        onClick = {
                            if (selectedFiles.size == filteredFiles.size) {
                                selectedFiles.clear()
                            } else {
                                selectedFiles.clear()
                                selectedFiles.addAll(filteredFiles)
                            }
                        },
                        modifier = Modifier.testTag("select_all_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SelectAll,
                            contentDescription = "Select All",
                            tint = if (selectedFiles.isNotEmpty()) type.accentColor else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Refresh Scan
                    IconButton(
                        onClick = { loadFiles() },
                        modifier = Modifier.testTag("refresh_files_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search ${type.title.lowercase()} files...",
                            color = F2WTextTertiary,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = F2WTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = F2WTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = type.accentColor,
                        unfocusedBorderColor = F2WCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 1.dp)

            // Content Area
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = type.accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Scanning device ${type.title.lowercase()} files...",
                            color = F2WTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    F2WEmptyState(
                        icon = type.icon,
                        title = "No ${type.title} Files Found",
                        description = "No local ${type.title.lowercase()} files were found on this device or all detected files are already in the vault.",
                        actionLabel = if (mediaPermissionState.hasAccess) "Refresh Scan" else "Grant Storage Permission",
                        actionIcon = Icons.Filled.Refresh,
                        onActionClick = {
                            if (mediaPermissionState.hasAccess) {
                                loadFiles()
                            } else {
                                mediaPermissionState.requestPermissions()
                            }
                        },
                        tipText = "Ensure media storage permission is granted",
                        testTag = "empty_${type.name.lowercase()}_state"
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredFiles,
                        key = { it.id }
                    ) { file ->
                        val isSelected = selectedFiles.any { it.id == file.id }

                        if (isListView) {
                            DeviceMediaFileListCard(
                                file = file,
                                type = type,
                                isSelected = isSelected,
                                accentColor = type.accentColor,
                                onClick = {
                                    if (isSelected) {
                                        selectedFiles.removeAll { it.id == file.id }
                                    } else {
                                        selectedFiles.add(file)
                                    }
                                }
                            )
                        } else {
                            DeviceMediaFileGridCard(
                                file = file,
                                type = type,
                                isSelected = isSelected,
                                accentColor = type.accentColor,
                                onClick = {
                                    if (isSelected) {
                                        selectedFiles.removeAll { it.id == file.id }
                                    } else {
                                        selectedFiles.add(file)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Action Bar: "Add to Privacy"
        if (selectedFiles.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Button(
                    onClick = { showConfirmMoveDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = type.accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("add_to_privacy_confirm_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF070B12),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add to Privacy (${selectedFiles.size} ${if (selectedFiles.size == 1) "File" else "Files"})",
                        color = Color(0xFF070B12),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    // Confirmation Dialog before moving
    if (showConfirmMoveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isMoving) showConfirmMoveDialog = false },
            containerColor = F2WSurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = type.accentColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Move to Privacy Vault?",
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "The ${selectedFiles.size} selected file(s) will be MOVED into the secure Privacy Vault.",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "After being moved into Privacy, the files will NO LONGER remain in their original location on device and will only be accessible inside Privacy.",
                        color = F2WTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    if (isMoving) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = type.accentColor,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(moveProgressText, color = type.accentColor, fontSize = 12.5.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isMoving) {
                            isMoving = true
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val result = vaultManager.moveDeviceFilesToVault(
                                    files = selectedFiles.toList(),
                                    mediaType = type.name,
                                    onProgress = { current, total ->
                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                            moveProgressText = "Moving file $current of $total..."
                                        }
                                    }
                                )
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    isMoving = false
                                    showConfirmMoveDialog = false

                                    if (result.successCount > 0) {
                                        Toast.makeText(
                                            context,
                                            "Successfully moved ${result.successCount} file(s) to Privacy!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onFilesMovedSuccess()
                                    } else if (result.failedCount > 0) {
                                        Toast.makeText(
                                            context,
                                            "Failed: ${result.errors.firstOrNull() ?: "Could not move files"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = type.accentColor),
                    enabled = !isMoving,
                    modifier = Modifier.testTag("confirm_move_dialog_btn")
                ) {
                    Text(
                        text = if (isMoving) "Moving..." else "Move to Privacy",
                        color = Color(0xFF070B12),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                if (!isMoving) {
                    TextButton(onClick = { showConfirmMoveDialog = false }) {
                        Text("Cancel", color = F2WTextSecondary)
                    }
                }
            }
        )
    }
}

/**
 * List Mode Card (Horizontal layout)
 */
@Composable
private fun DeviceMediaFileListCard(
    file: DeviceMediaFile,
    type: PrivacyAddType,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1B2433) else Color(0xFF161A24))
            .border(
                1.dp,
                if (isSelected) accentColor else F2WCardBorder.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(10.dp)
            .testTag("selectable_file_${file.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else Color(0xFF1E2330))
                    .border(
                        1.5.dp,
                        if (isSelected) accentColor else F2WCardBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF070B12),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10131A))
            ) {
                if (type == PrivacyAddType.AUDIO) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Audiotrack,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    val context = LocalContext.current
                    val modelObj = if (type == PrivacyAddType.VIDEO) {
                        com.example.util.media.VideoThumbnailHelper.buildThumbnailRequest(
                            context = context,
                            uriString = file.uri.toString()
                        )
                    } else {
                        if (file.path.isNotBlank() && File(file.path).exists()) File(file.path) else file.uri
                    }
                    AsyncImage(
                        model = modelObj,
                        contentDescription = file.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (type == PrivacyAddType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Meta Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.sizeFormatted,
                        color = F2WTextSecondary,
                        fontSize = 11.5.sp
                    )

                    if (file.durationMs > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = file.durationFormatted,
                            color = accentColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val ext = file.title.substringAfterLast(".", "").uppercase()
                    if (ext.isNotBlank() && ext.length <= 4) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF232838))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = ext,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grid Mode Card (2-column layout)
 */
@Composable
private fun DeviceMediaFileGridCard(
    file: DeviceMediaFile,
    type: PrivacyAddType,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1B2433) else Color(0xFF161A24))
            .border(
                1.5.dp,
                if (isSelected) accentColor else F2WCardBorder.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .testTag("selectable_file_${file.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Poster / Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color(0xFF10131A))
            ) {
                if (type == PrivacyAddType.AUDIO) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.2f),
                                        Color(0xFF0E111A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Audiotrack,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    val context = LocalContext.current
                    val modelObj = if (type == PrivacyAddType.VIDEO) {
                        com.example.util.media.VideoThumbnailHelper.buildThumbnailRequest(
                            context = context,
                            uriString = file.uri.toString()
                        )
                    } else {
                        if (file.path.isNotBlank() && File(file.path).exists()) File(file.path) else file.uri
                    }
                    AsyncImage(
                        model = modelObj,
                        contentDescription = file.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video Play Icon Overlay
                    if (type == PrivacyAddType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Selection Checkbox in Top-Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accentColor else Color.Black.copy(alpha = 0.6f))
                        .border(
                            1.5.dp,
                            if (isSelected) accentColor else Color.White.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF070B12),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Duration badge bottom left
                if (file.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.durationFormatted,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Title and Size
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = file.title,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.sizeFormatted,
                        color = F2WTextSecondary,
                        fontSize = 11.sp
                    )

                    val ext = file.title.substringAfterLast(".", "").uppercase()
                    if (ext.isNotBlank() && ext.length <= 4) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF222736))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = ext,
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
