package com.enterprise.manufacturing.drawings.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.model.DrawingStatus
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.drawings.data.DrawingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DrawingUploadEvent {
    data class Saved(val revisionId: Long) : DrawingUploadEvent
    data object NoFile : DrawingUploadEvent
    data object NoSession : DrawingUploadEvent
    data object BadFile : DrawingUploadEvent
    data object EmptyTitle : DrawingUploadEvent
    data object Error : DrawingUploadEvent
}

@HiltViewModel
class DrawingUploadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val drawingRepository: DrawingRepository,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val seriesArg: String = savedStateHandle.get<String>("seriesArg") ?: "new"

    /** `null` означает создание новой серии версий. */
    val existingSeriesId: String? = seriesArg.takeUnless { it == "new" }

    private val eventsChannel = Channel<DrawingUploadEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    suspend fun seriesTitleForExisting(seriesId: String): String =
        drawingRepository.getLatestInSeries(seriesId)?.seriesTitle.orEmpty()

    fun submit(
        pickedUri: Uri?,
        seriesTitleInput: String,
        description: String,
        status: DrawingStatus,
    ) {
        viewModelScope.launch {
            if (pickedUri == null) {
                eventsChannel.send(DrawingUploadEvent.NoFile)
                return@launch
            }
            val snap =
                authSessionRepository.observeSessionSnapshot()
                    .first { it !is SessionSnapshot.Loading }
            val active = snap as? SessionSnapshot.Active
            if (active == null) {
                eventsChannel.send(DrawingUploadEvent.NoSession)
                return@launch
            }

            val result = drawingRepository.addRevision(
                sourceUri = pickedUri,
                existingSeriesId = existingSeriesId,
                seriesTitle = seriesTitleInput,
                changeDescription = description,
                status = status,
                authorUserId = active.userId,
            )

            result.fold(
                onSuccess = { id -> eventsChannel.send(DrawingUploadEvent.Saved(id)) },
                onFailure = { e ->
                    val ev = when ((e as? IllegalArgumentException)?.message) {
                        "unsupported_file" -> DrawingUploadEvent.BadFile
                        "empty_series_title" -> DrawingUploadEvent.EmptyTitle
                        else -> DrawingUploadEvent.Error
                    }
                    eventsChannel.send(ev)
                },
            )
        }
    }
}
