package com.enterprise.manufacturing.drawings.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.db.entity.DrawingMessageEntity
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.drawings.data.DrawingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val drawingRepository: DrawingRepository,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val revisionId: Long = savedStateHandle.get<Long>("revisionId") ?: 0L

    val messages: StateFlow<List<DrawingMessageEntity>> =
        drawingRepository.observeDrawingMessages(revisionId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun send(text: String) {
        viewModelScope.launch {
            if (revisionId <= 0L) return@launch
            val snap =
                authSessionRepository.observeSessionSnapshot()
                    .first { it !is SessionSnapshot.Loading }
            val active = snap as? SessionSnapshot.Active ?: return@launch
            drawingRepository.sendDrawingMessage(revisionId, active.userId, text)
        }
    }
}
