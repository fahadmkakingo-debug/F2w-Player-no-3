package com.example.ui.screens.audio

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.AudioPlaybackManager
import com.example.data.playlist.PlaylistItemModel
import com.example.data.playlist.PlaylistManager
import com.example.ui.components.F2WEmptyState
import com.example.ui.components.permission.rememberMediaPermissionState
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary
import com.example.ui.theme.F2WVioletAccent
import com.example.util.media.AudioCoverHelper
import com.example.util.permission.MediaPermissionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AudioTab(val label: String) {
    SONGS("SONGS"),
    FOLDERS("FOLDERS"),
    ALBUMS("ALBUMS"),
    ARTISTS("ARTISTS")
}

@Composable
fun AudioScreen(
    onScanRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playlistManager = remember { PlaylistManager.getInstance(context) }
    val audioManager = remember { AudioPlaybackManager.getInstance(context) }

    val currentTrack by audioManager.currentTrack.collectAsState()
    val isAudioPlaying by audioManager.isPlaying.collectAsState()

    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* Permission response handled */ }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var selectedTab by remember { mutableStateOf(AudioTab.SONGS) }
    var audioTracks by remember { mutableStateOf<List<PlaylistItemModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Selected Detail Group (Folder, Album, or Artist)
    var selectedGroupTitle by remember { mutableStateOf<String?>(null) }
    var selectedGroupType by remember { mutableStateOf<AudioTab?>(null) }

    // Top 3-dots Menu state
    var showTopMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("Name") } // Name, Date, Size, Duration

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

    // Sorted and Filtered tracks
    val sortedTracks = remember(audioTracks, sortBy) {
        when (sortBy) {
            "Date" -> audioTracks.sortedByDescending { it.dateAdded }
            "Duration" -> audioTracks.sortedByDescending { it.durationMs }
            "Size" -> audioTracks.sortedByDescending { it.sizeText }
            else -> audioTracks.sortedBy { it.title.lowercase() }
        }
    }

    val filteredTracks = remember(sortedTracks, searchQuery) {
        if (searchQuery.isBlank()) sortedTracks
        else sortedTracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true) ||
            it.folder.contains(searchQuery, ignoreCase = true)
        }
    }

    // Back handler for Detail View or Search
    BackHandler(enabled = selectedGroupTitle != null || isSearchActive) {
        if (selectedGroupTitle != null) {
            selectedGroupTitle = null
            selectedGroupType = null
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("audio_screen_container")
    ) {
        // 1. Top Bar: Music Title or Search Input + Icons (matches user screenshot)
        if (isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isSearchActive = false
                        searchQuery = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Rudi",
                        tint = Color.White
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tafuta nyimbo, msanii, au albamu...", color = F2WTextSecondary, fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = F2WCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Futa", tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("audio_search_input")
                )
            }
        } else if (selectedGroupTitle == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Music",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier.testTag("audio_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Tafuta Nyimbo",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.testTag("audio_more_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort By (Panga kwa)") },
                                leadingIcon = { Icon(Icons.Filled.Sort, contentDescription = null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    showTopMenu = false
                                    showSortDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Shuffle All (Cheza Zote)") },
                                leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    showTopMenu = false
                                    if (filteredTracks.isNotEmpty()) {
                                        audioManager.toggleShuffle()
                                        audioManager.playPlaylist(filteredTracks.shuffled(), 0)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan / Refresh (Tafuta Tena)") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    showTopMenu = false
                                    scanAudio()
                                    onScanRequest()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Main 4 Tabs: SONGS, FOLDERS, ALBUMS, ARTISTS (Matches user screenshot)
        if (selectedGroupTitle == null) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = Color.White,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            ) {
                AudioTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.White else F2WTextSecondary,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("audio_tab_${tab.name}")
                    )
                }
            }
        }

        // 3. Tab Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            if (filteredTracks.isEmpty() && !isLoading && !mediaPermissionState.hasAccess) {
                F2WEmptyState(
                    icon = Icons.Outlined.Audiotrack,
                    title = "Ruhusa ya Audio Inahitajika",
                    description = "App inahitaji ruhusa ya kusoma nyimbo na audio zilizo kwenye internal storage ya simu yako ili kuzionyesha na kuzicheza.",
                    actionLabel = "Toa Ruhusa ya Audio",
                    actionIcon = Icons.Filled.Refresh,
                    onActionClick = {
                        mediaPermissionState.requestPermissions()
                        onScanRequest()
                    },
                    tipText = "Inasoma MP3, FLAC, WAV, AAC na format zote za audio",
                    testTag = "audio_permission_empty_state"
                )
            } else if (selectedGroupTitle != null) {
                // Detail Sub-Screen (Folder / Album / Artist)
                val groupTracks = remember(selectedGroupTitle, selectedGroupType, sortedTracks) {
                    when (selectedGroupType) {
                        AudioTab.FOLDERS -> sortedTracks.filter { it.folder.equals(selectedGroupTitle, ignoreCase = true) }
                        AudioTab.ALBUMS -> sortedTracks.filter { it.album.equals(selectedGroupTitle, ignoreCase = true) }
                        AudioTab.ARTISTS -> sortedTracks.filter { it.subtitle.equals(selectedGroupTitle, ignoreCase = true) || it.artist.equals(selectedGroupTitle, ignoreCase = true) }
                        else -> sortedTracks
                    }
                }

                GroupDetailView(
                    title = selectedGroupTitle!!,
                    groupType = selectedGroupType ?: AudioTab.FOLDERS,
                    tracks = groupTracks,
                    currentTrackId = currentTrack?.id,
                    isPlaying = isAudioPlaying,
                    onBack = {
                        selectedGroupTitle = null
                        selectedGroupType = null
                    },
                    onPlayTrack = { index ->
                        audioManager.playPlaylist(groupTracks, index)
                    },
                    onPlayAll = {
                        if (groupTracks.isNotEmpty()) audioManager.playPlaylist(groupTracks, 0)
                    },
                    onShuffleAll = {
                        if (groupTracks.isNotEmpty()) {
                            audioManager.toggleShuffle()
                            audioManager.playPlaylist(groupTracks.shuffled(), 0)
                        }
                    }
                )
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "audio_tab_content_transition"
                ) { currentTab ->
                    when (currentTab) {
                        AudioTab.SONGS -> SongsListView(
                            tracks = filteredTracks,
                            currentTrackId = currentTrack?.id,
                            isPlaying = isAudioPlaying,
                            onPlayTrack = { index ->
                                audioManager.playPlaylist(filteredTracks, index)
                            },
                            onShuffleAll = {
                                if (filteredTracks.isNotEmpty()) {
                                    audioManager.toggleShuffle()
                                    audioManager.playPlaylist(filteredTracks.shuffled(), 0)
                                }
                            }
                        )

                        AudioTab.FOLDERS -> FoldersListView(
                            tracks = filteredTracks,
                            onFolderClick = { folderName ->
                                selectedGroupTitle = folderName
                                selectedGroupType = AudioTab.FOLDERS
                            }
                        )

                        AudioTab.ALBUMS -> AlbumsGridView(
                            tracks = filteredTracks,
                            onAlbumClick = { albumName ->
                                selectedGroupTitle = albumName
                                selectedGroupType = AudioTab.ALBUMS
                            }
                        )

                        AudioTab.ARTISTS -> ArtistsListView(
                            tracks = filteredTracks,
                            onArtistClick = { artistName ->
                                selectedGroupTitle = artistName
                                selectedGroupType = AudioTab.ARTISTS
                            }
                        )
                    }
                }
            }
        }
    }

    // Sort By Dialog
    if (showSortDialog) {
        val sortOptions = listOf("Name (Jina)", "Date (Tarehe)", "Duration (Urefu wa Wimbo)", "Size (Ukubwa wa Faili)")
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            containerColor = Color(0xFF1C212D),
            title = {
                Text("Sort Music By", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column {
                    sortOptions.forEach { opt ->
                        val key = opt.substringBefore(" ")
                        val isSelected = sortBy == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x3310B981) else Color.Transparent)
                                .clickable {
                                    sortBy = key
                                    showSortDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) Color(0xFF10B981) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Close", color = F2WCyanPrimary)
                }
            }
        )
    }
}

// ==================== 1. SONGS TAB VIEW ====================
@Composable
private fun SongsListView(
    tracks: List<PlaylistItemModel>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Int) -> Unit,
    onShuffleAll: () -> Unit
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Hakuna nyimbo zilizopatikana", color = F2WTextSecondary, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Quick Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tracks.size} Songs",
                    color = F2WTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2433))
                        .clickable(onClick = onShuffleAll)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
            val isCurrent = currentTrackId == track.id
            SongListItemRow(
                index = index + 1,
                track = track,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                onPlayClick = { onPlayTrack(index) }
            )
        }
    }
}

// ==================== 2. FOLDERS TAB VIEW ====================
@Composable
private fun FoldersListView(
    tracks: List<PlaylistItemModel>,
    onFolderClick: (String) -> Unit
) {
    val foldersMap = remember(tracks) {
        tracks.groupBy { it.folder.ifBlank { "Music" } }
    }

    if (foldersMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hakuna folda zenye nyimbo zilizopatikana", color = F2WTextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(foldersMap.keys.toList()) { folderName ->
            val folderTracks = foldersMap[folderName] ?: emptyList()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(F2WSurfaceElevated)
                    .border(1.dp, F2WCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onFolderClick(folderName) }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF232B3C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folderName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${folderTracks.size} Songs",
                            color = F2WTextSecondary,
                            fontSize = 12.5.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Fungua Folder",
                        tint = F2WTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==================== 3. ALBUMS TAB VIEW ====================
@Composable
private fun AlbumsGridView(
    tracks: List<PlaylistItemModel>,
    onAlbumClick: (String) -> Unit
) {
    val albumsMap = remember(tracks) {
        tracks.groupBy { it.album.ifBlank { "Unknown Album" } }
    }

    if (albumsMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hakuna Albamu zilizopatikana", color = F2WTextSecondary)
        }
        return
    }

    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(albumsMap.keys.toList()) { albumName ->
            val albumTracks = albumsMap[albumName] ?: emptyList()
            val firstTrack = albumTracks.firstOrNull()

            var coverBmp by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(firstTrack?.id) {
                if (firstTrack != null) {
                    coverBmp = AudioCoverHelper.getAudioCoverBitmap(context, firstTrack.uriString)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(F2WSurfaceElevated)
                    .border(1.dp, F2WCardBorder, RoundedCornerShape(14.dp))
                    .clickable { onAlbumClick(albumName) }
                    .padding(8.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF131720)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverBmp != null) {
                            Image(
                                bitmap = coverBmp!!.asImageBitmap(),
                                contentDescription = albumName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                F2WCyanPrimary.copy(alpha = 0.25f),
                                                F2WVioletAccent.copy(alpha = 0.35f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = F2WCyanPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = albumName,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${albumTracks.size} Songs • ${firstTrack?.subtitle ?: "Artist"}",
                        color = F2WTextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================== 4. ARTISTS TAB VIEW ====================
@Composable
private fun ArtistsListView(
    tracks: List<PlaylistItemModel>,
    onArtistClick: (String) -> Unit
) {
    val artistsMap = remember(tracks) {
        tracks.groupBy { it.subtitle.ifBlank { it.artist.ifBlank { "Unknown Artist" } } }
    }

    if (artistsMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hakuna wasanii waliopatikana", color = F2WTextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(artistsMap.keys.toList()) { artistName ->
            val artistTracks = artistsMap[artistName] ?: emptyList()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(F2WSurfaceElevated)
                    .border(1.dp, F2WCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onArtistClick(artistName) }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF10B981).copy(alpha = 0.3f),
                                        F2WVioletAccent.copy(alpha = 0.3f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artistName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${artistTracks.size} Songs",
                            color = F2WTextSecondary,
                            fontSize = 12.5.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = F2WTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==================== DETAIL GROUP VIEW (Folder / Album / Artist) ====================
@Composable
private fun GroupDetailView(
    title: String,
    groupType: AudioTab,
    tracks: List<PlaylistItemModel>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Group Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Rudi", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${groupType.label.lowercase().replaceFirstChar { it.uppercase() }} • ${tracks.size} Songs",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp
                )
            }
        }

        // Action Buttons: Play All & Shuffle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10B981))
                    .clickable(onClick = onPlayAll)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF070B12), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", color = Color(0xFF070B12), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF222838))
                    .border(1.dp, F2WCardBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onShuffleAll)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }

        // List of tracks
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
                val isCurrent = currentTrackId == track.id
                SongListItemRow(
                    index = index + 1,
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onPlayClick = { onPlayTrack(index) }
                )
            }
        }
    }
}

// ==================== REUSABLE SONG ROW ====================
@Composable
private fun SongListItemRow(
    index: Int,
    track: PlaylistItemModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        coverBitmap = AudioCoverHelper.getAudioCoverBitmap(context, track.uriString)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0xFF1E2638) else F2WSurfaceElevated)
            .border(
                1.dp,
                if (isCurrent) Color(0xFF10B981).copy(alpha = 0.8f) else F2WCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onPlayClick)
            .padding(10.dp)
            .testTag("song_item_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Album Cover
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131720)),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                            contentDescription = null,
                            tint = F2WCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$index. ${track.title}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (isCurrent) Color(0xFF10B981) else Color.White,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.subtitle.ifBlank { "Unknown Artist" }} • ${track.durationText} • ${track.sizeText}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = F2WTextSecondary,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Playing Equalizer Indicator
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = "Playing",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = F2WTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = {
                            showMenu = false
                            onPlayClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, "Sharing: ${track.title}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
