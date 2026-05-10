package com.enterprise.manufacturing.core.chat.supabase

import com.enterprise.manufacturing.core.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDirectChatGateway @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) {

    fun clientOrNull(): SupabaseClient? = clientProvider.clientOrNull()

    suspend fun insertDirectMessage(row: DirectMessageInsert): DirectMessageRemoteRow {
        val c = clientOrNull() ?: error("Supabase не настроен")
        val rows =
            c.from("direct_messages").insert(row) {
                select()
            }.decodeList<DirectMessageRemoteRow>()
        return rows.singleOrNull() ?: error("Supabase вернул пустой ответ после insert")
    }

    /** Загрузка файла в bucket `chat-files`; возвращает путь объекта (не URL). */
    suspend fun uploadChatObject(conversationKey: String, file: File, mime: String): String {
        val c = clientOrNull() ?: error("Supabase не настроен")
        val folder = sanitizeStorageFolder(conversationKey)
        val safeName =
            file.name
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .take(120)
                .ifBlank { "file" }
        val objectPath = "$folder/${UUID.randomUUID()}_$safeName"
        val bytes = file.readBytes()
        c.storage.from(CHAT_FILES_BUCKET).upload(objectPath, bytes) {
            upsert = true
            contentType = ContentType.parse(mime)
        }
        return objectPath
    }

    fun publicUrlForStoragePath(objectPath: String): String {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val encoded =
            objectPath.split("/").joinToString("/") { seg ->
                URLEncoder.encode(seg, StandardCharsets.UTF_8.name()).replace("+", "%20")
            }
        return "$base/storage/v1/object/public/$CHAT_FILES_BUCKET/$encoded"
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
        const val CHAT_FILES_BUCKET = "chat-files"
        private const val QUERY_LIMIT = 120L

        fun sanitizeStorageFolder(conversationKey: String): String =
            conversationKey
                .replace(Regex("[^a-zA-Z0-9._-]+"), "_")
                .trim('_')
                .take(180)
                .ifBlank { "conv" }
    }
}
