package com.enterprise.manufacturing.core.chat.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.enterprise.manufacturing.core.db.dao.GeneralChatMessageDao
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import com.enterprise.manufacturing.core.model.SyncStatus
import com.enterprise.manufacturing.core.model.TeamChatMessageType
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneralChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: GeneralChatMessageDao,
    private val dispatchers: DispatchersProvider,
) : GeneralChatRepository {

    override fun observeDirectMessages(peerUserId: Long, currentUserId: Long): Flow<List<GeneralChatMessageEntity>> =
        dao.observeDirectThread(peerUserId, currentUserId)

    override suspend fun sendText(senderUserId: Long, recipientUserId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(dispatchers.io) {
            insertBase(
                senderUserId = senderUserId,
                recipientUserId = recipientUserId,
                type = TeamChatMessageType.TEXT,
                body = trimmed,
                attachmentLocalPath = null,
                attachmentMime = null,
                attachmentDisplayName = null,
                voiceDurationMs = 0L,
                transcript = "",
            )
        }
    }

    override suspend fun sendVoiceMessage(
        senderUserId: Long,
        recipientUserId: Long,
        audioFile: File,
        durationMs: Long,
    ) {
        if (!audioFile.exists()) return
        withContext(dispatchers.io) {
            insertBase(
                senderUserId = senderUserId,
                recipientUserId = recipientUserId,
                type = TeamChatMessageType.VOICE,
                body = "",
                attachmentLocalPath = audioFile.absolutePath,
                attachmentMime = "audio/mp4",
                attachmentDisplayName = audioFile.name,
                voiceDurationMs = durationMs,
                transcript = "",
            )
        }
    }

    override suspend fun sendFileMessage(senderUserId: Long, recipientUserId: Long, sourceUri: Uri, caption: String) {
        withContext(dispatchers.io) {
            val resolver = context.contentResolver
            val mime = resolver.getType(sourceUri)?.lowercase(Locale.US) ?: "application/octet-stream"
            val displayName = resolveDisplayName(sourceUri) ?: "file_${UUID.randomUUID()}"
            val destDir = File(context.filesDir, CHAT_ATTACH_DIR).apply { mkdirs() }
            val safeBase = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80).ifBlank { "attach" }
            val dest = File(destDir, "${UUID.randomUUID()}_$safeBase")

            resolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext

            insertBase(
                senderUserId = senderUserId,
                recipientUserId = recipientUserId,
                type = TeamChatMessageType.FILE,
                body = caption.trim(),
                attachmentLocalPath = dest.absolutePath,
                attachmentMime = mime,
                attachmentDisplayName = displayName,
                voiceDurationMs = 0L,
                transcript = "",
            )
        }
    }

    override suspend fun setTranscript(messageId: Long, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        withContext(dispatchers.io) {
            dao.updateTranscript(messageId, t)
        }
    }

    override suspend fun mergeTranscript(messageId: Long, spokenText: String) {
        val text = spokenText.trim()
        if (text.isEmpty()) return
        withContext(dispatchers.io) {
            val msg = dao.getById(messageId) ?: return@withContext
            val merged =
                if (msg.transcript.isBlank()) text else msg.transcript.trimEnd() + "\n" + text
            dao.updateTranscript(messageId, merged)
        }
    }

    private suspend fun insertBase(
        senderUserId: Long,
        recipientUserId: Long,
        type: TeamChatMessageType,
        body: String,
        attachmentLocalPath: String?,
        attachmentMime: String?,
        attachmentDisplayName: String?,
        voiceDurationMs: Long,
        transcript: String,
    ) {
        dao.insert(
            GeneralChatMessageEntity(
                senderUserId = senderUserId,
                recipientUserId = recipientUserId,
                messageType = type.name,
                body = body,
                createdAtEpochMs = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name,
                attachmentLocalPath = attachmentLocalPath,
                attachmentMime = attachmentMime,
                attachmentDisplayName = attachmentDisplayName,
                voiceDurationMs = voiceDurationMs,
                transcript = transcript,
            ),
        )
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor.use { c ->
            if (c == null || !c.moveToFirst()) return null
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx < 0) return null
            return c.getString(idx)
        }
    }

    companion object {
        const val CHAT_ATTACH_DIR = "chat_attach"
    }
}
