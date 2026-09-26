package com.pingcheck.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

class MainActivity : android.app.Activity() {
    private val executor = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var list: LinearLayout
    private val ips = linkedMapOf<String, View>()

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
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val title = TextView(this).apply {
            text = "Ping检测"
            textSize = 28f
            setTextColor(Color.rgb(15, 23, 42))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
        root.addView(title, LinearLayout.LayoutParams(-1, dp(40)))

        val sub = TextView(this).apply {
            text = "IPv4 / IPv6  ·  ICMP网络连通性检测"
            textSize = 13f
            setTextColor(Color.rgb(100, 116, 139))
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        root.addView(sub, LinearLayout.LayoutParams(-1, dp(28)))

        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
        }
        val input = EditText(this).apply {
            hint = "输入 IPv4 或 IPv6 地址"
            textSize = 16f
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
        inputCard.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))

        val add = actionView("添加", Color.WHITE, Color.rgb(37, 99, 235), 15)
        val addParams = LinearLayout.LayoutParams(dp(76), dp(48))
        addParams.leftMargin = dp(4)
        inputCard.addView(add, addParams)
        root.addView(inputCard, LinearLayout.LayoutParams(-1, dp(64)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val all = actionView("全部检测", Color.WHITE, Color.rgb(79, 70, 229), 15)
        actions.addView(all, LinearLayout.LayoutParams(0, dp(48), 1f))
        val clear = actionView("清空列表", Color.rgb(71, 85, 105), Color.WHITE, 15)
        val clearParams = LinearLayout.LayoutParams(dp(94), dp(48))
        clearParams.leftMargin = dp(8)
        actions.addView(clear, clearParams)
        val actionParams = LinearLayout.LayoutParams(-1, dp(48))
        actionParams.topMargin = dp(10)
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
                addIp(ip)
                input.text.clear()
            } else {
                Toast.makeText(this, "请输入有效的 IPv4 或 IPv6 地址", Toast.LENGTH_SHORT).show()
            }
        }
        all.setOnClickListener { ips.keys.toList().forEach { ping(it) } }
        clear.setOnClickListener { ips.clear(); list.removeAllViews() }

        addIp("8.8.8.8")
        addIp("1.1.1.1")
    }

    private fun validate(s: String): Boolean =
        try {
            InetAddress.getByName(s)
            s.isNotBlank() && !s.contains(" ")
        } catch (_: Exception) { false }

    private fun addIp(ip: String) {
        if (ips.containsKey(ip)) return

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(10), dp(10))
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val ipText = TextView(this).apply {
            text = ip
            textSize = 16f
            setTextColor(Color.rgb(15, 23, 42))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }

        val result = TextView(this).apply {
            text = "等待检测"
            textSize = 13f
            setTextColor(Color.rgb(100, 116, 139))
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        top.addView(ipText, LinearLayout.LayoutParams(-1, dp(28)))
        top.addView(result, LinearLayout.LayoutParams(-1, dp(46)))
        card.addView(top, LinearLayout.LayoutParams(-1, dp(74)))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val check = actionView("检测", Color.rgb(37, 99, 235), Color.rgb(239, 246, 255), 14)
        check.setOnClickListener { ping(ip) }
        buttons.addView(check, LinearLayout.LayoutParams(0, dp(40), 1f))

        val del = actionView("删除", Color.rgb(220, 38, 38), Color.rgb(254, 242, 242), 13)
        del.setOnClickListener { ips.remove(ip); list.removeView(card) }
        val delParams = LinearLayout.LayoutParams(0, dp(40), 1f)
        delParams.leftMargin = dp(8)
        buttons.addView(del, delParams)

        card.addView(buttons, LinearLayout.LayoutParams(-1, dp(40)))

        val p = LinearLayout.LayoutParams(-1, dp(124))
        p.topMargin = dp(10)
        list.addView(card, p)
        ips[ip] = card
    }

    private fun ping(ip: String) {
        val card = ips[ip] as? LinearLayout ?: return
        val info = card.getChildAt(0) as LinearLayout
        val result = info.getChildAt(1) as TextView
        result.text = "正在检测，请稍候…"
        result.setTextColor(Color.rgb(100, 116, 139))

        executor.execute {
            val r = realPing(ip)
            main.post {
                if (!ips.containsKey(ip)) return@post
                result.text = r
                result.setTextColor(if (r.startsWith("🟢")) Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38))
            }
        }
    }

    private fun realPing(host: String): String {
        return try {
            val addr = InetAddress.getByName(host)
            val result = runPingProcess(host, addr is Inet6Address)
            when (result.status) {
                PingStatus.SUCCESS -> if (result.latency != null) "🟢 PING通    ${formatLatency(result.latency)} ms" else "🟢 PING通"
                PingStatus.TIMEOUT -> "🔴 PING超时" + if (result.detail.isNotEmpty()) "\n" + result.detail else ""
                PingStatus.UNAVAILABLE -> "⚠️ ICMP不可用" + if (result.detail.isNotEmpty()) "\n" + result.detail else ""
                else -> "🔴 PING失败" + if (result.detail.isNotEmpty()) "\n" + result.detail else ""
            }
        } catch (_: Exception) {
            "🔴 地址解析失败"
        }
    }

    private enum class PingStatus { SUCCESS, TIMEOUT, UNAVAILABLE, FAILED }
    private data class PingResult(val status: PingStatus, val latency: Double? = null, val detail: String = "")

    private fun runPingProcess(host: String, v6: Boolean): PingResult {
        // Android Toybox 自带 ping，直接调用 toybox ping 可避免 /system/bin/ping
        // 在不同厂商 ROM 上的符号链接和参数差异。
        val family = if (v6) "-6" else "-4"
        val commands = listOf(
            arrayOf("/system/bin/toybox", "ping", family, "-c", "1", "-W", "3", host),
            arrayOf("/system/bin/toybox", "ping", family, "-c", "1", "-w", "4", host),
            if (v6) arrayOf("/system/bin/ping6", "-c", "1", "-W", "3", host)
            else arrayOf("/system/bin/ping", "-c", "1", "-W", "3", host),
            if (v6) arrayOf("ping6", "-c", "1", "-W", "3", host)
            else arrayOf("ping", "-c", "1", "-W", "3", host)
        )

        var executableFound = false
        var timeoutSeen = false
        var timeoutDetail = ""

        for (cmd in commands) {
            try {
                val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
                executableFound = true
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                process.destroy()

                val lower = output.lowercase()
                val latency = parsePingTime(output)

                // Toybox 成功输出包含 "icmp_seq" 和 "time="。
                // 以真实 ICMP 回包和 RTT 为最终依据，不依赖进程退出码。
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
                    lower.contains("socket") && lower.contains("denied")) {
                    return PingResult(PingStatus.UNAVAILABLE, detail = cleanDetail(output))
                }

                if (lower.contains("100% packet loss") ||
                    lower.contains("100.0% packet loss") ||
                    lower.contains("request timeout") ||
                    lower.contains("timed out")) {
                    timeoutSeen = true
                    timeoutDetail = cleanDetail(output)
                }
            } catch (_: java.io.IOException) {
                // 当前 ROM 没有该命令，继续尝试下一种。
            } catch (_: Exception) {
                // 继续尝试下一种兼容方式。
            }
        }

        return when {
            timeoutSeen -> PingResult(PingStatus.TIMEOUT, detail = timeoutDetail)
            executableFound -> PingResult(PingStatus.FAILED)
            else -> PingResult(PingStatus.UNAVAILABLE)
        }
    }

    private fun cleanDetail(output: String): String {
        val text = output.trim().replace("\r", "")
        return if (text.length > 180) text.take(180) + "…" else text
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
