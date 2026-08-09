package com.privategallery.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.privategallery.app.data.database.entity.PrivateAlbumEntity
import com.privategallery.app.data.database.entity.PrivateMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivateMediaDao {

    @Query("SELECT * FROM private_media ORDER BY dateAddedMillis DESC")
    fun observeAll(): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE isVideo = 0 ORDER BY dateAddedMillis DESC")
    fun observeImages(): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE isVideo = 1 ORDER BY dateAddedMillis DESC")
    fun observeVideos(): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE isFavorite = 1 ORDER BY dateAddedMillis DESC")
    fun observeFavorites(): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE albumId = :albumId ORDER BY dateAddedMillis DESC")
    fun observeByAlbum(albumId: String): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE secureId = :secureId")
    suspend fun getById(secureId: String): PrivateMediaEntity?

    @Query("SELECT * FROM private_media")
    suspend fun getAllOnce(): List<PrivateMediaEntity>

    @Query(
        "SELECT * FROM private_media WHERE originalDisplayName LIKE '%' || :query || '%'"
    )
    suspend fun search(query: String): List<PrivateMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PrivateMediaEntity)

    @Update
    suspend fun update(entity: PrivateMediaEntity)

    @Query("DELETE FROM private_media WHERE secureId = :secureId")
    suspend fun deleteById(secureId: String)

    @Query("SELECT * FROM private_albums ORDER BY createdAtMillis DESC")
    fun observeAlbums(): Flow<List<PrivateAlbumEntity>>

    @Query("SELECT * FROM private_albums WHERE id = :albumId")
    suspend fun getAlbum(albumId: String): PrivateAlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: PrivateAlbumEntity)

    @Update
    suspend fun updateAlbum(album: PrivateAlbumEntity)

    @Query("DELETE FROM private_albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: String)
}
