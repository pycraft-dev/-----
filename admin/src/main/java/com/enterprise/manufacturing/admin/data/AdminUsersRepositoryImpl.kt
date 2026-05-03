package com.enterprise.manufacturing.admin.data

import com.enterprise.manufacturing.auth.domain.RoleLoginGenerator
import com.enterprise.manufacturing.auth.security.PasswordHasher
import com.enterprise.manufacturing.core.db.dao.RoleDao
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.sync.UserDirectorySync
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminUsersRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val roleDao: RoleDao,
    private val roleLoginGenerator: RoleLoginGenerator,
    private val passwordHasher: PasswordHasher,
    private val dispatchers: DispatchersProvider,
    private val userDirectorySync: UserDirectorySync,
) : AdminUsersRepository {

    override fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    override suspend fun peekNextLogin(roleCode: String): String =
        roleLoginGenerator.nextLoginForRoleCode(normalize(roleCode))

    override suspend fun createUser(
        fullName: String,
        position: String,
        roleCode: String,
        plainPassword: String,
    ): Result<String> = withContext(dispatchers.io) {
        val name = fullName.trim()
        val pos = position.trim()
        val rc = normalize(roleCode)
        if (name.isEmpty() || pos.isEmpty() || plainPassword.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("empty"))
        }
        if (roleDao.getByCode(rc) == null) {
            return@withContext Result.failure(IllegalArgumentException("unknown role"))
        }

        val login = roleLoginGenerator.nextLoginForRoleCode(rc)
        val hash = passwordHasher.hash(plainPassword)
        userDao.upsert(
            UserEntity(
                login = login,
                passwordHash = hash,
                fullName = name,
                position = pos,
                groupKey = rc,
                role = rc,
            ),
        )
        val saved = userDao.getByLogin(login)
        if (saved != null) {
            runCatching { userDirectorySync.pushUserToRemote(saved) }
        }
        Result.success(login)
    }

    private fun normalize(code: String): String = code.trim().uppercase(Locale.US)
}
