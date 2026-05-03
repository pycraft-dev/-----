package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Настраиваемые коды ролей (ADMIN, MASTER, COSTOM_1 …). [UserEntity.role] хранит [code].
 */
@Entity(
    tableName = "app_roles",
    indices = [
        Index(value = ["sortOrder"]),
    ],
)
data class RoleDefinitionEntity(
    @PrimaryKey val code: String,
    val label: String,
    /** Порядок в выпадающем списке. */
    val sortOrder: Int,
)
