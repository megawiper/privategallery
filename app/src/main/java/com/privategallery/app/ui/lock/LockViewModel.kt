package com.privategallery.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.core.security.AuthSessionManager
import com.privategallery.app.data.preferences.AuthMethod
import com.privategallery.app.data.preferences.SecurityPreferences
import com.privategallery.app.data.preferences.SecurityScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LockUiState(
    val authMethod: AuthMethod = AuthMethod.NONE,
    val biometricEnabled: Boolean = false,
    val errorMessage: String? = null,
    val isUnlocked: Boolean = false
)

/**
 * STEP 46/47 — NORMAL GALLERY LOCK / PRIVATE SECURITY CONFIGURATION
 *
 * The same screen and ViewModel serve both scopes — the SecurityScope
 * passed in determines which independent credential set (STEP 47) is
 * checked. Successful verification marks that scope's session unlocked
 * via AuthSessionManager (STEP 49), never the other scope.
 */
class LockViewModel(
    private val securityPreferences: SecurityPreferences,
    private val scope: SecurityScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val method = securityPreferences.observeAuthMethod(scope).first()
            val biometricEnabled = securityPreferences.observeBiometricEnabled(scope).first()
            _uiState.value = _uiState.value.copy(authMethod = method, biometricEnabled = biometricEnabled)
        }
    }

    fun verifyCredential(rawCredential: CharArray) {
        viewModelScope.launch {
            val success = securityPreferences.verifyCredential(scope, rawCredential)
            if (success) {
                AuthSessionManager.markUnlocked(scope)
                _uiState.value = _uiState.value.copy(isUnlocked = true, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Incorrect credentials")
            }
        }
    }

    fun onBiometricSuccess() {
        AuthSessionManager.markUnlocked(scope)
        _uiState.value = _uiState.value.copy(isUnlocked = true, errorMessage = null)
    }

    fun onBiometricError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
