package com.privategallery.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.privategallery.app.core.di.AppContainer
import com.privategallery.app.core.media.PrivateMediaFetcher
import com.privategallery.app.core.security.AppLifecycleObserver

/**
 * Implements ImageLoaderFactory so Coil's singleton ImageLoader (used by
 * every AsyncImage/MediaThumbnail app-wide) includes PrivateMediaFetcher
 * (STEP 55) — this is what lets a "private://<id>" pseudo-URI resolve
 * correctly wherever a normal content:// URI would, without every
 * screen needing to know about private media specially.
 */
class PrivateGalleryApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // STEP 49 — single app-wide lifecycle observer driving auto-lock.
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(PrivateMediaFetcher.Factory(container.privateStorageManagerForImageLoading()))
            }
            .build()
    }
}
