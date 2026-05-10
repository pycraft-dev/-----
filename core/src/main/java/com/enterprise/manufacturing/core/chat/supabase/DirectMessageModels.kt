package com.enterprise.manufacturing.core.chat.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Вставка в `public.direct_messages` (ответ — [DirectMessageRemoteRow]). */
@Serializable
data class DirectMessageInsert(
    @SerialName("conversation_key") val conversationKey: String,
    @SerialName("sender_login") val senderLogin: String,
    @SerialName("recipient_login") val recipientLogin: String,
    val body: String,
    @SerialName("message_type") val messageType: String,
    @SerialName("attachment_storage_path") val attachmentStoragePath: String? = null,
    @SerialName("attachment_mime") val attachmentMime: String? = null,
    @SerialName("attachment_display_name") val attachmentDisplayName: String? = null,
    @SerialName("voice_duration_ms") val voiceDurationMs: Long = 0,
    val transcript: String = "",
)

/** Строка из PostgREST. */
@Serializable
data class DirectMessageRemoteRow(
    val id: String,
    @SerialName("conversation_key") val conversationKey: String,
    @SerialName("sender_login") val senderLogin: String,
    @SerialName("recipient_login") val recipientLogin: String,
    val body: String,
    @SerialName("message_type") val messageType: String,
    @SerialName("created_at") val createdAtIso: String,
    @SerialName("attachment_storage_path") val attachmentStoragePath: String? = null,
    @SerialName("attachment_mime") val attachmentMime: String? = null,
    @SerialName("attachment_display_name") val attachmentDisplayName: String? = null,
    @SerialName("voice_duration_ms") val voiceDurationMs: Long = 0,
    val transcript: String = "",
)
