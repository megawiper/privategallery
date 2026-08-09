package com.privategallery.app.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
    data class Error(val message: String) : BiometricAuthResult
}

/**
 * STEP 48 — BIOMETRIC SYSTEM
 *
 * A thin wrapper over androidx.biometric.BiometricPrompt. The OS alone
 * decides which enrolled fingerprint/face matched — this class only
 * ever receives a pass/fail callback and never queries or stores which
 * biometric identity was used, per STEP 48's explicit rule.
 */
object BiometricAuthHelper {

    fun isBiometricAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onResult(BiometricAuthResult.Cancelled)
                } else {
                    onResult(BiometricAuthResult.Error(errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                // Single failed attempt; BiometricPrompt keeps the sheet
                // open for retry, so no terminal callback is needed here.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
