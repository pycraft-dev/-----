package com.enterprise.manufacturing.admin.data

import com.enterprise.manufacturing.core.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface AdminUsersRepository {
    fun observeUsers(): Flow<List<UserEntity>>

    suspend fun peekNextLogin(roleCode: String): String

    suspend fun createUser(
        fullName: String,
        position: String,
        roleCode: String,
        plainPassword: String,
    ): Result<String>
}
