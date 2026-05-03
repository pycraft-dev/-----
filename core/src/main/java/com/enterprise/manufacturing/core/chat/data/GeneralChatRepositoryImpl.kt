package com.enterprise.manufacturing.core.chat.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.enterprise.manufacturing.core.chat.supabase.DirectMessageInsert
import com.enterprise.manufacturing.core.chat.supabase.DirectMessageRemoteRow
import com.enterprise.manufacturing.core.chat.supabase.SupabaseDirectChatGateway
import com.enterprise.manufacturing.core.chat.supabase.dmConversationKey
import com.enterprise.manufacturing.core.db.dao.GeneralChatMessageDao
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.SyncStatus
import com.enterprise.manufacturing.core.model.TeamChatMessageType
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneralChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: GeneralChatMessageDao,
    private val userDao: UserDao,
    private val dispatchers: DispatchersProvider,
    private val supabase: SupabaseDirectChatGateway,
) : GeneralChatRepository {

    private val supervisor = SupervisorJob()
    private val ioScope = CoroutineScope(supervisor + dispatchers.io)

    private val remoteJobs = ConcurrentHashMap<Long, Job>()

    override fun observeDirectMessages(peerUserId: Long, currentUserId: Long): Flow<List<GeneralChatMessageEntity>> =
        dao.observeDirectThread(peerUserId, currentUserId)

    override suspend fun sendText(senderUserId: Long, recipientUserId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(dispatchers.io) {
            val sender = userDao.getById(senderUserId) ?: return@withContext
            val recipient = userDao.getById(recipientUserId) ?: return@withContext
            val key = dmConversationKey(sender.login, recipient.login)

            val online =
                supabase.clientOrNull() != null

            if (online) {
                runCatching {
                    val inserted =
                        supabase.insertText(
                            DirectMessageInsert(
                                conversationKey = key,
                                senderLogin = sender.login,
                                recipientLogin = recipient.login,
                                body = trimmed,
                                messageType = TeamChatMessageType.TEXT.name,
                            ),
                        )
                    dao.insert(
                        entityForTextFromRemote(
                            row = inserted,
                            senderLocalId = senderUserId,
                            recipientLocalId = recipientUserId,
                        ),
                    )
                    return@withContext
                }
            }

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
                syncStatus = SyncStatus.PENDING,
                remoteId = null,
                createdAtEpochMs = System.currentTimeMillis(),
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
                syncStatus = SyncStatus.PENDING,
                remoteId = null,
                createdAtEpochMs = System.currentTimeMillis(),
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
                syncStatus = SyncStatus.PENDING,
                remoteId = null,
                createdAtEpochMs = System.currentTimeMillis(),
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

    override fun attachRemoteDirectConversation(peerUserId: Long, currentUserId: Long) {
        if (supabase.clientOrNull() == null) return

        detachRemoteDirectConversation(peerUserId)

        val job =
            ioScope.launch {
                val peer = userDao.getById(peerUserId) ?: return@launch
                val self = userDao.getById(currentUserId) ?: return@launch
                val convKey = dmConversationKey(peer.login, self.login)
                while (isActive) {
                    runCatching {
                        val batch = supabase.fetchRecentConversation(convKey)
                        for (remote in batch) {
                            mergeRemoteRow(remote)
                        }
                    }
                    delay(POLL_PERIOD_MS)
                }
            }

        remoteJobs[peerUserId] = job
    }

    override fun detachRemoteDirectConversation(peerUserId: Long) {
        remoteJobs.remove(peerUserId)?.cancel()
    }

    private suspend fun mergeRemoteRow(row: DirectMessageRemoteRow) {
        if (dao.getByRemoteId(row.id) != null) return
        val parsedType =
            runCatching { TeamChatMessageType.valueOf(row.messageType) }.getOrNull()
                ?: TeamChatMessageType.TEXT
        if (parsedType != TeamChatMessageType.TEXT) return

        val senderId = ensureStubUser(row.senderLogin)
        val recipientId = ensureStubUser(row.recipientLogin)
        if (senderId <= 0L || recipientId <= 0L) return

        dao.insert(
            GeneralChatMessageEntity(
                senderUserId = senderId,
                recipientUserId = recipientId,
                messageType = parsedType.name,
                body = row.body,
                createdAtEpochMs = parseCreatedAtEpoch(row.createdAtIso),
                syncStatus = SyncStatus.SENT.name,
                attachmentLocalPath = null,
                attachmentMime = null,
                attachmentDisplayName = null,
                voiceDurationMs = 0L,
                transcript = "",
                remoteId = row.id,
            ),
        )
    }

    private suspend fun ensureStubUser(loginRaw: String): Long {
        val login = loginRaw.trim().ifBlank { return 0L }
        userDao.getByLogin(login)?.id?.let { return it }
        userDao.upsert(
            UserEntity(
                login = login,
                passwordHash = SYNC_USER_PASSWORD_PLACEHOLDER,
                fullName = login,
                position = "",
                groupKey = "sync_stub",
                role = UserRole.WORKER.name,
            ),
        )
        return userDao.getByLogin(login)?.id ?: 0L
    }

    private fun entityForTextFromRemote(
        row: DirectMessageRemoteRow,
        senderLocalId: Long,
        recipientLocalId: Long,
    ): GeneralChatMessageEntity =
        GeneralChatMessageEntity(
            senderUserId = senderLocalId,
            recipientUserId = recipientLocalId,
            messageType = TeamChatMessageType.TEXT.name,
            body = row.body,
            createdAtEpochMs = parseCreatedAtEpoch(row.createdAtIso),
            syncStatus = SyncStatus.SENT.name,
            attachmentLocalPath = null,
            attachmentMime = null,
            attachmentDisplayName = null,
            voiceDurationMs = 0L,
            transcript = "",
            remoteId = row.id,
        )

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
        syncStatus: SyncStatus,
        remoteId: String?,
        createdAtEpochMs: Long,
    ) {
        dao.insert(
            GeneralChatMessageEntity(
                senderUserId = senderUserId,
                recipientUserId = recipientUserId,
                messageType = type.name,
                body = body,
                createdAtEpochMs = createdAtEpochMs,
                syncStatus = syncStatus.name,
                attachmentLocalPath = attachmentLocalPath,
                attachmentMime = attachmentMime,
                attachmentDisplayName = attachmentDisplayName,
                voiceDurationMs = voiceDurationMs,
                transcript = transcript,
                remoteId = remoteId,
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

    private fun parseCreatedAtEpoch(iso: String): Long =
        runCatching { Instant.parse(iso).toEpochMilli() }
            .getOrElse { System.currentTimeMillis() }

    companion object {
        const val CHAT_ATTACH_DIR = "chat_attach"

        private const val POLL_PERIOD_MS = 5_000L

        /**
         * Невалидный PBKDF2-хеш: `PasswordHasher.verify()` вернёт false (три части есть, но Base64 некорректен).
         * Пользователи синхронизации не должны входить по этому паролю.
         */
        private const val SYNC_USER_PASSWORD_PLACEHOLDER = "invalid:inactive:inactive"
    }
}
