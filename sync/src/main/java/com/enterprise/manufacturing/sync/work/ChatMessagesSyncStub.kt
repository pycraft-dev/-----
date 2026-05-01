package com.enterprise.manufacturing.sync.work

import com.enterprise.manufacturing.core.db.dao.DrawingMessageDao
import com.enterprise.manufacturing.core.db.dao.GeneralChatMessageDao
import com.enterprise.manufacturing.core.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Заглушка синхронизации сообщений чатов (чертежи + общий канал) до появления Retrofit.
 */
@Singleton
class ChatMessagesSyncStub @Inject constructor(
    private val drawingMessageDao: DrawingMessageDao,
    private val generalChatMessageDao: GeneralChatMessageDao,
) {
    suspend fun flushPendingToSent() {
        val pending = SyncStatus.PENDING.name
        val sent = SyncStatus.SENT.name
        drawingMessageDao.getBySyncStatus(pending).forEach {
            drawingMessageDao.updateSyncStatus(it.id, sent)
        }
        generalChatMessageDao.getBySyncStatus(pending).forEach {
            generalChatMessageDao.updateSyncStatus(it.id, sent)
        }
    }
}
