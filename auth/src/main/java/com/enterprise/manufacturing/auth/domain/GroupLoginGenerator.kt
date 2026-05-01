package com.enterprise.manufacturing.auth.domain

import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Генерация логинов вида `{groupKey}_{n}` (рабочий_1, конструктор_2 …).
 * Используется при создании пользователя администратором (модуль :admin).
 */
@Singleton
class GroupLoginGenerator @Inject constructor(
    private val userDao: UserDao,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun nextLoginForGroup(groupKey: String): String = withContext(dispatchers.io) {
        val taken = userDao.observeAll().first().map(UserEntity::login)
        val index = nextAvailableIndex(taken, groupKey)
        format(groupKey, index)
    }

    fun format(groupKey: String, index: Int): String = "${groupKey}_$index"

    fun nextAvailableIndex(existingLogins: Collection<String>, groupKey: String): Int {
        val prefix = "${groupKey}_"
        val numbers = existingLogins.asSequence()
            .filter { it.startsWith(prefix) }
            .mapNotNull { runCatching { it.substring(prefix.length).toInt() }.getOrNull() }
            .toSet()
        var candidate = 1
        while (candidate in numbers) {
            candidate++
        }
        return candidate
    }
}
