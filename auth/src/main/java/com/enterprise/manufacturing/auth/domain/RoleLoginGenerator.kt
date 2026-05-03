package com.enterprise.manufacturing.auth.domain

import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Автоматический логин по коду роли (`worker_1`, `inspector_2`, …).
 */
@Singleton
class RoleLoginGenerator @Inject constructor(
    private val userDao: UserDao,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun nextLoginForRoleCode(roleCode: String): String = withContext(dispatchers.io) {
        val slug =
            loginSlug(roleCode)
        val taken = userDao.observeAll().first().map(UserEntity::login)
        val index = nextAvailableIndex(taken, slug)
        "${slug}_${index}"
    }

    private fun loginSlug(roleCodeRaw: String): String {
        val c = roleCodeRaw.trim().uppercase(Locale.US)
        return c.lowercase(Locale.US).replace(Regex("[^a-z0-9_]+"), "_").trim('_')
            .ifEmpty { "user" }
    }

    fun nextAvailableIndex(existingLogins: Collection<String>, slug: String): Int {
        val prefix = "${slug}_"
        val numbers =
            existingLogins
                .asSequence()
                .filter { it.startsWith(prefix, ignoreCase = false) }
                .mapNotNull { runCatching { it.substring(prefix.length).toInt() }.getOrNull() }
                .toSet()
        var candidate = 1
        while (candidate in numbers) {
            candidate++
        }
        return candidate
    }
}
