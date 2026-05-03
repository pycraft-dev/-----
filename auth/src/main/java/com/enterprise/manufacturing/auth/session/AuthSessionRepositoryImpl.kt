package com.enterprise.manufacturing.auth.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enterprise.manufacturing.auth.datastore.authSessionDataStore
import com.enterprise.manufacturing.auth.security.PasswordHasher
import com.enterprise.manufacturing.core.data.RolesRepository
import com.enterprise.manufacturing.core.db.dao.RoleDao
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.BuiltInRoleCodes
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
import java.util.Locale
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
    private val roleDao: RoleDao,
    private val rolesRepository: RolesRepository,
    private val passwordHasher: PasswordHasher,
    private val dispatchers: DispatchersProvider,
) : AuthSessionRepository {

    private val dataStore = context.authSessionDataStore
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            rolesRepository.seedBuiltinRolesIfEmpty()
            seedDevAdminIfNeeded()
        }
    }

    override fun observeSessionSnapshot(): Flow<SessionSnapshot> =
        flow {
            emit(SessionSnapshot.Loading)
            dataStore.data.collect { prefs ->
                val snap =
                    withContext(dispatchers.io) {
                        computeSessionFromPrefsAndRoom(prefs)
                    }
                emit(snap)
            }
        }.distinctUntilChanged()

    override fun observeSessionRole(): Flow<String?> =
        observeSessionSnapshot().map { snap ->
            when (snap) {
                is SessionSnapshot.Active -> snap.roleCode
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

            val roleCode = normalizedRole(user.role)
            roleDao.getByCode(roleCode)
                ?: return@withContext Result.failure(IllegalStateException("bad role"))

            dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.id
                prefs[KEY_LOGIN] = user.login
                prefs[KEY_FULL_NAME] = user.fullName
                prefs[KEY_ROLE] = roleCode
            }
            Result.success(Unit)
        }

    override suspend fun signOut() {
        withContext(dispatchers.io) {
            dataStore.edit { it.clear() }
        }
    }

    private suspend fun computeSessionFromPrefsAndRoom(prefs: Preferences): SessionSnapshot {
        val cached = activeSnapshotFromPrefsOnlyOrLoggedOut(prefs)
        val activePrefs = cached as? SessionSnapshot.Active ?: return cached

        val rowById = userDao.getById(activePrefs.userId)
        val row =
            rowById
                ?: userDao.getByLogin(activePrefs.login.trim()).takeUnless {
                    activePrefs.login.isBlank()
                }

        if (row == null) {
            if (prefs[KEY_USER_ID] != null) {
                scheduleDatastoreClear()
            }
            return SessionSnapshot.LoggedOut
        }

        val roleCodeRow = normalizedRole(row.role)
        if (roleDao.getByCode(roleCodeRow) == null) {
            scheduleDatastoreClear()
            return SessionSnapshot.LoggedOut
        }

        if (datastoreNeedsRepair(row, prefs, roleCodeRow)) {
            scheduleDatastoreUserPatch(row, roleCodeRow)
        }

        return SessionSnapshot.Active(
            userId = row.id,
            login = row.login,
            fullName = row.fullName,
            roleCode = roleCodeRow,
        )
    }

    private fun datastoreNeedsRepair(row: UserEntity, prefs: Preferences, normalizedRoleCode: String): Boolean {
        val fullNameStored = prefs[KEY_FULL_NAME].orEmpty().trimEnd()
        val fullNameRoom = row.fullName.trimEnd()
        return prefs[KEY_USER_ID] != row.id ||
            prefs[KEY_LOGIN] != row.login ||
            fullNameStored != fullNameRoom ||
            prefs[KEY_ROLE] != normalizedRoleCode
    }

    private fun scheduleDatastoreClear() {
        scope.launch {
            dataStore.edit { it.clear() }
        }
    }

    private fun scheduleDatastoreUserPatch(row: UserEntity, roleCode: String) {
        scope.launch {
            dataStore.edit { prefsOut ->
                prefsOut[KEY_USER_ID] = row.id
                prefsOut[KEY_LOGIN] = row.login
                prefsOut[KEY_FULL_NAME] = row.fullName
                prefsOut[KEY_ROLE] = roleCode
            }
        }
    }

    private suspend fun activeSnapshotFromPrefsOnlyOrLoggedOut(prefs: Preferences): SessionSnapshot {
        val userId = prefs[KEY_USER_ID] ?: return SessionSnapshot.LoggedOut
        val login = prefs[KEY_LOGIN] ?: return SessionSnapshot.LoggedOut
        val fullName = prefs[KEY_FULL_NAME].orEmpty()
        val roleCodeRaw = prefs[KEY_ROLE] ?: return SessionSnapshot.LoggedOut
        val roleCode = normalizedRole(roleCodeRaw)
        if (roleDao.getByCode(roleCode) == null) return SessionSnapshot.LoggedOut
        return SessionSnapshot.Active(
            userId = userId,
            login = login,
            fullName = fullName,
            roleCode = roleCode,
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
                groupKey = BuiltInRoleCodes.ADMIN,
                role = BuiltInRoleCodes.ADMIN,
            ),
        )
    }

    private fun normalizedRole(raw: String): String =
        raw.trim().uppercase(Locale.US)

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
