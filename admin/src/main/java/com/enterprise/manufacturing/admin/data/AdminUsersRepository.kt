package com.enterprise.manufacturing.admin.data

import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AdminUsersRepository {
    fun observeUsers(): Flow<List<UserEntity>>

    suspend fun createUser(
        fullName: String,
        position: String,
        groupKey: String,
        role: UserRole,
        plainPassword: String,
    ): Result<String>
}
