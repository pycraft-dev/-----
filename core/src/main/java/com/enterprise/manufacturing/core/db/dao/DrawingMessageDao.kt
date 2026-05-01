package com.enterprise.manufacturing.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enterprise.manufacturing.core.db.entity.DrawingMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: DrawingMessageEntity): Long

    @Query(
        "SELECT * FROM drawing_messages WHERE revisionId = :revisionId ORDER BY createdAtEpochMs ASC",
    )
    fun observeThread(revisionId: Long): Flow<List<DrawingMessageEntity>>

    @Query("SELECT * FROM drawing_messages WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: String): List<DrawingMessageEntity>

    @Query("UPDATE drawing_messages SET syncStatus = :status WHERE id = :messageId")
    suspend fun updateSyncStatus(messageId: Long, status: String)

    @Query("SELECT COUNT(*) FROM drawing_messages WHERE syncStatus = 'PENDING'")
    fun observePendingSyncCount(): Flow<Int>
}
