package com.enterprise.manufacturing.defect.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.db.entity.DefectEntity
import com.enterprise.manufacturing.defect.data.DefectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DefectListViewModel @Inject constructor(
    defectRepository: DefectRepository,
) : ViewModel() {

    val defects: StateFlow<List<DefectEntity>> =
        defectRepository.observeDefects().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
