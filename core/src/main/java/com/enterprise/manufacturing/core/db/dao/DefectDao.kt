package com.enterprise.manufacturing.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enterprise.manufacturing.core.db.entity.DefectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DefectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(defect: DefectEntity)

    @Query("SELECT * FROM defects ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<DefectEntity>>

    @Query("SELECT * FROM defects WHERE defectId = :id LIMIT 1")
    suspend fun getById(id: String): DefectEntity?

    @Query("SELECT * FROM defects WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: String): List<DefectEntity>

    @Query("UPDATE defects SET syncStatus = :status WHERE defectId = :defectId")
    suspend fun updateSyncStatus(defectId: String, status: String)

    @Query("SELECT COUNT(*) FROM defects WHERE syncStatus = 'PENDING'")
    fun observePendingSyncCount(): Flow<Int>
}
