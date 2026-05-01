package com.enterprise.manufacturing.defect.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.defect.data.DefectRepository
import com.enterprise.manufacturing.defect.device.DeviceIdProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface DefectCaptureEvent {
    data class Created(val defectId: String) : DefectCaptureEvent
    data object MissingSession : DefectCaptureEvent
    data object Failure : DefectCaptureEvent
}

@HiltViewModel
class DefectCaptureViewModel @Inject constructor(
    private val defectRepository: DefectRepository,
    private val authSessionRepository: AuthSessionRepository,
    private val deviceIdProvider: DeviceIdProvider,
) : ViewModel() {

    private val eventsChannel = Channel<DefectCaptureEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun submitCapturedPhoto(tempFile: File, notes: String?) {
        viewModelScope.launch {
            val snap = authSessionRepository.observeSessionSnapshot().first()
            val active = snap as? SessionSnapshot.Active
            if (active == null) {
                eventsChannel.send(DefectCaptureEvent.MissingSession)
                return@launch
            }

            val result = defectRepository.createDefectWithPhoto(
                photoFile = tempFile,
                notes = notes,
                authorUserId = active.userId,
                deviceId = deviceIdProvider.androidId,
            )
            result.fold(
                onSuccess = { id -> eventsChannel.send(DefectCaptureEvent.Created(id)) },
                onFailure = { eventsChannel.send(DefectCaptureEvent.Failure) },
            )
        }
    }
}
