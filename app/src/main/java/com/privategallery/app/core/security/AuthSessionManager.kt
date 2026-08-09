package com.privategallery.app.core.security

import com.privategallery.app.data.preferences.SecurityScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * STEP 46/47/49 — NORMAL GALLERY LOCK / PRIVATE SECURITY CONFIGURATION /
 * AUTO-LOCK.
 *
 * A single in-memory session tracker, scoped independently per
 * SecurityScope so unlocking Normal Gallery never unlocks Private
 * Gallery and vice versa (STEP 47's core requirement). Backgrounding
 * timestamps feed the auto-lock check in STEP 49; this class has no
 * Android lifecycle dependency itself — the Application-level
 * lifecycle observer (STEP 49/61) calls into it.
 */
object AuthSessionManager {

    private val unlockedScopes = mutableMapOf<SecurityScope, Boolean>()
    private val lastBackgroundedAtMillis = mutableMapOf<SecurityScope, Long>()

    private val _isPrivateUnlocked = MutableStateFlow(false)
    val isPrivateUnlocked: StateFlow<Boolean> = _isPrivateUnlocked.asStateFlow()

    fun isUnlocked(scope: SecurityScope): Boolean = unlockedScopes[scope] == true

    fun markUnlocked(scope: SecurityScope) {
        unlockedScopes[scope] = true
        if (scope == SecurityScope.PRIVATE_GALLERY) _isPrivateUnlocked.value = true
    }

    /** STEP 61 — Lock Now / quick switch: clears session state immediately. */
    fun lock(scope: SecurityScope) {
        unlockedScopes[scope] = false
        if (scope == SecurityScope.PRIVATE_GALLERY) _isPrivateUnlocked.value = false
    }

    fun lockAll() {
        SecurityScope.values().forEach { lock(it) }
    }

    /** STEP 49 — called when the app moves to background. */
    fun onBackgrounded(scope: SecurityScope, atMillis: Long) {
        lastBackgroundedAtMillis[scope] = atMillis
    }

    /**
     * STEP 49 — called when the app returns to foreground. Returns true
     * if the scope should now require re-authentication given its
     * configured auto-lock duration.
     */
    fun shouldRelockOnForeground(
        scope: SecurityScope,
        nowMillis: Long,
        autoLockMillis: Long
    ): Boolean {
        val backgroundedAt = lastBackgroundedAtMillis[scope] ?: return false
        if (autoLockMillis == Long.MAX_VALUE) return false
        val elapsed = nowMillis - backgroundedAt
        return elapsed >= autoLockMillis
    }
}
