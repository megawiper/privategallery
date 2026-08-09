package com.privategallery.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.core.security.AuthSessionManager
import com.privategallery.app.data.preferences.AppPreferences
import com.privategallery.app.data.preferences.AuthMethod
import com.privategallery.app.data.preferences.AutoLockDuration
import com.privategallery.app.data.preferences.GridSizePreference
import com.privategallery.app.data.preferences.SecurityPreferences
import com.privategallery.app.data.preferences.SecurityScope
import com.privategallery.app.data.preferences.SortOrderPreference
import com.privategallery.app.data.preferences.ThemePreference
import com.privategallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val gridSize: GridSizePreference = GridSizePreference.THREE,
    val sortOrder: SortOrderPreference = SortOrderPreference.DATE_NEWEST,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val showFileNames: Boolean = false,
    val showVideoDuration: Boolean = true,
    val animationsEnabled: Boolean = true,
    val trashRetentionDays: Int = 30,
    val replaceOriginalOnEdit: Boolean = false,
    val preserveMetadata: Boolean = true,
    val screenshotProtectionEnabled: Boolean = true
)

/**
 * STEP 62 — COMPLETE SETTINGS
 *
 * A single ViewModel spanning every settings section the roadmap lists
 * (Gallery, Security, Albums, Media, Trash, Editing, Appearance, About)
 * so Settings reads as one coherent screen rather than N disconnected
 * ones. Security-section writes go through SecurityPreferences (STEP
 * 45) and everything else through AppPreferences (STEP 49) — the two
 * remain separate files on disk even though this ViewModel presents
 * them together in one UI.
 */
class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val securityPreferences: SecurityPreferences,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val uiState = combine(
        appPreferences.gridSize,
        appPreferences.sortOrder,
        appPreferences.theme,
        appPreferences.showFileNames,
        appPreferences.showVideoDuration,
        appPreferences.animationsEnabled,
        appPreferences.trashRetentionDays,
        appPreferences.replaceOriginalOnEdit,
        appPreferences.preserveMetadata,
        appPreferences.screenshotProtectionEnabled
    ) { values ->
        SettingsUiState(
            gridSize = values[0] as GridSizePreference,
            sortOrder = values[1] as SortOrderPreference,
            theme = values[2] as ThemePreference,
            showFileNames = values[3] as Boolean,
            showVideoDuration = values[4] as Boolean,
            animationsEnabled = values[5] as Boolean,
            trashRetentionDays = values[6] as Int,
            replaceOriginalOnEdit = values[7] as Boolean,
            preserveMetadata = values[8] as Boolean,
            screenshotProtectionEnabled = values[9] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val normalAuthMethod = securityPreferences.observeAuthMethod(SecurityScope.NORMAL_GALLERY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthMethod.NONE)
    val privateAuthMethod = securityPreferences.observeAuthMethod(SecurityScope.PRIVATE_GALLERY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthMethod.NONE)
    val normalAutoLock = securityPreferences.observeAutoLockDuration(SecurityScope.NORMAL_GALLERY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoLockDuration.NEVER)
    val privateAutoLock = securityPreferences.observeAutoLockDuration(SecurityScope.PRIVATE_GALLERY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoLockDuration.IMMEDIATELY)

    fun setGridSize(value: GridSizePreference) = launchSet { appPreferences.setGridSize(value) }
    fun setSortOrder(value: SortOrderPreference) = launchSet { appPreferences.setSortOrder(value) }
    fun setTheme(value: ThemePreference) = launchSet { appPreferences.setTheme(value) }
    fun setShowFileNames(value: Boolean) = launchSet { appPreferences.setShowFileNames(value) }
    fun setShowVideoDuration(value: Boolean) = launchSet { appPreferences.setShowVideoDuration(value) }
    fun setAnimationsEnabled(value: Boolean) = launchSet { appPreferences.setAnimationsEnabled(value) }
    fun setTrashRetentionDays(days: Int) = launchSet { appPreferences.setTrashRetentionDays(days) }
    fun setReplaceOriginalOnEdit(value: Boolean) = launchSet { appPreferences.setReplaceOriginalOnEdit(value) }
    fun setPreserveMetadata(value: Boolean) = launchSet { appPreferences.setPreserveMetadata(value) }
    fun setScreenshotProtectionEnabled(value: Boolean) =
        launchSet { appPreferences.setScreenshotProtectionEnabled(value) }

    fun setCredential(scope: SecurityScope, method: AuthMethod, rawCredential: CharArray) = launchSet {
        securityPreferences.setCredential(scope, method, rawCredential)
    }

    fun setBiometricEnabled(scope: SecurityScope, enabled: Boolean) = launchSet {
        securityPreferences.setBiometricEnabled(scope, enabled)
    }

    fun setAutoLockDuration(scope: SecurityScope, duration: AutoLockDuration) = launchSet {
        securityPreferences.setAutoLockDuration(scope, duration)
    }

    fun clearLock(scope: SecurityScope) = launchSet {
        securityPreferences.clearCredential(scope)
    }

    /** STEP 62/49 — "Lock Now" from Settings, for whichever scope is active. */
    fun lockNow(scope: SecurityScope) {
        AuthSessionManager.lock(scope)
    }

    fun refreshMediaLibrary() {
        viewModelScope.launch { mediaRepository.refresh() }
    }

    private fun launchSet(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
