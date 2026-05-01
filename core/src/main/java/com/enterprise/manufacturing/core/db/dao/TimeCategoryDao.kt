package com.enterprise.manufacturing.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enterprise.manufacturing.core.db.entity.TimeCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeCategoryDao {
    @Query("SELECT * FROM time_categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<TimeCategoryEntity>>

    @Query("SELECT * FROM time_categories ORDER BY sortOrder ASC, id ASC")
    suspend fun loadAll(): List<TimeCategoryEntity>

    @Query("SELECT COUNT(*) FROM time_categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TimeCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<TimeCategoryEntity>)
}
