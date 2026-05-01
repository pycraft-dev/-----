package com.enterprise.manufacturing.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.enterprise.manufacturing.core.db.entity.TimeEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
    @Insert
    suspend fun insert(entity: TimeEntryEntity): Long

    @Query(
        """
        UPDATE time_entries
        SET endEpochMs = :endMs
        WHERE id = :id AND userId = :userId AND endEpochMs IS NULL
        """,
    )
    suspend fun completeEntry(id: Long, userId: Long, endMs: Long): Int

    @Query(
        """
        SELECT * FROM time_entries
        WHERE userId = :userId AND endEpochMs IS NULL
        LIMIT 1
        """,
    )
    fun observeActiveForUser(userId: Long): Flow<TimeEntryEntity?>

    @Query(
        """
        SELECT * FROM time_entries
        WHERE userId = :userId AND endEpochMs IS NULL
        LIMIT 1
        """,
    )
    suspend fun getActiveForUser(userId: Long): TimeEntryEntity?

    @Query(
        """
        SELECT * FROM time_entries
        WHERE userId = :userId AND endEpochMs IS NOT NULL
        ORDER BY startEpochMs DESC
        """,
    )
    fun observeCompletedForUser(userId: Long): Flow<List<TimeEntryEntity>>

    @Query(
        """
        SELECT * FROM time_entries
        WHERE userId = :userId AND endEpochMs IS NOT NULL
        ORDER BY startEpochMs ASC
        """,
    )
    suspend fun loadCompletedForUserCsv(userId: Long): List<TimeEntryEntity>
}
