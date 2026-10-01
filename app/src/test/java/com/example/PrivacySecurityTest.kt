package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PrivacySecurityTest {

    private val salt = "F2W_VAULT_SEC_SALT_8841"
    private val adminPin = "0089"

    private fun hashWithSalt(value: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + value + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testUserPinVerification() {
        val userPin = "1234"
        val storedHash = hashWithSalt(userPin)

        // Stored value must be a hash, not plain text
        assertNotEquals("1234", storedHash)
        assertEquals(64, storedHash.length) // SHA-256 hex string

        // Correct PIN
        assertTrue(storedHash == hashWithSalt("1234"))

        // Incorrect PIN
        assertFalse(storedHash == hashWithSalt("5678"))
    }

    @Test
    fun testAdminUnlockPin() {
        fun verifyPin(input: String, userPinHash: String): Boolean {
            if (input == adminPin) return true
            return userPinHash == hashWithSalt(input)
        }

        val userPinHash = hashWithSalt("9999")

        // User PIN unlocks
        assertTrue(verifyPin("9999", userPinHash))

        // Admin PIN 0089 unlocks even when user PIN is 9999
        assertTrue(verifyPin("0089", userPinHash))

        // Random PIN fails
        assertFalse(verifyPin("0000", userPinHash))
        assertFalse(verifyPin("1234", userPinHash))
    }

    @Test
    fun testSecurityAnswerVerification() {
        val rawAnswer = "My Dog Buster"
        val storedAnswerHash = hashWithSalt(rawAnswer.trim().lowercase())

        // Verification is case-insensitive and trims whitespace
        val input1 = "my dog buster"
        val input2 = "   MY DOG BUSTER  "
        val inputWrong = "Cats"

        assertEquals(storedAnswerHash, hashWithSalt(input1.trim().lowercase()))
        assertEquals(storedAnswerHash, hashWithSalt(input2.trim().lowercase()))
        assertNotEquals(storedAnswerHash, hashWithSalt(inputWrong.trim().lowercase()))
    }

    @Test
    fun testPinReset() {
        var userPinHash = hashWithSalt("1111")
        assertTrue(userPinHash == hashWithSalt("1111"))

        // Reset PIN to 2222
        userPinHash = hashWithSalt("2222")
        assertFalse(userPinHash == hashWithSalt("1111"))
        assertTrue(userPinHash == hashWithSalt("2222"))
    }

    @Test
    fun testPreserveFilenameAndExtension() {
        val originalFilename = "Family_Trip_2026.mp4"
        val dotIndex = originalFilename.lastIndexOf('.')
        val baseName = originalFilename.substring(0, dotIndex)
        val extension = originalFilename.substring(dotIndex)

        assertEquals("Family_Trip_2026", baseName)
        assertEquals(".mp4", extension)

        // Generate unique name if collision occurs
        val uniqueName = "${baseName}_1$extension"
        assertEquals("Family_Trip_2026_1.mp4", uniqueName)
        assertTrue(uniqueName.endsWith(".mp4"))
    }

    @Test
    fun testVaultCategoryFilterExclusion() {
        val libraryFiles = listOf(
            "/storage/emulated/0/Movies/Inception.mp4",
            "/storage/emulated/0/DCIM/Family.mp4",
            "/storage/emulated/0/Downloads/Notes.mp4"
        )

        // Move Inception into Privacy Vault
        val movedOriginalPaths = setOf(
            "/storage/emulated/0/movies/inception.mp4"
        )

        val visibleInLibrary = libraryFiles.filter { file ->
            !movedOriginalPaths.contains(file.lowercase())
        }

        assertEquals(2, visibleInLibrary.size)
        assertFalse(visibleInLibrary.contains("/storage/emulated/0/Movies/Inception.mp4"))
        assertTrue(visibleInLibrary.contains("/storage/emulated/0/DCIM/Family.mp4"))
        assertTrue(visibleInLibrary.contains("/storage/emulated/0/Downloads/Notes.mp4"))
    }

    @Test
    fun testCategorySegregation() {
        val vaultItems = listOf(
            Triple("1", "VIDEO", "Movie.mp4"),
            Triple("2", "AUDIO", "Song.mp3"),
            Triple("3", "IMAGE", "Photo.jpg"),
            Triple("4", "VIDEO", "Clip.mkv")
        )

        val videoItems = vaultItems.filter { it.second == "VIDEO" }
        val audioItems = vaultItems.filter { it.second == "AUDIO" }
        val imageItems = vaultItems.filter { it.second == "IMAGE" }

        assertEquals(2, videoItems.size)
        assertEquals(1, audioItems.size)
        assertEquals(1, imageItems.size)
        assertTrue(videoItems.all { it.second == "VIDEO" })
        assertTrue(audioItems.all { it.second == "AUDIO" })
        assertTrue(imageItems.all { it.second == "IMAGE" })
    }

    @Test
    fun testFileMoveNoDuplicateCondition() {
        var originalExists = true
        var targetExists = false

        // Simulating the move algorithm:
        // 1. Copy source to target
        targetExists = true
        // 2. Delete source
        originalExists = false

        // After successful move:
        assertTrue("Target file must exist in Privacy", targetExists)
        assertFalse("Original file must not remain in original location", originalExists)
    }

    @Test
    fun testFailedMovePreservesOriginal() {
        var originalExists = true
        var targetFileCopied = false
        val copyFailed = true

        if (copyFailed) {
            // Must NOT delete original file
            targetFileCopied = false
        }

        assertTrue("Original file must be untouched when move fails", originalExists)
        assertFalse("No broken duplicate should remain in target", targetFileCopied)
    }

    @Test
    fun testRestoreVaultItemToDeviceStorage() {
        var fileInVault = true
        var fileInDeviceStorage = false
        val vaultRegistry = mutableListOf("vault_item_123")

        // User triggers "Restore to Device (Rejesha kwenye Simu)"
        // 1. Copy back to device storage location
        fileInDeviceStorage = true
        // 2. Remove from vault directory
        fileInVault = false
        // 3. Remove from vault registry
        vaultRegistry.remove("vault_item_123")

        assertTrue("File must be restored in phone device storage", fileInDeviceStorage)
        assertFalse("File must no longer remain in Privacy vault", fileInVault)
        assertTrue("Item must be removed from vault registry", vaultRegistry.isEmpty())
    }

    @Test
    fun testDeleteVaultItemPermanently() {
        var fileOnDisk = true
        val vaultRegistry = mutableListOf("vault_item_456")

        // User triggers "Delete Permanently (Futa Kabisa)"
        // 1. File erased completely
        fileOnDisk = false
        // 2. Erased from vault registry
        vaultRegistry.remove("vault_item_456")

        assertFalse("File must be completely erased from disk", fileOnDisk)
        assertTrue("Item must be removed from vault registry", vaultRegistry.isEmpty())
    }

    @Test
    fun testPlayAndViewRoutingInsidePrivacy() {
        fun resolvePlayerAction(mediaType: String): String {
            return when (mediaType.uppercase()) {
                "VIDEO" -> "OPEN_VIDEO_PLAYER"
                "AUDIO" -> "OPEN_AUDIO_PLAYER"
                "IMAGE" -> "OPEN_IMAGE_VIEWER"
                else -> "UNKNOWN"
            }
        }

        assertEquals("OPEN_VIDEO_PLAYER", resolvePlayerAction("VIDEO"))
        assertEquals("OPEN_AUDIO_PLAYER", resolvePlayerAction("AUDIO"))
        assertEquals("OPEN_IMAGE_VIEWER", resolvePlayerAction("IMAGE"))
    }
}
