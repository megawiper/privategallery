package com.privategallery.app.domain.repository

import com.privategallery.app.domain.model.Album
import com.privategallery.app.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * STEP 10 — MEDIA REPOSITORY (contract)
 *
 * Single source of truth for normal-gallery media operations. ViewModels
 * depend on this interface, never on MediaStoreScanner or Room directly.
 * The implementation (data/repository, next) fuses MediaStore data
 * (STEP 8) with Room-tracked metadata (favorites/albums, STEP 20 & 17).
 */
interface MediaRepository {

    fun observeMedia(
        filter: MediaFilter = MediaFilter(),
        sortOrder: MediaSortOrder = MediaSortOrder.DATE_NEWEST_FIRST
    ): Flow<List<MediaItem>>

    suspend fun getMediaById(id: Long): MediaItem?

    fun observeFavoriteStatus(mediaId: Long): kotlinx.coroutines.flow.Flow<Boolean>

    suspend fun setFavorite(mediaId: Long, isFavorite: Boolean): Boolean

    suspend fun setFavorites(mediaIds: List<Long>, isFavorite: Boolean): Boolean

    suspend fun searchMedia(query: String): List<MediaItem>

    suspend fun refresh()

    suspend fun moveToTrash(mediaIds: List<Long>): Boolean

    fun observeTrash(): kotlinx.coroutines.flow.Flow<List<com.privategallery.app.domain.model.TrashItem>>

    suspend fun restoreFromTrash(trashIds: List<String>): Boolean

    suspend fun permanentlyDelete(trashIds: List<String>): Boolean

    fun observeAlbums(): Flow<List<Album>>

    suspend fun createAlbum(name: String, isLocked: Boolean): Album

    suspend fun renameAlbum(albumId: String, newName: String): Boolean

    suspend fun setAlbumLock(albumId: String, isLocked: Boolean, securityRef: String?): Boolean

    suspend fun addMediaToAlbum(albumId: String, mediaIds: List<Long>): Boolean

    suspend fun removeMediaFromAlbum(albumId: String, mediaIds: List<Long>): Boolean

    suspend fun deleteAlbum(albumId: String, deleteMedia: Boolean): Boolean
}
