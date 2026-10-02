package com.example.data.security

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Robust Local AES-256-GCM Cryptographic Engine for F2W Private Vault.
 * Provides 100% offline, authenticated encryption for media files and metadata.
 */
object VaultCryptoManager {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    fun generateRandomSalt(length: Int = 16): ByteArray {
        val salt = ByteArray(length)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun generateRandomIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    fun deriveKey(pinOrAnswer: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pinOrAnswer.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun generateVerifierHash(key: SecretKey): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = key.encoded + "F2W_VAULT_VERIFIER_KEY".toByteArray(Charsets.UTF_8)
        val digest = md.digest(input)
        return bytesToHex(digest)
    }

    fun verifyKey(key: SecretKey, storedVerifierHash: String): Boolean {
        if (storedVerifierHash.isBlank()) return false
        return generateVerifierHash(key) == storedVerifierHash
    }

    /**
     * Encrypts source input stream to target output file using AES-256-GCM.
     * Writes 12-byte IV at the beginning of output file.
     */
    fun encryptStreamToFile(
        inputStream: InputStream,
        targetFile: File,
        key: SecretKey,
        onProgress: (Long) -> Unit = {}
    ): Long {
        val iv = generateRandomIv()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        targetFile.outputStream().use { fileOut ->
            // Write 12-byte IV header
            fileOut.write(iv)

            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalWritten = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    cipherOut.write(buffer, 0, bytesRead)
                    totalWritten += bytesRead
                    onProgress(totalWritten)
                }
                cipherOut.flush()
            }
        }
        return targetFile.length()
    }

    /**
     * Decrypts encrypted file (with 12-byte IV header) to output stream using AES-256-GCM.
     */
    fun decryptFileToStream(
        encryptedFile: File,
        outputStream: OutputStream,
        key: SecretKey
    ) {
        encryptedFile.inputStream().use { fileIn ->
            val iv = ByteArray(GCM_IV_LENGTH)
            val readIv = fileIn.read(iv)
            if (readIv != GCM_IV_LENGTH) {
                throw IllegalArgumentException("Invalid encrypted vault file header")
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            CipherInputStream(fileIn, cipher).use { cipherIn ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
        }
    }

    /**
     * Decrypts encrypted file to a temporary file (e.g. in cacheDir for playback).
     */
    fun decryptFileToTempFile(
        encryptedFile: File,
        tempFile: File,
        key: SecretKey
    ) {
        tempFile.outputStream().use { tempOut ->
            decryptFileToStream(encryptedFile, tempOut, key)
        }
    }

    fun encryptString(plainText: String, key: SecretKey): String {
        val iv = generateRandomIv()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + encryptedBytes
        return bytesToHex(combined)
    }

    fun decryptString(encryptedHex: String, key: SecretKey): String {
        val combined = hexToBytes(encryptedHex)
        if (combined.size < GCM_IV_LENGTH) throw IllegalArgumentException("Invalid encrypted payload")

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun hexToBytes(hex: String): ByteArray {
        if (hex.isBlank()) return byteArrayOf()
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
