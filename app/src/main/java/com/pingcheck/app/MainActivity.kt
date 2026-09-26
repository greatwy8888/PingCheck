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

class MainActivity : android.app.Activity() {
    private val executor = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var list: LinearLayout
    private val ips = linkedMapOf<String, View>()

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun actionView(
        textValue: String,
        textColor: Int,
        bgColor: Int,
        size: Int = 15
    ): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = size.toFloat()
            setTextColor(textColor)
            gravity = Gravity.CENTER
            includeFontPadding = false
            isSingleLine = true
            background = rounded(bgColor, 14)
        }
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

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Ping检测"
            textSize = 28f
            setTextColor(Color.rgb(15, 23, 42))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
        header.addView(title, LinearLayout.LayoutParams(-1, dp(38)))

        val sub = TextView(this).apply {
            text = "IPv4 / IPv6  ·  真实网络延迟检测"
            textSize = 13f
            setTextColor(Color.rgb(100, 116, 139))
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        header.addView(sub, LinearLayout.LayoutParams(-1, dp(28)))
        root.addView(header, LinearLayout.LayoutParams(-1, dp(72)))

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

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

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

        all.setOnClickListener {
            ips.keys.toList().forEach { ping(it) }
        }

        clear.setOnClickListener {
            ips.clear()
            list.removeAllViews()
        }

        addIp("8.8.8.8")
        addIp("1.1.1.1")
    }

    private fun validate(s: String): Boolean =
        try {
            InetAddress.getByName(s)
            s.isNotBlank() && !s.contains(" ")
        } catch (_: Exception) {
            false
        }

    private fun addIp(ip: String) {
        if (ips.containsKey(ip)) return

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(10), dp(10))
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
        }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
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
            textSize = 14f
            setTextColor(Color.rgb(100, 116, 139))
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        info.addView(ipText, LinearLayout.LayoutParams(-1, dp(30)))
        info.addView(result, LinearLayout.LayoutParams(-1, dp(28)))
        card.addView(info, LinearLayout.LayoutParams(0, -1, 1f))

        val check = actionView("检测", Color.rgb(37, 99, 235), Color.rgb(239, 246, 255), 14)
        check.setOnClickListener { ping(ip) }
        val checkParams = LinearLayout.LayoutParams(dp(68), dp(44))
        checkParams.leftMargin = dp(6)
        card.addView(check, checkParams)

        val del = actionView("删除", Color.rgb(220, 38, 38), Color.rgb(254, 242, 242), 13)
        del.setOnClickListener {
            ips.remove(ip)
            list.removeView(card)
        }
        val delParams = LinearLayout.LayoutParams(dp(68), dp(44))
        delParams.leftMargin = dp(6)
        card.addView(del, delParams)

        val p = LinearLayout.LayoutParams(-1, dp(78))
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
                result.setTextColor(
                    if (r.startsWith("🟢")) Color.rgb(22, 163, 74)
                    else Color.rgb(220, 38, 38)
                )
            }
        }
    }

    private fun realPing(host: String): String {
        return try {
            val addr = InetAddress.getByName(host)
            val start = System.nanoTime()

            val ok = if (addr is Inet6Address) {
                runPingProcess(host, true)
            } else {
                runPingProcess(host, false)
            }

            val ms = (System.nanoTime() - start) / 1_000_000

            if (ok) "🟢 PING通    ${ms} ms"
            else "🔴 PING不通"
        } catch (_: Exception) {
            "🔴 PING不通"
        }
    }

    private fun runPingProcess(host: String, v6: Boolean): Boolean {
        return try {
            val cmd = if (v6) {
                arrayOf("ping", "-6", "-c", "1", "-W", "2", host)
            } else {
                arrayOf("ping", "-c", "1", "-W", "2", host)
            }

            val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val ok = process.waitFor() == 0
            process.destroy()
            ok
        } catch (_: Exception) {
            try {
                InetAddress.getByName(host).isReachable(2500)
            } catch (_: Exception) {
                false
            }
        }
    }
}
