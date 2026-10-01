package com.example.ui.screens.playlist

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.playlist.PlaylistManager
import com.example.data.playlist.PlaylistMediaType
import com.example.data.playlist.UserPlaylist
import com.example.ui.components.F2WEmptyState
import com.example.ui.screens.video.VideoItem
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onPlayVideoPlaylist: (List<VideoItem>, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlistManager = remember { PlaylistManager.getInstance(context) }

    // Two Top Tabs: Left = AUDIO, Right = VIDEO
    var selectedMediaTypeTab by remember { mutableStateOf(PlaylistMediaType.AUDIO) }

    var allPlaylists by remember { mutableStateOf(playlistManager.getAllPlaylists()) }
    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // For Audio Playlist playback dialog
    var activeAudioPlaylistToPlay by remember { mutableStateOf<Pair<UserPlaylist, Int>?>(null) }

    // Playlist rename & delete state
    var playlistToRename by remember { mutableStateOf<UserPlaylist?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<UserPlaylist?>(null) }

    LaunchedEffect(Unit) {
        playlistManager.playlistUpdates.collect {
            allPlaylists = playlistManager.getAllPlaylists()
        }
    }

    // Detail view if a playlist is selected
    if (activePlaylistId != null) {
        PlaylistDetailScreen(
            playlistId = activePlaylistId!!,
            onBack = { activePlaylistId = null },
            onPlayAll = { playlist, startIndex ->
                if (playlist.type == PlaylistMediaType.VIDEO) {
                    val videoItems = playlist.items.map { item ->
                        VideoItem(
                            id = item.id,
                            title = item.title,
                            durationText = item.durationText,
                            durationMs = item.durationMs,
                            playbackProgressMs = 0L,
                            sizeText = item.sizeText,
                            resolution = "1080P",
                            folderName = item.subtitle.ifBlank { "Playlist" },
                            uriString = item.uriString
                        )
                    }
                    onPlayVideoPlaylist(videoItems, startIndex)
                } else {
                    activeAudioPlaylistToPlay = Pair(playlist, startIndex)
                }
            }
        )

        // Audio Player dialog if audio playlist is being played
        activeAudioPlaylistToPlay?.let { (playlist, index) ->
            PlaylistAudioPlayerDialog(
                playlist = playlist,
                initialIndex = index,
                onClose = { activeAudioPlaylistToPlay = null }
            )
        }
        return
    }

    val currentTypePlaylists = remember(allPlaylists, selectedMediaTypeTab) {
        allPlaylists.filter { it.type == selectedMediaTypeTab }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("playlist_screen_container")
    ) {
        // Top 2 Tabs (Left/Right: Audio / Video)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(F2WSurfaceElevated)
                .border(1.dp, F2WCardBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left Tab: AUDIO
            val isAudioSelected = selectedMediaTypeTab == PlaylistMediaType.AUDIO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isAudioSelected) {
                            Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            F2WVioletAccent.copy(alpha = 0.35f),
                                            F2WVioletAccent.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(1.dp, F2WVioletAccent, RoundedCornerShape(12.dp))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable { selectedMediaTypeTab = PlaylistMediaType.AUDIO }
                    .padding(vertical = 10.dp)
                    .testTag("playlist_tab_audio"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = "Audio Playlists",
                        tint = if (isAudioSelected) F2WVioletAccent else F2WTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audio",
                        color = if (isAudioSelected) Color.White else F2WTextSecondary,
                        fontWeight = if (isAudioSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            // Right Tab: VIDEO
            val isVideoSelected = selectedMediaTypeTab == PlaylistMediaType.VIDEO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isVideoSelected) {
                            Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            F2WCyanPrimary.copy(alpha = 0.35f),
                                            F2WCyanPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(1.dp, F2WCyanPrimary, RoundedCornerShape(12.dp))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable { selectedMediaTypeTab = PlaylistMediaType.VIDEO }
                    .padding(vertical = 10.dp)
                    .testTag("playlist_tab_video"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Video Playlists",
                        tint = if (isVideoSelected) F2WCyanPrimary else F2WTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Video",
                        color = if (isVideoSelected) Color.White else F2WTextSecondary,
                        fontWeight = if (isVideoSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Subheader with Count & "Create New Playlist" Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) "Audio Playlists" else "Video Playlists",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = F2WTextPrimary
                    )
                )
                Text(
                    text = "${currentTypePlaylists.size} Collections Available",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = F2WTextTertiary
                    )
                )
            }

            // Create New Playlist Button for current tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) {
                            Brush.horizontalGradient(listOf(F2WVioletAccent.copy(alpha = 0.25f), Color(0xFF9333EA).copy(alpha = 0.25f)))
                        } else {
                            Brush.horizontalGradient(listOf(F2WCyanPrimary.copy(alpha = 0.25f), F2WVioletAccent.copy(alpha = 0.25f)))
                        }
                    )
                    .border(
                        1.dp,
                        if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) F2WVioletAccent else F2WCyanPrimary,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        newPlaylistName = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) "Nyimbo Zangu" else "Video Zangu"
                        showCreateDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("create_playlist_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create New Playlist",
                        tint = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) F2WVioletAccent else F2WCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Playlist Mpya",
                        color = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) F2WVioletAccent else F2WCyanPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Playlists List
        if (currentTypePlaylists.isEmpty()) {
            F2WEmptyState(
                icon = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) Icons.Filled.Audiotrack else Icons.Filled.Videocam,
                title = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) "Hakuna Playlist ya Audio" else "Hakuna Playlist ya Video",
                description = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO)
                    "Tengeneza playlist mpya ya audio ili kupanga nyimbo unazopenda na kuzicheza kwa kufuatana (oda)."
                else
                    "Tengeneza playlist mpya ya video ili kupanga vipande vya video na filamu zako uzicheze kwa kufuatana (oda).",
                actionLabel = "Tengeneza Playlist Mpya",
                actionIcon = Icons.Filled.Add,
                onActionClick = {
                    newPlaylistName = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) "Nyimbo Zangu" else "Video Zangu"
                    showCreateDialog = true
                },
                tipText = "Video na Audio huchezwa kwa mfuatano wa oda",
                testTag = "playlist_empty_state"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentTypePlaylists, key = { it.id }) { playlist ->
                    PlaylistCardRow(
                        playlist = playlist,
                        onClick = { activePlaylistId = playlist.id },
                        onPlayClick = {
                            if (playlist.items.isEmpty()) {
                                Toast.makeText(context, "Hii playlist haina faili. Ongeza video/audio kwanza!", Toast.LENGTH_SHORT).show()
                                activePlaylistId = playlist.id
                            } else {
                                if (playlist.type == PlaylistMediaType.VIDEO) {
                                    val videoItems = playlist.items.map { item ->
                                        VideoItem(
                                            id = item.id,
                                            title = item.title,
                                            durationText = item.durationText,
                                            durationMs = item.durationMs,
                                            playbackProgressMs = 0L,
                                            sizeText = item.sizeText,
                                            resolution = "1080P",
                                            folderName = item.subtitle.ifBlank { "Playlist" },
                                            uriString = item.uriString
                                        )
                                    }
                                    onPlayVideoPlaylist(videoItems, 0)
                                } else {
                                    activeAudioPlaylistToPlay = Pair(playlist, 0)
                                }
                            }
                        },
                        onRename = {
                            playlistToRename = playlist
                            renameInput = playlist.name
                        },
                        onDelete = {
                            playlistToDelete = playlist
                        }
                    )
                }
            }
        }
    }

    // Create New Playlist Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = F2WSurfaceElevated,
            title = {
                Text(
                    text = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) "Tengeneza Playlist ya Audio" else "Tengeneza Playlist ya Video",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Weka jina la playlist yako mpya ya ${if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) "audio" else "video"}:",
                        color = F2WTextSecondary,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = {
                            Text(
                                text = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) "k.m. Workout Hits, Bongo Flava" else "k.m. Filamu za Action, Safari Clips",
                                color = F2WTextTertiary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) F2WVioletAccent else F2WCyanPrimary,
                            unfocusedBorderColor = F2WCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_playlist_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            val created = playlistManager.createPlaylist(newPlaylistName.trim(), selectedMediaTypeTab)
                            allPlaylists = playlistManager.getAllPlaylists()
                            showCreateDialog = false
                            // Immediately open the newly created playlist so user can add media right away!
                            activePlaylistId = created.id
                            Toast.makeText(context, "Playlist ya \"${created.name}\" imetengenezwa!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMediaTypeTab == PlaylistMediaType.AUDIO) F2WVioletAccent else F2WCyanPrimary
                    ),
                    modifier = Modifier.testTag("confirm_create_playlist_btn")
                ) {
                    Text("Tengeneza", color = Color(0xFF070B12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Ghairi", color = F2WTextSecondary)
                }
            }
        )
    }

    // Rename Dialog
    playlistToRename?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = F2WSurfaceElevated,
            title = { Text("Badili Jina la Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = F2WCyanPrimary,
                        unfocusedBorderColor = F2WCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            playlistManager.renamePlaylist(pl.id, renameInput)
                            allPlaylists = playlistManager.getAllPlaylists()
                            playlistToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary)
                ) {
                    Text("Hifadhi", color = Color(0xFF070B12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Ghairi", color = F2WTextSecondary)
                }
            }
        )
    }

    // Delete Dialog
    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = F2WSurfaceElevated,
            title = { Text("Futa Playlist?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Je, una uhakika unataka kufuta playlist \"${pl.name}\"? Faili za video/audio hazitafutwa kwenye simu.",
                    color = F2WTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        playlistManager.deletePlaylist(pl.id)
                        allPlaylists = playlistManager.getAllPlaylists()
                        playlistToDelete = null
                        Toast.makeText(context, "Playlist imefutwa", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Futa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Ghairi", color = F2WTextSecondary)
                }
            }
        )
    }

    // Audio Player Dialog if opened from playlist card
    activeAudioPlaylistToPlay?.let { (playlist, index) ->
        PlaylistAudioPlayerDialog(
            playlist = playlist,
            initialIndex = index,
            onClose = { activeAudioPlaylistToPlay = null }
        )
    }
}

@Composable
private fun PlaylistCardRow(
    playlist: UserPlaylist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAudio = playlist.type == PlaylistMediaType.AUDIO
    var showDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(F2WSurfaceElevated)
            .border(1.dp, F2WCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("playlist_card_${playlist.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isAudio) {
                        Brush.linearGradient(
                            listOf(
                                F2WVioletAccent.copy(alpha = 0.35f),
                                Color(0xFF9333EA).copy(alpha = 0.2f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                F2WCyanPrimary.copy(alpha = 0.35f),
                                F2WVioletAccent.copy(alpha = 0.2f)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAudio) Icons.Filled.Audiotrack else Icons.Filled.Videocam,
                contentDescription = null,
                tint = if (isAudio) F2WVioletAccent else F2WCyanPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${playlist.items.size} ${if (isAudio) "nyimbo" else "video"}",
                    color = if (isAudio) F2WVioletAccent else F2WCyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " • ${playlist.totalDurationText}",
                    color = F2WTextTertiary,
                    fontSize = 11.5.sp
                )
            }
        }

        // Quick Play All button
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isAudio) F2WVioletAccent.copy(alpha = 0.15f)
                    else F2WCyanPrimary.copy(alpha = 0.15f)
                )
                .testTag("play_playlist_${playlist.id}")
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Cheza Zote",
                tint = if (isAudio) F2WVioletAccent else F2WCyanPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 3-dots Menu
        Box {
            IconButton(
                onClick = { showDropdown = true },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Chaguzi",
                    tint = F2WTextSecondary
                )
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                modifier = Modifier.background(F2WSurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Cheza Zote (Oda)", color = F2WTextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = F2WCyanPrimary)
                    },
                    onClick = {
                        showDropdown = false
                        onPlayClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Badili Jina", color = F2WTextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = F2WVioletAccent)
                    },
                    onClick = {
                        showDropdown = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Futa Playlist", color = Color(0xFFEF4444)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                    },
                    onClick = {
                        showDropdown = false
                        onDelete()
                    }
                )
            }
        }
    }
}
