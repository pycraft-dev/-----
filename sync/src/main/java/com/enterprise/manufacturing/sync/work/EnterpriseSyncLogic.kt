package com.enterprise.manufacturing.sync.work

import com.enterprise.manufacturing.core.sync.UserDirectorySync
import com.enterprise.manufacturing.defect.sync.DefectSyncStub
import com.enterprise.manufacturing.update.background.UpdateBackgroundCheck

/**
 * Suspend-логика синхронизации для [EnterpriseSyncWorker] на Java ([androidx.hilt.work.HiltWorker] + Kotlin 2.2 kapt не дружат).
 * Наружному коду модуль использовать не нужен — точка входа только `EnterpriseSyncWorker`.
 */
@Suppress("Unused") // Вызывается из EnterpriseSyncWorker.java
object EnterpriseSyncLogic {
    suspend fun flushAll(
        defectSyncStub: DefectSyncStub,
        chatMessagesSyncStub: ChatMessagesSyncStub,
        userDirectorySync: UserDirectorySync,
        updateBackgroundCheck: UpdateBackgroundCheck,
    ) {
        defectSyncStub.flushPendingToSent()
        chatMessagesSyncStub.flushPendingToSent()
        runCatching { userDirectorySync.pullFromRemote() }
        runCatching { updateBackgroundCheck.runIfConfigured() }
    }
}
