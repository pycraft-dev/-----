package com.enterprise.manufacturing.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enterprise.manufacturing.defect.sync.DefectSyncStub
import com.enterprise.manufacturing.sync.work.ChatMessagesSyncStub
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Единая точка фоновой синхронизации. Сейчас: брак ([DefectSyncStub]), сообщения чатов ([ChatMessagesSyncStub]).
 * Дальше: чертежи, табели, конфликты last-write-wins и очереди Retrofit.
 */
@HiltWorker
class EnterpriseSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val defectSyncStub: DefectSyncStub,
    private val chatMessagesSyncStub: ChatMessagesSyncStub,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        try {
            defectSyncStub.flushPendingToSent()
            chatMessagesSyncStub.flushPendingToSent()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
}
