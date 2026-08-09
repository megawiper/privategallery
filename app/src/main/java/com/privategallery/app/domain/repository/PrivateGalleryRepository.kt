package com.privategallery.app.domain.repository

import com.privategallery.app.domain.model.Album
import com.privategallery.app.domain.model.PrivateMedia
import kotlinx.coroutines.flow.Flow

sealed interface MoveToPrivateResult {
    data class Success(val secureId: String) : MoveToPrivateResult
    data class Failure(val reason: String) : MoveToPrivateResult
}

sealed interface MoveToNormalResult {
    data class Success(val newMediaUri: String) : MoveToNormalResult
    data class Failure(val reason: String) : MoveToNormalResult
}

/**
 * STEP 51-58 — the Private Gallery's own repository contract, entirely
 * separate from MediaRepository (normal gallery). Nothing here ever
 * touches MediaStore directly except at the exact hand-off points of
 * STEP 53 (move in) and STEP 60 (move back) — private storage itself
 * (STEP 52) is never reachable from MediaRepository.
 */
interface PrivateGalleryRepository {

    fun observeAll(): Flow<List<PrivateMedia>>
    fun observeImages(): Flow<List<PrivateMedia>>
    fun observeVideos(): Flow<List<PrivateMedia>>
    fun observeFavorites(): Flow<List<PrivateMedia>>

    suspend fun getById(secureId: String): PrivateMedia?

    /** STEP 57 — searches only the private database; never touches
     *  normal-gallery search (RULE J). */
    suspend fun search(query: String): List<PrivateMedia>

    suspend fun setFavorite(secureId: String, isFavorite: Boolean): Boolean

    /**
     * STEP 53 — MOVE NORMAL MEDIA INTO PRIVATE.
     * Encrypts and stores a copy first, verifies it, and only then
     * deletes the public MediaStore original — never the reverse order
     * (RULE G).
     */
    suspend fun moveFromNormalGallery(
        sourceUri: String,
        displayName: String,
        mimeType: String,
        isVideo: Boolean
    ): MoveToPrivateResult

    /**
     * STEP 54 — PRIVATE IMPORT.
     * Same encrypt-verify-store path as moveFromNormalGallery, but the
     * source is never deleted — import is additive, not a move, unlike
     * STEP 53's flow.
     */
    suspend fun importCopy(
        sourceUri: String,
        displayName: String,
        mimeType: String,
        isVideo: Boolean
    ): MoveToPrivateResult

    /**
     * STEP 60 — MOVE PRIVATE MEDIA BACK TO NORMAL.
     * Creates a verified public MediaStore copy first, and only then
     * removes the private encrypted copy (RULE G).
     */
    suspend fun moveToNormalGallery(secureId: String): MoveToNormalResult

    suspend fun deletePrivateMedia(secureId: String): Boolean

    fun observeAlbums(): Flow<List<Album>>
    suspend fun createAlbum(name: String, isLocked: Boolean): Album
    suspend fun renameAlbum(albumId: String, newName: String): Boolean
    suspend fun setAlbumLock(albumId: String, isLocked: Boolean, securityRef: String?): Boolean
    suspend fun deleteAlbum(albumId: String): Boolean
    suspend fun addToAlbum(albumId: String, secureIds: List<String>): Boolean
}
