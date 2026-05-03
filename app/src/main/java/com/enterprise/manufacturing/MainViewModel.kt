package com.enterprise.manufacturing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    /** Сбрасывает подписки в UI при [android.app.Activity.onNewIntent] с deep-link extra. */
    private val _newIntentTick = MutableStateFlow(0L)
    val newIntentTick: StateFlow<Long> = _newIntentTick.asStateFlow()

    fun notifyLaunchIntentMayHaveChanged() {
        _newIntentTick.update { it + 1L }
    }

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
