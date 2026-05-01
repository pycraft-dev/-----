package com.enterprise.manufacturing.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.auth.R
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginNavEvent {
    data object NavigateHome : LoginNavEvent
}

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessageRes: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginNavEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onLoginChange(value: String) {
        _uiState.update { it.copy(login = value, errorMessageRes = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessageRes = null) }
    }

    fun submit() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessageRes = null) }
            val result = authSessionRepository.signIn(snapshot.login, snapshot.password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(LoginNavEvent.NavigateHome)
                },
                onFailure = { error ->
                    val resId = when (error) {
                        is IllegalArgumentException -> R.string.auth_error_generic
                        else -> R.string.auth_error_invalid_credentials
                    }
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessageRes = resId)
                    }
                },
            )
        }
    }
}
