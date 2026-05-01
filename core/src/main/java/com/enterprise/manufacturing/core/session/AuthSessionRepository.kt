package com.enterprise.manufacturing.core.session

import com.enterprise.manufacturing.core.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Локальная сессия + вход по учётным данным из Room.
 * Позже JWT заменит сохранение пароля/хеша на серверной модели.
 */
interface AuthSessionRepository {
    fun observeSessionSnapshot(): Flow<SessionSnapshot>

    fun observeSessionRole(): Flow<UserRole?>

    suspend fun signIn(login: String, password: String): Result<Unit>

    suspend fun signOut()
}
