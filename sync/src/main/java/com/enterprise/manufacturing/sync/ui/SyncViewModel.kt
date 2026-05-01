package com.enterprise.manufacturing.sync.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.enterprise.manufacturing.core.db.dao.DefectDao
import com.enterprise.manufacturing.core.db.dao.DefectMessageDao
import com.enterprise.manufacturing.core.db.dao.DrawingMessageDao
import com.enterprise.manufacturing.core.db.dao.GeneralChatMessageDao
import com.enterprise.manufacturing.sync.EnterpriseSyncScheduler
import com.enterprise.manufacturing.sync.work.EnterpriseSyncNames
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SyncDashboardUiState(
    val pendingDefects: Int = 0,
    val pendingMessages: Int = 0,
    val immediateWorkState: WorkInfo.State? = null,
    val periodicWorkState: WorkInfo.State? = null,
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext context: Context,
    defectDao: DefectDao,
    defectMessageDao: DefectMessageDao,
    drawingMessageDao: DrawingMessageDao,
    generalChatMessageDao: GeneralChatMessageDao,
    private val scheduler: EnterpriseSyncScheduler,
) : ViewModel() {

    private val pendingChatMessagesFlow = combine(
        defectMessageDao.observePendingSyncCount(),
        drawingMessageDao.observePendingSyncCount(),
        generalChatMessageDao.observePendingSyncCount(),
    ) { defectMsgs, drawingMsgs, generalMsgs ->
        defectMsgs + drawingMsgs + generalMsgs
    }

    val uiState: StateFlow<SyncDashboardUiState> = combine(
        defectDao.observePendingSyncCount(),
        pendingChatMessagesFlow,
        uniqueWorkInfosFlow(context, EnterpriseSyncNames.IMMEDIATE),
        uniqueWorkInfosFlow(context, EnterpriseSyncNames.PERIODIC),
    ) { defects, msgs, immediateInfos, periodicInfos ->
        SyncDashboardUiState(
            pendingDefects = defects,
            pendingMessages = msgs,
            immediateWorkState = immediateInfos.firstOrNull()?.state,
            periodicWorkState = periodicInfos.firstOrNull()?.state,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncDashboardUiState(),
    )

    fun requestSyncNow() {
        scheduler.enqueueImmediateSync()
    }
}
