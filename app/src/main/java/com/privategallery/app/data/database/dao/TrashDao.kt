package com.privategallery.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.privategallery.app.data.database.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash ORDER BY deletedAtMillis DESC")
    fun observeTrash(): Flow<List<TrashEntity>>

    @Query("SELECT * FROM trash WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<TrashEntity>

    @Query("SELECT * FROM trash WHERE expiresAtMillis <= :nowMillis")
    suspend fun getExpired(nowMillis: Long): List<TrashEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TrashEntity>)

    @Query("DELETE FROM trash WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM trash")
    suspend fun clearAll()
}
