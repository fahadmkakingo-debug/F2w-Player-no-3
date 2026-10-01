package com.example.ui.screens.audio

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.playlist.PlaylistItemModel
import com.example.data.playlist.PlaylistManager
import com.example.ui.components.F2WEmptyState
import com.example.ui.components.permission.rememberMediaPermissionState
import com.example.ui.screens.playlist.PlaylistAudioPlayerDialog
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent
import com.example.util.permission.MediaPermissionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AudioScreen(
    onScanRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playlistManager = remember { PlaylistManager.getInstance(context) }

    var selectedCategory by remember { mutableStateOf("Tracks") }
    val categories = listOf("Tracks", "Albums", "Artists", "Folders", "Genres")

    var audioTracks by remember { mutableStateOf<List<PlaylistItemModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var activePlayingTrack by remember { mutableStateOf<PlaylistItemModel?>(null) }

    fun scanAudio() {
        if (isLoading) return
        coroutineScope.launch {
            isLoading = true
            val audios = withContext(Dispatchers.IO) {
                playlistManager.getAvailableAudios()
            }
            audioTracks = audios
            isLoading = false
        }
    }

    // Runtime Media Permissions Handler for Audio
    val mediaPermissionState = rememberMediaPermissionState(
        type = MediaPermissionType.AUDIO,
        onPermissionGranted = {
            scanAudio()
        }
    )

    LaunchedEffect(mediaPermissionState.hasAccess) {
        if (mediaPermissionState.hasAccess) {
            scanAudio()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("audio_screen_container")
    ) {
        // Audio Category Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                F2WCyanPrimary.copy(alpha = 0.2f),
                                                F2WVioletAccent.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(F2WCyanPrimary, F2WVioletAccent)
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            } else {
                                Modifier
                                    .background(F2WSurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = F2WCardBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            }
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("audio_chip_$category")
                ) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) F2WCyanPrimary else F2WTextSecondary
                    )
                }
            }
        }

        // Sub-header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${audioTracks.size} Tracks",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = F2WTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = " • Hi-Res Audio",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = F2WTextTertiary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = F2WCyanPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = {
                        if (mediaPermissionState.hasAccess) {
                            scanAudio()
                        } else {
                            mediaPermissionState.requestPermissions()
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("audio_sort_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Music",
                        tint = F2WTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (audioTracks.isNotEmpty()) {
                            audioTracks = audioTracks.shuffled()
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("audio_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle Music",
                        tint = F2WTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Content Area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (audioTracks.isEmpty() && !isLoading) {
                item {
                    F2WEmptyState(
                        icon = Icons.Outlined.Audiotrack,
                        title = if (mediaPermissionState.hasAccess) "Hakuna Audio Kwenye Simu" else "Ruhusa ya Audio Inahitajika",
                        description = if (mediaPermissionState.hasAccess)
                            "Hatujapata nyimbo au audio zozote kwenye internal storage ya simu yako. Hakikisha umehifadhi faili za audio na ubonyeze 'Scan Audio' kutafuta tena."
                        else
                            "App inahitaji ruhusa ya kusoma nyimbo na audio zilizo kwenye internal storage ya simu yako ili kuzionyesha na kuzicheza.",
                        actionLabel = if (mediaPermissionState.hasAccess) "Scan Audio Za Simu" else "Toa Ruhusa ya Audio",
                        actionIcon = Icons.Filled.Refresh,
                        onActionClick = {
                            if (mediaPermissionState.hasAccess) {
                                scanAudio()
                            } else {
                                mediaPermissionState.requestPermissions()
                            }
                            onScanRequest()
                        },
                        tipText = "Inasoma MP3, FLAC, WAV, AAC na format zote za audio",
                        testTag = "audio_empty_state"
                    )
                }
            } else {
                items(audioTracks, key = { it.id }) { track ->
                    AudioTrackItem(
                        track = track,
                        onPlayClick = { activePlayingTrack = track }
                    )
                }
            }
        }
    }

    // Audio Playback Dialog
    activePlayingTrack?.let { track ->
        val singlePlaylist = remember(track) {
            com.example.data.playlist.UserPlaylist(
                id = "audio_temp_${track.id}",
                name = "Audio Player",
                type = com.example.data.playlist.PlaylistMediaType.AUDIO,
                items = listOf(track)
            )
        }
        PlaylistAudioPlayerDialog(
            playlist = singlePlaylist,
            initialIndex = 0,
            onClose = { activePlayingTrack = null }
        )
    }
}

@Composable
private fun AudioTrackItem(
    track: PlaylistItemModel,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(F2WSurfaceElevated)
            .border(1.dp, F2WCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onPlayClick)
            .padding(12.dp)
            .testTag("audio_track_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                F2WCyanPrimary.copy(alpha = 0.2f),
                                F2WVioletAccent.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Music",
                    tint = F2WCyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = F2WTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.subtitle.ifBlank { "Unknown Artist" }} • ${track.durationText} • ${track.sizeText}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = F2WTextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Track",
                    tint = F2WCyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
