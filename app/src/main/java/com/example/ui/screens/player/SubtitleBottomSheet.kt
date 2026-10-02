package com.example.ui.screens.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.video.SubtitleTrackOption
import com.example.data.video.VideoPlaybackManager
import com.example.ui.theme.F2WCyanPrimary
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleBottomSheet(
    videoManager: VideoPlaybackManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isSubtitlesEnabled by videoManager.isSubtitlesEnabled.collectAsState()
    val currentSubtitleName by videoManager.currentSubtitleName.collectAsState()
    val availableSubtitles by videoManager.availableSubtitles.collectAsState()

    var showTrackSelectionDialog by remember { mutableStateOf(false) }

    // SAF File Picker for Subtitle files (.srt, .vtt, .ass, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not supported
            }

            val displayName = getFileNameFromUri(context, uri)
            videoManager.addExternalSubtitle(uri, displayName)
            Toast.makeText(context, "Manukuu yamepakiwa: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16181C),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.testTag("subtitle_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header Row: [<- Back] "Subtitle" [Toggle Switch]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.testTag("subtitle_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Subtitle",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Subtitle Master Switch (Green when ON, Gray when OFF)
                Switch(
                    checked = isSubtitlesEnabled,
                    onCheckedChange = { enabled ->
                        videoManager.setSubtitlesEnabled(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF22C55E),
                        uncheckedThumbColor = Color(0xFFD1D5DB),
                        uncheckedTrackColor = Color(0xFF383B40),
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("subtitle_master_switch")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Option 1: "Current subtitle" -> Shows current name & right chevron arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showTrackSelectionDialog = true
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("row_current_subtitle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current subtitle",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!isSubtitlesEnabled || currentSubtitleName.equals("None", ignoreCase = true)) {
                            "None"
                        } else {
                            "$currentSubtitleName (Automatic)"
                        },
                        color = if (isSubtitlesEnabled && !currentSubtitleName.equals("None", ignoreCase = true)) Color(0xFF22C55E) else Color(0xFF949BA4),
                        fontSize = 13.5.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open subtitle selection",
                    tint = Color(0xFF949BA4),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Subtle Divider
            HorizontalDivider(
                color = Color(0xFF2B2E36),
                thickness = 0.8.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Option 2: "Open file" -> Launches system storage file picker for .srt/.vtt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("row_open_file"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Open file",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Chagua faili la .srt au .vtt kutoka kwenye simu",
                        color = Color(0xFF949BA4),
                        fontSize = 13.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Pick file",
                    tint = Color(0xFF949BA4),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Dialog for selecting from available subtitle tracks or choosing "None"
    if (showTrackSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showTrackSelectionDialog = false },
            containerColor = Color(0xFF1E2126),
            title = {
                Text(
                    text = "Current subtitle",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Option: None (Off)
                    item {
                        val isNoneSelected = !isSubtitlesEnabled || currentSubtitleName.equals("None", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    videoManager.disableSubtitles()
                                    showTrackSelectionDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isNoneSelected,
                                onClick = {
                                    videoManager.disableSubtitles()
                                    showTrackSelectionDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "None (Hakuna manukuu)",
                                color = if (isNoneSelected) Color.White else Color(0xFF949BA4),
                                fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Available tracks (embedded & loaded external)
                    items(availableSubtitles) { track ->
                        val isTrackActive = isSubtitlesEnabled && track.name == currentSubtitleName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    videoManager.selectSubtitleTrack(track)
                                    showTrackSelectionDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isTrackActive,
                                onClick = {
                                    videoManager.selectSubtitleTrack(track)
                                    showTrackSelectionDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    color = if (isTrackActive) Color.White else Color(0xFF949BA4),
                                    fontWeight = if (isTrackActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (track.isEmbedded) "Embedded Movie Track" else "External Subtitle File",
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackSelectionDialog = false }) {
                    Text("Funga", color = F2WCyanPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "subtitle.srt"
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: "subtitle.srt"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        uri.path?.let { p ->
            name = File(p).name
        }
    }
    return name
}
