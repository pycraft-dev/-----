package com.enterprise.manufacturing.sync.work

import com.enterprise.manufacturing.defect.sync.DefectSyncStub

/**
 * Suspend-логика синхронизации для [EnterpriseSyncWorker] на Java ([androidx.hilt.work.HiltWorker] + Kotlin 2.2 kapt не дружат).
 * Наружному коду модуль использовать не нужен — точка входа только `EnterpriseSyncWorker`.
 */
@Suppress("Unused") // Вызывается из EnterpriseSyncWorker.java
object EnterpriseSyncLogic {
    suspend fun flushAll(defectSyncStub: DefectSyncStub, chatMessagesSyncStub: ChatMessagesSyncStub) {
        defectSyncStub.flushPendingToSent()
        chatMessagesSyncStub.flushPendingToSent()
    }
}
