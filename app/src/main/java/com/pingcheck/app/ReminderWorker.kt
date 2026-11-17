package com.pingcheck.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = SecureStore(applicationContext)
        val accounts = store.loadAccounts()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val now = System.currentTimeMillis()
        val due = accounts.filter { account ->
            account.lastSuccess > 0L &&
                (now - account.lastSuccess) / 86400000L >= 75L &&
                account.lastReminderDate != today
        }

        if (due.isEmpty()) return Result.success()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel("serv00_reminders", "Serv00账号提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        due.forEachIndexed { index, account ->
            val days = (now - account.lastSuccess) / 86400000L
            val notification = NotificationCompat.Builder(applicationContext, "serv00_reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Serv00账号登录提醒")
                .setContentText(account.name + " 已距上次成功登录 " + days + " 天，请及时检查")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    account.name + "（" + account.username + "@" + account.host + "）已距上次成功登录 " +
                        days + " 天。建议尽快进行一次 SSH 认证检测。"
                ))
                .setAutoCancel(true)
                .build()
            manager.notify(7500 + index, notification)
            account.lastReminderDate = today
        }
        store.saveAccounts(accounts)
        return Result.success()
    }
}
