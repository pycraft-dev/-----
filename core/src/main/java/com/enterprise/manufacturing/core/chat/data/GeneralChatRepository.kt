package com.enterprise.manufacturing.core.chat.data

import android.net.Uri
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

interface GeneralChatRepository {
    fun observeDirectMessages(peerUserId: Long, currentUserId: Long): Flow<List<GeneralChatMessageEntity>>

    /**
     * Включает фоновый опрос Supabase для переписки (no-op если URL/ключ не заданы).
     */
    fun attachRemoteDirectConversation(peerUserId: Long, currentUserId: Long)

    fun detachRemoteDirectConversation(peerUserId: Long)

    suspend fun sendText(senderUserId: Long, recipientUserId: Long, text: String)

    suspend fun sendVoiceMessage(senderUserId: Long, recipientUserId: Long, audioFile: File, durationMs: Long)

    suspend fun sendFileMessage(senderUserId: Long, recipientUserId: Long, sourceUri: Uri, caption: String)

    suspend fun mergeTranscript(messageId: Long, spokenText: String)

    suspend fun setTranscript(messageId: Long, text: String)
}
