package com.enterprise.manufacturing.core.session

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
        /** Код роли из таблицы [com.enterprise.manufacturing.core.db.entity.RoleDefinitionEntity]. */
        val roleCode: String,
    ) : SessionSnapshot
}
