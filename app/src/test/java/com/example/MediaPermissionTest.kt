package com.example

import com.example.util.permission.MediaPermissionManager
import com.example.util.permission.MediaPermissionType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionTest {

    @Test
    fun testRequiredPermissionsNotNullOrEmpty() {
        val allPermissions = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.ALL_MEDIA)
        assertNotNull(allPermissions)
        assertTrue(allPermissions.isNotEmpty())

        val videoPermissions = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.VIDEO)
        assertNotNull(videoPermissions)
        assertTrue(videoPermissions.isNotEmpty())

        val audioPermissions = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.AUDIO)
        assertNotNull(audioPermissions)
        assertTrue(audioPermissions.isNotEmpty())

        val imagePermissions = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.IMAGES)
        assertNotNull(imagePermissions)
        assertTrue(imagePermissions.isNotEmpty())
    }

    @Test
    fun testPermissionTypesContainCorrectConstants() {
        val videoPermissions = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.VIDEO)
        assertTrue(
            videoPermissions.any {
                it.contains("READ_MEDIA_VIDEO") || it.contains("READ_EXTERNAL_STORAGE")
            }
        )

        val audioPermissions = MediaPermissionManager.getRequiredPermissions(MediaPermissionType.AUDIO)
        assertTrue(
            audioPermissions.any {
                it.contains("READ_MEDIA_AUDIO") || it.contains("READ_EXTERNAL_STORAGE")
            }
        )
    }
}
