package com.example.myagorawebapp // 替换为实际包名，如 lgh13127.apktest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class BackgroundKeepAliveService : Service() {
    private val CHANNEL_ID = "BackgroundServiceChannel"
    private val NOTIFICATION_ID = 1001
    private var wakeLock: PowerManager.WakeLock? = null // Kotlin 空安全，用?标记可空

    override fun onCreate() {
        super.onCreate()
        // 1. 获取并持有唤醒锁
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp:WakeLockTag"
        ).apply {
            acquire(10 * 60 * 1000L) // 10分钟超时，按需调整
        }

        // 2. 创建前台服务通知渠道（Android 8.0+）
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("应用后台运行中")
            .setContentText("为保证听说功能正常，应用将持续后台运行")
            .setSmallIcon(R.mipmap.ic_launcher) // 替换为你的应用图标
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 3. 启动为前台服务
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY：服务被杀死后系统尝试重启
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放唤醒锁（Kotlin 空安全判断）
        wakeLock?.takeIf { it.isHeld }?.release()
        // 重启服务，防止被意外杀死
        val restartIntent = Intent(this, BackgroundKeepAliveService::class.java)
        startService(restartIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 无需绑定，返回null
    }

    // 创建通知渠道（Android 8.0+ 必须）
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "后台服务通知",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}