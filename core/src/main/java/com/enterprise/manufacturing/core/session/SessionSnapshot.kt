package com.enterprise.manufacturing.core.session

import com.enterprise.manufacturing.core.model.UserRole

/**
 * Состояние восстановления сессии при старте и после выхода.
 * Пока данные из DataStore не прочитаны — [Loading].
 */
sealed interface SessionSnapshot {
    data object Loading : SessionSnapshot

    data object LoggedOut : SessionSnapshot

    data class Active(
        val userId: Long,
        val login: String,
        val fullName: String,
        val role: UserRole,
    ) : SessionSnapshot
}
