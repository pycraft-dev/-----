package com.enterprise.manufacturing.defect.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.db.entity.DefectMessageEntity
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.defect.data.DefectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DefectChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val defectRepository: DefectRepository,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val defectId: String = savedStateHandle.get<String>("defectId").orEmpty()

    val messages: StateFlow<List<DefectMessageEntity>> =
        defectRepository.observeMessages(defectId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun send(text: String) {
        viewModelScope.launch {
            val snap = authSessionRepository.observeSessionSnapshot().first()
            val active = snap as? SessionSnapshot.Active ?: return@launch
            defectRepository.sendTextMessage(defectId, active.userId, text)
        }
    }
}
