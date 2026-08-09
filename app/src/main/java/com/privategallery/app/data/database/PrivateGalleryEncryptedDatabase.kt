package com.privategallery.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.privategallery.app.data.database.dao.PrivateMediaDao
import com.privategallery.app.data.database.entity.PrivateAlbumEntity
import com.privategallery.app.data.database.entity.PrivateMediaEntity
import com.privategallery.app.data.security.PrivateDbPassphraseManager
import net.sqlcipher.database.SupportFactory

/**
 * STEP 52 — SECURE PRIVATE STORAGE SYSTEM
 *
 * A completely separate database file from PrivateGalleryDatabase
 * (normal-gallery metadata) — different filename, different Room
 * instance, encrypted at rest with SQLCipher using a passphrase that is
 * itself envelope-encrypted by the Keystore-backed key (STEP 45, see
 * PrivateDbPassphraseManager). Even if an attacker extracts
 * private_gallery.db (normal), it contains zero rows about private
 * media, and private_gallery_encrypted.db is unreadable without that
 * passphrase. This is what makes RULE J/K hold at the storage layer,
 * not just in application logic.
 *
 * getInstance is suspend because deriving the passphrase requires a
 * DataStore read (and possibly a first-run Keystore-encrypt) before the
 * database can even be opened.
 */
@Database(
    entities = [
        PrivateMediaEntity::class,
        PrivateAlbumEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrivateGalleryEncryptedDatabase : RoomDatabase() {

    abstract fun privateMediaDao(): PrivateMediaDao

    companion object {
        @Volatile
        private var instance: PrivateGalleryEncryptedDatabase? = null

        suspend fun getInstance(context: Context): PrivateGalleryEncryptedDatabase {
            instance?.let { return it }
            val passphrase = PrivateDbPassphraseManager.getOrCreatePassphrase(context)
            val factory = SupportFactory(passphrase)
            val built = Room.databaseBuilder(
                context.applicationContext,
                PrivateGalleryEncryptedDatabase::class.java,
                "private_gallery_encrypted.db"
            )
                .openHelperFactory(factory)
                .build()
            instance = built
            return built
        }
    }
}
