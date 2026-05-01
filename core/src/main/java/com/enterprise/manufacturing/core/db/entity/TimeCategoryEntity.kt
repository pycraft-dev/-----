package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_categories")
data class TimeCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)
