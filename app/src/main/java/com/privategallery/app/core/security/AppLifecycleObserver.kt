package com.privategallery.app.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.privategallery.app.data.preferences.SecurityScope

/**
 * STEP 49 — AUTO-LOCK
 *
 * Registered once against ProcessLifecycleOwner (STEP 61 wires this in
 * the Application class) so it fires exactly once per whole-app
 * foreground/background transition, not per-Activity. Recording the
 * backgrounded timestamp here is what lets LockScreen (STEP 46/47)
 * later decide whether to require re-authentication.
 */
class AppLifecycleObserver : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        val now = System.currentTimeMillis()
        SecurityScope.values().forEach { scope ->
            if (AuthSessionManager.isUnlocked(scope)) {
                AuthSessionManager.onBackgrounded(scope, now)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // The actual re-lock decision needs each scope's configured
        // auto-lock duration (DataStore, STEP 45/47), which requires a
        // suspend read. That check runs from GalleryNavHost's startup
        // logic (STEP 61) via AuthSessionManager.shouldRelockOnForeground,
        // keeping this observer itself free of async work.
    }
}
