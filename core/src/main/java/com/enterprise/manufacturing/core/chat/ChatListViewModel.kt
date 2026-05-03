package com.enterprise.manufacturing.core.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    userDao: UserDao,
    authSessionRepository: AuthSessionRepository,
) : ViewModel() {

    val users: StateFlow<List<UserEntity>> =
        userDao.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val currentUserId: StateFlow<Long?> =
        authSessionRepository.observeSessionSnapshot().map { snap ->
            (snap as? SessionSnapshot.Active)?.userId
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val isAdmin =
        authSessionRepository.observeSessionSnapshot().map { snap ->
            (snap as? SessionSnapshot.Active)?.role == UserRole.ADMIN
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _screenVisible = MutableStateFlow(false)
    val screenVisible = _screenVisible.asStateFlow()

    fun setScreenVisible(visible: Boolean) {
        _screenVisible.value = visible
    }

    fun isUserOnline(userId: Long): Boolean {
        val self = currentUserId.value ?: return false
        return userId == self && _screenVisible.value
    }
}
