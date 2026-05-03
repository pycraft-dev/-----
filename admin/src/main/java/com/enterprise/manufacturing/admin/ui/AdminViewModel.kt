package com.enterprise.manufacturing.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.admin.R
import com.enterprise.manufacturing.admin.data.AdminUsersRepository
import com.enterprise.manufacturing.core.data.RolesRepository
import com.enterprise.manufacturing.core.db.entity.RoleDefinitionEntity
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.BuiltInRoleCodes
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
    val selectedRoleCode: String = BuiltInRoleCodes.WORKER,
    val plainPassword: String = "",
    val nextLoginPreview: String = "",
    val isSaving: Boolean = false,
    /** Успешно созданный логин для Snackbar/подсказки. */
    val statusCreatedLogin: String? = null,
    val errorMessageRes: Int? = null,
    /** Последнее сообщение после добавления роли (локальное, не из strings). */
    val addRoleMessage: String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminUsersRepository: AdminUsersRepository,
    private val rolesRepository: RolesRepository,
) : ViewModel() {

    val users: StateFlow<List<UserEntity>> =
        adminUsersRepository.observeUsers().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val roles: StateFlow<List<RoleDefinitionEntity>> =
        rolesRepository.observeRoles().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _form = MutableStateFlow(AdminUiState())
    val form: StateFlow<AdminUiState> = _form

    init {
        viewModelScope.launch {
            refreshNextLoginPreview(_form.value.selectedRoleCode)
        }
    }

    private suspend fun refreshNextLoginPreview(roleCode: String) {
        val peek = adminUsersRepository.peekNextLogin(roleCode)
        _form.update { it.copy(nextLoginPreview = peek, addRoleMessage = null) }
    }

    fun onFullNameChange(value: String) {
        _form.update {
            it.copy(fullName = value, errorMessageRes = null, statusCreatedLogin = null, addRoleMessage = null)
        }
    }

    fun onPositionChange(value: String) {
        _form.update {
            it.copy(position = value, errorMessageRes = null, statusCreatedLogin = null, addRoleMessage = null)
        }
    }

    fun onRoleCodeChange(roleCode: String) {
        _form.update {
            it.copy(
                selectedRoleCode = roleCode,
                errorMessageRes = null,
                statusCreatedLogin = null,
                addRoleMessage = null,
            )
        }
        viewModelScope.launch { refreshNextLoginPreview(roleCode) }
    }

    fun onPasswordChange(value: String) {
        _form.update {
            it.copy(plainPassword = value, errorMessageRes = null, statusCreatedLogin = null, addRoleMessage = null)
        }
    }

    /** Добавить новый код роли в справочник (появится в выпадающем списке). */
    fun addCustomRole(code: String, label: String) {
        viewModelScope.launch {
            val result = rolesRepository.addRole(code, label)
            val msg =
                if (result.isSuccess) {
                    null
                } else {
                    when (result.exceptionOrNull()?.message) {
                        "bad_code" ->
                            // Текст из strings в UI по коду ошибки можно заменить на stringRes через callback
                            "Код роли: 2–32 символа латиницы, начинается с буквы (напр. STOREKEEPER)."
                        "empty_label" -> "Укажите отображаемое название роли."
                        else -> "Не удалось добавить роль (возможно, код уже занят)."
                    }
                }
            _form.update { it.copy(addRoleMessage = msg) }
            refreshNextLoginPreview(_form.value.selectedRoleCode)
        }
    }

    fun clearAddRoleMessage() {
        _form.update { it.copy(addRoleMessage = null) }
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
                    addRoleMessage = null,
                )
            }
            val current = _form.value
            val result =
                adminUsersRepository.createUser(
                    fullName = current.fullName,
                    position = current.position,
                    roleCode = current.selectedRoleCode,
                    plainPassword = current.plainPassword,
                )
            if (result.isSuccess) {
                val login = result.getOrThrow()
                val nextPreview =
                    adminUsersRepository.peekNextLogin(current.selectedRoleCode)
                _form.value =
                    AdminUiState(
                        selectedRoleCode = current.selectedRoleCode,
                        nextLoginPreview = nextPreview,
                        statusCreatedLogin = login,
                    )
            } else {
                val resId =
                    when {
                        result.exceptionOrNull()?.message == "unknown role" ->
                            R.string.admin_error_unknown_role

                        result.exceptionOrNull() is IllegalArgumentException ->
                            R.string.admin_error_empty

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
