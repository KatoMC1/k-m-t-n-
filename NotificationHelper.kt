package com.autoreply.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "auto_reply_channel"
    private const val CHANNEL_NAME = "Tin nhắn tự động"
    private var notifId = 1000

    fun showReplySentNotification(context: Context, phoneNumber: String, trigger: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Thông báo khi đã gửi tin nhắn tự động"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Format số điện thoại cho đẹp
        val displayPhone = formatPhoneForDisplay(phoneNumber)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("✅ Đã gửi tin nhắn tự động")
            .setContentText("Gửi tới $displayPhone ($trigger)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notifId++, notification)
    }

    fun showServiceNotification(context: Context): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "service_channel",
                "Dịch vụ nền",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, "service_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Auto Reply đang chạy")
            .setContentText("Đang lắng nghe tin nhắn và cuộc gọi...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun formatPhoneForDisplay(phone: String): String {
        val digits = phone.replace(Regex("[^0-9+]"), "")
        return if (digits.length >= 10) {
            "${digits.take(4)} ${digits.substring(4, 7)} ${digits.substring(7)}"
        } else digits
    }
}
