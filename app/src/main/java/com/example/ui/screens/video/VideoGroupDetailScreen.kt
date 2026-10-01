package com.example.ui.screens.video

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
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WBackground
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurface
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WVioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGroupDetailScreen(
    group: VideoNameGroup,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    isListView: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var activeVideoForMenu by remember { mutableStateOf<VideoItem?>(null) }
    val columnsCount = if (isListView) 1 else 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .statusBarsPadding()
            .testTag("group_detail_screen")
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
                modifier = Modifier.testTag("group_detail_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Rudi Nyuma",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = group.title,
                    color = F2WTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.count} Videos • Auto-Grouped Franchise",
                    color = F2WCyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick Play All button
            IconButton(
                onClick = {
                    group.videos.firstOrNull()?.let { onVideoClick(it) }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(F2WCyanPrimary.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Cheza Zote",
                    tint = F2WCyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        HorizontalDivider(
            color = F2WCardBorder.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // 2-Column Grid of videos in this group
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 14.dp,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card Banner
            item(span = { GridItemSpan(columnsCount) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    F2WCyanPrimary.copy(alpha = 0.15f),
                                    F2WVioletAccent.copy(alpha = 0.12f),
                                    Color(0xFF131722)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    F2WCyanPrimary.copy(alpha = 0.4f),
                                    F2WVioletAccent.copy(alpha = 0.25f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Collections,
                                    contentDescription = null,
                                    tint = F2WCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Movie Collection",
                                    color = F2WCyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Video zote zenye jina linalofanana zimejumuishwa pamoja kiotomatiki.",
                                color = F2WTextSecondary,
                                fontSize = 11.5.sp
                            )
                        }

                        Button(
                            onClick = {
                                group.videos.firstOrNull()?.let { onVideoClick(it) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = F2WCyanPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cheza Zote", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Video Cards in List or 2 columns Grid
            items(
                items = group.videos,
                key = { it.id }
            ) { video ->
                if (isListView) {
                    VideoListCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onMoreOptionsClick = { activeVideoForMenu = video }
                    )
                } else {
                    VideoThumbnailCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onMoreOptionsClick = { activeVideoForMenu = video }
                    )
                }
            }
        }
    }

    // Smart Action Menu on video card options click
    activeVideoForMenu?.let { video ->
        VideoActionSmartMenu(
            video = video,
            onDismissRequest = { activeVideoForMenu = null }
        )
    }
}
