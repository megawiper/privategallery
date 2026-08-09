package com.privategallery.app.core.media

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.privategallery.app.data.privatestore.PrivateStorageManager
import okio.Buffer
import java.io.ByteArrayOutputStream

/**
 * STEP 55 — PRIVATE MEDIA VIEWER
 *
 * Registered as a Coil component (STEP 61's app-wide ImageLoader setup)
 * so any AsyncImage/MediaThumbnail can transparently display a
 * "private://<secureId>" pseudo-URI (as produced by
 * PrivateMedia.toDisplayMediaItem, STEP 51) by decrypting it in memory
 * and handing Coil the plaintext bytes directly — nothing is ever
 * written to a temp file on disk, satisfying RULE H even for display.
 */
class PrivateMediaFetcher(
    private val secureId: String,
    private val storageManager: PrivateStorageManager,
    private val context: android.content.Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val output = ByteArrayOutputStream()
        val success = storageManager.readMedia(secureId, output)
        if (!success) {
            throw IllegalStateException("Could not decrypt private media: $secureId")
        }
        val buffer = Buffer().write(output.toByteArray())
        return SourceResult(
            source = ImageSource(buffer, context),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val storageManager: PrivateStorageManager) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!data.startsWith("private://")) return null
            val secureId = data.removePrefix("private://")
            return PrivateMediaFetcher(secureId, storageManager, options.context)
        }
    }
}
