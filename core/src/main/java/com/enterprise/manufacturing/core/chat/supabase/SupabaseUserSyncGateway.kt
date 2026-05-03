package com.enterprise.manufacturing.core.chat.supabase

import com.enterprise.manufacturing.core.db.entity.UserEntity
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseUserSyncGateway @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) {

    fun isConfigured(): Boolean = clientProvider.clientOrNull() != null

    suspend fun fetchAll(): List<EnterpriseUserRow> {
        val c = clientProvider.clientOrNull() ?: return emptyList()
        return c.from("enterprise_users").select().decodeList()
    }

    suspend fun upsert(user: UserEntity): Result<Unit> =
        runCatching {
            val c = clientProvider.clientOrNull() ?: error("supabase_disabled")
            c.from("enterprise_users").upsert(user.toEnterpriseUserRow())
        }
}
