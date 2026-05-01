package com.enterprise.manufacturing.core.db.dao



import androidx.room.Dao

import androidx.room.Insert

import androidx.room.OnConflictStrategy

import androidx.room.Query

import com.enterprise.manufacturing.core.db.entity.DrawingRevisionEntity

import kotlinx.coroutines.flow.Flow



@Dao

interface DrawingDao {

    @Query("SELECT * FROM drawing_revisions ORDER BY createdAtEpochMs DESC")

    fun observeAll(): Flow<List<DrawingRevisionEntity>>



    @Query("SELECT * FROM drawing_revisions WHERE seriesId = :seriesId ORDER BY version ASC")

    fun observeSeries(seriesId: String): Flow<List<DrawingRevisionEntity>>



    @Query("SELECT * FROM drawing_revisions WHERE id = :id")

    suspend fun getById(id: Long): DrawingRevisionEntity?



    @Query("SELECT * FROM drawing_revisions WHERE id = :id")

    fun observeById(id: Long): Flow<DrawingRevisionEntity?>



    @Query("SELECT * FROM drawing_revisions WHERE seriesId = :seriesId ORDER BY version DESC LIMIT 1")

    suspend fun getLatestInSeries(seriesId: String): DrawingRevisionEntity?



    @Query("SELECT COALESCE(MAX(version), 0) FROM drawing_revisions WHERE seriesId = :seriesId")

    suspend fun maxVersion(seriesId: String): Int



    @Insert(onConflict = OnConflictStrategy.ABORT)

    suspend fun insert(entity: DrawingRevisionEntity): Long



    @Query("UPDATE drawing_revisions SET status = :status WHERE id = :id")

    suspend fun updateStatus(id: Long, status: String): Int

}

