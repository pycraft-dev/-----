package com.enterprise.manufacturing.core.chat.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDirectChatGateway @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) {

    fun clientOrNull(): SupabaseClient? = clientProvider.clientOrNull()

    suspend fun insertText(row: DirectMessageInsert): DirectMessageRemoteRow {
        val c = clientOrNull() ?: error("Supabase не настроен")
        val rows =
            c.from("direct_messages").insert(row) {
                select()
            }.decodeList<DirectMessageRemoteRow>()
        return rows.singleOrNull() ?: error("Supabase вернул пустой ответ после insert")
    }

    /** Последние сообщения в диалоге (от старых к новым). */
    suspend fun fetchRecentConversation(
        conversationKey: String,
        limit: Long = QUERY_LIMIT,
    ): List<DirectMessageRemoteRow> {
        val c = clientOrNull() ?: return emptyList()
        return c.from("direct_messages").select {
            filter {
                eq("conversation_key", conversationKey)
            }
            order(column = "created_at", order = Order.DESCENDING)
            limit(count = limit)
        }.decodeList<DirectMessageRemoteRow>().asReversed()
    }

    companion object {
        private const val QUERY_LIMIT = 120L
    }
}
