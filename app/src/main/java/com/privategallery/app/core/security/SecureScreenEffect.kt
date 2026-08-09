package com.privategallery.app.core.security

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * STEP 50 — PRIVATE SCREEN PROTECTION
 *
 * Call this once from the top of any private-gallery Composable (Private
 * Gallery Home STEP 51, Private Viewer STEP 55, Private Video STEP 56).
 * FLAG_SECURE is enabled on composition and cleared on disposal, so
 * leaving the private area — including via quick-switch back to Normal
 * Gallery (STEP 61) — always restores normal screenshot behavior rather
 * than leaking the flag into unrelated screens.
 */
@Composable
fun SecureScreenEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.let { SecureScreenController.enable(it) }
        onDispose {
            activity?.let { SecureScreenController.disable(it) }
        }
    }
}
