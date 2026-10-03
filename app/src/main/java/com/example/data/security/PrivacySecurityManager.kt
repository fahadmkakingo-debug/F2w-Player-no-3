package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import java.security.MessageDigest

class PrivacySecurityManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("f2w_privacy_security", Context.MODE_PRIVATE)

    companion object {
        private const val ADMIN_PIN = "0089"
        private const val KEY_PIN_HASH = "user_pin_hash"
        private const val KEY_QUESTION = "security_question"
        private const val KEY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_FINGERPRINT_ENABLED = "fingerprint_auth_enabled"
        private const val SALT = "F2W_VAULT_SEC_SALT_8841"

        @Volatile
        private var INSTANCE: PrivacySecurityManager? = null

        fun getInstance(context: Context): PrivacySecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrivacySecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun hashWithSalt(value: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((SALT + value + SALT).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks if the user has already configured their Privacy PIN.
     */
    fun isPinConfigured(): Boolean {
        val pinHash = prefs.getString(KEY_PIN_HASH, null)
        return !pinHash.isNullOrBlank()
    }

    /**
     * Sets up the 4-digit PIN, security question, and hashed security answer.
     */
    fun setupPinAndSecurity(
        pin: String,
        question: String,
        answer: String,
        fingerprintEnabled: Boolean
    ) {
        prefs.edit()
            .putString(KEY_PIN_HASH, hashWithSalt(pin.trim()))
            .putString(KEY_QUESTION, question.trim())
            .putString(KEY_ANSWER_HASH, hashWithSalt(answer.trim().lowercase()))
            .putBoolean(KEY_FINGERPRINT_ENABLED, fingerprintEnabled)
            .apply()

        // Also initialize master encryption key and vault header in persistent storage
        val vaultManager = PrivacyVaultManager.getInstance(appContext)
        vaultManager.initializeVaultKey(pin, question, answer)
    }

    /**
     * Verifies the entered PIN against the user's saved PIN or the hidden Admin unlock PIN (0089).
     */
    fun verifyPin(input: String): Boolean {
        val trimmed = input.trim()
        val vaultManager = PrivacyVaultManager.getInstance(appContext)

        // Try unlocking vault with input PIN
        val vaultUnlocked = vaultManager.unlockVaultWithPinOrAnswer(trimmed, isAnswer = false)

        // Hidden separate Admin unlock PIN
        if (trimmed == ADMIN_PIN) {
            if (!vaultManager.isVaultUnlocked()) {
                val storedHash = prefs.getString(KEY_PIN_HASH, null)
                if (storedHash != null) {
                    vaultManager.unlockVaultWithPinOrAnswer(storedHash, isAnswer = false)
                }
            }
            return true
        }

        if (vaultUnlocked) {
            // Keep local prefs in sync
            prefs.edit().putString(KEY_PIN_HASH, hashWithSalt(trimmed)).apply()
            return true
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val matched = (storedHash == hashWithSalt(trimmed))
        if (matched) {
            vaultManager.unlockVaultWithPinOrAnswer(trimmed, isAnswer = false)
        }
        return matched
    }

    /**
     * Gets the configured security question (without exposing the answer).
     */
    fun getSecurityQuestion(): String {
        val vaultHeader = PrivacyVaultManager.getInstance(appContext).getVaultHeaderJson()
        if (vaultHeader != null && vaultHeader.has("question") && vaultHeader.getString("question").isNotBlank()) {
            return vaultHeader.getString("question")
        }
        return prefs.getString(KEY_QUESTION, "What was the name of your first school or pet?") ?: ""
    }

    /**
     * Verifies the answer provided for security PIN recovery.
     * The stored answer is never shown and only the hash is compared.
     */
    fun verifySecurityAnswer(inputAnswer: String): Boolean {
        val trimmed = inputAnswer.trim().lowercase()
        val vaultManager = PrivacyVaultManager.getInstance(appContext)

        val vaultUnlocked = vaultManager.unlockVaultWithPinOrAnswer(trimmed, isAnswer = true)
        if (vaultUnlocked) {
            return true
        }

        val storedHash = prefs.getString(KEY_ANSWER_HASH, null) ?: return false
        return storedHash == hashWithSalt(trimmed)
    }

    /**
     * Resets the user's 4-digit Privacy PIN.
     */
    fun resetPin(newPin: String) {
        val trimmed = newPin.trim()
        prefs.edit()
            .putString(KEY_PIN_HASH, hashWithSalt(trimmed))
            .apply()

        val vaultManager = PrivacyVaultManager.getInstance(appContext)
        val question = getSecurityQuestion()
        vaultManager.initializeVaultKey(trimmed, question, "")
    }

    /**
     * Updates security question and answer.
     */
    fun updateSecurityQuestion(question: String, newAnswer: String) {
        prefs.edit()
            .putString(KEY_QUESTION, question.trim())
            .putString(KEY_ANSWER_HASH, hashWithSalt(newAnswer.trim().lowercase()))
            .apply()
    }

    /**
     * Returns whether fingerprint unlock is currently enabled by user.
     */
    fun isFingerprintEnabled(): Boolean {
        return prefs.getBoolean(KEY_FINGERPRINT_ENABLED, false)
    }

    /**
     * Sets whether fingerprint unlock is enabled.
     */
    fun setFingerprintEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FINGERPRINT_ENABLED, enabled).apply()
    }

    /**
     * Detects whether the device hardware supports fingerprint or biometric authentication.
     */
    fun isDeviceBiometricSupported(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                if (bm != null) {
                    val status = bm.canAuthenticate(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
                    )
                    return status == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS ||
                            status == android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                }
            }
            val fmc = FingerprintManagerCompat.from(context)
            fmc.isHardwareDetected
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Prompts the user with standard Android BiometricPrompt authentication.
     */
    fun authenticateWithBiometrics(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
                    .setTitle("Privacy Vault Unlock")
                    .setSubtitle("Use your fingerprint to access private media")
                    .setDescription("Verify identity with your registered fingerprint sensor.")
                    .setNegativeButton("Enter PIN", executor) { _, _ -> onCancel() }
                    .build()

                val cancellationSignal = CancellationSignal()
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            val vaultManager = PrivacyVaultManager.getInstance(appContext)
                            val storedHash = prefs.getString(KEY_PIN_HASH, null)
                            if (storedHash != null) {
                                vaultManager.unlockVaultWithPinOrAnswer(storedHash, isAnswer = false)
                            }
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            if (errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                                errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                                errorCode == 10 || // User clicked negative button
                                errorCode == 13
                            ) {
                                onCancel()
                            } else {
                                onError(errString?.toString() ?: "Biometric authentication error")
                            }
                        }

                        override fun onAuthenticationFailed() {
                            onError("Fingerprint not recognized. Please try again.")
                        }
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Biometric prompt not available")
            }
        } else {
            onError("Biometric hardware requires Android 9+")
        }
    }
}
