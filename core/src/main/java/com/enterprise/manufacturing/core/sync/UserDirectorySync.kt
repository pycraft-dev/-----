package com.enterprise.manufacturing.core.sync

import com.enterprise.manufacturing.core.chat.supabase.EnterpriseUserRow
import com.enterprise.manufacturing.core.chat.supabase.SupabaseUserSyncGateway
import com.enterprise.manufacturing.core.chat.supabase.toUserEntity
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Слияние каталога пользователей с таблицей Supabase `enterprise_users`
 * (логин совпадает с локальным `users.login`).
 */
interface UserDirectorySync {
    /** Подтянуть все строки с сервера и объединить в Room (сохраняются локальные `id` по логину). */
    suspend fun pullFromRemote(): Result<Int>

    /** Отправить одного пользователя на сервер (после создания в админке). */
    suspend fun pushUserToRemote(user: UserEntity): Result<Unit>
}

@Singleton
class UserDirectorySyncImpl @Inject constructor(
    private val userDao: UserDao,
    private val gateway: SupabaseUserSyncGateway,
    private val dispatchers: DispatchersProvider,
) : UserDirectorySync {

    override suspend fun pullFromRemote(): Result<Int> =
        withContext(dispatchers.io) {
            if (!gateway.isConfigured()) {
                return@withContext Result.failure(IllegalStateException("supabase_disabled"))
            }
            runCatching {
                val rows = gateway.fetchAll()
                var merged = 0
                for (row in rows) {
                    mergeRemoteRow(row)
                    merged++
                }
                merged
            }
        }

    override suspend fun pushUserToRemote(user: UserEntity): Result<Unit> =
        withContext(dispatchers.io) {
            gateway.upsert(user)
        }

    private suspend fun mergeRemoteRow(row: EnterpriseUserRow) {
        val incoming = row.toUserEntity(localId = 0L)
        val existing = userDao.getByLogin(incoming.login)
        if (existing != null) {
            userDao.upsert(incoming.copy(id = existing.id))
        } else {
            userDao.upsert(incoming.copy(id = 0L))
        }
    }
}
