package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "defect_messages",
    foreignKeys = [
        ForeignKey(
            entity = DefectEntity::class,
            parentColumns = ["defectId"],
            childColumns = ["defectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["defectId"]),
        Index(value = ["createdAtEpochMs"]),
    ],
)
data class DefectMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val defectId: String,
    val senderUserId: Long,
    /** [com.enterprise.manufacturing.core.model.DefectMessageType] name */
    val messageType: String,
    val body: String,
    val mediaPath: String?,
    val createdAtEpochMs: Long,
    /** [com.enterprise.manufacturing.core.model.SyncStatus] name */
    val syncStatus: String,
)
