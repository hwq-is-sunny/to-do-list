package com.campus.todo.data.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class PasswordHasherTest {

    private val salt = "00112233445566778899aabbccddeeff"

    @Test
    fun verifiesCorrectPasswordAndRejectsWrongPassword() {
        val hash = PasswordHasher.hash("campus-password-123", salt)

        assertTrue(hash.startsWith("pbkdf2_sha256\$"))
        assertTrue(PasswordHasher.verify("campus-password-123", salt, hash).matches)
        assertFalse(PasswordHasher.verify("wrong-password", salt, hash).matches)
    }

    @Test
    fun legacyHashIsAcceptedAndMarkedForUpgrade() {
        val password = "legacy-password"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$password".toByteArray(StandardCharsets.UTF_8))
        val legacyHash = digest.joinToString("") { "%02x".format(it) }

        val result = PasswordHasher.verify(password, salt, legacyHash)

        assertTrue(result.matches)
        assertTrue(result.needsUpgrade)
        assertFalse(PasswordHasher.verify("wrong-password", salt, legacyHash).matches)
    }

    @Test
    fun rejectsMalformedHashAndSalt() {
        assertFalse(PasswordHasher.verify("password", salt, "pbkdf2_sha256\$broken").matches)
        assertFalse(PasswordHasher.verify("password", "bad-salt", "not-a-hash").matches)
    }
}
