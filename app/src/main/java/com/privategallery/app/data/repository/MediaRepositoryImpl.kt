package com.privategallery.app.data.repository

import com.privategallery.app.data.database.dao.AlbumDao
import com.privategallery.app.data.database.dao.FavoriteDao
import com.privategallery.app.data.database.dao.TrashDao
import com.privategallery.app.data.database.entity.AlbumEntity
import com.privategallery.app.data.database.entity.AlbumMediaCrossRef
import com.privategallery.app.data.database.entity.FavoriteEntity
import com.privategallery.app.data.database.entity.TrashEntity
import com.privategallery.app.data.media.MediaStoreScanner
import com.privategallery.app.data.media.RawMediaRecord
import com.privategallery.app.domain.model.Album
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.model.MediaType
import com.privategallery.app.domain.repository.MediaFilter
import com.privategallery.app.domain.repository.MediaRepository
import com.privategallery.app.domain.repository.MediaSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * STEP 10 — MEDIA REPOSITORY (implementation)
 *
 * Fuses MediaStore scan results (STEP 8) with Room-tracked favorite and
 * album membership metadata (this step's database work) into the domain
 * MediaItem shape. No MediaStore or Room code exists in any ViewModel or
 * Composable — it all funnels through this single class, per RULE C.
 */
class MediaRepositoryImpl(
    private val context: android.content.Context,
    private val scanner: MediaStoreScanner,
    private val favoriteDao: FavoriteDao,
    private val albumDao: AlbumDao,
    private val trashDao: TrashDao
) : MediaRepository {

    /** In-memory cache of the last scan, refreshed via refresh(). */
    private val rawMediaCache = MutableStateFlow<List<RawMediaRecord>>(emptyList())

    override fun observeMedia(
        filter: MediaFilter,
        sortOrder: MediaSortOrder
    ): Flow<List<MediaItem>> {
        return combine(
            rawMediaCache,
            favoriteDao.observeFavoriteIds(),
            albumMediaIdsFlow(filter.albumId)
        ) { raw, favoriteIds, albumMediaIds ->
            val favoriteSet = favoriteIds.toHashSet()
            raw
                .asSequence()
                .filter { record ->
                    (filter.includeImages && !record.isVideo) ||
                        (filter.includeVideos && record.isVideo)
                }
                .filter { record ->
                    !filter.favoritesOnly || favoriteSet.contains(record.id)
                }
                .filter { record ->
                    albumMediaIds == null || albumMediaIds.contains(record.id)
                }
                .filter { record ->
                    val taken = record.dateTakenMillis ?: (record.dateAddedSeconds * 1000L)
                    (filter.dateRangeStartMillis == null || taken >= filter.dateRangeStartMillis) &&
                        (filter.dateRangeEndMillis == null || taken <= filter.dateRangeEndMillis)
                }
                .map { record -> record.toDomain(favoriteSet.contains(record.id), emptyList()) }
                .sortedWith(sortOrder.comparator())
                .toList()
        }
    }

    private fun albumMediaIdsFlow(albumId: String?): Flow<Set<Long>?> {
        return if (albumId == null) {
            kotlinx.coroutines.flow.flowOf(null)
        } else {
            albumDao.observeMediaIdsForAlbum(albumId).map { it.toHashSet() as Set<Long>? }
        }
    }

    override suspend fun getMediaById(id: Long): MediaItem? {
        val record = rawMediaCache.value.firstOrNull { it.id == id } ?: return null
        val isFavorite = favoriteDao.isFavorite(id)
        return record.toDomain(isFavorite, emptyList())
    }

    override fun observeFavoriteStatus(mediaId: Long): Flow<Boolean> {
        return favoriteDao.observeFavoriteIds().map { ids -> ids.contains(mediaId) }
    }

    override suspend fun setFavorite(mediaId: Long, isFavorite: Boolean): Boolean {
        if (isFavorite) {
            favoriteDao.addFavorite(FavoriteEntity(mediaId, System.currentTimeMillis()))
        } else {
            favoriteDao.removeFavorite(mediaId)
        }
        return true
    }

    override suspend fun setFavorites(mediaIds: List<Long>, isFavorite: Boolean): Boolean {
        if (mediaIds.isEmpty()) return false
        if (isFavorite) {
            val now = System.currentTimeMillis()
            mediaIds.forEach { favoriteDao.addFavorite(FavoriteEntity(it, now)) }
        } else {
            favoriteDao.removeFavorites(mediaIds)
        }
        return true
    }

    override suspend fun searchMedia(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val lowered = query.lowercase().trim()
        val monthNames = listOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        )
        val yearMatch = lowered.toIntOrNull()?.takeIf { it in 1990..2100 }
        val monthMatch = monthNames.firstOrNull { it.startsWith(lowered) || lowered.startsWith(it) }

        return rawMediaCache.value
            .filter { record ->
                val nameMatches = record.displayName.lowercase().contains(lowered)
                val pathMatches = record.relativePath?.lowercase()?.contains(lowered) == true
                val dateMillis = record.dateTakenMillis ?: (record.dateAddedSeconds * 1000L)
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
                val yearMatches = yearMatch != null && calendar.get(java.util.Calendar.YEAR) == yearMatch
                val monthMatches = monthMatch != null &&
                    monthNames[calendar.get(java.util.Calendar.MONTH)] == monthMatch
                nameMatches || pathMatches || yearMatches || monthMatches
            }
            .map { it.toDomain(favoriteDao.isFavorite(it.id), emptyList()) }
    }

    override suspend fun refresh() {
        val images = scanner.scanImages()
        val videos = scanner.scanVideos()
        rawMediaCache.value = images + videos
    }

    override suspend fun moveToTrash(mediaIds: List<Long>): Boolean {
        val nowMillis = System.currentTimeMillis()
        val expiresAtMillis = nowMillis + TRASH_RETENTION_MILLIS
        val entities = mediaIds.mapNotNull { id ->
            val record = rawMediaCache.value.firstOrNull { it.id == id } ?: return@mapNotNull null
            TrashEntity(
                id = UUID.randomUUID().toString(),
                originalMediaId = record.id,
                originalUriString = record.uri.toString(),
                trashedUriString = record.uri.toString(),
                displayName = record.displayName,
                isVideo = record.isVideo,
                deletedAtMillis = nowMillis,
                expiresAtMillis = expiresAtMillis
            )
        }
        if (entities.isEmpty()) return false
        trashDao.insertAll(entities)
        return true
    }

    override suspend fun restoreFromTrash(trashIds: List<String>): Boolean {
        val entries = trashDao.getByIds(trashIds)
        if (entries.isEmpty()) return false
        trashDao.deleteByIds(trashIds)
        return true
    }

    override fun observeTrash(): Flow<List<com.privategallery.app.domain.model.TrashItem>> {
        return trashDao.observeTrash().map { entities ->
            entities.map { entity ->
                com.privategallery.app.domain.model.TrashItem(
                    id = entity.id,
                    originalMediaId = entity.originalMediaId,
                    originalUriString = entity.originalUriString,
                    trashedUriString = entity.trashedUriString,
                    displayName = entity.displayName,
                    type = if (entity.isVideo) MediaType.VIDEO else MediaType.IMAGE,
                    deletedAtMillis = entity.deletedAtMillis,
                    expiresAtMillis = entity.expiresAtMillis
                )
            }
        }
    }

    override suspend fun permanentlyDelete(trashIds: List<String>): Boolean {
        val entries = trashDao.getByIds(trashIds)
        if (entries.isEmpty()) return false
        entries.forEach { entry ->
            try {
                context.contentResolver.delete(android.net.Uri.parse(entry.trashedUriString), null, null)
            } catch (e: Exception) {
                // If deletion of the underlying file fails (e.g. already
                // gone), still clear the trash row below rather than
                // leaving a permanently stuck entry.
            }
        }
        trashDao.deleteByIds(trashIds)
        return true
    }

    override fun observeAlbums(): Flow<List<Album>> {
        return albumDao.observeAlbums().map { entities ->
            entities.map { entity -> entity.toDomain(mediaCount = 0) }
        }
    }

    override suspend fun createAlbum(name: String, isLocked: Boolean): Album {
        val entity = AlbumEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            isLocked = isLocked,
            lockSecurityRef = null,
            coverMediaId = null,
            createdAtMillis = System.currentTimeMillis()
        )
        albumDao.insertAlbum(entity)
        return entity.toDomain(mediaCount = 0)
    }

    override suspend fun renameAlbum(albumId: String, newName: String): Boolean {
        val existing = albumDao.getAlbum(albumId) ?: return false
        if (newName.isBlank()) return false
        albumDao.updateAlbum(existing.copy(name = newName.trim()))
        return true
    }

    override suspend fun setAlbumLock(albumId: String, isLocked: Boolean, securityRef: String?): Boolean {
        val existing = albumDao.getAlbum(albumId) ?: return false
        albumDao.updateAlbum(existing.copy(isLocked = isLocked, lockSecurityRef = securityRef))
        return true
    }

    override suspend fun addMediaToAlbum(albumId: String, mediaIds: List<Long>): Boolean {
        if (mediaIds.isEmpty()) return false
        albumDao.addMediaToAlbum(mediaIds.map { AlbumMediaCrossRef(albumId, it) })
        return true
    }

    override suspend fun removeMediaFromAlbum(albumId: String, mediaIds: List<Long>): Boolean {
        if (mediaIds.isEmpty()) return false
        albumDao.removeMediaFromAlbum(albumId, mediaIds)
        return true
    }

    override suspend fun deleteAlbum(albumId: String, deleteMedia: Boolean): Boolean {
        // Per STEP 19: deleting an album never deletes underlying media
        // unless deleteMedia is explicitly true, which is handled by the
        // caller issuing a separate moveToTrash() for those media IDs
        // before calling this — this method only removes the album shell.
        albumDao.clearAlbumMedia(albumId)
        albumDao.deleteAlbum(albumId)
        return true
    }

    private fun MediaSortOrder.comparator(): Comparator<MediaItem> = when (this) {
        MediaSortOrder.DATE_NEWEST_FIRST -> compareByDescending { it.dateTakenMillis ?: it.dateAddedMillis }
        MediaSortOrder.DATE_OLDEST_FIRST -> compareBy { it.dateTakenMillis ?: it.dateAddedMillis }
        MediaSortOrder.NAME_A_Z -> compareBy { it.displayName.lowercase() }
        MediaSortOrder.NAME_Z_A -> compareByDescending { it.displayName.lowercase() }
        MediaSortOrder.SIZE_LARGEST_FIRST -> compareByDescending { it.sizeBytes }
        MediaSortOrder.SIZE_SMALLEST_FIRST -> compareBy { it.sizeBytes }
    }

    private companion object {
        const val TRASH_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days default
    }
}
