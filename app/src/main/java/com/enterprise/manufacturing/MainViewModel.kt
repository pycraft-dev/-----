package com.enterprise.manufacturing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    val sessionSnapshot: StateFlow<SessionSnapshot> =
        authSessionRepository.observeSessionSnapshot().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SessionSnapshot.Loading,
        )

    fun signOut() {
        viewModelScope.launch {
            authSessionRepository.signOut()
        }
    }
}
