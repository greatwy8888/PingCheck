package com.pingcheck.app

import android.content.Context
import android.util.Base64
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

data class SshCheckResult(
    val success: Boolean,
    val detail: String,
    val fingerprint: String = "",
    val diagnostics: String = ""
)

class SshChecker(context: Context) {
    private val appContext = context.applicationContext

    fun check(account: ServerAccount): SshCheckResult {
        var session: Session? = null
        var stage = "输入检查"
        var dnsDetail = "尚未执行"
        var tcpDetail = "尚未执行"
        var authChallengeDetail = "服务器尚未发起键盘交互式认证"
        var serverAuthMethods = "尚未获取"
        try {
            if (account.host.isBlank() || account.port !in 1..65535 || account.username.isBlank()) {
                throw IllegalArgumentException("服务器、端口或用户名无效")
            }

            stage = "DNS域名解析"
            val addresses = InetAddress.getAllByName(account.host)
                .sortedBy { if (it.address.size == 4) 0 else 1 }
            if (addresses.isEmpty()) throw IOException("域名没有解析到IP地址")
            dnsDetail = addresses.joinToString(", ") { it.hostAddress }

            stage = "TCP端口连通性测试"
            val tcpErrors = mutableListOf<String>()
            var tcpConnectedAddress = ""
            for (address in addresses.take(4)) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(address, account.port), 4000)
                    }
                    tcpConnectedAddress = address.hostAddress
                    break
                } catch (e: Exception) {
                    tcpErrors.add(
                        address.hostAddress + ": " + e.javaClass.simpleName +
                            (e.message?.let { " - " + it } ?: "")
                    )
                }
            }
            if (tcpConnectedAddress.isBlank()) {
                tcpDetail = "失败；" + tcpErrors.joinToString("；")
                throw IOException("TCP端口无法建立连接")
            }
            tcpDetail = "成功；连接到 " + tcpConnectedAddress + ":" + account.port

            stage = "SSH握手、主机密钥验证和用户认证"
            val jsch = JSch()
            jsch.hostKeyRepository = PinningHostKeyRepository(appContext)

            if (account.authType == "key") {
                val privateKey = account.secret.toByteArray(Charsets.UTF_8)
                val passphrase = account.passphrase.takeIf { it.isNotEmpty() }?.toByteArray(Charsets.UTF_8)
                jsch.addIdentity("serv00-" + account.id, privateKey, null, passphrase)
            }

            session = jsch.getSession(account.username, tcpConnectedAddress, account.port)
            session.setHostKeyAlias(account.host + ":" + account.port)
            session.setConfig("StrictHostKeyChecking", "yes")

            // Serv00 may advertise keyboard-interactive instead of the SSH "password" method.
            // A terminal SSH client handles this by asking for the password interactively.
            // JSch must be explicitly configured for that method and given a UIKeyboardInteractive handler.
            session.setConfig("PreferredAuthentications", "keyboard-interactive,password,publickey")
            session.timeout = 15000
            session.setServerAliveInterval(5000)

            if (account.authType == "password") {
                session.setPassword(account.secret)
                val passwordPromptHandler = object : UserInfo, UIKeyboardInteractive {
                    override fun getPassphrase(): String = ""
                    override fun getPassword(): String = account.secret

                    override fun promptPassword(message: String?): Boolean {
                        authChallengeDetail = "SSH密码提示：" + (message ?: "服务器要求输入密码")
                        return true
                    }

                    override fun promptPassphrase(message: String?): Boolean = false

                    override fun promptYesNo(message: String?): Boolean = false

                    override fun showMessage(message: String?) {
                        if (!message.isNullOrBlank()) {
                            authChallengeDetail = "服务器交互提示：" + message
                        }
                    }

                    override fun promptKeyboardInteractive(
                        destination: String?,
                        name: String?,
                        instruction: String?,
                        prompt: Array<out String>?,
                        echo: BooleanArray?
                    ): Array<String>? {
                        val questions = prompt?.map { it.toString() }.orEmpty()
                        authChallengeDetail = buildString {
                            append("keyboard-interactive提示")
                            if (!destination.isNullOrBlank()) append("；目标：").append(destination)
                            if (!name.isNullOrBlank()) append("；名称：").append(name)
                            if (!instruction.isNullOrBlank()) append("；说明：").append(instruction)
                            if (questions.isNotEmpty()) append("；问题：").append(questions.joinToString(" | "))
                        }
                        // Only send the stored password when the server explicitly asks for a password.
                        // Do not guess at OTP, recovery-code, or other multi-factor prompts.
                        if (questions.size != 1) return null
                        val question = questions[0].lowercase()
                        if (!question.contains("password") && !question.contains("passwd")) return null
                        return arrayOf(account.secret)
                    }
                }
                session.setUserInfo(passwordPromptHandler)
            }

            // connect() completes SSH key exchange, host-key verification and user authentication.
            session.connect(15000)
            val hostKey = session.hostKey
            val fingerprint = hostKey?.getFingerPrint(jsch).orEmpty()
            val diagnostics = buildString {
                appendLine("检测目标：${account.host}:${account.port}")
                appendLine("用户名：${account.username}")
                appendLine("DNS解析：${dnsDetail}")
                appendLine("TCP端口测试：${tcpDetail}")
                appendLine("SSH实际连接IP：${tcpConnectedAddress}")
                appendLine("客户端优先认证方式：keyboard-interactive,password,publickey")
                appendLine("SSH握手与用户认证：成功")
                if (fingerprint.isNotBlank()) append("服务器主机密钥指纹：${fingerprint}")
            }.trim()
            return SshCheckResult(true, "SSH认证成功", fingerprint, diagnostics)
        } catch (e: Exception) {
            val message = (e.message ?: e.javaClass.simpleName).trim()
            val lower = (e.toString() + " " + message).lowercase()
            val authPrefix = "Auth fail for methods '"
            val authStart = message.indexOf(authPrefix)
            if (authStart >= 0) {
                val methods = message.substring(authStart + authPrefix.length).substringBefore("'")
                if (methods.isNotBlank()) serverAuthMethods = methods
            }
            val detail = when {
                lower.contains("auth fail") || lower.contains("authentication failed") ->
                    "SSH认证失败：服务器拒绝了本次认证。客户端现已启用keyboard-interactive密码交互；请核对密码是否正确，以及账号是否被暂停或限制登录"
                lower.contains("hostkey") || lower.contains("host key") ->
                    "主机密钥验证失败：服务器密钥与本机首次记录不一致"
                lower.contains("algorithm negotiation") || lower.contains("no matching") ->
                    "SSH算法协商失败：客户端与服务器没有找到共同的加密算法"
                lower.contains("invalid privatekey") || lower.contains("invalid private key") ->
                    "私钥格式无法识别：请确认粘贴的是完整、受支持的SSH私钥"
                lower.contains("unknownhost") ->
                    "DNS解析失败：无法找到该服务器域名"
                lower.contains("connection refused") ->
                    "TCP连接被拒绝：服务器地址可达，但该端口没有接受连接"
                lower.contains("no route to host") || lower.contains("network is unreachable") ->
                    "网络不可达：当前网络无法到达该服务器"
                lower.contains("timeout") || lower.contains("timed out") ->
                    "连接超时：服务器或端口没有在规定时间内响应"
                lower.contains("tcp端口无法建立连接") ->
                    "SSH端口不可连接：虽然域名/IP可能可以Ping通，但TCP端口${account.port}连接失败"
                lower.contains("end of io stream") || lower.contains("socket is closed") ->
                    "SSH连接被服务器提前关闭：可能是服务器端限制或SSH服务异常"
                else -> "SSH连接失败：${e.javaClass.simpleName}"
            }
            val causeChain = generateSequence(e as Throwable?) { it.cause }
                .take(5)
                .joinToString("\n") { it.javaClass.name + (it.message?.let { msg -> ": " + msg } ?: "") }
            val stack = e.stackTrace.take(8).joinToString("\n") { "  at " + it }
            val diagnostics = buildString {
                appendLine("检测目标：${account.host}:${account.port}")
                appendLine("用户名：${account.username}")
                appendLine("认证方式：${if (account.authType == "key") "SSH私钥" else "密码"}")
                appendLine("失败阶段：${stage}")
                appendLine("DNS解析结果：${dnsDetail}")
                appendLine("TCP端口测试：${tcpDetail}")
                appendLine("客户端优先认证方式：keyboard-interactive,password,publickey")
                appendLine("服务器公布的认证方式：${serverAuthMethods}")
                appendLine("服务器交互认证提示：${authChallengeDetail}")
                appendLine("异常类型：${e.javaClass.name}")
                appendLine("异常原文：${message}")
                appendLine("异常原因链：")
                appendLine(causeChain)
                appendLine("调用位置：")
                append(stack)
                appendLine()
                append("提示：Ping使用ICMP协议，Ping通不代表SSH使用的TCP端口${account.port}可以连接。")
            }.trim()
            return SshCheckResult(false, detail, diagnostics = diagnostics)
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
