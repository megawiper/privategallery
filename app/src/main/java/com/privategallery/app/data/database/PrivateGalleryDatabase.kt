package com.privategallery.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.privategallery.app.data.database.dao.AlbumDao
import com.privategallery.app.data.database.dao.FavoriteDao
import com.privategallery.app.data.database.dao.TrashDao
import com.privategallery.app.data.database.entity.AlbumEntity
import com.privategallery.app.data.database.entity.AlbumMediaCrossRef
import com.privategallery.app.data.database.entity.FavoriteEntity
import com.privategallery.app.data.database.entity.TrashEntity

/**
 * STEP 10 — single Room database for the whole app (RULE C: one
 * architecture, no competing databases) covering NORMAL-gallery
 * metadata only: favorites, custom albums, trash. This is intentionally
 * NOT where private-media metadata lives — that is a fully separate,
 * SQLCipher-encrypted database (PrivateGalleryEncryptedDatabase, STEP
 * 52) so a compromise of one database file never exposes the other,
 * and so RULE J/K (private metadata stays fully separate) holds at the
 * storage layer, not just in application logic.
 */
@Database(
    entities = [
        FavoriteEntity::class,
        AlbumEntity::class,
        AlbumMediaCrossRef::class,
        TrashEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrivateGalleryDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun albumDao(): AlbumDao
    abstract fun trashDao(): TrashDao

    companion object {
        @Volatile
        private var instance: PrivateGalleryDatabase? = null

        fun getInstance(context: Context): PrivateGalleryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrivateGalleryDatabase::class.java,
                    "private_gallery.db"
                ).build().also { instance = it }
            }
        }
    }
}
