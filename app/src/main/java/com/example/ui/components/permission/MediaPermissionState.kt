package com.example.ui.components.permission

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.permission.MediaPermissionManager
import com.example.util.permission.MediaPermissionType

@Stable
class MediaPermissionState(
    val permissionType: MediaPermissionType,
    private val context: Context,
    private val onRequestLauncher: (Array<String>) -> Unit,
    private val onGrantedCallback: (() -> Unit)?
) {
    var hasAccess by mutableStateOf(MediaPermissionManager.hasMediaAccess(context, permissionType))
        private set

    var isPartialAccess by mutableStateOf(MediaPermissionManager.isPartialAccessGranted(context))
        private set

    var hasRequestedOnce by mutableStateOf(false)
        private set

    var showRationaleDialog by mutableStateOf(false)

    fun refresh() {
        hasAccess = MediaPermissionManager.hasMediaAccess(context, permissionType)
        isPartialAccess = MediaPermissionManager.isPartialAccessGranted(context)
    }

    fun requestPermissions() {
        val permissions = MediaPermissionManager.getRequiredPermissions(permissionType)
        onRequestLauncher(permissions)
    }

    fun onPermissionResult(results: Map<String, Boolean>) {
        hasRequestedOnce = true
        refresh()
        if (hasAccess) {
            showRationaleDialog = false
            onGrantedCallback?.invoke()
        } else {
            // Check if user denied with or without rationale
            val activity = context as? Activity
            if (activity != null && !MediaPermissionManager.shouldShowRationale(activity, permissionType)) {
                // Permanently denied or clicked don't ask again
                showRationaleDialog = true
            } else {
                showRationaleDialog = true
            }
        }
    }

    fun openSettings() {
        MediaPermissionManager.openApplicationSettings(context)
        showRationaleDialog = false
    }

    fun dismissRationale() {
        showRationaleDialog = false
    }
}

/**
 * Creates and remembers a [MediaPermissionState] configured for the target media permission type.
 * Automatically checks and updates permission status when the app resumes (e.g. after returning from settings).
 */
@Composable
fun rememberMediaPermissionState(
    type: MediaPermissionType = MediaPermissionType.ALL_MEDIA,
    onPermissionGranted: (() -> Unit)? = null
): MediaPermissionState {
    val context = LocalContext.current
    var permissionStateRef by remember { mutableStateOf<MediaPermissionState?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionStateRef?.onPermissionResult(results)
    }

    val state = remember(type, context) {
        MediaPermissionState(
            permissionType = type,
            context = context,
            onRequestLauncher = { perms -> launcher.launch(perms) },
            onGrantedCallback = onPermissionGranted
        ).also { permissionStateRef = it }
    }

    // Observe lifecycle events to refresh permission state when coming back from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return state
}
