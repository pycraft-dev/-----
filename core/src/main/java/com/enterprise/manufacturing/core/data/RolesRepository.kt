package com.enterprise.manufacturing.core.data

import com.enterprise.manufacturing.core.db.dao.RoleDao
import com.enterprise.manufacturing.core.db.entity.RoleDefinitionEntity
import com.enterprise.manufacturing.core.model.BuiltInRoleCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface RolesRepository {
    fun observeRoles(): Flow<List<RoleDefinitionEntity>>

    suspend fun seedBuiltinRolesIfEmpty()

    suspend fun addRole(code: String, label: String): Result<Unit>
}

@Singleton
class RolesRepositoryImpl @Inject constructor(
    private val roleDao: RoleDao,
) : RolesRepository {

    override fun observeRoles(): Flow<List<RoleDefinitionEntity>> =
        roleDao.observeAllOrdered()

    override suspend fun seedBuiltinRolesIfEmpty() {
        if (roleDao.count() > 0) return
        roleDao.upsertAll(
            listOf(
                RoleDefinitionEntity(BuiltInRoleCodes.ADMIN, "Администратор", 0),
                RoleDefinitionEntity(BuiltInRoleCodes.CONSTRUCTOR, "Конструктор", 10),
                RoleDefinitionEntity(BuiltInRoleCodes.WORKER, "Рабочий", 20),
                RoleDefinitionEntity(BuiltInRoleCodes.MASTER, "Мастер / бригадир", 30),
            ),
        )
    }

    override suspend fun addRole(rawCode: String, label: String): Result<Unit> =
        runCatching {
            val c =
                rawCode.trim().uppercase(Locale.US)
                    .replace(Regex("\\s+"), "_")
                    .replace(INVALID_CHARS, "_")
                    .replace(Regex("_+"), "_")
                    .trim('_')
            check(c.length in CODE_LEN_RANGE) { "bad_code" }
            check(CODE_REGEX.matches(c)) { "bad_code" }
            val l = label.trim()
            check(l.isNotEmpty()) { "empty_label" }
            roleDao.insert(
                RoleDefinitionEntity(
                    code = c,
                    label = l,
                    sortOrder = computeNextSortOrder(),
                ),
            )
            Unit
        }

    private suspend fun computeNextSortOrder(): Int {
        val roles = observeRoles().first()
        return (roles.maxOfOrNull { it.sortOrder } ?: 99) + 1
    }

    companion object {
        private val CODE_REGEX = Regex("^[A-Z][A-Z0-9_]*$")
        private val INVALID_CHARS = Regex("[^A-Z0-9_]")
        private val CODE_LEN_RANGE = 2..32
    }
}
