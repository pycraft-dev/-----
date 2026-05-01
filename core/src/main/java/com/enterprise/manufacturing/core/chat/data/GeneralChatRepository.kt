package com.enterprise.manufacturing.core.chat.data

import android.net.Uri
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

interface GeneralChatRepository {
    fun observeMessages(): Flow<List<GeneralChatMessageEntity>>

    suspend fun sendText(senderUserId: Long, text: String)

    suspend fun sendVoiceMessage(senderUserId: Long, audioFile: File, durationMs: Long)

    suspend fun sendFileMessage(senderUserId: Long, sourceUri: Uri, caption: String)

    suspend fun mergeTranscript(messageId: Long, spokenText: String)
}
