package com.enterprise.manufacturing.core.session

import kotlinx.coroutines.flow.Flow

/**
 * Локальная сессия + вход по учётным данным из Room.
 * Позже JWT заменит сохранение пароля/хеша на серверной модели.
 */
interface AuthSessionRepository {
    fun observeSessionSnapshot(): Flow<SessionSnapshot>

    /** Код активной роли из [com.enterprise.manufacturing.core.db.entity.RoleDefinitionEntity.code] или null. */
    fun observeSessionRole(): Flow<String?>

    suspend fun signIn(login: String, password: String): Result<Unit>

    suspend fun signOut()
}
