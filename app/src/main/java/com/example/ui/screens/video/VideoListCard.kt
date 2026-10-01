package com.example.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary

@Composable
fun VideoListCard(
    video: VideoItem,
    onClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Derive format and badge colors matching the user's reference image
    val format = remember(video) {
        val titleLower = video.title.lowercase()
        val uriLower = video.uriString.lowercase()
        when {
            titleLower.contains("mkv") || uriLower.endsWith(".mkv") -> "MKV"
            titleLower.contains("avi") || uriLower.endsWith(".avi") -> "AVI"
            titleLower.contains("webm") || uriLower.endsWith(".webm") -> "WEBM"
            titleLower.contains("mov") || uriLower.endsWith(".mov") -> "MOV"
            else -> "MP4"
        }
    }

    val (badgeBg, badgeText, badgeBorder) = when (format) {
        "MKV" -> Triple(Color(0x338A4DFF), Color(0xFFB588FF), Color(0x668A4DFF))
        "AVI" -> Triple(Color(0x33FF9800), Color(0xFFFFB74D), Color(0x66FF9800))
        "WEBM" -> Triple(Color(0x3300E676), Color(0xFF69F0AE), Color(0x6600E676))
        else -> Triple(Color(0x3300D1FF), Color(0xFF00D1FF), Color(0x6600D1FF))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1B1E28))
            .border(1.dp, F2WCardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = F2WCyanPrimary),
                onClick = onClick
            )
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)
            .testTag("video_list_card_${video.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left: Video Thumbnail with Center Play Button
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                modifier = Modifier
                    .size(width = 104.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF12141C))
            ) {
                if (video.uriString.isNotBlank()) {
                    AsyncImage(
                        model = com.example.util.media.VideoThumbnailHelper.buildThumbnailRequest(
                            context = context,
                            uriString = video.uriString
                        ),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Play icon overlay in center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Cheza",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Progress bar if partially watched
                if (video.progressFraction > 0.02f) {
                    LinearProgressIndicator(
                        progress = { video.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter),
                        color = F2WCyanPrimary,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 2. Middle: Title, Format + Duration + Size, and Folder name
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Video Title
                Text(
                    text = video.title,
                    color = F2WTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Second row: [Format Badge] Duration Size
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Format Pill (e.g. MKV, MP4, AVI)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = format,
                            color = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = video.durationText,
                        color = F2WTextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = video.sizeText,
                        color = F2WTextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Third row: Folder Name
                Text(
                    text = video.folderName,
                    color = F2WTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. Right: Favorite Heart & 3-dot options
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(68.dp)
            ) {
                // Heart Favorite Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF2A6D) else Color(0xFF6C768A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3-dot More Options Button
                IconButton(
                    onClick = onMoreOptionsClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Chaguzi Zaidi",
                        tint = Color(0xFF6C768A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
