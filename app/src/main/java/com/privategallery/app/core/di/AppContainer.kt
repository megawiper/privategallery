package com.privategallery.app.core.di

import android.content.Context
import com.privategallery.app.core.media.DuplicateDetectionEngine
import com.privategallery.app.data.database.PrivateGalleryDatabase
import com.privategallery.app.data.database.PrivateGalleryEncryptedDatabase
import com.privategallery.app.data.media.MediaStoreScanner
import com.privategallery.app.data.preferences.AppPreferences
import com.privategallery.app.data.preferences.SecurityPreferences
import com.privategallery.app.data.privatestore.PrivateStorageManager
import com.privategallery.app.data.repository.MediaRepositoryImpl
import com.privategallery.app.data.repository.PrivateGalleryRepositoryImpl
import com.privategallery.app.domain.repository.MediaRepository
import com.privategallery.app.domain.repository.PrivateGalleryRepository
import com.privategallery.app.domain.usecase.GetMediaCollectionUseCase
import com.privategallery.app.domain.usecase.SaveEditedImageUseCase

/**
 * Lightweight manual dependency container. A DI framework (Hilt) can be
 * introduced later without changing any ViewModel's public API, since
 * ViewModels depend on the MediaRepository interface, not this class.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = PrivateGalleryDatabase.getInstance(context)
    private val scanner = MediaStoreScanner(context)

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(
            context = appContext,
            scanner = scanner,
            favoriteDao = database.favoriteDao(),
            albumDao = database.albumDao(),
            trashDao = database.trashDao()
        )
    }

    val getMediaCollectionUseCase: GetMediaCollectionUseCase by lazy {
        GetMediaCollectionUseCase(mediaRepository)
    }

    val saveEditedImageUseCase: SaveEditedImageUseCase by lazy {
        SaveEditedImageUseCase(appContext)
    }

    val duplicateDetectionEngine: DuplicateDetectionEngine by lazy {
        DuplicateDetectionEngine(appContext)
    }

    val securityPreferences: SecurityPreferences by lazy {
        SecurityPreferences(appContext)
    }

    val appPreferences: AppPreferences by lazy {
        AppPreferences(appContext)
    }

    private val privateStorageManager: PrivateStorageManager by lazy {
        PrivateStorageManager(appContext)
    }

    /** Exposed for Coil's ImageLoader setup (PrivateMediaFetcher, STEP 55). */
    fun privateStorageManagerForImageLoading(): PrivateStorageManager = privateStorageManager

    @Volatile
    private var cachedPrivateGalleryRepository: PrivateGalleryRepository? = null

    /**
     * Suspend because the encrypted database (STEP 52) requires a
     * DataStore read (and possibly a first-run Keystore-encrypt) before
     * it can be opened — see PrivateGalleryEncryptedDatabase.getInstance.
     */
    suspend fun getPrivateGalleryRepository(): PrivateGalleryRepository {
        cachedPrivateGalleryRepository?.let { return it }
        val encryptedDb = PrivateGalleryEncryptedDatabase.getInstance(appContext)
        val repository = PrivateGalleryRepositoryImpl(
            context = appContext,
            privateMediaDao = encryptedDb.privateMediaDao(),
            storageManager = privateStorageManager
        )
        cachedPrivateGalleryRepository = repository
        return repository
    }
}
