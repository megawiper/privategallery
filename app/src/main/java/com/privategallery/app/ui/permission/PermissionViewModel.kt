package com.privategallery.app.ui.permission

import androidx.lifecycle.ViewModel
import com.privategallery.app.core.permissions.MediaPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * STEP 7 — MEDIA PERMISSION SYSTEM
 *
 * Holds UI state for the permission screen. The actual OS permission
 * request is triggered from the Composable via
 * rememberLauncherForActivityResult (system API, cannot be invoked from
 * a ViewModel) — this class only tracks the resulting status so the UI
 * can react (show rationale, partial-access notice, or proceed).
 */
class PermissionViewModel : ViewModel() {

    private val _status = MutableStateFlow<MediaPermissionStatus?>(null)
    val status: StateFlow<MediaPermissionStatus?> = _status.asStateFlow()

    private val _hasRequestedOnce = MutableStateFlow(false)
    val hasRequestedOnce: StateFlow<Boolean> = _hasRequestedOnce.asStateFlow()

    fun onPermissionResult(status: MediaPermissionStatus) {
        _hasRequestedOnce.value = true
        _status.value = status
    }
}
