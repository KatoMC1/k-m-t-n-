package com.autoreply.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class AutoReplyService : Service() {

    companion object {
        private const val TAG = "AutoReplyService"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AutoReplyService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AutoReplyService started")
        // Chạy foreground để không bị kill bởi hệ thống
        startForeground(NOTIFICATION_ID, NotificationHelper.showServiceNotification(this))
        return START_STICKY // Tự khởi động lại nếu bị kill
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AutoReplyService destroyed")
    }
}
