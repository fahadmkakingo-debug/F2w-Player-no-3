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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WVioletAccent

@Composable
fun VideoNameGroupCard(
    group: VideoNameGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(F2WSurfaceElevated)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (group.isMultiVideo) listOf(
                        F2WCyanPrimary.copy(alpha = 0.5f),
                        F2WVioletAccent.copy(alpha = 0.25f),
                        F2WCardBorder.copy(alpha = 0.2f)
                    ) else listOf(
                        F2WCardBorder.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = F2WCyanPrimary, bounded = true),
                onClick = onClick
            )
            .padding(8.dp)
            .testTag("group_card_${group.id}")
    ) {
        // Thumbnail Poster with Stacked Effect for multiple videos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131720))
        ) {
            // Secondary background layer if it's a collection (gives stacked 3D folder feel)
            if (group.isMultiVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = 3.dp, y = (-3).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(F2WVioletAccent.copy(alpha = 0.25f))
                )
            }

            // Primary thumbnail image
            val thumbnailUri = group.primaryVideo?.uriString
            if (!thumbnailUri.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbnailUri,
                    contentDescription = group.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (group.isMultiVideo) Icons.Filled.Collections else Icons.Filled.VideoLibrary,
                        contentDescription = null,
                        tint = F2WCyanPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Dark gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // Top Badge: Video Count Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (group.isMultiVideo) Brush.horizontalGradient(
                            listOf(F2WCyanPrimary, F2WVioletAccent)
                        ) else Brush.horizontalGradient(
                            listOf(Color(0xCC1A2130), Color(0xCC1A2130))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (group.isMultiVideo) Icons.Filled.Collections else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (group.isMultiVideo) Color.Black else Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (group.isMultiVideo) "${group.count} Videos" else "1 Video",
                        color = if (group.isMultiVideo) Color.Black else Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Center Play Overlay icon on hover/card
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
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

        Spacer(modifier = Modifier.height(8.dp))

        // Title Row: Group Title & Chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    color = F2WTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (group.isMultiVideo) {
                        "Movie Collection • ${group.count} Parts"
                    } else {
                        group.primaryVideo?.durationText ?: "Single Video"
                    },
                    color = if (group.isMultiVideo) F2WCyanPrimary else F2WTextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Fungua Kundi",
                tint = F2WTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
