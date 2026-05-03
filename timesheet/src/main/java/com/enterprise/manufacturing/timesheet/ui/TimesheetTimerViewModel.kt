package com.enterprise.manufacturing.timesheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.timesheet.data.TimesheetHistoryRow
import com.enterprise.manufacturing.timesheet.data.TimesheetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TimesheetTimerEvent {
    data object NoSession : TimesheetTimerEvent

    data object EmptyTitle : TimesheetTimerEvent

    data object InvalidDuration : TimesheetTimerEvent

    data object Added : TimesheetTimerEvent

    data object Error : TimesheetTimerEvent
}

@HiltViewModel
class TimesheetTimerViewModel @Inject constructor(
    private val timesheetRepository: TimesheetRepository,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val activeSessionFlow =
        authSessionRepository.observeSessionSnapshot()
            .filterIsInstance<SessionSnapshot.Active>()
            .distinctUntilChanged()

    val recentEntries: StateFlow<List<TimesheetHistoryRow>> =
        activeSessionFlow.flatMapLatest { active ->
            timesheetRepository.observeCompletedDisplay(active.userId).map { list ->
                list.take(15)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val eventsChannel = Channel<TimesheetTimerEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private suspend fun requireActiveUser(): SessionSnapshot.Active? {
        val snap =
            authSessionRepository.observeSessionSnapshot()
                .first { it !is SessionSnapshot.Loading }
        return snap as? SessionSnapshot.Active
    }

    fun addTask(description: String, hoursText: String, minutesText: String) {
        viewModelScope.launch {
            val active =
                requireActiveUser() ?: run {
                    eventsChannel.send(TimesheetTimerEvent.NoSession)
                    return@launch
                }

            val title = description.trim()
            if (title.isEmpty()) {
                eventsChannel.send(TimesheetTimerEvent.EmptyTitle)
                return@launch
            }

            val hours = hoursText.trim().toIntOrNull()
            val minutes = minutesText.trim().toIntOrNull()
            if (hours == null || minutes == null || hours < 0 || minutes !in 0..59 || (hours == 0 && minutes == 0)) {
                eventsChannel.send(TimesheetTimerEvent.InvalidDuration)
                return@launch
            }

            timesheetRepository.addManualEntry(active.userId, title, hours, minutes).fold(
                onSuccess = { eventsChannel.send(TimesheetTimerEvent.Added) },
                onFailure = { e ->
                    when (e.message) {
                        "empty_title" -> eventsChannel.send(TimesheetTimerEvent.EmptyTitle)
                        "invalid_time", "zero_duration" -> eventsChannel.send(TimesheetTimerEvent.InvalidDuration)
                        else -> eventsChannel.send(TimesheetTimerEvent.Error)
                    }
                },
            )
        }
    }
}
