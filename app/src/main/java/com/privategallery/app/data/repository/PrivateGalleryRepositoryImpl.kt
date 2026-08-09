package com.privategallery.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.privategallery.app.data.database.dao.PrivateMediaDao
import com.privategallery.app.data.database.entity.PrivateAlbumEntity
import com.privategallery.app.data.database.entity.PrivateMediaEntity
import com.privategallery.app.data.privatestore.PrivateStorageManager
import com.privategallery.app.domain.model.Album
import com.privategallery.app.domain.model.AlbumType
import com.privategallery.app.domain.model.MediaType
import com.privategallery.app.domain.model.PrivateMedia
import com.privategallery.app.domain.repository.MoveToNormalResult
import com.privategallery.app.domain.repository.MoveToPrivateResult
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * STEP 51-58 — implementation backing the Private Gallery. Every read
 * goes through PrivateMediaDao (the SQLCipher-encrypted database, STEP
 * 52) and every file operation goes through PrivateStorageManager
 * (encrypted app-private storage, STEP 52) — this class never opens a
 * public file path directly except at the two explicit MediaStore
 * hand-off points (moveFromNormalGallery / moveToNormalGallery).
 */
class PrivateGalleryRepositoryImpl(
    private val context: Context,
    private val privateMediaDao: PrivateMediaDao,
    private val storageManager: PrivateStorageManager
) : PrivateGalleryRepository {

    override fun observeAll(): Flow<List<PrivateMedia>> = privateMediaDao.observeAll().map { it.toDomainList() }
    override fun observeImages(): Flow<List<PrivateMedia>> =
        privateMediaDao.observeImages().map { it.toDomainList() }
    override fun observeVideos(): Flow<List<PrivateMedia>> =
        privateMediaDao.observeVideos().map { it.toDomainList() }
    override fun observeFavorites(): Flow<List<PrivateMedia>> =
        privateMediaDao.observeFavorites().map { it.toDomainList() }

    override suspend fun getById(secureId: String): PrivateMedia? {
        return privateMediaDao.getById(secureId)?.toDomain()
    }

    override suspend fun search(query: String): List<PrivateMedia> {
        if (query.isBlank()) return emptyList()
        val lowered = query.lowercase().trim()

        val monthNames = listOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        )
        val yearMatch = lowered.toIntOrNull()?.takeIf { it in 1990..2100 }
        val monthMatch = monthNames.firstOrNull { it.startsWith(lowered) || lowered.startsWith(it) }
        val typeMatch: Boolean? = when (lowered) {
            "video", "videos" -> true
            "image", "images", "photo", "photos" -> false
            else -> null
        }

        // Plain name match always applies (STEP 57's baseline case).
        val nameMatches = privateMediaDao.search(query).map { it.secureId }.toHashSet()

        // Date/month/year/type require scanning metadata, done in Kotlin
        // over the private set rather than a second SQL query per
        // signal — the private library is expected to be small relative
        // to the normal gallery, so this stays well within budget.
        val needsMetadataScan = yearMatch != null || monthMatch != null || typeMatch != null
        val metadataMatches: Set<String> = if (needsMetadataScan) {
            privateMediaDao.getAllOnce().filter { entity ->
                val dateMillis = entity.dateTakenMillis ?: entity.dateAddedMillis
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
                val yearOk = yearMatch != null && calendar.get(java.util.Calendar.YEAR) == yearMatch
                val monthOk = monthMatch != null &&
                    monthNames[calendar.get(java.util.Calendar.MONTH)] == monthMatch
                val typeOk = typeMatch != null && entity.isVideo == typeMatch
                yearOk || monthOk || typeOk
            }.map { it.secureId }.toHashSet()
        } else {
            emptySet()
        }

        val matchedIds = nameMatches + metadataMatches
        if (matchedIds.isEmpty()) return emptyList()

        return privateMediaDao.getAllOnce()
            .filter { matchedIds.contains(it.secureId) }
            .map { it.toDomain() }
    }

    override suspend fun setFavorite(secureId: String, isFavorite: Boolean): Boolean {
        val entity = privateMediaDao.getById(secureId) ?: return false
        privateMediaDao.update(entity.copy(isFavorite = isFavorite))
        return true
    }

    override suspend fun moveFromNormalGallery(
        sourceUri: String,
        displayName: String,
        mimeType: String,
        isVideo: Boolean
    ): MoveToPrivateResult = withContext(Dispatchers.IO) {
        val storeResult = storeEncryptedCopy(sourceUri, displayName, isVideo)
        if (storeResult is MoveToPrivateResult.Success) {
            // Only now — after the private copy is confirmed stored and
            // the database row is committed — remove the public original.
            try {
                context.contentResolver.delete(Uri.parse(sourceUri), null, null)
            } catch (e: Exception) {
                // The private copy already exists and is recorded; if the
                // public delete fails, the media simply appears in both
                // places until the user retries deletion — RULE G forbids
                // data loss, not this less-severe failure mode.
            }
        }
        storeResult
    }

    override suspend fun importCopy(
        sourceUri: String,
        displayName: String,
        mimeType: String,
        isVideo: Boolean
    ): MoveToPrivateResult = withContext(Dispatchers.IO) {
        // STEP 54 — additive import: same storage path, source untouched.
        storeEncryptedCopy(sourceUri, displayName, isVideo)
    }

    private suspend fun storeEncryptedCopy(
        sourceUri: String,
        displayName: String,
        isVideo: Boolean
    ): MoveToPrivateResult {
        val uri = Uri.parse(sourceUri)

        val input = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        } ?: return MoveToPrivateResult.Failure("Could not read source media")

        val reference = input.use { storageManager.storeMedia(it) }
            ?: return MoveToPrivateResult.Failure("Failed to write encrypted copy")

        val verified = storageManager.mediaExists(reference)
        if (!verified) {
            storageManager.deleteMedia(reference)
            return MoveToPrivateResult.Failure("Verification failed after encryption")
        }

        val metadata = readBasicMetadata(uri)
        val secureId = UUID.randomUUID().toString()
        val entity = PrivateMediaEntity(
            secureId = secureId,
            encryptedStorageReference = reference,
            encryptedThumbnailReference = null,
            isVideo = isVideo,
            originalDisplayName = displayName,
            dateAddedMillis = System.currentTimeMillis(),
            dateTakenMillis = null,
            sizeBytes = metadata.sizeBytes,
            width = metadata.width,
            height = metadata.height,
            durationMillis = metadata.durationMillis,
            albumId = null,
            isFavorite = false
        )
        privateMediaDao.insert(entity)

        return MoveToPrivateResult.Success(secureId)
    }

    /**
     * STEP 60 exact flow: create normal MediaStore copy -> verify ->
     * remove private encrypted copy -> update private database. If the
     * public write fails, the private copy is kept fully untouched.
     */
    override suspend fun moveToNormalGallery(secureId: String): MoveToNormalResult = withContext(Dispatchers.IO) {
        val entity = privateMediaDao.getById(secureId)
            ?: return@withContext MoveToNormalResult.Failure("Private media not found")

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, entity.originalDisplayName)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (entity.isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val newUri = context.contentResolver.insert(collection, values)
            ?: return@withContext MoveToNormalResult.Failure("Could not create destination entry")

        val writeSucceeded = try {
            context.contentResolver.openOutputStream(newUri)?.use { output ->
                storageManager.readMedia(entity.encryptedStorageReference, output)
            } ?: false
        } catch (e: Exception) {
            false
        }

        if (!writeSucceeded) {
            context.contentResolver.delete(newUri, null, null)
            return@withContext MoveToNormalResult.Failure("Failed to decrypt and write to gallery")
        }

        val pendingValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(newUri, pendingValues, null, null)

        val verified = try {
            context.contentResolver.openInputStream(newUri)?.use { it.read() != -1 } ?: false
        } catch (e: Exception) {
            false
        }
        if (!verified) {
            context.contentResolver.delete(newUri, null, null)
            return@withContext MoveToNormalResult.Failure("Verification failed after restore")
        }

        storageManager.deleteMedia(entity.encryptedStorageReference)
        privateMediaDao.deleteById(secureId)

        MoveToNormalResult.Success(newUri.toString())
    }

    override suspend fun deletePrivateMedia(secureId: String): Boolean {
        val entity = privateMediaDao.getById(secureId) ?: return false
        val deleted = storageManager.deleteMedia(entity.encryptedStorageReference)
        if (deleted) privateMediaDao.deleteById(secureId)
        return deleted
    }

    override fun observeAlbums(): Flow<List<Album>> {
        return privateMediaDao.observeAlbums().map { entities ->
            entities.map { entity ->
                Album(
                    id = entity.id,
                    name = entity.name,
                    type = AlbumType.CUSTOM,
                    coverMediaUriString = null,
                    isLocked = entity.isLocked,
                    createdAtMillis = entity.createdAtMillis
                )
            }
        }
    }

    override suspend fun createAlbum(name: String, isLocked: Boolean): Album {
        val entity = PrivateAlbumEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            isLocked = isLocked,
            lockSecurityRef = null,
            coverSecureId = null,
            createdAtMillis = System.currentTimeMillis()
        )
        privateMediaDao.insertAlbum(entity)
        return Album(
            id = entity.id,
            name = entity.name,
            type = AlbumType.CUSTOM,
            coverMediaUriString = null,
            isLocked = entity.isLocked,
            createdAtMillis = entity.createdAtMillis
        )
    }

    /** STEP 59 — rename works at any time, locked or not. */
    override suspend fun renameAlbum(albumId: String, newName: String): Boolean {
        if (newName.isBlank()) return false
        val existing = privateMediaDao.getAlbum(albumId) ?: return false
        privateMediaDao.updateAlbum(existing.copy(name = newName.trim()))
        return true
    }

    /**
     * STEP 59 — an album can be locked or unlocked after creation.
     * securityRef is null to remove a lock, or a stored reference the
     * caller obtained from SecurityPreferences after the user configured
     * a credential for this specific album.
     */
    override suspend fun setAlbumLock(albumId: String, isLocked: Boolean, securityRef: String?): Boolean {
        val existing = privateMediaDao.getAlbum(albumId) ?: return false
        privateMediaDao.updateAlbum(existing.copy(isLocked = isLocked, lockSecurityRef = securityRef))
        return true
    }

    override suspend fun deleteAlbum(albumId: String): Boolean {
        privateMediaDao.deleteAlbum(albumId)
        return true
    }

    override suspend fun addToAlbum(albumId: String, secureIds: List<String>): Boolean {
        if (secureIds.isEmpty()) return false
        secureIds.forEach { id ->
            val entity = privateMediaDao.getById(id) ?: return@forEach
            privateMediaDao.update(entity.copy(albumId = albumId))
        }
        return true
    }

    private data class BasicMetadata(
        val width: Int,
        val height: Int,
        val sizeBytes: Long,
        val durationMillis: Long?
    )

    private fun readBasicMetadata(uri: Uri): BasicMetadata {
        var width = 0
        var height = 0
        var size = 0L
        var duration: Long? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                    val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                    val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                    if (sizeCol >= 0) size = cursor.getLong(sizeCol)
                    if (widthCol >= 0) width = cursor.getInt(widthCol)
                    if (heightCol >= 0) height = cursor.getInt(heightCol)
                    if (durationCol >= 0 && !cursor.isNull(durationCol)) duration = cursor.getLong(durationCol)
                }
            }
        } catch (e: Exception) {
            // Metadata is best-effort; the move itself is unaffected.
        }
        return BasicMetadata(width, height, size, duration)
    }

    private fun List<PrivateMediaEntity>.toDomainList(): List<PrivateMedia> = map { it.toDomain() }

    private fun PrivateMediaEntity.toDomain(): PrivateMedia = PrivateMedia(
        secureId = secureId,
        encryptedStorageReference = encryptedStorageReference,
        type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
        originalDisplayName = originalDisplayName,
        dateAddedMillis = dateAddedMillis,
        dateTakenMillis = dateTakenMillis,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        durationMillis = durationMillis,
        albumId = albumId,
        isFavorite = isFavorite
    )
}
