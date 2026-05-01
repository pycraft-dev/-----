package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Локальная учётная запись (этап 1: схема под будущий модуль :auth).
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["login"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val login: String,
    /** PBKDF2 / bcrypt и т.д. — конкретный алгоритм задаст модуль :auth. */
    val passwordHash: String,
    val fullName: String,
    val position: String,
    /** Ключ группы для генерации логинов (рабочий, конструктор, …). */
    val groupKey: String,
    /** Сериализованное имя [com.enterprise.manufacturing.core.model.UserRole]. */
    val role: String,
)
