package com.privategallery.app.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * STEP 33 — SHARING SYSTEM (normal media)
 *
 * Normal media is shared via the standard Android Sharesheet, using the
 * already-public content:// URIs from MediaStore (STEP 8) — no copying
 * or temp files needed since these are already shareable content URIs.
 * Private media sharing (also STEP 33) is a controlled, separate flow —
 * see PrivateShareHelper in data/private (STEP 52/56) which stages a
 * temporary decrypted copy rather than ever exposing the permanent
 * private storage path, per RULE H.
 */
object ShareHelper {

    fun shareSingle(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareMultiple(context: Context, uris: List<Uri>, mimeType: String) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            shareSingle(context, uris.first(), mimeType)
            return
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    /**
     * Resolves a mixed-mime-type sharesheet type. When items differ
     * (some image, some video) "*/*" is used so the sharesheet still
     * offers every valid target instead of filtering incorrectly.
     */
    fun resolveShareMimeType(mimeTypes: List<String>): String {
        val distinct = mimeTypes.map { it.substringBefore("/") }.distinct()
        return if (distinct.size == 1) "${distinct.first()}/*" else "*/*"
    }
}
