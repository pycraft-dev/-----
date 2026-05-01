package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "time_entries",
    foreignKeys = [
        ForeignKey(
            entity = TimeCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("userId"),
        Index(value = ["userId", "endEpochMs"]),
    ],
)
data class TimeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val categoryId: Long?,
    val taskTitle: String,
    val note: String?,
    val startEpochMs: Long,
    /** `null` — интервал ещё идёт (таймер активен). */
    val endEpochMs: Long?,
)
