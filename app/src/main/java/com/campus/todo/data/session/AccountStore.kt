package com.campus.todo.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
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
    }

    sealed class LoginResult {
        object Success : LoginResult()
        object AccountNotFound : LoginResult()
        object PasswordMismatch : LoginResult()
    }

    suspend fun register(username: String, password: String): RegisterResult {
        val name = username.trim()
        val pwd = password.trim()
        if (name.isBlank() || pwd.isBlank()) return RegisterResult.Exists
        var result: RegisterResult = RegisterResult.Success
        context.accountDataStore.edit { prefs ->
            val accounts = parseAccounts(prefs[keyAccounts].orEmpty())
            if (accounts.any { it.username.equals(name, ignoreCase = true) }) {
                result = RegisterResult.Exists
                return@edit
            }
            val salt = randomSaltHex()
            val createdAt = System.currentTimeMillis()
            val hash = sha256Hex("$salt:$pwd")
            val updated = accounts + LocalAccount(name, salt, hash, createdAt)
            prefs[keyAccounts] = toJson(updated)
            result = RegisterResult.Success
        }
        return result
    }

    suspend fun verifyLogin(username: String, password: String): LoginResult {
        val name = username.trim()
        val pwd = password.trim()
        if (name.isBlank() || pwd.isBlank()) return LoginResult.AccountNotFound
        val data = context.accountDataStore.data.first()
        val account = parseAccounts(data[keyAccounts].orEmpty())
            .firstOrNull { it.username.equals(name, ignoreCase = true) }
        return when {
            account == null -> LoginResult.AccountNotFound
            account.passwordHash == sha256Hex("${account.salt}:$pwd") -> LoginResult.Success
            else -> LoginResult.PasswordMismatch
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
                        add(
                            LocalAccount(
                                username = username,
                                salt = salt,
                                passwordHash = hash,
                                createdAt = createdAt
                            )
                        )
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

    private fun sha256Hex(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
