package com.privategallery.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.privategallery.app.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * STEP 43 — DUPLICATE DETECTION ENGINE
 *
 * Two-tier detection, per the roadmap:
 *  1. Exact duplicates — same file hash (or same size+dimensions as a
 *     fast pre-filter before hashing, since hashing every file up front
 *     doesn't scale to "thousands" of items per STEP 63's test matrix).
 *  2. Perceptual similarity — a coarse difference-hash (dHash) grouping
 *     visually similar (not necessarily byte-identical) images.
 *
 * Per STEP 43's explicit rule, perceptually similar images are never
 * reported as guaranteed duplicates — they're returned in a separate,
 * lower-confidence group so the UI (STEP 44) can label them
 * differently and never auto-select them for deletion.
 */
data class DuplicateGroup(
    val id: String,
    val items: List<MediaItem>,
    val isExactMatch: Boolean
)

class DuplicateDetectionEngine(private val context: Context) {

    suspend fun findDuplicates(items: List<MediaItem>): List<DuplicateGroup> = withContext(Dispatchers.Default) {
        val exactGroups = findExactDuplicates(items)
        val itemsInExactGroups = exactGroups.flatMap { it.items }.map { it.id }.toHashSet()
        val remaining = items.filterNot { itemsInExactGroups.contains(it.id) }
        val perceptualGroups = findPerceptualDuplicates(remaining)
        exactGroups + perceptualGroups
    }

    /** Tier 1 — same size+dimensions pre-filter, then confirmed by hash. */
    private fun findExactDuplicates(items: List<MediaItem>): List<DuplicateGroup> {
        val bySizeAndDimensions = items.groupBy { Triple(it.sizeBytes, it.width, it.height) }
            .filter { it.value.size > 1 }

        val groups = mutableListOf<DuplicateGroup>()
        bySizeAndDimensions.values.forEach { candidates ->
            val byHash = candidates.groupBy { computeFileHash(Uri.parse(it.uriString)) }
                .filterKeys { it != null }
                .filter { it.value.size > 1 }
            byHash.forEach { (hash, group) ->
                groups += DuplicateGroup(
                    id = "exact_$hash",
                    items = group,
                    isExactMatch = true
                )
            }
        }
        return groups
    }

    /** Tier 2 — perceptual hash on a downscaled grayscale thumbnail. */
    private fun findPerceptualDuplicates(items: List<MediaItem>): List<DuplicateGroup> {
        val hashes = items.mapNotNull { item ->
            computePerceptualHash(Uri.parse(item.uriString))?.let { item to it }
        }

        val groups = mutableListOf<DuplicateGroup>()
        val used = mutableSetOf<Long>()

        for (i in hashes.indices) {
            val (itemA, hashA) = hashes[i]
            if (used.contains(itemA.id)) continue
            val cluster = mutableListOf(itemA)
            for (j in i + 1 until hashes.size) {
                val (itemB, hashB) = hashes[j]
                if (used.contains(itemB.id)) continue
                if (hammingDistance(hashA, hashB) <= PERCEPTUAL_SIMILARITY_THRESHOLD) {
                    cluster += itemB
                    used += itemB.id
                }
            }
            if (cluster.size > 1) {
                used += itemA.id
                groups += DuplicateGroup(
                    id = "similar_${itemA.id}",
                    items = cluster,
                    isExactMatch = false
                )
            }
        }
        return groups
    }

    private fun computeFileHash(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            } ?: return null
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun computePerceptualHash(uri: Uri): Long? {
        return try {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
            val small = Bitmap.createScaledBitmap(bitmap, HASH_SIZE + 1, HASH_SIZE, true)

            var hash = 0L
            var bit = 0
            for (y in 0 until HASH_SIZE) {
                for (x in 0 until HASH_SIZE) {
                    val left = grayscale(small.getPixel(x, y))
                    val right = grayscale(small.getPixel(x + 1, y))
                    if (left > right) hash = hash or (1L shl bit)
                    bit++
                }
            }
            hash
        } catch (e: Exception) {
            null
        }
    }

    private fun grayscale(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3
    }

    private fun hammingDistance(a: Long, b: Long): Int {
        return java.lang.Long.bitCount(a xor b)
    }

    private companion object {
        const val HASH_SIZE = 8
        const val PERCEPTUAL_SIMILARITY_THRESHOLD = 5
    }
}
