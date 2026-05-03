package com.enterprise.manufacturing.sync.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.enterprise.manufacturing.core.db.dao.DefectDao
import com.enterprise.manufacturing.core.db.dao.DefectMessageDao
import com.enterprise.manufacturing.core.db.dao.DrawingMessageDao
import com.enterprise.manufacturing.core.db.dao.GeneralChatMessageDao
import com.enterprise.manufacturing.core.sync.UserDirectorySync
import com.enterprise.manufacturing.sync.EnterpriseSyncScheduler
import com.enterprise.manufacturing.sync.R
import com.enterprise.manufacturing.sync.work.EnterpriseSyncNames
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncDashboardUiState(
    val pendingDefects: Int = 0,
    val pendingMessages: Int = 0,
    val immediateWorkState: WorkInfo.State? = null,
    val periodicWorkState: WorkInfo.State? = null,
    val userSyncMessageRes: Int? = null,
    val userSyncMessageArg: Int = 0,
    val userSyncBusy: Boolean = false,
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext context: Context,
    defectDao: DefectDao,
    defectMessageDao: DefectMessageDao,
    drawingMessageDao: DrawingMessageDao,
    generalChatMessageDao: GeneralChatMessageDao,
    private val scheduler: EnterpriseSyncScheduler,
    private val userDirectorySync: UserDirectorySync,
) : ViewModel() {

    private val userSyncOverlay = MutableStateFlow(Triple<Int?, Int, Boolean>(null, 0, false))

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
        userSyncOverlay,
    ) { defects, msgs, immediateInfos, periodicInfos, overlay ->
        SyncDashboardUiState(
            pendingDefects = defects,
            pendingMessages = msgs,
            immediateWorkState = immediateInfos.firstOrNull()?.state,
            periodicWorkState = periodicInfos.firstOrNull()?.state,
            userSyncMessageRes = overlay.first,
            userSyncMessageArg = overlay.second,
            userSyncBusy = overlay.third,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncDashboardUiState(),
    )

    fun requestSyncNow() {
        scheduler.enqueueImmediateSync()
    }

    fun pullUsersFromServer() {
        viewModelScope.launch {
            userSyncOverlay.update { Triple(null, 0, true) }
            val result = userDirectorySync.pullFromRemote()
            userSyncOverlay.update {
                when {
                    result.isSuccess ->
                        Triple(R.string.sync_users_ok, result.getOrNull() ?: 0, false)

                    result.exceptionOrNull()?.message == "supabase_disabled" ->
                        Triple(R.string.sync_users_skipped, 0, false)

                    else -> Triple(R.string.sync_users_err, 0, false)
                }
            }
        }
    }

    fun clearUserSyncMessage() {
        userSyncOverlay.update { Triple(null, 0, false) }
    }
}
