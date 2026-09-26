package com.pingcheck.app

import android.graphics.Color
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(245,247,251)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245,247,251))
            setPadding(20,18,20,18)
        }

        val title = TextView(this).apply {
            text = "Ping检测"
            textSize = 28f
            setTextColor(Color.rgb(17,24,39))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title, LinearLayout.LayoutParams(-1,60))

        val sub = TextView(this).apply {
            text = "IPv4 / IPv6 真实网络检测"
            textSize = 14f
            setTextColor(Color.rgb(107,114,128))
            gravity = Gravity.CENTER
        }
        root.addView(sub, LinearLayout.LayoutParams(-1,40))

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = "输入 IP 地址"
            textSize = 16f
            setSingleLine(true)
            setBackgroundResource(R.drawable.bg_input)
            setPadding(16,0,16,0)
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0,56,1f))

        val add = Button(this).apply {
            text = "添加"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.bg_button)
            isAllCaps = false
        }
        val ap = LinearLayout.LayoutParams(92,56)
        ap.leftMargin = 10
        inputRow.addView(add, ap)
        root.addView(inputRow)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val all = Button(this).apply {
            text = "全部检测"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.bg_button)
            isAllCaps = false
        }
        val clear = Button(this).apply {
            text = "清空"
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.DKGRAY)
        }
        actions.addView(all, LinearLayout.LayoutParams(0,54,1f))
        val cp = LinearLayout.LayoutParams(80,54)
        cp.leftMargin = 8
        actions.addView(clear, cp)
        val aparams = LinearLayout.LayoutParams(-1,64)
        aparams.topMargin = 12
        root.addView(actions, aparams)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
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
            setBackgroundResource(R.drawable.bg_card)
            elevation = 2f
        }

        val infoBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val name = TextView(this).apply {
            text = ip
            textSize = 16f
            setTextColor(Color.rgb(17,24,39))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val result = TextView(this).apply {
            text = "等待检测"
            textSize = 13f
            setTextColor(Color.rgb(107,114,128))
        }

        infoBox.addView(name)
        infoBox.addView(result)
        card.addView(infoBox, LinearLayout.LayoutParams(0,-2,1f))

        val btn = TextView(this).apply {
            text = "检测"
            textSize = 14f
            setTextColor(Color.rgb(37,99,235))
            gravity = Gravity.CENTER
            setPadding(16,10,16,10)
        }
        btn.setOnClickListener { ping(ip) }
        card.addView(btn, LinearLayout.LayoutParams(76,48))

        val del = TextView(this).apply {
            text = "删除"
            textSize = 13f
            setTextColor(Color.rgb(156,163,175))
            gravity = Gravity.CENTER
        }
        del.setOnClickListener {
            ips.remove(ip)
            list.removeView(card)
        }
        card.addView(del, LinearLayout.LayoutParams(40,48))

        val p = LinearLayout.LayoutParams(-1,76)
        p.topMargin = 10
        list.addView(card, p)
        ips[ip] = card
    }

    private fun ping(ip: String) {
        val card = ips[ip] ?: return
        val infoBox = (card as LinearLayout).getChildAt(0) as LinearLayout
        val tv = infoBox.getChildAt(1) as TextView
        tv.text = "检测中…"
        tv.setTextColor(Color.rgb(107,114,128))

        executor.execute {
            val r = realPing(ip)
            main.post {
                tv.text = r
                tv.setTextColor(
                    if (r.startsWith("🟢")) Color.rgb(22,163,74)
                    else Color.rgb(220,38,38)
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
            if (ok) "PING通  ${ms} ms" else "PING不通"
        } catch (_: Exception) {
            "🔴 PING不通"
        }
    }

    private fun runPingProcess(host: String, v6: Boolean): Boolean {
        return try {
            val cmd = if (v6) {
                arrayOf("ping","-6","-c","1","-W","2",host)
            } else {
                arrayOf("ping","-c","1","-W","2",host)
            }
            val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val ok = p.waitFor() == 0
            p.destroy()
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
