package com.enterprise.manufacturing.auth.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

/**
 * PBKDF2-HMAC-SHA256 (API 26+). Формат строки в Room:
 * `iterations:saltB64:hashB64`.
 */
class Pbkdf2PasswordHasher @Inject constructor() : PasswordHasher {

    override fun hash(password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS)
        return "$ITERATIONS:${encode(salt)}:${encode(hash)}"
    }

    override fun verify(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(':')
        if (parts.size != EXPECTED_PARTS) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val salt = decode(parts[1]) ?: return false
        val expected = decode(parts[2]) ?: return false
        val actual = pbkdf2(password, salt, iterations)
        return constantTimeEquals(expected, actual)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val factory = SecretKeyFactory.getInstance(PBKDF2)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return factory.generateSecret(spec).encoded
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray? =
        runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull()

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    private companion object {
        const val PBKDF2 = "PBKDF2WithHmacSHA256"
        const val SALT_BYTES = 16
        const val KEY_LENGTH_BITS = 256
        const val ITERATIONS = 310_000
        const val EXPECTED_PARTS = 3
    }
}
