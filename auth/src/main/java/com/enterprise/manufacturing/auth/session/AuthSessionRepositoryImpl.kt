package com.enterprise.manufacturing.auth.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enterprise.manufacturing.auth.datastore.authSessionDataStore
import com.enterprise.manufacturing.auth.security.PasswordHasher
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import com.enterprise.manufacturing.core.session.SessionSnapshot
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Локальная сессия в DataStore + проверка пароля по Room.
 *
 * Для пустой БД один раз создаётся учётная запись **admin_1** с паролем [DEFAULT_ADMIN_PASSWORD]
 * (только dev/bootstrap до экрана создания пользователей в :admin).
 */
@Singleton
class AuthSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher,
    private val dispatchers: DispatchersProvider,
) : AuthSessionRepository {

    private val dataStore = context.authSessionDataStore
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            seedDevAdminIfNeeded()
        }
    }

    override fun observeSessionSnapshot(): Flow<SessionSnapshot> =
        flow {
            emit(SessionSnapshot.Loading)
            dataStore.data.collect { prefs ->
                emit(mapPrefs(prefs))
            }
        }.distinctUntilChanged()

    override fun observeSessionRole(): Flow<UserRole?> =
        observeSessionSnapshot().map { snap ->
            when (snap) {
                is SessionSnapshot.Active -> snap.role
                else -> null
            }
        }.distinctUntilChanged()

    override suspend fun signIn(login: String, password: String): Result<Unit> =
        withContext(dispatchers.io) {
            val trimmedLogin = login.trim()
            if (trimmedLogin.isBlank() || password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("empty credentials"))
            }

            val user = userDao.getByLogin(trimmedLogin)
                ?: return@withContext Result.failure(IllegalStateException("unknown user"))

            val ok = passwordHasher.verify(password, user.passwordHash)
            if (!ok) {
                return@withContext Result.failure(IllegalStateException("bad password"))
            }

            val role = parseRole(user.role) ?: return@withContext Result.failure(
                IllegalStateException("bad role"),
            )

            dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.id
                prefs[KEY_LOGIN] = user.login
                prefs[KEY_FULL_NAME] = user.fullName
                prefs[KEY_ROLE] = role.name
            }
            Result.success(Unit)
        }

    override suspend fun signOut() {
        withContext(dispatchers.io) {
            dataStore.edit { it.clear() }
        }
    }

    private fun mapPrefs(prefs: Preferences): SessionSnapshot {
        val userId = prefs[KEY_USER_ID] ?: return SessionSnapshot.LoggedOut
        val login = prefs[KEY_LOGIN] ?: return SessionSnapshot.LoggedOut
        val fullName = prefs[KEY_FULL_NAME].orEmpty()
        val roleName = prefs[KEY_ROLE] ?: return SessionSnapshot.LoggedOut
        val role = parseRole(roleName) ?: return SessionSnapshot.LoggedOut
        return SessionSnapshot.Active(
            userId = userId,
            login = login,
            fullName = fullName,
            role = role,
        )
    }

    private suspend fun seedDevAdminIfNeeded() {
        if (userDao.countUsers() > 0) return

        val hash = passwordHasher.hash(DEFAULT_ADMIN_PASSWORD)
        userDao.upsert(
            UserEntity(
                login = DEFAULT_ADMIN_LOGIN,
                passwordHash = hash,
                fullName = "Локальный администратор",
                position = "Администратор",
                groupKey = "admin",
                role = UserRole.ADMIN.name,
            ),
        )
    }

    private fun parseRole(raw: String): UserRole? =
        runCatching { UserRole.valueOf(raw) }.getOrNull()

    private companion object {
        val KEY_USER_ID = longPreferencesKey("session_user_id")
        val KEY_LOGIN = stringPreferencesKey("session_login")
        val KEY_FULL_NAME = stringPreferencesKey("session_full_name")
        val KEY_ROLE = stringPreferencesKey("session_role")

        /** Явно зафиксированный bootstrap-логин до появления модуля :admin. */
        const val DEFAULT_ADMIN_LOGIN = "admin_1"

        /**
         * Пароль первого локального администратора. Замените политикой выдачи паролей из :admin.
         */
        const val DEFAULT_ADMIN_PASSWORD = "admin123"
    }
}
