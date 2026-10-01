package com.example.util.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Types of media permissions required by various screens in the app.
 */
enum class MediaPermissionType {
    ALL_MEDIA,
    VIDEO,
    AUDIO,
    IMAGES
}

/**
 * Helper utility for determining and managing runtime storage and media permissions
 * across Android versions (Android 14+ partial access, Android 13 Tiramisu granular permissions,
 * and legacy Android 12 & below storage permissions).
 */
object MediaPermissionManager {

    /**
     * Get the list of manifest permissions needed based on current Android OS API level and media type.
     */
    fun getRequiredPermissions(type: MediaPermissionType = MediaPermissionType.ALL_MEDIA): Array<String> {
        return when {
            // Android 14+ (API 34+) - Supports partial visual media selection
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                when (type) {
                    MediaPermissionType.VIDEO -> arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                    MediaPermissionType.AUDIO -> arrayOf(
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                    MediaPermissionType.IMAGES -> arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                    MediaPermissionType.ALL_MEDIA -> arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                }
            }
            // Android 13 (API 33) - Granular media permissions
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when (type) {
                    MediaPermissionType.VIDEO -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                    MediaPermissionType.AUDIO -> arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                    MediaPermissionType.IMAGES -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                    MediaPermissionType.ALL_MEDIA -> arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                }
            }
            // Android 12 and below (API <= 32)
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Checks whether the app currently has permission to read media for the requested type.
     */
    fun hasMediaAccess(context: Context, type: MediaPermissionType = MediaPermissionType.ALL_MEDIA): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasUserSelected = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED

            return when (type) {
                MediaPermissionType.VIDEO -> {
                    val hasVideo = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED
                    hasVideo || hasUserSelected
                }
                MediaPermissionType.AUDIO -> {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                }
                MediaPermissionType.IMAGES -> {
                    val hasImages = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                    hasImages || hasUserSelected
                }
                MediaPermissionType.ALL_MEDIA -> {
                    val hasVideo = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasAudio = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    (hasVideo || hasUserSelected) && hasAudio
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return when (type) {
                MediaPermissionType.VIDEO -> ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
                MediaPermissionType.AUDIO -> ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                MediaPermissionType.IMAGES -> ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                MediaPermissionType.ALL_MEDIA -> {
                    val hasVideo = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasAudio = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    hasVideo && hasAudio
                }
            }
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if only partial media access is granted (Android 14+).
     */
    fun isPartialAccessGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasUserSelected = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
            val hasFullVideo = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            val hasFullImages = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            return hasUserSelected && (!hasFullVideo || !hasFullImages)
        }
        return false
    }

    /**
     * Checks if the app should show permission rationale to the user.
     */
    fun shouldShowRationale(activity: Activity, type: MediaPermissionType = MediaPermissionType.ALL_MEDIA): Boolean {
        val permissions = getRequiredPermissions(type)
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * Navigates the user directly to the application details screen in Android System Settings.
     */
    fun openApplicationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
