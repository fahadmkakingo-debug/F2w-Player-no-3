package com.example.ui.screens.playlist

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.ui.components.F2WEmptyState
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlayAll: (UserPlaylist, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlistManager = remember { PlaylistManager.getInstance(context) }
    var currentPlaylist by remember { mutableStateOf(playlistManager.getPlaylistById(playlistId)) }

    var showAddMediaSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(currentPlaylist?.name ?: "") }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(playlistId) {
        playlistManager.playlistUpdates.collect {
            currentPlaylist = playlistManager.getPlaylistById(playlistId)
        }
    }

    BackHandler(onBack = onBack)

    if (currentPlaylist == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val playlist = currentPlaylist!!
    val isAudio = playlist.type == PlaylistMediaType.AUDIO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("playlist_detail_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("playlist_detail_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isAudio) F2WVioletAccent.copy(alpha = 0.2f)
                                else F2WCyanPrimary.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAudio) "AUDIO PLAYLIST" else "VIDEO PLAYLIST",
                            color = if (isAudio) F2WVioletAccent else F2WCyanPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${playlist.items.size} items • ${playlist.totalDurationText}",
                        color = F2WTextTertiary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(F2WSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Playlist", color = F2WTextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = F2WCyanPrimary)
                        },
                        onClick = {
                            showMenu = false
                            renameText = playlist.name
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = Color(0xFFEF4444)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                        },
                        onClick = {
                            showMenu = false
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }

        // Action Banner: Big Play All Button & Add Media Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play All Button (Sequential Order)
            Button(
                onClick = {
                    if (playlist.items.isNotEmpty()) {
                        onPlayAll(playlist, 0)
                    } else {
                        Toast.makeText(context, "Ongeza video/audio kwanza kucheza", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAudio) F2WVioletAccent else F2WCyanPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("playlist_play_all_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = if (isAudio) Color.White else Color(0xFF070B12),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Play All (Oda)",
                    color = if (isAudio) Color.White else Color(0xFF070B12),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Add Media Button
            Button(
                onClick = { showAddMediaSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = F2WSurfaceElevated
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, F2WCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("playlist_add_media_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAudio) "+ Ongeza Audio" else "+ Ongeza Video",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Content: List of Items or Empty State
        if (playlist.items.isEmpty()) {
            F2WEmptyState(
                icon = if (isAudio) Icons.Filled.Audiotrack else Icons.Filled.Videocam,
                title = if (isAudio) "Playlist ya Audio Haina Nyimbo" else "Playlist Haina Video Bado",
                description = if (isAudio)
                    "Bofya kitufe cha chini kuongeza nyimbo unazopenda kwenye playlist hii."
                else
                    "Bofya kitufe cha chini kuongeza video unazopenda kwenye playlist hii.",
                actionLabel = if (isAudio) "Ongeza Audio" else "Ongeza Video",
                actionIcon = Icons.Filled.Add,
                onActionClick = { showAddMediaSheet = true },
                tipText = "Video/Audio zitachezwa kwa mfuatano wa oda",
                testTag = "empty_playlist_state"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlist.items, key = { _, item -> item.id }) { index, item ->
                    PlaylistItemRow(
                        index = index,
                        item = item,
                        isAudio = isAudio,
                        onClick = { onPlayAll(playlist, index) },
                        onRemove = {
                            playlistManager.removeItemFromPlaylist(playlist.id, item.id)
                            currentPlaylist = playlistManager.getPlaylistById(playlist.id)
                            Toast.makeText(context, "\"${item.title}\" imeondolewa", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = F2WSurfaceElevated,
            title = { Text("Rename Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
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
                        if (renameText.isNotBlank()) {
                            playlistManager.renamePlaylist(playlist.id, renameText)
                            currentPlaylist = playlistManager.getPlaylistById(playlist.id)
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary)
                ) {
                    Text("Save", color = Color(0xFF070B12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = F2WSurfaceElevated,
            title = { Text("Delete Playlist?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Je, una uhakika unataka kufuta playlist ya \"${playlist.name}\"? Faili halisi za simu hazitafutwa.",
                    color = F2WTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        playlistManager.deletePlaylist(playlist.id)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = F2WTextSecondary)
                }
            }
        )
    }

    // Add Media Modal Bottom Sheet
    if (showAddMediaSheet) {
        AddMediaToPlaylistSheet(
            playlist = playlist,
            onDismiss = { showAddMediaSheet = false },
            onAddConfirmed = { selectedItems ->
                playlistManager.addItemsToPlaylist(playlist.id, selectedItems)
                currentPlaylist = playlistManager.getPlaylistById(playlist.id)
                showAddMediaSheet = false
                Toast.makeText(context, "${selectedItems.size} zimeongezwa kwenye playlist", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun PlaylistItemRow(
    index: Int,
    item: PlaylistItemModel,
    isAudio: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(F2WSurfaceElevated)
            .border(1.dp, F2WCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("playlist_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number index
        Text(
            text = "${index + 1}",
            color = F2WTextTertiary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )

        // Media Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isAudio) F2WVioletAccent.copy(alpha = 0.18f)
                    else F2WCyanPrimary.copy(alpha = 0.18f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAudio) Icons.Filled.Audiotrack else Icons.Filled.Videocam,
                contentDescription = null,
                tint = if (isAudio) F2WVioletAccent else F2WCyanPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.durationText,
                    color = if (isAudio) F2WVioletAccent else F2WCyanPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = " • ${item.subtitle}",
                        color = F2WTextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Quick Play Icon
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Remove from Playlist button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp).testTag("remove_item_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Remove from Playlist",
                tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMediaToPlaylistSheet(
    playlist: UserPlaylist,
    onDismiss: () -> Unit,
    onAddConfirmed: (List<PlaylistItemModel>) -> Unit
) {
    val context = LocalContext.current
    val playlistManager = remember { PlaylistManager.getInstance(context) }
    val isAudio = playlist.type == PlaylistMediaType.AUDIO

    val allAvailable = remember {
        if (isAudio) playlistManager.getAvailableAudios()
        else playlistManager.getAvailableVideos()
    }

    val existingIds = remember(playlist) {
        playlist.items.map { it.id }.toSet()
    }

    var searchQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<PlaylistItemModel>() }

    val filteredList = remember(allAvailable, searchQuery) {
        if (searchQuery.isBlank()) allAvailable
        else allAvailable.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F131D),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAudio) "Ongeza Nyimbo kwenye Playlist" else "Ongeza Video kwenye Playlist",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Chagua faili za kuweka kwenye \"${playlist.name}\"",
                        color = F2WTextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Close",
                        tint = F2WTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search filter field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isAudio) "Tafuta wimbo au msanii..." else "Tafuta video...",
                        color = F2WTextTertiary,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = F2WCyanPrimary, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = F2WCyanPrimary,
                    unfocusedBorderColor = F2WCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Media list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredList) { _, item ->
                    val isAlreadyAdded = existingIds.contains(item.id)
                    val isSelected = selectedItems.any { it.id == item.id }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) F2WCyanPrimary.copy(alpha = 0.12f)
                                else F2WSurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) F2WCyanPrimary else F2WCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !isAlreadyAdded) {
                                if (isSelected) {
                                    selectedItems.removeAll { it.id == item.id }
                                } else {
                                    selectedItems.add(item)
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAlreadyAdded || isSelected,
                            onCheckedChange = { checked ->
                                if (!isAlreadyAdded) {
                                    if (checked) selectedItems.add(item)
                                    else selectedItems.removeAll { it.id == item.id }
                                }
                            },
                            enabled = !isAlreadyAdded,
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (isAlreadyAdded) Color.Gray else F2WCyanPrimary,
                                uncheckedColor = F2WTextTertiary
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = if (isAlreadyAdded) F2WTextTertiary else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.durationText} • ${item.sizeText} • ${item.subtitle}",
                                color = F2WTextTertiary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        if (isAlreadyAdded) {
                            Text(
                                text = "Ipo tayari",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Add Button
            Button(
                onClick = { onAddConfirmed(selectedItems.toList()) },
                enabled = selectedItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAudio) F2WVioletAccent else F2WCyanPrimary,
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_add_media_btn")
            ) {
                Text(
                    text = "Ongeza (${selectedItems.size}) Kwenye Playlist",
                    color = if (selectedItems.isNotEmpty()) Color(0xFF070B12) else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
