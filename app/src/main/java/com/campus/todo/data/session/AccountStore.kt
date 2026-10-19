package com.campus.todo.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.SecureRandom
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.accountDataStore by preferencesDataStore(name = "campus_todo_accounts")

class AccountStore(private val context: Context) {

    private val keyAccounts = stringPreferencesKey("accounts_json")

    sealed class RegisterResult {
        object Success : RegisterResult()
        object Exists : RegisterResult()
        object InvalidPassword : RegisterResult()
    }

    sealed class LoginResult {
        object Success : LoginResult()
        object AccountNotFound : LoginResult()
        object PasswordMismatch : LoginResult()
    }

    suspend fun register(username: String, password: String): RegisterResult {
        val name = username.trim()
        if (name.isBlank() || password.length < 8) return RegisterResult.InvalidPassword

        var result: RegisterResult = RegisterResult.Success
        context.accountDataStore.edit { prefs ->
            val accounts = parseAccounts(prefs[keyAccounts].orEmpty())
            if (accounts.any { it.username.equals(name, ignoreCase = true) }) {
                result = RegisterResult.Exists
                return@edit
            }
            val salt = randomSaltHex()
            val hash = PasswordHasher.hash(password, salt)
            val updated = accounts + LocalAccount(name, salt, hash, System.currentTimeMillis())
            prefs[keyAccounts] = toJson(updated)
            result = RegisterResult.Success
        }
        return result
    }

    suspend fun verifyLogin(username: String, password: String): LoginResult {
        val name = username.trim()
        if (name.isBlank() || password.isEmpty()) return LoginResult.AccountNotFound

        val account = parseAccounts(
            context.accountDataStore.data.first()[keyAccounts].orEmpty()
        ).firstOrNull { it.username.equals(name, ignoreCase = true) }
            ?: return LoginResult.AccountNotFound

        val isLegacyHash = !account.passwordHash.startsWith("pbkdf2_sha256\$")
        var verifiedPassword = password
        var verification = PasswordHasher.verify(password, account.salt, account.passwordHash)
        if (!verification.matches && isLegacyHash && password != password.trim()) {
            // The original login flow trimmed passwords before hashing and verification.
            verifiedPassword = password.trim()
            verification = PasswordHasher.verify(verifiedPassword, account.salt, account.passwordHash)
        }
        if (!verification.matches) return LoginResult.PasswordMismatch

        if (verification.needsUpgrade) {
            upgradePasswordHash(account, verifiedPassword)
        }
        return LoginResult.Success
    }

    private suspend fun upgradePasswordHash(account: LocalAccount, password: String) {
        context.accountDataStore.edit { prefs ->
            val current = parseAccounts(prefs[keyAccounts].orEmpty())
            val updated = current.map { item ->
                if (item.username.equals(account.username, ignoreCase = true) &&
                    item.passwordHash == account.passwordHash
                ) {
                    val newSalt = randomSaltHex()
                    item.copy(
                        salt = newSalt,
                        passwordHash = PasswordHasher.hash(password, newSalt)
                    )
                } else {
                    item
                }
            }
            prefs[keyAccounts] = toJson(updated)
        }
    }

    private data class LocalAccount(
        val username: String,
        val salt: String,
        val passwordHash: String,
        val createdAt: Long
    )

    private fun parseAccounts(raw: String): List<LocalAccount> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val obj = json.optJSONObject(i) ?: continue
                    val username = obj.optString("username").trim()
                    val salt = obj.optString("salt").trim()
                    val hash = obj.optString("passwordHash").trim()
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    if (username.isNotBlank() && salt.isNotBlank() && hash.isNotBlank()) {
                        add(LocalAccount(username, salt, hash, createdAt))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun toJson(accounts: List<LocalAccount>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject().apply {
                    put("username", account.username)
                    put("salt", account.salt)
                    put("passwordHash", account.passwordHash)
                    put("createdAt", account.createdAt)
                }
            )
        }
        return array.toString()
    }

    private fun randomSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
