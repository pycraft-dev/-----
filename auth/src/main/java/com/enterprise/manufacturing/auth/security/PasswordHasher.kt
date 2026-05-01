package com.enterprise.manufacturing.auth.security

/** Хеширование паролей для локальной БД до перехода на серверную аутентификацию. */
interface PasswordHasher {
    fun hash(password: String): String

    fun verify(password: String, storedHash: String): Boolean
}
