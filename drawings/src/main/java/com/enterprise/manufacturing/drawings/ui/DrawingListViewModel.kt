package com.enterprise.manufacturing.drawings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.db.entity.DrawingRevisionEntity
import com.enterprise.manufacturing.core.model.BuiltInRoleCodes
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.drawings.data.DrawingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DrawingListViewModel @Inject constructor(
    drawingRepository: DrawingRepository,
    authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    val revisions: StateFlow<List<DrawingRevisionEntity>> =
        drawingRepository.observeAllRevisions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val canUpload: StateFlow<Boolean> =
        authSessionRepository.observeSessionSnapshot().map { snap ->
            snap is SessionSnapshot.Active &&
                (snap.roleCode == BuiltInRoleCodes.ADMIN ||
                    snap.roleCode == BuiltInRoleCodes.CONSTRUCTOR)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )
}
