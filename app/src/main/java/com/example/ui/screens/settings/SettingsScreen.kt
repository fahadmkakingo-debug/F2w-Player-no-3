package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.F2WBackground
import com.example.ui.theme.F2WCardBorder
import com.example.ui.theme.F2WCyanPrimary
import com.example.ui.theme.F2WSurfaceElevated
import com.example.ui.theme.F2WTextPrimary
import com.example.ui.theme.F2WTextSecondary
import com.example.ui.theme.F2WTextTertiary

import android.widget.Toast
import androidx.compose.material.icons.filled.Warning
import com.example.data.security.PrivacyVaultManager
import com.example.ui.components.UninstallPrivacyWarningDialog

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vaultManager = remember { PrivacyVaultManager.getInstance(context) }
    var showUninstallWarningDialog by remember { mutableStateOf(false) }
    var privacyItemCount by remember { mutableStateOf(0) }
    var selectedSection by remember { mutableStateOf<String?>(null) }

    if (selectedSection == "Video") {
        VideoSettingsScreen(
            onBack = { selectedSection = null }
        )
        return
    }

    if (selectedSection == "About App") {
        AboutAppScreen(
            onBack = { selectedSection = null }
        )
        return
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(F2WBackground)
            .statusBarsPadding()
            .testTag("settings_screen_container")
    ) {
        // Top Bar with Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B252E))
                        .border(1.dp, F2WCardBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onBack)
                        .testTag("settings_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = F2WTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "Preferences & Configurations",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = F2WTextTertiary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Exactly 4 Settings Options List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Video
            SettingsOptionCard(
                title = "Video",
                subtitle = "Playback controls, hardware decoder, aspect ratio",
                icon = Icons.Filled.PlayCircle,
                testTag = "settings_option_video",
                onClick = { selectedSection = "Video" }
            )

            // 2. Music
            SettingsOptionCard(
                title = "Music",
                subtitle = "Audio playback, equaliser presets, background play",
                icon = Icons.Filled.Headphones,
                testTag = "settings_option_music",
                onClick = { selectedSection = "Music" }
            )

            // 3. Subtitles
            SettingsOptionCard(
                title = "Subtitles",
                subtitle = "Text encoding, font styling, position & timing sync",
                icon = Icons.Filled.Subtitles,
                testTag = "settings_option_subtitles",
                onClick = { selectedSection = "Subtitles" }
            )

            // 4. About App
            SettingsOptionCard(
                title = "About App",
                subtitle = "F2W Player v1.0.0, build details & licenses",
                icon = Icons.Filled.Info,
                testTag = "settings_option_about_app",
                onClick = { selectedSection = "About App" }
            )

            // 5. Uninstall Warning & Data Protection
            SettingsOptionCard(
                title = "Uninstall Warning",
                subtitle = "Check for private items before uninstalling app",
                icon = Icons.Filled.Warning,
                testTag = "settings_option_uninstall_warning",
                onClick = {
                    val items = vaultManager.getVaultItems()
                    privacyItemCount = items.size
                    if (privacyItemCount > 0) {
                        showUninstallWarningDialog = true
                    } else {
                        Toast.makeText(context, "No private items found. You can safely uninstall the app.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    if (showUninstallWarningDialog) {
        UninstallPrivacyWarningDialog(
            itemCount = privacyItemCount,
            onGoToPrivacy = {
                showUninstallWarningDialog = false
                onBack()
                onNavigateToPrivacy()
            },
            onDismiss = { showUninstallWarningDialog = false }
        )
    }
}

@Composable
private fun SettingsOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x3300E5FF)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(F2WSurfaceElevated)
            .border(1.dp, F2WCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C2B38))
                        .border(1.dp, F2WCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = F2WCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = F2WTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = F2WTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Forward Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = F2WTextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
