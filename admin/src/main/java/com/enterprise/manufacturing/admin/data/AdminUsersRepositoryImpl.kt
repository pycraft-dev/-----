package com.enterprise.manufacturing.admin.data

import com.enterprise.manufacturing.auth.domain.GroupLoginGenerator
import com.enterprise.manufacturing.auth.security.PasswordHasher
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminUsersRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val groupLoginGenerator: GroupLoginGenerator,
    private val passwordHasher: PasswordHasher,
    private val dispatchers: DispatchersProvider,
) : AdminUsersRepository {

    override fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    override suspend fun createUser(
        fullName: String,
        position: String,
        groupKey: String,
        role: UserRole,
        plainPassword: String,
    ): Result<String> = withContext(dispatchers.io) {
        val name = fullName.trim()
        val pos = position.trim()
        val group = groupKey.trim()
        if (name.isEmpty() || pos.isEmpty() || group.isEmpty() || plainPassword.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("empty"))
        }

        val login = groupLoginGenerator.nextLoginForGroup(group)
        val hash = passwordHasher.hash(plainPassword)
        userDao.upsert(
            UserEntity(
                login = login,
                passwordHash = hash,
                fullName = name,
                position = pos,
                groupKey = group,
                role = role.name,
            ),
        )
        Result.success(login)
    }
}
