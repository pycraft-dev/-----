package com.enterprise.manufacturing.timesheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.timesheet.data.TimesheetHistoryRow
import com.enterprise.manufacturing.timesheet.data.TimesheetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface TimesheetExportEvent {
    data object NoSession : TimesheetExportEvent
    data object Success : TimesheetExportEvent
    data object Failed : TimesheetExportEvent
}

@HiltViewModel
class TimesheetHistoryViewModel @Inject constructor(
    private val timesheetRepository: TimesheetRepository,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val activeSessionFlow = authSessionRepository.observeSessionSnapshot()
        .filterIsInstance<SessionSnapshot.Active>()
        .distinctUntilChanged()

    val rows: StateFlow<List<TimesheetHistoryRow>> =
        activeSessionFlow.flatMapLatest { active ->
            timesheetRepository.observeCompletedDisplay(active.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    suspend fun exportTo(uri: android.net.Uri): TimesheetExportEvent {
        val snap = authSessionRepository.observeSessionSnapshot()
            .first { it !is SessionSnapshot.Loading }
        val active = snap as? SessionSnapshot.Active ?: return TimesheetExportEvent.NoSession
        val ok = timesheetRepository.writeCsvToUri(active.userId, uri)
        return if (ok) TimesheetExportEvent.Success else TimesheetExportEvent.Failed
    }
}
