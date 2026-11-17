package com.pingcheck.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CheckRecord(
    val time: Long,
    val success: Boolean,
    val detail: String,
    val diagnostics: String = ""
)

data class ServerAccount(
    val id: String,
    var name: String,
    var username: String,
    var host: String,
    var port: Int,
    var authType: String,
    var secret: String,
    var passphrase: String = "",
    var lastCheck: Long = 0L,
    var lastSuccess: Long = 0L,
    var status: String = "尚未检测",
    var detail: String = "",
    var diagnostics: String = "",
    var lastReminderDate: String = "",
    val history: MutableList<CheckRecord> = mutableListOf()
)

class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("serv00_secure_data", Context.MODE_PRIVATE)
    private val alias = "serv00_account_store_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getKey(alias, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun loadAccounts(): MutableList<ServerAccount> {
        val encoded = prefs.getString("accounts", null) ?: return mutableListOf()
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = packed.copyOfRange(0, 12)
            val encrypted = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            decodeAccounts(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveAccounts(accounts: List<ServerAccount>) {
        val json = encodeAccounts(accounts).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(json)
        val packed = cipher.iv + encrypted
        prefs.edit().putString("accounts", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    private fun encodeAccounts(accounts: List<ServerAccount>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            val history = JSONArray()
            account.history.take(30).forEach { record ->
                history.put(JSONObject().apply {
                    put("time", record.time)
                    put("success", record.success)
                    put("detail", record.detail)
                    put("diagnostics", record.diagnostics)
                })
            }
            array.put(JSONObject().apply {
                put("id", account.id)
                put("name", account.name)
                put("username", account.username)
                put("host", account.host)
                put("port", account.port)
                put("authType", account.authType)
                put("secret", account.secret)
                put("passphrase", account.passphrase)
                put("lastCheck", account.lastCheck)
                put("lastSuccess", account.lastSuccess)
                put("status", account.status)
                put("detail", account.detail)
                put("diagnostics", account.diagnostics)
                put("lastReminderDate", account.lastReminderDate)
                put("history", history)
            })
        }
        return array.toString()
    }

    private fun decodeAccounts(raw: String): MutableList<ServerAccount> {
        val result = mutableListOf<ServerAccount>()
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val account = ServerAccount(
                id = item.optString("id"),
                name = item.optString("name"),
                username = item.optString("username"),
                host = item.optString("host"),
                port = item.optInt("port", 22),
                authType = item.optString("authType", "password"),
                secret = item.optString("secret"),
                passphrase = item.optString("passphrase"),
                lastCheck = item.optLong("lastCheck"),
                lastSuccess = item.optLong("lastSuccess"),
                status = item.optString("status", "尚未检测"),
                detail = item.optString("detail"),
                diagnostics = item.optString("diagnostics"),
                lastReminderDate = item.optString("lastReminderDate")
            )
            val history = item.optJSONArray("history") ?: JSONArray()
            for (j in 0 until history.length()) {
                val record = history.getJSONObject(j)
                account.history.add(
                    CheckRecord(
                        record.optLong("time"),
                        record.optBoolean("success"),
                        record.optString("detail"),
                        record.optString("diagnostics")
                    )
                )
            }
            result.add(account)
        }
        return result
    }
}
