package com.enterprise.manufacturing.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enterprise.manufacturing.core.db.entity.DefectMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DefectMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: DefectMessageEntity): Long

    @Query(
        "SELECT * FROM defect_messages WHERE defectId = :defectId ORDER BY createdAtEpochMs ASC",
    )
    fun observeThread(defectId: String): Flow<List<DefectMessageEntity>>

    @Query("SELECT * FROM defect_messages WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: String): List<DefectMessageEntity>

    @Query("UPDATE defect_messages SET syncStatus = :status WHERE id = :messageId")
    suspend fun updateSyncStatus(messageId: Long, status: String)

    @Query("SELECT COUNT(*) FROM defect_messages WHERE syncStatus = 'PENDING'")
    fun observePendingSyncCount(): Flow<Int>
}
