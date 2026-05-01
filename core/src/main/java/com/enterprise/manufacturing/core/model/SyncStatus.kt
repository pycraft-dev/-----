package com.enterprise.manufacturing.core.model

/**
 * Статус синхронизации сущностей offline-first (Room → сервер).
 */
enum class SyncStatus {
    PENDING,
    SENT,
    FAILED,
}
