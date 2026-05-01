package com.enterprise.manufacturing.core.model

/**
 * Роли доменной модели. [MASTER] — производственный мастер / бригадир.
 */
enum class UserRole {
    ADMIN,
    CONSTRUCTOR,
    WORKER,
    MASTER,
}
