package com.enterprise.manufacturing.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.admin.R
import com.enterprise.manufacturing.admin.data.AdminUsersRepository
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val fullName: String = "",
    val position: String = "",
    val groupKey: String = "",
    val selectedRole: UserRole = UserRole.WORKER,
    val plainPassword: String = "",
    val isSaving: Boolean = false,
    /** Успешно созданный логин для Snackbar/подсказки. */
    val statusCreatedLogin: String? = null,
    val errorMessageRes: Int? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminUsersRepository: AdminUsersRepository,
) : ViewModel() {

    val users: StateFlow<List<UserEntity>> =
        adminUsersRepository.observeUsers().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _form = MutableStateFlow(AdminUiState())
    val form: StateFlow<AdminUiState> = _form

    fun onFullNameChange(value: String) {
        _form.update { it.copy(fullName = value, errorMessageRes = null, statusCreatedLogin = null) }
    }

    fun onPositionChange(value: String) {
        _form.update { it.copy(position = value, errorMessageRes = null, statusCreatedLogin = null) }
    }

    fun onGroupKeyChange(value: String) {
        _form.update { it.copy(groupKey = value, errorMessageRes = null, statusCreatedLogin = null) }
    }

    fun onRoleChange(role: UserRole) {
        _form.update { it.copy(selectedRole = role, errorMessageRes = null, statusCreatedLogin = null) }
    }

    fun onPasswordChange(value: String) {
        _form.update { it.copy(plainPassword = value, errorMessageRes = null, statusCreatedLogin = null) }
    }

    fun submit() {
        val snapshot = _form.value
        if (snapshot.isSaving) return
        viewModelScope.launch {
            _form.update {
                it.copy(
                    isSaving = true,
                    errorMessageRes = null,
                    statusCreatedLogin = null,
                )
            }
            val current = _form.value
            val result = adminUsersRepository.createUser(
                fullName = current.fullName,
                position = current.position,
                groupKey = current.groupKey,
                role = current.selectedRole,
                plainPassword = current.plainPassword,
            )
            if (result.isSuccess) {
                val login = result.getOrThrow()
                _form.value = AdminUiState(statusCreatedLogin = login)
            } else {
                val resId = when (result.exceptionOrNull()) {
                    is IllegalArgumentException -> R.string.admin_error_empty
                    else -> R.string.admin_error_create
                }
                _form.update {
                    it.copy(
                        isSaving = false,
                        errorMessageRes = resId,
                    )
                }
            }
        }
    }
}
