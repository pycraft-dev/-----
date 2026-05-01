package com.enterprise.manufacturing.defect.sync

import com.enterprise.manufacturing.core.db.dao.DefectDao
import com.enterprise.manufacturing.core.db.dao.DefectMessageDao
import com.enterprise.manufacturing.core.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Локальная логика синхронизации брака (заглушка до Retrofit).
 * Вызывается из общего [com.enterprise.manufacturing.sync.work.EnterpriseSyncWorker].
 */
@Singleton
class DefectSyncStub @Inject constructor(
    private val defectDao: DefectDao,
    private val defectMessageDao: DefectMessageDao,
) {

    suspend fun flushPendingToSent() {
        val pending = SyncStatus.PENDING.name
        val sent = SyncStatus.SENT.name
        defectDao.getBySyncStatus(pending).forEach {
            defectDao.updateSyncStatus(it.defectId, sent)
        }
        defectMessageDao.getBySyncStatus(pending).forEach {
            defectMessageDao.updateSyncStatus(it.id, sent)
        }
    }
}
