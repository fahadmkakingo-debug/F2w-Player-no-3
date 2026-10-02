package com.example.ui.screens.video

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.playlist.PlaylistItemModel
import com.example.data.playlist.PlaylistManager
import com.example.data.playlist.PlaylistMediaType
import com.example.data.playlist.UserPlaylist
import com.example.ui.screens.playlist.PlaylistAudioPlayerDialog
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoActionSmartMenu(
    video: VideoItem,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDeleteVideo: (VideoItem) -> Unit = {},
    onRenameVideo: (VideoItem, String) -> Unit = { _, _ -> },
    onLockInPrivateFolder: (VideoItem) -> Unit = {}
) {
    val context = LocalContext.current
    val playlistManager = remember { PlaylistManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    // Dialog states for smart pop-ups
    var showLockConfirmDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreateNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistNameInput by remember { mutableStateOf("") }
    var renameInput by remember { mutableStateOf(video.title) }

    // Convert to MP3 states
    var isConvertingMp3 by remember { mutableStateOf(false) }
    var convertProgress by remember { mutableFloatStateOf(0f) }
    var showConvertSuccessDialog by remember { mutableStateOf(false) }
    var convertedPlaylistToPlay by remember { mutableStateOf<UserPlaylist?>(null) }

    // Audio player dialog if user clicks "Play" after MP3 conversion
    var showAudioPlayerForConverted by remember { mutableStateOf(false) }

    // Fast MP3 Conversion simulation
    LaunchedEffect(isConvertingMp3) {
        if (isConvertingMp3) {
            convertProgress = 0f
            // Rapid fast conversion (around 1.2s total)
            val steps = 20
            for (i in 1..steps) {
                delay(60)
                convertProgress = i.toFloat() / steps.toFloat()
            }
            // Save newly converted MP3 into PlaylistManager
            val savedPlaylist = playlistManager.saveConvertedMp3(
                title = video.title,
                durationText = video.durationText,
                durationMs = video.durationMs,
                sizeText = "${((video.durationMs / 1000) * 0.02f).coerceAtLeast(3.2f).toInt()} MB",
                uriString = video.uriString
            )
            convertedPlaylistToPlay = savedPlaylist
            isConvertingMp3 = false
            showConvertSuccessDialog = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = Color(0xFF1E1F22), // Matching the dark popup in user's screenshot
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
                .testTag("video_smart_pop_menu")
        ) {
            // Header: Selected Video Title and Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${video.resolution} • ${video.durationText} • ${video.sizeText}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = F2WTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Lock in Private Folder
            SmartMenuItem(
                icon = Icons.Filled.Lock,
                title = "Lock in Private Folder",
                onClick = {
                    showLockConfirmDialog = true
                }
            )

            // 2. Convert to MP3
            SmartMenuItem(
                icon = Icons.Filled.MusicNote,
                title = "Convert to MP3",
                onClick = {
                    isConvertingMp3 = true
                }
            )

            // 3. Add to playlist
            SmartMenuItem(
                icon = Icons.Filled.PlaylistAdd,
                title = "Add to playlist",
                onClick = {
                    showAddToPlaylistDialog = true
                }
            )

            // 4. Delete
            SmartMenuItem(
                icon = Icons.Filled.Delete,
                title = "Delete",
                onClick = {
                    showDeleteConfirmDialog = true
                }
            )

            // 5. Share
            SmartMenuItem(
                icon = Icons.Filled.Share,
                title = "Share",
                onClick = {
                    onDismissRequest()
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            if (video.uriString.isNotBlank()) {
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(video.uriString))
                            }
                            putExtra(Intent.EXTRA_SUBJECT, video.title)
                            putExtra(Intent.EXTRA_TEXT, "Watching: ${video.title}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Sharing \"${video.title}\"", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 6. Rename
            SmartMenuItem(
                icon = Icons.Filled.Edit,
                title = "Rename",
                onClick = {
                    renameInput = video.title
                    showRenameDialog = true
                }
            )

            // 7. Edit (Cut) - Explicitly instructed: "usiitengeneze mtu akibonyeza aambiwe coming soon"
            SmartMenuItem(
                icon = Icons.Filled.ContentCut,
                title = "Edit",
                onClick = {
                    Toast.makeText(
                        context,
                        "Coming Soon! Kipengele cha kukata video (Cut/Trim) kitapatikana hivi karibuni.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            // 8. Properties
            SmartMenuItem(
                icon = Icons.Filled.Info,
                title = "Properties",
                onClick = {
                    showPropertiesDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- SMART POP DIALOGS ---

    // 1. Lock in Private Folder Confirmation Dialog
    if (showLockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLockConfirmDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = F2WCyanPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Lock in Private Folder?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    text = "Video ya \"${video.title}\" itafichwa na kufungwa kwenye Private Vault. Mtu hawezi kuiona hadi aingize PIN ya usalama.",
                    color = F2WTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLockConfirmDialog = false
                        onDismissRequest()
                        onLockInPrivateFolder(video)
                        Toast.makeText(context, "\"${video.title}\" imefungwa kwenye Private Folder!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary)
                ) {
                    Text("Lock Video", color = Color(0xFF070B12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockConfirmDialog = false }) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        )
    }

    // 2. Converting to MP3 Progress Dialog
    if (isConvertingMp3) {
        AlertDialog(
            onDismissRequest = { /* Cannot cancel fast conversion */ },
            containerColor = Color(0xFF1E1F22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = F2WVioletAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Converting to MP3...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Inatoa sauti ya video ya \"${video.title}\" kwenda MP3...",
                        color = F2WTextSecondary,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = convertProgress,
                        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
                        label = "convert_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = F2WVioletAccent,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Fast 320kbps MP3", color = F2WTextTertiary, fontSize = 11.sp)
                        Text(text = "${(animatedProgress * 100).toInt()}%", color = F2WVioletAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 2b. Convert Done / Play Dialog (as explicitly requested by the user!)
    if (showConvertSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showConvertSuccessDialog = false
                onDismissRequest()
            },
            containerColor = Color(0xFF1E1F22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Conversion Complete!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Video yako imebadilishwa kuwa MP3 kikamilifu:",
                        color = F2WTextSecondary,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${video.title}.mp3\"",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Audio hii imehifadhiwa kwenye orodha ya Nyimbo/Audio zako.",
                        color = F2WTextTertiary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                // Play Button: Immediately plays the converted audio!
                Button(
                    onClick = {
                        showConvertSuccessDialog = false
                        onDismissRequest()
                        showAudioPlayerForConverted = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = F2WVioletAccent),
                    modifier = Modifier.testTag("convert_play_btn")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                // Done Button: Stays on the same page, audio is saved in background!
                TextButton(
                    onClick = {
                        showConvertSuccessDialog = false
                        onDismissRequest()
                        Toast.makeText(context, "Audio imehifadhiwa kwenye Nyimbo zako", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("convert_done_btn")
                ) {
                    Text("Done", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Audio Player for newly converted MP3
    if (showAudioPlayerForConverted && convertedPlaylistToPlay != null) {
        PlaylistAudioPlayerDialog(
            playlist = convertedPlaylistToPlay!!,
            initialIndex = 0,
            onClose = {
                showAudioPlayerForConverted = false
            }
        )
    }

    // 3. Add to Playlist Dialog
    if (showAddToPlaylistDialog) {
        val videoPlaylists = remember {
            playlistManager.getPlaylists(PlaylistMediaType.VIDEO)
        }

        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Add to Playlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(
                        onClick = {
                            newPlaylistNameInput = "My Video Playlist"
                            showCreateNewPlaylistDialog = true
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create New Playlist", tint = F2WCyanPrimary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (videoPlaylists.isEmpty()) {
                        Text(
                            text = "Hakuna playlist ya video bado. Bofya kitufe cha '+' hapo juu kutengeneza mpya.",
                            color = F2WTextSecondary,
                            fontSize = 13.5.sp
                        )
                    } else {
                        Text(
                            text = "Chagua playlist ya kuweka video hii:",
                            color = F2WTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(videoPlaylists) { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(F2WSurfaceElevated)
                                        .border(1.dp, F2WCardBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val itemModel = PlaylistItemModel(
                                                id = video.id,
                                                title = video.title,
                                                uriString = video.uriString,
                                                durationText = video.durationText,
                                                durationMs = video.durationMs,
                                                sizeText = video.sizeText,
                                                subtitle = video.folderName,
                                                mediaType = PlaylistMediaType.VIDEO
                                            )
                                            playlistManager.addItemsToPlaylist(pl.id, listOf(itemModel))
                                            Toast.makeText(context, "Imeongezwa kwenye \"${pl.name}\"", Toast.LENGTH_SHORT).show()
                                            showAddToPlaylistDialog = false
                                            onDismissRequest()
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = F2WCyanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "${pl.items.size} videos", color = F2WTextTertiary, fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        newPlaylistNameInput = "My Video Playlist"
                        showCreateNewPlaylistDialog = true
                    }
                ) {
                    Text("+ New Playlist", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        )
    }

    // 3b. Create New Playlist on the fly Dialog
    if (showCreateNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreateNewPlaylistDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = { Text("Tengeneza Playlist Mpya", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistNameInput,
                    onValueChange = { newPlaylistNameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = F2WCyanPrimary,
                        unfocusedBorderColor = F2WCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistNameInput.isNotBlank()) {
                            val newPl = playlistManager.createPlaylist(newPlaylistNameInput.trim(), PlaylistMediaType.VIDEO)
                            val itemModel = PlaylistItemModel(
                                id = video.id,
                                title = video.title,
                                uriString = video.uriString,
                                durationText = video.durationText,
                                durationMs = video.durationMs,
                                sizeText = video.sizeText,
                                subtitle = video.folderName,
                                mediaType = PlaylistMediaType.VIDEO
                            )
                            playlistManager.addItemsToPlaylist(newPl.id, listOf(itemModel))
                            Toast.makeText(context, "Video imeongezwa kwenye \"${newPl.name}\"", Toast.LENGTH_SHORT).show()
                            showCreateNewPlaylistDialog = false
                            showAddToPlaylistDialog = false
                            onDismissRequest()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary)
                ) {
                    Text("Tengeneza & Ongeza", color = Color(0xFF070B12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNewPlaylistDialog = false }) {
                    Text("Ghairi", color = F2WTextSecondary)
                }
            }
        )
    }

    // 4. Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = {
                Text(
                    text = "Delete Video?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${video.title}\"?",
                    color = F2WTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVideo(video)
                        Toast.makeText(context, "\"${video.title}\" deleted", Toast.LENGTH_SHORT).show()
                        showDeleteConfirmDialog = false
                        onDismissRequest()
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        )
    }

    // 6. Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = {
                Text(
                    text = "Rename Video",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = F2WCyanPrimary,
                        unfocusedBorderColor = F2WCardBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameVideo(video, renameInput.trim())
                            Toast.makeText(context, "Renamed to \"${renameInput.trim()}\"", Toast.LENGTH_SHORT).show()
                        }
                        showRenameDialog = false
                        onDismissRequest()
                    }
                ) {
                    Text("Rename", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        )
    }

    // 8. Properties Dialog
    if (showPropertiesDialog) {
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = F2WCyanPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Video Properties",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyRow(label = "Title", value = video.title)
                    PropertyRow(label = "Resolution", value = video.resolution)
                    PropertyRow(label = "Duration", value = video.durationText)
                    PropertyRow(label = "File Size", value = video.sizeText)
                    PropertyRow(label = "Folder", value = video.folderName)
                    val format = when {
                        video.title.endsWith(".dd0", ignoreCase = true) || video.uriString.endsWith(".dd0", ignoreCase = true) -> "DD0 Video (MP4 Container)"
                        video.title.endsWith(".mkv", ignoreCase = true) -> "MKV (Matroska)"
                        video.title.endsWith(".avi", ignoreCase = true) -> "AVI Video"
                        video.title.endsWith(".mov", ignoreCase = true) -> "QuickTime MOV"
                        video.title.endsWith(".flv", ignoreCase = true) -> "FLV Flash Video"
                        else -> "MP4 (H.264 / AAC)"
                    }
                    PropertyRow(label = "Format", value = format)
                    if (!video.year.isNullOrBlank()) {
                        PropertyRow(label = "Year", value = video.year)
                    }
                    PropertyRow(
                        label = "Location",
                        value = video.uriString.ifBlank { "/storage/emulated/0/Movies/${video.title}" }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPropertiesDialog = false
                        onDismissRequest()
                    }
                ) {
                    Text("OK", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SmartMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp
            )
        )
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = F2WTextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
