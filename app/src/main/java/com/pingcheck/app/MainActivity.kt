package com.pingcheck.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : android.app.Activity() {
    private val executor = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var list: LinearLayout
    private val ips = linkedMapOf<String, View>()
    private val notes = linkedMapOf<String, String>()
    private val prefs by lazy { getSharedPreferences("ping_list", Context.MODE_PRIVATE) }
    private val savedIps = "saved_ips"
    private val savedData = "saved_ip_data"

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun actionView(textValue: String, textColor: Int, bgColor: Int, size: Int = 15) =
        TextView(this).apply {
            text = textValue
            textSize = size.toFloat()
            setTextColor(textColor)
            gravity = Gravity.CENTER
            includeFontPadding = false
            isSingleLine = true
            background = rounded(bgColor, 14)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(246, 248, 252)
        window.navigationBarColor = Color.rgb(246, 248, 252)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(246, 248, 252))
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }

        val title = TextView(this).apply {
            text = "Ping检测"
            textSize = 26f
            setTextColor(Color.rgb(15, 23, 42))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
        root.addView(title, LinearLayout.LayoutParams(-1, dp(30)))

        val sub = TextView(this).apply {
            text = "IPv4 / IPv6  ·  ICMP网络连通性检测"
            textSize = 11.5f
            setTextColor(Color.rgb(100, 116, 139))
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        root.addView(sub, LinearLayout.LayoutParams(-1, dp(18)))

        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
        }
        val input = EditText(this).apply {
            hint = "输入 IPv4 或 IPv6 地址"
            textSize = 14f
            setTextColor(Color.rgb(15, 23, 42))
            setHintTextColor(Color.rgb(148, 163, 184))
            setSingleLine(true)
            maxLines = 1
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            background = null
            setPadding(dp(10), 0, dp(8), 0)
            minWidth = 0
            minHeight = 0
        }
        inputCard.addView(input, LinearLayout.LayoutParams(0, dp(38), 1f))

        val add = actionView("添加", Color.WHITE, Color.rgb(37, 99, 235), 14)
        val addParams = LinearLayout.LayoutParams(dp(58), dp(38))
        addParams.leftMargin = dp(4)
        inputCard.addView(add, addParams)
        root.addView(inputCard, LinearLayout.LayoutParams(-1, dp(46)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val all = actionView("全部检测", Color.WHITE, Color.rgb(79, 70, 229), 14)
        all.typeface = android.graphics.Typeface.DEFAULT_BOLD
        actions.addView(all, LinearLayout.LayoutParams(0, dp(38), 1f))
        val diagnostic = actionView("网络诊断", Color.rgb(5, 150, 105), Color.rgb(236, 253, 245), 14)
        val diagnosticParams = LinearLayout.LayoutParams(0, dp(38), 1f)
        diagnosticParams.leftMargin = dp(8)
        actions.addView(diagnostic, diagnosticParams)

        val clear = actionView("清空", Color.rgb(71, 85, 105), Color.WHITE, 13)
        val clearParams = LinearLayout.LayoutParams(dp(50), dp(38))
        clearParams.leftMargin = dp(8)
        actions.addView(clear, clearParams)
        val actionParams = LinearLayout.LayoutParams(-1, dp(46))
        actionParams.topMargin = dp(4)
        root.addView(actions, actionParams)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setClipToPadding(false)
            setPadding(0, dp(2), 0, 0)
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        add.setOnClickListener {
            val ip = input.text.toString().trim()
            if (validate(ip)) {
                showAddIpDialog(ip) {
                    input.text.clear()
                }
            } else {
                Toast.makeText(this, "请输入有效的 IPv4 或 IPv6 地址", Toast.LENGTH_SHORT).show()
            }
        }
        all.setOnClickListener { ips.keys.toList().forEach { ping(it) } }
        diagnostic.setOnClickListener { showNetworkDiagnostics() }
        clear.setOnClickListener { confirmClearAll() }

        loadSavedIps()
    }

    private fun loadSavedIps() {
        val data = prefs.getString(savedData, null)
        if (!data.isNullOrBlank()) {
            try {
                val array = JSONArray(data)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val ip = item.optString("ip").trim()
                    val note = item.optString("note")
                    if (ip.isNotEmpty() && validate(ip)) addIp(ip, note)
                }
                if (ips.isNotEmpty()) return
            } catch (_: Exception) {
                // 新格式损坏时继续尝试读取旧格式
            }
        }

        val saved = prefs.getStringSet(savedIps, emptySet())?.toList() ?: emptyList()
        if (saved.isEmpty()) {
            addIp("8.8.8.8", "")
            addIp("1.1.1.1", "")
        } else {
            saved.forEach { addIp(it, "") }
        }
        saveIps()
    }

    private fun saveIps() {
        val array = JSONArray()
        ips.keys.forEach { ip ->
            array.put(JSONObject().apply {
                put("ip", ip)
                put("note", notes[ip] ?: "")
            })
        }
        prefs.edit()
            .putString(savedData, array.toString())
            .putStringSet(savedIps, ips.keys.toSet())
            .apply()
    }

    private fun showAddIpDialog(ip: String, onAdded: () -> Unit) {
        if (ips.containsKey(ip)) {
            Toast.makeText(this, "这个 IP 已经添加", Toast.LENGTH_SHORT).show()
            return
        }

        val noteInput = EditText(this).apply {
            hint = "例如：Google DNS"
            textSize = 15f
            setSingleLine(true)
            maxLines = 1
            setPadding(dp(4), 0, dp(4), 0)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(4), dp(24), 0)
        }
        box.addView(noteInput, LinearLayout.LayoutParams(-1, dp(46)))

        android.app.AlertDialog.Builder(this)
            .setTitle("添加 IP")
            .setMessage(ip)
            .setView(box)
            .setNegativeButton("取消", null)
            .setPositiveButton("添加") { _, _ ->
                val note = noteInput.text.toString().trim()
                addIp(ip, note)
                saveIps()
                onAdded()
            }
            .show()
    }

    private fun validate(s: String): Boolean {
        if (s.isBlank() || s.contains(" ")) return false
        return try {
            val addr = InetAddress.getByName(s)
            addr.hostAddress != null && (addr is java.net.Inet4Address || addr is Inet6Address)
        } catch (_: Exception) {
            false
        }
    }

    private fun addIp(ip: String, note: String = "") {
        if (ips.containsKey(ip)) return
        notes[ip] = note

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(4), dp(5), dp(4))
            background = rounded(Color.WHITE, 14)
            elevation = dp(1).toFloat()
        }

        val ipText = TextView(this).apply {
            text = ip
            textSize = 14f
            setTextColor(Color.rgb(15, 23, 42))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }
        card.addView(ipText, LinearLayout.LayoutParams(0, dp(38), 1.05f))

        val result = TextView(this).apply {
            text = note.ifBlank { "等待检测" }
            textSize = 11.5f
            setTextColor(Color.rgb(100, 116, 139))
            gravity = Gravity.CENTER
            includeFontPadding = false
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        card.addView(result, LinearLayout.LayoutParams(0, dp(38), 1.35f))

        val check = actionView("检测", Color.rgb(37, 99, 235), Color.rgb(239, 246, 255), 12)
        check.typeface = android.graphics.Typeface.DEFAULT_BOLD
        check.setOnClickListener { ping(ip) }
        val checkParams = LinearLayout.LayoutParams(dp(48), dp(32))
        checkParams.leftMargin = dp(5)
        card.addView(check, checkParams)

        val del = actionView("删除", Color.rgb(220, 38, 38), Color.rgb(254, 242, 242), 12)
        del.typeface = android.graphics.Typeface.DEFAULT_BOLD
        del.setOnClickListener { confirmDeleteIp(ip, card) }
        val delParams = LinearLayout.LayoutParams(dp(48), dp(32))
        delParams.leftMargin = dp(5)
        card.addView(del, delParams)

        val p = LinearLayout.LayoutParams(-1, dp(46))
        p.topMargin = dp(4)
        list.addView(card, p)
        ips[ip] = card
    }

    private fun confirmDeleteIp(ip: String, card: View) {
        android.app.AlertDialog.Builder(this)
            .setTitle("删除 IP")
            .setMessage("确定要删除 " + ip + " 吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                ips.remove(ip)
                notes.remove(ip)
                list.removeView(card)
                saveIps()
            }
            .show()
    }

    private fun confirmClearAll() {
        if (ips.isEmpty()) {
            Toast.makeText(this, "当前没有可清空的 IP", Toast.LENGTH_SHORT).show()
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("清空全部 IP")
            .setMessage("确定要删除当前保存的全部 " + ips.size + " 个 IP 吗？此操作不可恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("全部清空") { _, _ ->
                ips.clear()
                notes.clear()
                list.removeAllViews()
                saveIps()
            }
            .show()
    }


    private fun ping(ip: String) {
        val card = ips[ip] as? LinearLayout ?: return
        val result = card.getChildAt(1) as TextView
        result.text = notes[ip].orEmpty().ifBlank { "检测中…" }
        result.setTextColor(Color.rgb(100, 116, 139))

        executor.execute {
            val r = realPing(ip)
            main.post {
                if (!ips.containsKey(ip)) return@post
                result.text = displayPingResult(ip, r)
                result.setTextColor(
                    if (r.contains(" ms")) Color.rgb(22, 163, 74)
                    else Color.rgb(100, 116, 139)
                )
            }
        }
    }

    private fun realPing(host: String): String {
        return try {
            val addr = InetAddress.getByName(host)
            val result = runPingProcess(host, addr is Inet6Address)
            when (result.status) {
                PingStatus.SUCCESS -> result.latency?.let { "${formatLatency(it)} ms" } ?: "—"
                PingStatus.TIMEOUT -> "—"
                PingStatus.UNAVAILABLE -> "—"
                else -> "—"
            }
        } catch (_: Exception) {
            "—"
        }
    }

    private fun displayPingResult(ip: String, pingValue: String): String {
        val note = notes[ip].orEmpty().trim()
        return if (note.isEmpty()) pingValue else "$note  $pingValue"
    }

    private enum class PingStatus { SUCCESS, TIMEOUT, UNAVAILABLE, FAILED }
    private data class PingResult(val status: PingStatus, val latency: Double? = null, val detail: String = "")

    private fun runPingProcess(host: String, v6: Boolean): PingResult {
        val family = if (v6) "-6" else "-4"
        val commands = listOf(
            arrayOf("/system/bin/toybox", "ping", family, "-c", "4", "-W", "3", host),
            arrayOf("/system/bin/toybox", "ping", family, "-c", "4", "-w", "12", host),
            if (v6) arrayOf("/system/bin/ping6", "-c", "4", "-W", "3", host)
            else arrayOf("/system/bin/ping", "-c", "4", "-W", "3", host),
            if (v6) arrayOf("ping6", "-c", "4", "-W", "3", host)
            else arrayOf("ping", "-c", "4", "-W", "3", host)
        )

        var executableFound = false
        var timeoutSeen = false
        var timeoutDetail = ""

        for (cmd in commands) {
            try {
                val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
                executableFound = true
                val output = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()
                process.destroy()

                val lower = output.lowercase()
                val latency = parsePingTime(output)

                if (latency != null &&
                    (lower.contains("bytes from") ||
                     lower.contains("icmp_seq") ||
                     lower.contains("icmp_req") ||
                     lower.contains("reply from"))) {
                    return PingResult(PingStatus.SUCCESS, latency, cleanDetail(output))
                }

                if (lower.contains("permission denied") ||
                    lower.contains("operation not permitted") ||
                    lower.contains("cannot create socket") ||
                    (lower.contains("socket") && lower.contains("denied"))) {
                    return PingResult(PingStatus.UNAVAILABLE, detail = cleanDetail(output))
                }

                if (lower.contains("100% packet loss") ||
                    lower.contains("100.0% packet loss") ||
                    lower.contains("0 received") ||
                    lower.contains("request timeout") ||
                    lower.contains("timed out") ||
                    lower.contains("network is unreachable") ||
                    lower.contains("no route to host")) {
                    timeoutSeen = true
                    timeoutDetail = cleanDetail(output)
                }
            } catch (_: java.io.IOException) {
            } catch (_: Exception) {
            }
        }

        return when {
            timeoutSeen -> PingResult(PingStatus.TIMEOUT, detail = timeoutDetail)
            executableFound -> PingResult(PingStatus.FAILED)
            else -> PingResult(PingStatus.UNAVAILABLE)
        }
    }

    private fun showNetworkDiagnostics() {
        val dialog = android.app.Dialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(12))
            background = rounded(Color.WHITE, 22)
        }

        val title = TextView(this).apply {
            text = "网络诊断"
            textSize = 21f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(15, 23, 42))
        }
        box.addView(title, LinearLayout.LayoutParams(-1, dp(34)))

        val note = TextView(this).apply {
            text = "真实ICMP与DNS分开显示，不把TCP连接当成Ping。"
            textSize = 12f
            setTextColor(Color.rgb(100, 116, 139))
        }
        box.addView(note, LinearLayout.LayoutParams(-1, dp(42)))

        val result = TextView(this).apply {
            text = "正在诊断，请稍候…"
            textSize = 13f
            setTextColor(Color.rgb(30, 41, 59))
            isSingleLine = false
            setHorizontallyScrolling(false)
        }
        val scroll = ScrollView(this).apply { addView(result) }
        box.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val close = actionView("关闭", Color.WHITE, Color.rgb(37, 99, 235), 14)
        val closeParams = LinearLayout.LayoutParams(-1, dp(44))
        closeParams.topMargin = dp(10)
        box.addView(close, closeParams)
        close.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(box)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(dp(340), dp(560))

        executor.execute {
            val report = buildNetworkReport()
            main.post { if (dialog.isShowing) result.text = report }
        }
    }

    private fun buildNetworkReport(): String {
        val sb = StringBuilder()

        val gateway = findDefaultGateway()
        sb.append("【本地网关 ICMP】\n")
        if (gateway != null && gateway != "0.0.0.0" && gateway != "::") {
            sb.append(formatDiagnosticPing(gateway))
        } else {
            sb.append("⚠️ 未找到默认网关")
        }
        sb.append("\n\n")

        sb.append("【IPv4 ICMP】\n")
        sb.append(formatDiagnosticPing("8.8.8.8")).append("\n\n")
        sb.append(formatDiagnosticPing("1.1.1.1")).append("\n\n")

        sb.append("【IPv6 ICMP】\n")
        sb.append(formatDiagnosticPing("2001:4860:4860::8888")).append("\n\n")
        sb.append(formatDiagnosticPing("2606:4700:4700::1111")).append("\n\n")

        sb.append("【DNS解析】\n")
        sb.append(dnsReport()).append("\n\n")

        sb.append("【当前网络】\n")
        sb.append(linkReport())
        return sb.toString()
    }

    private fun formatDiagnosticPing(host: String): String {
        val v6 = host.contains(":")
        val r = runPingProcess(host, v6)
        return when (r.status) {
            PingStatus.SUCCESS -> {
                val stats = extractPingStats(r.detail)
                "🟢 " + host + "  →  " +
                    (r.latency?.let { formatLatency(it) + " ms" } ?: "收到ICMP回包") +
                    if (stats.isNotEmpty()) "\n" + stats else ""
            }
            PingStatus.TIMEOUT -> "🔴 " + host + "  →  超时\n" + firstLines(r.detail, if (v6) 6 else 4)
            PingStatus.UNAVAILABLE -> "⚠️ " + host + "  →  ICMP不可用\n" + firstLines(r.detail, 6)
            PingStatus.FAILED -> "🔴 " + host + "  →  Ping失败\n" + firstLines(r.detail, 6)
        }
    }

    private fun extractPingStats(text: String): String {
        val lines = text.lines().filter { it.isNotBlank() }
        return lines.filter {
            val s = it.lowercase()
            s.contains("packet loss") ||
            s.contains("packets transmitted") ||
            s.contains("round-trip") ||
            s.contains("rtt min/avg/max")
        }.joinToString("\n")
    }

    private fun firstLines(text: String, maxLines: Int): String =
        text.lines().filter { it.isNotBlank() }.take(maxLines).joinToString("\n")

    private fun findDefaultGateway(): String? {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return null
            val lp: LinkProperties = cm.getLinkProperties(network) ?: return null
            lp.routes.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun dnsReport(): String {
        return try {
            val start = System.currentTimeMillis()
            val addresses = InetAddress.getAllByName("dns.google")
            val ms = System.currentTimeMillis() - start
            "🟢 dns.google 解析成功，" + addresses.size + " 个地址，耗时约 " + ms + " ms\n" +
                addresses.take(4).joinToString("\n") { "  " + it.hostAddress }
        } catch (e: Exception) {
            "🔴 dns.google 解析失败：" + (e.message ?: "未知错误")
        }
    }

    private fun linkReport(): String {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "🔴 当前没有活动网络"
            val lp: LinkProperties = cm.getLinkProperties(network)
                ?: return "⚠️ 无法读取当前网络参数"

            val lines = mutableListOf<String>()
            lp.linkAddresses.forEach { lines.add("本机地址：" + it.address.hostAddress) }
            lp.dnsServers.forEach { lines.add("DNS：" + it.hostAddress) }
            lp.routes.filter { it.isDefaultRoute }.forEach {
                lines.add("默认网关：" + (it.gateway?.hostAddress ?: "系统路由"))
            }
            lines.joinToString("\n").ifEmpty {
                "⚠️ 当前网络没有可显示的地址/DNS/默认路由信息"
            }
        } catch (e: Exception) {
            "⚠️ 网络信息读取失败：" + (e.message ?: "未知错误")
        }
    }

    private fun cleanDetail(output: String): String {
        val text = output.trim().replace("\r", "")
        return if (text.length > 800) text.take(800) + "\n[输出过长，仅显示前800字符]" else text
    }

    private fun formatLatency(value: Double): String =
        if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)

    private fun parsePingTime(output: String): Double? {
        val matcher = Pattern.compile("time[=<]([0-9]+(?:\\.[0-9]+)?)").matcher(output)
        return if (matcher.find()) {
            matcher.group(1)?.toDoubleOrNull()
        } else null
    }
}
