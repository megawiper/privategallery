package com.privategallery.app.core.security

import android.app.Activity
import android.view.WindowManager

/**
 * STEP 50 — PRIVATE SCREEN PROTECTION
 *
 * FLAG_SECURE does two things at once: it blocks screenshots/screen
 * recording of the current window, and it blocks that window's content
 * from appearing in the recent-apps (overview) thumbnail — the OS shows
 * a blank preview instead. Applied only while Private Gallery screens
 * are visible (STEP 55/56/51), never globally, so normal-gallery
 * screenshots keep working elsewhere in the app.
 */
object SecureScreenController {

    fun enable(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun disable(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
