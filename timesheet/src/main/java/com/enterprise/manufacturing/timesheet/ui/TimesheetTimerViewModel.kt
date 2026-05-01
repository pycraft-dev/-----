package com.enterprise.manufacturing.timesheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.db.entity.TimeCategoryEntity
import com.enterprise.manufacturing.core.db.entity.TimeEntryEntity
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.timesheet.data.TimesheetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TimesheetTimerEvent {
    data object NoSession : TimesheetTimerEvent
    data object EmptyTitle : TimesheetTimerEvent
    data object AlreadyRunning : TimesheetTimerEvent
    data object Started : TimesheetTimerEvent
    data object Stopped : TimesheetTimerEvent
    data object StopFailed : TimesheetTimerEvent
    data object Error : TimesheetTimerEvent
}

@HiltViewModel
class TimesheetTimerViewModel @Inject constructor(
    private val timesheetRepository: TimesheetRepository,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val activeSessionFlow = authSessionRepository.observeSessionSnapshot()
        .filterIsInstance<SessionSnapshot.Active>()
        .distinctUntilChanged()

    val categories: StateFlow<List<TimeCategoryEntity>> =
        timesheetRepository.observeCategories().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val activeEntry: StateFlow<TimeEntryEntity?> =
        activeSessionFlow.flatMapLatest { active ->
            timesheetRepository.observeActiveEntry(active.userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val eventsChannel = Channel<TimesheetTimerEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private suspend fun requireActiveUser(): SessionSnapshot.Active? {
        val snap = authSessionRepository.observeSessionSnapshot()
            .first { it !is SessionSnapshot.Loading }
        return snap as? SessionSnapshot.Active
    }

    fun start(taskTitle: String, note: String?, categoryId: Long?) {
        viewModelScope.launch {
            val active = requireActiveUser() ?: run {
                eventsChannel.send(TimesheetTimerEvent.NoSession)
                return@launch
            }
            val result = timesheetRepository.startTimer(
                userId = active.userId,
                categoryId = categoryId,
                taskTitle = taskTitle,
                note = note,
            )
            result.fold(
                onSuccess = { eventsChannel.send(TimesheetTimerEvent.Started) },
                onFailure = { e ->
                    when (e.message) {
                        "empty_title" -> eventsChannel.send(TimesheetTimerEvent.EmptyTitle)
                        "timer_already_running" -> eventsChannel.send(TimesheetTimerEvent.AlreadyRunning)
                        else -> eventsChannel.send(TimesheetTimerEvent.Error)
                    }
                },
            )
        }
    }

    fun stop() {
        viewModelScope.launch {
            val activeUser = requireActiveUser() ?: run {
                eventsChannel.send(TimesheetTimerEvent.NoSession)
                return@launch
            }
            val entry = timesheetRepository.observeActiveEntry(activeUser.userId).first()
                ?: run {
                    eventsChannel.send(TimesheetTimerEvent.StopFailed)
                    return@launch
                }
            val ok = timesheetRepository.stopTimer(entry.id, activeUser.userId)
            eventsChannel.send(if (ok) TimesheetTimerEvent.Stopped else TimesheetTimerEvent.StopFailed)
        }
    }
}
