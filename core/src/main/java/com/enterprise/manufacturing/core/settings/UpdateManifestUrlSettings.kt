package com.enterprise.manufacturing.core.settings

import kotlinx.coroutines.flow.Flow

/**
 * Переопределение URL манифеста обновлений (`update.json`) без пересборки APK.
 * Хранится локально в DataStore на устройстве (задаётся в админ-панели).
 */
interface UpdateManifestUrlSettings {
    /** Только переопределение из DataStore (пусто — нет переопределения). */
    fun observeOverride(): Flow<String>

    /** `override.trim()` если не пусто, иначе [buildTimeDefault].trim(). */
    suspend fun resolveEffectiveUrl(buildTimeDefault: String): String

    /**
     * Сохранить URL. После [trim] пустая строка эквивалентна [clearManifestUrlOverride].
     * Непустое значение должно начинаться с `https://`.
     */
    suspend fun setManifestUrlOverride(raw: String)

    suspend fun clearManifestUrlOverride()
}
