package com.pingcheck.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private val executor = Executors.newFixedThreadPool(3)
    private lateinit var store: SecureStore
    private lateinit var accounts: MutableList<ServerAccount>
    private lateinit var list: LinearLayout
    private val checkingIds = mutableSetOf<String>()
    private var checkingAll = false
    private val blue = Color.rgb(37, 99, 235)
    private val dark = Color.rgb(15, 23, 42)
    private val muted = Color.rgb(100, 116, 139)
    private val pageBackground = Color.rgb(246, 248, 252)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun button(text: String, color: Int = blue, textColor: Int = Color.WHITE): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            background = rounded(color, 12)
            isClickable = true
            isFocusable = true
        }

    private fun label(text: String, size: Float = 13f, color: Int = dark, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SecureStore(this)
        accounts = store.loadAccounts()
        window.statusBarColor = pageBackground
        window.navigationBarColor = pageBackground
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(8))
            setBackgroundColor(pageBackground)
        }

        val title = label("Serv00账号检测", 25f, dark, true).apply { gravity = Gravity.CENTER }
        root.addView(title, LinearLayout.LayoutParams(-1, dp(38)))
        val subtitle = label("多账号管理 · SSH认证检测 · 详细故障定位", 12f, muted).apply { gravity = Gravity.CENTER }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, dp(24)))

        val add = button("＋ 添加账号", blue)
        val batch = button("检测全部", Color.rgb(79, 70, 229))
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topRow.addView(add, LinearLayout.LayoutParams(0, dp(44), 1f))
        val batchParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) }
        topRow.addView(batch, batchParams)
        val topParams = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(5) }
        root.addView(topRow, topParams)

        val securityNote = label(
            "先检测DNS和SSH端口，再验证SSH账号；Ping通不代表SSH端口可连接。凭据仅加密保存在本机。",
            11f, muted
        )
        securityNote.setPadding(dp(3), dp(3), dp(3), dp(7))
        root.addView(securityNote)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setClipToPadding(false)
            addView(list)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        add.setOnClickListener { showAccountDialog(null) }
        batch.setOnClickListener { checkAllAccounts() }

        refreshList()
        scheduleReminder()
        requestNotificationPermission()
    }

    private fun scheduleReminder() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "serv00_account_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7501)
        }
    }

    private fun save() = store.saveAccounts(accounts)

    private fun refreshList() {
        list.removeAllViews()
        if (accounts.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(60), dp(20), dp(30))
            }
            empty.addView(label("还没有添加Serv00账号", 18f, dark, true).apply { gravity = Gravity.CENTER })
            empty.addView(label("点击上方“添加账号”，填写用户名、服务器和认证信息。", 13f, muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
            })
            list.addView(empty)
            return
        }

        accounts.forEach { account ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(11), dp(13), dp(11))
                background = rounded(Color.WHITE, 16)
                elevation = dp(1).toFloat()
            }
            val heading = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            heading.addView(label(account.name, 16f, dark, true), LinearLayout.LayoutParams(0, dp(27), 1f))
            val statusColor = when {
                account.status == "SSH认证成功" -> Color.rgb(22, 163, 74)
                account.status == "检测中…" -> blue
                account.lastCheck > 0L -> Color.rgb(220, 38, 38)
                else -> muted
            }
            val status = label(account.status, 12f, statusColor, true)
            heading.addView(status)
            card.addView(heading)

            val address = label(
                account.username + "@" + account.host + ":" + account.port +
                    if (account.authType == "key") "  ·  SSH密钥" else "  ·  密码",
                12f, muted
            )
            address.setPadding(0, dp(3), 0, dp(5))
            card.addView(address)

            val lastText = if (account.lastSuccess > 0L) {
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(account.lastSuccess))
                val days = ((System.currentTimeMillis() - account.lastSuccess) / 86400000L).coerceAtLeast(0)
                "最近成功登录：$date（$days 天前）"
            } else {
                "最近成功登录：暂无记录"
            }
            card.addView(label(lastText, 11.5f, muted))

            if (account.lastSuccess > 0L &&
                (System.currentTimeMillis() - account.lastSuccess) / 86400000L >= 75L) {
                val warning = label("提醒：距离上次成功登录已达75天，请尽快检测。", 11.5f, Color.rgb(194, 65, 12), true)
                warning.setPadding(0, dp(5), 0, 0)
                card.addView(warning)
            }

            if (account.detail.isNotBlank()) {
                val detail = label(account.detail, 11.5f, muted)
                detail.setPadding(0, dp(4), 0, 0)
                card.addView(detail)
            }

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val check = button(if (checkingIds.contains(account.id)) "检测中" else "检测", Color.rgb(239, 246, 255), blue)
            val history = button("记录", Color.rgb(241, 245, 249), Color.rgb(51, 65, 85))
            val details = button("详情", Color.rgb(254, 249, 195), Color.rgb(133, 77, 14))
            val edit = button("编辑", Color.rgb(241, 245, 249), Color.rgb(51, 65, 85))
            val delete = button("删除", Color.rgb(254, 242, 242), Color.rgb(220, 38, 38))
            listOf(check, history, details, edit, delete).forEachIndexed { index, view ->
                val params = LinearLayout.LayoutParams(0, dp(34), 1f)
                if (index > 0) params.leftMargin = dp(4)
                actions.addView(view, params)
            }
            check.isEnabled = !checkingIds.contains(account.id) && !checkingAll
            check.alpha = if (check.isEnabled) 1f else 0.55f
            check.setOnClickListener { checkAccount(account) }
            history.setOnClickListener { showHistory(account) }
            details.setOnClickListener { showDetail(account) }
            edit.setOnClickListener { showAccountDialog(account) }
            delete.setOnClickListener { confirmDelete(account) }
            val actionParams = LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(8) }
            card.addView(actions, actionParams)

            val cardParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) }
            list.addView(card, cardParams)
        }
    }

    private fun showAccountDialog(existing: ServerAccount?) {
        val name = EditText(this).apply {
            hint = "账号备注，例如：我的Serv00"
            setSingleLine(true)
            setText(existing?.name ?: "")
        }
        val username = EditText(this).apply {
            hint = "Serv00用户名"
            setSingleLine(true)
            setText(existing?.username ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        val host = EditText(this).apply {
            hint = "服务器，例如 cache9.serv00.com"
            setSingleLine(true)
            setText(existing?.host ?: "cache9.serv00.com")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        val port = EditText(this).apply {
            hint = "SSH端口"
            setSingleLine(true)
            setText((existing?.port ?: 22).toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val auth = Spinner(this)
        auth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("密码登录", "SSH私钥登录"))
        auth.setSelection(if (existing?.authType == "key") 1 else 0)

        val secret = EditText(this).apply {
            hint = "输入Serv00登录密码"
            setText(existing?.secret ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            minHeight = dp(48)
        }
        val passphrase = EditText(this).apply {
            hint = "私钥口令（没有可留空）"
            setText(existing?.passphrase ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        fun updateSecretMode() {
            if (auth.selectedItemPosition == 1) {
                secret.hint = "粘贴完整SSH私钥内容（例如以 BEGIN OPENSSH PRIVATE KEY 开头）"
                secret.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                secret.minHeight = dp(145)
                secret.gravity = Gravity.TOP
            } else {
                secret.hint = "输入Serv00登录密码"
                secret.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                secret.minHeight = dp(48)
                secret.gravity = Gravity.CENTER_VERTICAL
            }
        }
        auth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updateSecretMode()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateSecretMode()

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }
        fun addField(caption: String, view: View, height: Int = 48) {
            box.addView(label(caption, 12f, muted, true), LinearLayout.LayoutParams(-1, dp(22)))
            box.addView(view, LinearLayout.LayoutParams(-1, dp(height)))
            val gap = View(this)
            box.addView(gap, LinearLayout.LayoutParams(-1, dp(5)))
        }
        addField("账号备注", name)
        addField("用户名", username)
        addField("服务器地址", host)
        addField("SSH端口", port)
        addField("认证方式", auth)
        addField("密码或私钥内容", secret, if (existing?.authType == "key") 145 else 64)
        addField("私钥口令（可选）", passphrase)

        val scroll = ScrollView(this).apply {
            addView(box)
            isFillViewport = true
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "添加Serv00账号" else "编辑账号")
            .setView(scroll)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val userValue = username.text.toString().trim()
                val hostValue = host.text.toString().trim()
                val portValue = port.text.toString().trim().toIntOrNull()
                val secretValue = secret.text.toString()
                if (userValue.isBlank() || hostValue.isBlank() || hostValue.contains(" ") ||
                    portValue == null || portValue !in 1..65535 || secretValue.isBlank()) {
                    Toast.makeText(this, "请完整填写用户名、服务器、有效端口和认证信息", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val account = existing ?: ServerAccount(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    username = "",
                    host = "",
                    port = 22,
                    authType = "password",
                    secret = ""
                )
                account.name = name.text.toString().trim().ifBlank { userValue }
                account.username = userValue
                account.host = hostValue
                account.port = portValue
                account.authType = if (auth.selectedItemPosition == 1) "key" else "password"
                account.secret = secretValue
                account.passphrase = passphrase.text.toString()
                if (existing == null) accounts.add(account)
                save()
                refreshList()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun checkAccount(account: ServerAccount) {
        if (checkingAll || checkingIds.contains(account.id)) return
        if (account.lastCheck > 0L && System.currentTimeMillis() - account.lastCheck < 20000L) {
            Toast.makeText(this, "刚刚检测过，请稍后再试，避免频繁认证", Toast.LENGTH_SHORT).show()
            return
        }
        checkingIds.add(account.id)
        account.status = "检测中…"
        account.detail = "正在建立SSH连接并验证账号认证"
        refreshList()
        executor.execute {
            val result = SshChecker(this).check(account)
            mainUpdate {
                checkingIds.remove(account.id)
                applyResult(account, result)
            }
        }
    }

    private fun checkAllAccounts() {
        if (checkingAll || checkingIds.isNotEmpty()) return
        if (accounts.isEmpty()) {
            Toast.makeText(this, "请先添加账号", Toast.LENGTH_SHORT).show()
            return
        }
        checkingAll = true
        refreshList()
        val snapshot = accounts.toList()
        executor.execute {
            snapshot.forEachIndexed { index, account ->
                if (accounts.any { it.id == account.id }) {
                    mainUpdate {
                        checkingIds.add(account.id)
                        account.status = "检测中…"
                        account.detail = "批量检测中（" + (index + 1) + "/" + snapshot.size + "）"
                        refreshList()
                    }
                    val result = SshChecker(this).check(account)
                    mainUpdate {
                        checkingIds.remove(account.id)
                        applyResult(account, result)
                    }
                    if (index < snapshot.lastIndex) Thread.sleep(2500)
                }
            }
            mainUpdate {
                checkingAll = false
                refreshList()
            }
        }
    }

    private fun applyResult(account: ServerAccount, result: SshCheckResult) {
        val now = System.currentTimeMillis()
        account.lastCheck = now
        account.status = if (result.success) "SSH认证成功" else "登录失败"
        account.detail = result.detail
        account.diagnostics = result.diagnostics.ifBlank { result.detail }
        if (result.success) account.lastSuccess = now
        account.history.add(0, CheckRecord(now, result.success, account.detail, account.diagnostics))
        while (account.history.size > 30) account.history.removeAt(account.history.lastIndex)
        save()
        refreshList()
    }

    private fun mainUpdate(action: () -> Unit) {
        runOnUiThread { action() }
    }

    private fun showHistory(account: ServerAccount) {
        val text = if (account.history.isEmpty()) {
            "暂无检测记录"
        } else {
            account.history.joinToString("\n\n") { record ->
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.time))
                (if (record.success) "✓ 成功  " else "✕ 失败  ") + date + "\n" + record.detail +
                    if (record.diagnostics.isNotBlank()) "\n\n详细诊断：\n" + record.diagnostics else ""
            }
        }
        val view = ScrollView(this).apply {
            val body = label(text, 13f, dark)
            body.setPadding(dp(18), dp(12), dp(18), dp(12))
            addView(body)
        }
        AlertDialog.Builder(this)
            .setTitle(account.name + " · 检测历史")
            .setView(view)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun showDetail(account: ServerAccount) {
        val diagnosticText = buildString {
            appendLine("账号：${account.name}")
            appendLine("检测状态：${account.status}")
            appendLine("结果摘要：${account.detail.ifBlank { "尚未检测" }}")
            appendLine()
            append(account.diagnostics.ifBlank { "暂无详细诊断，请先执行一次检测。" })
        }
        val body = label(diagnosticText, 12.5f, dark).apply {
            setTextIsSelectable(true)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val scroll = ScrollView(this).apply {
            addView(body)
            isFillViewport = true
        }
        AlertDialog.Builder(this)
            .setTitle(account.name + " · SSH错误详情")
            .setView(scroll)
            .setPositiveButton("复制详情") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Serv00 SSH检测详情", diagnosticText))
                Toast.makeText(this, "检测详情已复制", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun confirmDelete(account: ServerAccount) {
        AlertDialog.Builder(this)
            .setTitle("删除账号")
            .setMessage("确定删除“" + account.name + "”及其本机检测记录吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                accounts.removeAll { it.id == account.id }
                save()
                refreshList()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}
