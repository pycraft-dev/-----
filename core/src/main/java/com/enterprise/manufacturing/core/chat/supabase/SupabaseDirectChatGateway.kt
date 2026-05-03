package com.enterprise.manufacturing.core.chat.supabase

import com.enterprise.manufacturing.core.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDirectChatGateway @Inject constructor() {

    private val clientLazy =
        kotlin.lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
            val key = BuildConfig.SUPABASE_ANON_KEY.trim()
            if (url.isEmpty() || key.isEmpty()) {
                null
            } else {
                createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = key,
                ) {
                    defaultSerializer =
                        KotlinXSerializer(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                                encodeDefaults = true
                            },
                        )
                    install(Postgrest)
                }
            }
        }

    fun clientOrNull(): SupabaseClient? = clientLazy.value

    suspend fun insertText(row: DirectMessageInsert): DirectMessageRemoteRow {
        val c = clientOrNull() ?: error("Supabase не настроен")
        return c.from("direct_messages").insert(row) {
            select()
        }.decodeSingle<DirectMessageRemoteRow>()
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
