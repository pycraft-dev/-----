package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "defects")
data class DefectEntity(
    @PrimaryKey val defectId: String,
    val authorUserId: Long,
    /** Исполнитель; позже назначение из админки / правил маршрутизации. */
    val assignedUserId: Long?,
    val deviceId: String,
    val createdAtEpochMs: Long,
    val photoPath: String?,
    val videoPath: String?,
    val notes: String?,
    /** [com.enterprise.manufacturing.core.model.SyncStatus] name */
    val syncStatus: String,
)
