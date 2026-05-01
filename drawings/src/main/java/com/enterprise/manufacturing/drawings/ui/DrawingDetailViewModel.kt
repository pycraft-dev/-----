package com.enterprise.manufacturing.drawings.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.db.entity.DrawingRevisionEntity
import com.enterprise.manufacturing.core.model.DrawingStatus
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.drawings.data.DrawingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val drawingRepository: DrawingRepository,
    authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val revisionId: Long = checkNotNull(savedStateHandle.get<Long>("revisionId"))

    private val revisionFlow = drawingRepository.observeRevision(revisionId)

    val revision: StateFlow<DrawingRevisionEntity?> =
        revisionFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val seriesRevisions: StateFlow<List<DrawingRevisionEntity>> =
        revisionFlow.flatMapLatest { rev ->
            if (rev == null) flowOf(emptyList())
            else drawingRepository.observeSeriesRevisions(rev.seriesId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val role: StateFlow<UserRole?> =
        authSessionRepository.observeSessionSnapshot().map { snap ->
            (snap as? SessionSnapshot.Active)?.role
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun setStatus(status: DrawingStatus) {
        viewModelScope.launch {
            drawingRepository.updateStatus(revisionId, status)
        }
    }
}
