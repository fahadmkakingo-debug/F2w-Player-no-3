package com.example.ui.screens.privacy

import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import coil.compose.AsyncImage
import com.example.data.security.PrivacyVaultItem
import com.example.data.security.PrivacyVaultManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import java.io.File
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.PrivacySecurityManager
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
fun PrivacyVaultContentScreen(
    securityManager: PrivacySecurityManager,
    onLockVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showChangeSecurityQuestionDialog by remember { mutableStateOf(false) }
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var activeAddSubScreen by remember { mutableStateOf<PrivacyAddType?>(null) }

    val isBiometricSupported = remember { securityManager.isDeviceBiometricSupported(context) }
    var isFingerprintEnabled by remember { mutableStateOf(securityManager.isFingerprintEnabled()) }

    val coroutineScope = rememberCoroutineScope()
    val vaultManager = remember { PrivacyVaultManager.getInstance(context) }
    var vaultItems by remember { mutableStateOf(vaultManager.getVaultItems()) }
    var viewingImageItem by remember { mutableStateOf<PrivacyVaultItem?>(null) }
    var viewingVideoItem by remember { mutableStateOf<PrivacyVaultItem?>(null) }
    var viewingAudioItem by remember { mutableStateOf<PrivacyVaultItem?>(null) }
    var isVaultListView by remember { mutableStateOf(false) }

    var activeItemForActions by remember { mutableStateOf<PrivacyVaultItem?>(null) }
    var itemToRestore by remember { mutableStateOf<PrivacyVaultItem?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PrivacyVaultItem?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vaultManager.vaultUpdates.collect {
            vaultItems = vaultManager.getVaultItems()
        }
    }

    var selectedVaultTab by remember { mutableStateOf("All") }

    val displayedItems = remember(vaultItems, selectedVaultTab) {
        when (selectedVaultTab) {
            "Video" -> vaultItems.filter { it.mediaType.equals("VIDEO", ignoreCase = true) }
            "Audio" -> vaultItems.filter { it.mediaType.equals("AUDIO", ignoreCase = true) }
            "Image" -> vaultItems.filter { it.mediaType.equals("IMAGE", ignoreCase = true) }
            else -> vaultItems
        }
    }

    if (viewingVideoItem != null) {
        PrivacyVideoPlayerScreen(
            item = viewingVideoItem!!,
            onClose = { viewingVideoItem = null },
            onRestore = {
                val target = viewingVideoItem
                viewingVideoItem = null
                itemToRestore = target
            },
            onDeletePermanently = {
                val target = viewingVideoItem
                viewingVideoItem = null
                itemToDelete = target
            }
        )
        return
    }

    if (activeAddSubScreen != null) {
        PrivacyAddSubScreen(
            type = activeAddSubScreen!!,
            onBack = {
                activeAddSubScreen = null
                vaultItems = vaultManager.getVaultItems()
            }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(F2WSurface)
            .testTag("privacy_vault_unlocked_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Vault Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(F2WVioletAccent.copy(alpha = 0.2f))
                            .border(1.dp, F2WVioletAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = F2WVioletAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Encrypted Vault",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Secured with 4-Digit PIN & Biometrics",
                            color = F2WCyanPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isVaultListView = !isVaultListView },
                        modifier = Modifier.testTag("vault_toggle_view_btn")
                    ) {
                        Icon(
                            imageVector = if (isVaultListView) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (isVaultListView) "Grid View" else "List View",
                            tint = F2WCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showAddOptionsSheet = true },
                        modifier = Modifier.testTag("vault_add_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = F2WCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("vault_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Vault Settings",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier.testTag("lock_vault_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Lock Vault",
                            tint = F2WCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

        HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 1.dp)

        // Filter Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Video", "Audio", "Image").forEach { tab ->
                val isSelected = selectedVaultTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) F2WCyanPrimary else Color(0xFF1B202D))
                        .border(
                            1.dp,
                            if (isSelected) F2WCyanPrimary else F2WCardBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedVaultTab = tab }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color(0xFF070B12) else Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isVaultListView) 1 else 2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (displayedItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Empty Vault Status Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        F2WVioletAccent.copy(alpha = 0.15f),
                                        Color(0xFF131824)
                                    )
                                )
                            )
                            .border(1.dp, F2WVioletAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1B202D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = F2WVioletAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "No Hidden ${if (selectedVaultTab == "All") "Files" else selectedVaultTab} Yet",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Tap the Add button below to import and secure your ${if (selectedVaultTab == "All") "media" else selectedVaultTab.lowercase()} files into the private vault.",
                                color = F2WTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = { showAddOptionsSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = F2WVioletAccent),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("empty_vault_add_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Media",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${displayedItems.size} ${if (selectedVaultTab == "All") "Files" else "$selectedVaultTab Files"} in Vault",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Secured",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                items(
                    items = displayedItems,
                    key = { it.id }
                ) { item ->
                    if (isVaultListView) {
                        PrivacyVaultItemRow(
                            item = item,
                            onClick = {
                                when (item.mediaType.uppercase()) {
                                    "IMAGE" -> viewingImageItem = item
                                    "VIDEO" -> viewingVideoItem = item
                                    "AUDIO" -> viewingAudioItem = item
                                }
                            },
                            onMoreClick = {
                                activeItemForActions = item
                            }
                        )
                    } else {
                        PrivacyVaultItemGridCard(
                            item = item,
                            onClick = {
                                when (item.mediaType.uppercase()) {
                                    "IMAGE" -> viewingImageItem = item
                                    "VIDEO" -> viewingVideoItem = item
                                    "AUDIO" -> viewingAudioItem = item
                                }
                            },
                            onMoreClick = {
                                activeItemForActions = item
                            }
                        )
                    }
                }
            }
        }

        // Close Column so ExtendedFloatingActionButton is placed inside BoxScope
    }

    // Floating Action Button for "Add"
    ExtendedFloatingActionButton(
        onClick = { showAddOptionsSheet = true },
        containerColor = F2WCyanPrimary,
        contentColor = Color(0xFF070B12),
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        text = {
            Text(
                text = "Add",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 24.dp)
            .testTag("privacy_home_add_fab")
    )
}

    // Modal Bottom Sheet with the three options: Video, Audio, Image
    if (showAddOptionsSheet) {
        PrivacyAddOptionBottomSheet(
            onDismissRequest = { showAddOptionsSheet = false },
            onOptionSelect = { option ->
                activeAddSubScreen = option
            }
        )
    }

    // Vault Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = F2WSurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = F2WCyanPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Privacy Security Settings",
                    color = F2WTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Option 1: Change PIN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showSettingsDialog = false
                                showChangePinDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockReset,
                            contentDescription = null,
                            tint = F2WCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Change 4-Digit PIN", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Update your master security code", color = F2WTextSecondary, fontSize = 11.5.sp)
                        }
                    }

                    HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Option 2: Change Security Question & Answer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showSettingsDialog = false
                                showChangeSecurityQuestionDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HelpOutline,
                            contentDescription = null,
                            tint = F2WVioletAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Security Question & Answer", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Update PIN recovery question", color = F2WTextSecondary, fontSize = 11.5.sp)
                        }
                    }

                    if (isBiometricSupported) {
                        HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Option 3: Enable/Disable Fingerprint
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = null,
                                    tint = F2WVioletAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Fingerprint Unlock", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Use phone's biometric sensor", color = F2WTextSecondary, fontSize = 11.5.sp)
                                }
                            }

                            Switch(
                                checked = isFingerprintEnabled,
                                onCheckedChange = {
                                    isFingerprintEnabled = it
                                    securityManager.setFingerprintEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = F2WVioletAccent
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = F2WCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Option 4: Lock Vault Now
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showSettingsDialog = false
                                onLockVault()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Lock Vault Now", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Require authentication to re-enter", color = F2WTextSecondary, fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Embedded Security Status Info inside Settings
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131722))
                            .border(1.dp, F2WCardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = F2WCyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Security Status",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Protection Level:", color = F2WTextSecondary, fontSize = 12.sp)
                                Text("SHA-256 Encrypted", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fingerprint Unlock:", color = F2WTextSecondary, fontSize = 12.sp)
                                Text(
                                    if (isFingerprintEnabled) "Enabled" else "Disabled",
                                    color = if (isFingerprintEnabled) F2WCyanPrimary else F2WTextTertiary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = F2WCyanPrimary)
                }
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            securityManager = securityManager,
            onDismissRequest = { showChangePinDialog = false }
        )
    }

    // Change Security Question Dialog
    if (showChangeSecurityQuestionDialog) {
        ChangeSecurityQuestionDialog(
            securityManager = securityManager,
            onDismissRequest = { showChangeSecurityQuestionDialog = false }
        )
    }

    // Full screen image viewer dialog
    viewingImageItem?.let { imageItem ->
        PrivacyImageViewerDialog(
            item = imageItem,
            onClose = { viewingImageItem = null },
            onRestore = {
                viewingImageItem = null
                itemToRestore = imageItem
            },
            onDeletePermanently = {
                viewingImageItem = null
                itemToDelete = imageItem
            }
        )
    }

    // Dedicated Audio Player Dialog
    viewingAudioItem?.let { audioItem ->
        PrivacyAudioPlayerDialog(
            item = audioItem,
            onClose = { viewingAudioItem = null },
            onRestore = {
                viewingAudioItem = null
                itemToRestore = audioItem
            },
            onDeletePermanently = {
                viewingAudioItem = null
                itemToDelete = audioItem
            }
        )
    }

    // Vault Item Action Sheet (Play/View, Restore to Device, Delete Permanently)
    activeItemForActions?.let { targetItem ->
        PrivacyItemActionBottomSheet(
            item = targetItem,
            onDismissRequest = { activeItemForActions = null },
            onPlayOrView = {
                val target = activeItemForActions ?: targetItem
                activeItemForActions = null
                when (target.mediaType.uppercase()) {
                    "IMAGE" -> viewingImageItem = target
                    "VIDEO" -> viewingVideoItem = target
                    "AUDIO" -> viewingAudioItem = target
                }
            },
            onRestore = {
                val target = activeItemForActions ?: targetItem
                activeItemForActions = null
                itemToRestore = target
            },
            onDeletePermanently = {
                val target = activeItemForActions ?: targetItem
                activeItemForActions = null
                itemToDelete = target
            }
        )
    }

    // Restore to Device Confirmation Dialog
    itemToRestore?.let { targetItem ->
        PrivacyRestoreConfirmDialog(
            item = targetItem,
            isRestoring = isRestoring,
            onDismissRequest = { if (!isRestoring) itemToRestore = null },
            onConfirmRestore = {
                isRestoring = true
                coroutineScope.launch {
                    val restored = vaultManager.restoreVaultItemToDevice(targetItem)
                    isRestoring = false
                    val name = targetItem.fileName
                    itemToRestore = null
                    if (restored) {
                        Toast.makeText(context, "\"$name\" imerejeshwa kwenye simu!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Imeshindikana kurejesha \"$name\"", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Delete Permanently Confirmation Dialog
    itemToDelete?.let { targetItem ->
        PrivacyDeletePermanentlyConfirmDialog(
            item = targetItem,
            isDeleting = isDeleting,
            onDismissRequest = { if (!isDeleting) itemToDelete = null },
            onConfirmDelete = {
                isDeleting = true
                coroutineScope.launch {
                    val deleted = vaultManager.deleteVaultItemPermanently(targetItem)
                    isDeleting = false
                    val name = targetItem.fileName
                    itemToDelete = null
                    if (deleted) {
                        Toast.makeText(context, "\"$name\" imefutwa kabisa!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Imeshindikana kufuta \"$name\"", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun ChangePinDialog(
    securityManager: PrivacySecurityManager,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }

    var currentPinError by remember { mutableStateOf(false) }
    var newPinMismatchError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = F2WSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.LockReset,
                contentDescription = null,
                tint = F2WCyanPrimary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text("Change 4-Digit PIN", color = F2WTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentPinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPinInput = it
                        currentPinError = false
                    },
                    label = { Text("Current PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = currentPinError,
                    supportingText = if (currentPinError) {
                        { Text("Current PIN is incorrect", color = Color(0xFFEF4444)) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = F2WCyanPrimary,
                        unfocusedBorderColor = F2WCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it
                    },
                    label = { Text("New 4-Digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = F2WCyanPrimary,
                        unfocusedBorderColor = F2WCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinInput = it
                        newPinMismatchError = false
                    },
                    label = { Text("Confirm New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = newPinMismatchError,
                    supportingText = if (newPinMismatchError) {
                        { Text("New PINs do not match", color = Color(0xFFEF4444)) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = F2WCyanPrimary,
                        unfocusedBorderColor = F2WCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!securityManager.verifyPin(currentPinInput)) {
                        currentPinError = true
                    } else if (newPinInput.length != 4 || newPinInput != confirmPinInput) {
                        newPinMismatchError = true
                    } else {
                        securityManager.resetPin(newPinInput)
                        Toast.makeText(context, "PIN successfully updated!", Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary)
            ) {
                Text("Update PIN", color = Color(0xFF070B12), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = F2WTextSecondary)
            }
        }
    )
}

@Composable
private fun ChangeSecurityQuestionDialog(
    securityManager: PrivacySecurityManager,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val defaultQuestions = listOf(
        "What was the name of your first school or pet?",
        "What is your mother's maiden name?",
        "What is your favorite movie or book?",
        "What city were you born in?",
        "What was the model of your first phone?"
    )
    var selectedQuestion by remember { mutableStateOf(defaultQuestions[0]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var newAnswerInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = F2WSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.HelpOutline,
                contentDescription = null,
                tint = F2WVioletAccent,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text("Security Recovery Question", color = F2WTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select a question and enter your secret answer for PIN recovery.",
                    color = F2WTextSecondary,
                    fontSize = 12.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2433))
                        .border(1.dp, F2WCardBorder, RoundedCornerShape(8.dp))
                        .clickable { isDropdownExpanded = true }
                        .padding(12.dp)
                ) {
                    Text(text = selectedQuestion, color = Color.White, fontSize = 13.5.sp)

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        defaultQuestions.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q) },
                                onClick = {
                                    selectedQuestion = q
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The answer is never displayed after saving
                OutlinedTextField(
                    value = newAnswerInput,
                    onValueChange = { newAnswerInput = it },
                    label = { Text("New Secret Answer") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = F2WCyanPrimary,
                        unfocusedBorderColor = F2WCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newAnswerInput.trim().length >= 2) {
                        securityManager.updateSecurityQuestion(selectedQuestion, newAnswerInput)
                        Toast.makeText(context, "Security question updated!", Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    } else {
                        Toast.makeText(context, "Please enter a valid answer", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = F2WCyanPrimary)
            ) {
                Text("Save", color = Color(0xFF070B12), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = F2WTextSecondary)
            }
        }
    )
}
