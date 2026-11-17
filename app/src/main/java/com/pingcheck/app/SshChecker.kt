package com.pingcheck.app

import android.content.Context
import android.util.Base64
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import java.security.MessageDigest

data class SshCheckResult(
    val success: Boolean,
    val detail: String,
    val fingerprint: String = ""
)

class SshChecker(context: Context) {
    private val appContext = context.applicationContext

    fun check(account: ServerAccount): SshCheckResult {
        var session: Session? = null
        return try {
            val jsch = JSch()
            val pinStore = PinningHostKeyRepository(appContext)
            jsch.hostKeyRepository = pinStore

            if (account.authType == "key") {
                val privateKey = account.secret.toByteArray(Charsets.UTF_8)
                val passphrase = account.passphrase.takeIf { it.isNotEmpty() }?.toByteArray(Charsets.UTF_8)
                jsch.addIdentity("serv00-" + account.id, privateKey, null, passphrase)
            }

            session = jsch.getSession(account.username, account.host, account.port)
            session.setHostKeyAlias(account.host + ":" + account.port)
            session.setConfig("StrictHostKeyChecking", "yes")
            session.setConfig("PreferredAuthentications", "publickey,password")
            session.timeout = 15000
            session.setServerAliveInterval(5000)

            if (account.authType == "password") {
                session.setPassword(account.secret)
            }

            // Session.connect() completes SSH authentication. No shell command is executed.
            session.connect(15000)
            val hostKey = session.hostKey
            val fingerprint = hostKey?.getFingerPrint(jsch).orEmpty()
            SshCheckResult(true, "SSH认证成功", fingerprint)
        } catch (e: Exception) {
            val message = (e.message ?: e.javaClass.simpleName).trim()
            val lower = message.lowercase()
            val detail = when {
                lower.contains("auth fail") || lower.contains("authentication failed") ->
                    "认证失败：请检查用户名、密码或私钥"
                lower.contains("hostkey") || lower.contains("host key") ->
                    "服务器主机密钥验证失败：密钥可能已变化，请核实服务器"
                lower.contains("timeout") || lower.contains("timed out") ->
                    "连接超时：请检查服务器地址、端口和网络"
                lower.contains("unknownhost") ->
                    "无法解析服务器地址"
                else -> "SSH连接失败：" + message.take(180)
            }
            SshCheckResult(false, detail)
        } finally {
            try { session?.disconnect() } catch (_: Exception) {}
        }
    }
}

/**
 * Trust-on-first-use pinning: the first observed SSH host key is stored locally.
 * Later key changes are rejected before user authentication is attempted.
 */
private class PinningHostKeyRepository(context: Context) : HostKeyRepository {
    private val prefs = context.getSharedPreferences("serv00_host_key_pins", Context.MODE_PRIVATE)

    override fun check(host: String?, key: ByteArray?): Int {
        if (host.isNullOrBlank() || key == null) return HostKeyRepository.NOT_INCLUDED
        val digest = MessageDigest.getInstance("SHA-256").digest(key)
        val fingerprint = "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP).trimEnd('=')
        val saved = prefs.getString(host, null)
        if (saved == null) {
            prefs.edit().putString(host, fingerprint).apply()
            return HostKeyRepository.OK
        }
        return if (saved == fingerprint) HostKeyRepository.OK else HostKeyRepository.CHANGED
    }

    override fun add(hostkey: HostKey?, ui: UserInfo?) {}

    override fun remove(host: String?, type: String?) {
        if (host != null) prefs.edit().remove(host).apply()
    }

    override fun remove(host: String?, type: String?, key: ByteArray?) {
        if (host != null) prefs.edit().remove(host).apply()
    }

    override fun getHostKey(): Array<HostKey> = emptyArray()

    override fun getKnownHostsRepositoryID(): String = "serv00-pinned-hostkeys"

    override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()
}
