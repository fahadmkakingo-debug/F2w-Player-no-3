package com.example.ui.screens.video

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.data.media.SearchHistoryManager
import com.example.ui.components.F2WEmptyState
import com.example.ui.theme.F2WTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSearchOverlayScreen(
    allVideos: List<VideoItem>,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyManager = remember { SearchHistoryManager.getInstance(context) }
    val historyList by historyManager.historyList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeVideoForMenu by remember { mutableStateOf<VideoItem?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle back button press
    BackHandler {
        onBack()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Dynamic colors from current Theme!
    val themeColors = F2WTheme.colors
    val backgroundColor = themeColors.background
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val textTertiary = themeColors.textTertiary
    val primaryColor = themeColors.primary
    val cardBorder = themeColors.cardBorder

    // Filter videos by exact substring matching on title or folder name
    val filteredVideos = remember(searchQuery, allVideos) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            allVideos.filter { video ->
                video.title.contains(trimmed, ignoreCase = true) ||
                        video.folderName.contains(trimmed, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("video_search_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Search Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Arrow Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("search_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Rudi",
                        tint = textPrimary
                    )
                }

                // Search Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search Video",
                            color = textTertiary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    historyManager.addSearchQuery(searchQuery)
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("search_text_input")
                    )
                }

                // Clear / Close Button
                IconButton(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("search_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Futa / Funga",
                        tint = textSecondary
                    )
                }
            }

            HorizontalDivider(
                color = cardBorder.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            // 2. Content Area: History (if query empty) or Search Results (if query not empty)
            if (searchQuery.isBlank()) {
                // History List Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "History",
                        color = textTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (historyList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hakuna historia ya search",
                                color = textTertiary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = historyList,
                                key = { it }
                            ) { historyItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = historyItem
                                            historyManager.addSearchQuery(historyItem)
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = textTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = historyItem,
                                            color = textPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            historyManager.removeHistoryItem(historyItem)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Odoa historia",
                                            tint = textTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Search Results Mode
                if (filteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        F2WEmptyState(
                            icon = Icons.Outlined.VideoLibrary,
                            title = "Hakuna Video Iliyopatikana",
                            description = "Hakuna video inayolingana na \"$searchQuery\". Tafadhali jaribu kuandika maneno mengine.",
                            actionLabel = "Futa Search",
                            actionIcon = Icons.Default.Close,
                            onActionClick = { searchQuery = "" },
                            tipText = "Tafuta kwa sehemu tu ya jina la video au folder"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filteredVideos,
                            key = { it.id }
                        ) { video ->
                            SearchResultItemCard(
                                video = video,
                                searchQuery = searchQuery,
                                onClick = {
                                    historyManager.addSearchQuery(searchQuery)
                                    onVideoClick(video)
                                },
                                onMoreOptionsClick = {
                                    activeVideoForMenu = video
                                }
                            )
                        }
                    }
                }
            }
        }

        // Smart Pop Menu with Options matching app style
        activeVideoForMenu?.let { video ->
            VideoActionSmartMenu(
                video = video,
                onDismissRequest = { activeVideoForMenu = null },
                onDeleteVideo = { activeVideoForMenu = null },
                onRenameVideo = { _, _ -> activeVideoForMenu = null },
                onLockInPrivateFolder = { activeVideoForMenu = null }
            )
        }
    }
}

@Composable
private fun SearchResultItemCard(
    video: VideoItem,
    searchQuery: String,
    onClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = F2WTheme.colors
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val highlightColor = themeColors.primary

    // Highlight matching query in title
    val annotatedTitle = remember(video.title, searchQuery) {
        val title = video.title
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            buildAnnotatedString { append(title) }
        } else {
            val lowerTitle = title.lowercase()
            val lowerQuery = query.lowercase()
            val startIndex = lowerTitle.indexOf(lowerQuery)
            if (startIndex >= 0) {
                val endIndex = startIndex + query.length
                buildAnnotatedString {
                    append(title.substring(0, startIndex))
                    withStyle(
                        style = SpanStyle(
                            color = highlightColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(title.substring(startIndex, endIndex))
                    }
                    append(title.substring(endIndex))
                }
            } else {
                buildAnnotatedString { append(title) }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Poster with Duration Badge at Bottom
        Box(
            modifier = Modifier
                .size(width = 108.dp, height = 66.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141824))
        ) {
            if (video.uriString.isNotBlank()) {
                AsyncImage(
                    model = video.uriString,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            // Duration badge at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.durationText,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Small play button in center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Cheza",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = annotatedTitle,
                color = textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${video.folderName} • ${video.resolution}",
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 3-dot options
        IconButton(
            onClick = onMoreOptionsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Chaguzi",
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
