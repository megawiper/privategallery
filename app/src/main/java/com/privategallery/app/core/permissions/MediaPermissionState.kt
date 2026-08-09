package com.privategallery.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * STEP 7 — MEDIA PERMISSION SYSTEM
 *
 * Represents the four states the roadmap requires the app to handle:
 * full access, partial access, denied, and revoked-after-grant.
 * "Revoked" is distinguished from "denied" by the caller (it tracks
 * whether access was ever granted before, via preferences in STEP 62);
 * this object only reports the current OS-level truth.
 */
enum class MediaPermissionStatus {
    FULL_ACCESS,
    PARTIAL_ACCESS,
    DENIED
}

object MediaPermissionState {

    /** Permissions to request, resolved per API level. */
    fun requiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun currentStatus(context: Context): MediaPermissionStatus {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val fullImages = isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                val fullVideo = isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
                val partial = isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                when {
                    fullImages && fullVideo -> MediaPermissionStatus.FULL_ACCESS
                    partial -> MediaPermissionStatus.PARTIAL_ACCESS
                    else -> MediaPermissionStatus.DENIED
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val fullImages = isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                val fullVideo = isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
                if (fullImages && fullVideo) MediaPermissionStatus.FULL_ACCESS else MediaPermissionStatus.DENIED
            }
            else -> {
                if (isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    MediaPermissionStatus.FULL_ACCESS
                } else {
                    MediaPermissionStatus.DENIED
                }
            }
        }
    }

    private fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
