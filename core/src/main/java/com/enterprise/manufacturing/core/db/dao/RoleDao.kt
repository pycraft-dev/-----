package com.enterprise.manufacturing.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enterprise.manufacturing.core.db.entity.RoleDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoleDao {

    @Query("SELECT COUNT(*) FROM app_roles")
    suspend fun count(): Int

    @Query("SELECT * FROM app_roles WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): RoleDefinitionEntity?

    @Query("SELECT * FROM app_roles ORDER BY sortOrder ASC, code ASC")
    fun observeAllOrdered(): Flow<List<RoleDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(role: RoleDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(roles: List<RoleDefinitionEntity>)
}
