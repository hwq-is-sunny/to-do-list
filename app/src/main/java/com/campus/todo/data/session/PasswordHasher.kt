package com.campus.todo.data.session

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing for the app's local demo accounts.
 *
 * New hashes use PBKDF2-HMAC-SHA256. Legacy salted SHA-256 hashes are accepted
 * only to support existing installs and are upgraded after a successful login.
 * This is local-device authentication, not a replacement for server auth.
 */
internal object PasswordHasher {
    const val ITERATIONS = 210_000
    private const val ALGORITHM = "pbkdf2_sha256"
    private const val KEY_BITS = 256

    data class Verification(val matches: Boolean, val needsUpgrade: Boolean)

    fun hash(password: String, saltHex: String): String {
        val salt = decodeHexSalt(saltHex)
        val derived = derive(password, salt, ITERATIONS)
        return listOf(ALGORITHM, ITERATIONS.toString(), Base64.getEncoder().encodeToString(derived)).joinToString("\$")
    }

    fun verify(password: String, saltHex: String, storedHash: String): Verification {
        if (storedHash.startsWith("$ALGORITHM\$")) {
            val parts = storedHash.split('$')
            if (parts.size != 3 || parts[0] != ALGORITHM) {
                return Verification(matches = false, needsUpgrade = false)
            }
            val iterations = parts[1].toIntOrNull()
                ?: return Verification(matches = false, needsUpgrade = false)
            if (iterations !in 100_000..1_000_000) {
                return Verification(matches = false, needsUpgrade = false)
            }
            val expected = runCatching { Base64.getDecoder().decode(parts[2]) }
                .getOrElse { return Verification(matches = false, needsUpgrade = false) }
            val actual = runCatching { derive(password, decodeHexSalt(saltHex), iterations) }
                .getOrElse { return Verification(matches = false, needsUpgrade = false) }
            val matches = MessageDigest.isEqual(actual, expected)
            return Verification(matches, matches && iterations < ITERATIONS)
        }

        // Backward compatibility with the original salt + SHA-256 format.
        val legacy = sha256Hex("$saltHex:$password")
        val matches = MessageDigest.isEqual(
            legacy.toByteArray(StandardCharsets.UTF_8),
            storedHash.toByteArray(StandardCharsets.UTF_8)
        )
        return Verification(matches = matches, needsUpgrade = matches)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun decodeHexSalt(saltHex: String): ByteArray {
        require(saltHex.length == 32 && saltHex.all { it in "0123456789abcdefABCDEF" }) {
            "Invalid password salt"
        }
        return ByteArray(saltHex.length / 2) { index ->
            saltHex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun sha256Hex(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
