package com.privategallery.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.privategallery.app.data.database.entity.AlbumEntity
import com.privategallery.app.data.database.entity.AlbumMediaCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY createdAtMillis DESC")
    fun observeAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbum(albumId: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: String)

    @Query("SELECT mediaId FROM album_media_cross_ref WHERE albumId = :albumId")
    fun observeMediaIdsForAlbum(albumId: String): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM album_media_cross_ref WHERE albumId = :albumId")
    suspend fun getMediaCount(albumId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMediaToAlbum(refs: List<AlbumMediaCrossRef>)

    @Query("DELETE FROM album_media_cross_ref WHERE albumId = :albumId AND mediaId IN (:mediaIds)")
    suspend fun removeMediaFromAlbum(albumId: String, mediaIds: List<Long>)

    @Query("DELETE FROM album_media_cross_ref WHERE albumId = :albumId")
    suspend fun clearAlbumMedia(albumId: String)
}
