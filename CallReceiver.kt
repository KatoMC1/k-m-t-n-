package com.autoreply.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        // Lưu trạng thái cuộc gọi trước đó để phát hiện cuộc gọi nhỡ
        private var lastCallState = TelephonyManager.CALL_STATE_IDLE
        private var incomingNumber = ""
        private var callStartTime = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return

        val prefs = PrefsManager(context)
        if (!prefs.isEnabled || !prefs.replyOnCall) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

        Log.d(TAG, "Phone state: $state, number: $phoneNumber")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Điện thoại đang rung - có cuộc gọi đến
                if (phoneNumber.isNotEmpty()) {
                    incomingNumber = phoneNumber
                    callStartTime = System.currentTimeMillis()
                    Log.d(TAG, "Incoming call from: $incomingNumber")
                }
                lastCallState = TelephonyManager.CALL_STATE_RINGING
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Đã bắt máy
                lastCallState = TelephonyManager.CALL_STATE_OFFHOOK
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Cuộc gọi kết thúc
                if (lastCallState == TelephonyManager.CALL_STATE_RINGING && incomingNumber.isNotEmpty()) {
                    // Trạng thái trước là đang rung → đây là cuộc gọi nhỡ (missed call)
                    Log.d(TAG, "Missed call from: $incomingNumber")
                    handleMissedCall(context, incomingNumber, prefs)
                }
                lastCallState = TelephonyManager.CALL_STATE_IDLE
                incomingNumber = ""
            }
        }
    }

    private fun handleMissedCall(context: Context, phoneNumber: String, prefs: PrefsManager) {
        // Kiểm tra cooldown
        if (!prefs.canSendReply(phoneNumber)) {
            val remaining = prefs.getRemainingCooldownMinutes(phoneNumber)
            Log.d(TAG, "Cooldown active for $phoneNumber, $remaining minutes remaining.")
            return
        }

        // Gửi SMS tự động sau cuộc gọi nhỡ
        sendAutoReply(context, phoneNumber, prefs.replyMessage, prefs)
    }

    private fun sendAutoReply(
        context: Context,
        phoneNumber: String,
        message: String,
        prefs: PrefsManager
    ) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

            prefs.recordReplySent(phoneNumber)

            Log.d(TAG, "Auto-reply (missed call) sent to $phoneNumber")

            NotificationHelper.showReplySentNotification(context, phoneNumber, "Cuộc gọi nhỡ")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send auto-reply to $phoneNumber: ${e.message}")
        }
    }
}
