package com.autoreply.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val prefs = PrefsManager(context)

        // Kiểm tra tính năng có đang bật không
        if (!prefs.isEnabled || !prefs.replyOnSms) {
            Log.d(TAG, "Auto reply disabled or SMS reply disabled, skipping.")
            return
        }

        // Lấy số điện thoại từ tin nhắn
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format") ?: "3gpp"

        val senderPhone = try {
            val smsMessage = SmsMessage.createFromPdu(pdus[0] as ByteArray, format)
            smsMessage.originatingAddress ?: return
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS: ${e.message}")
            return
        }

        Log.d(TAG, "SMS received from: $senderPhone")

        // Kiểm tra cooldown
        if (!prefs.canSendReply(senderPhone)) {
            val remaining = prefs.getRemainingCooldownMinutes(senderPhone)
            Log.d(TAG, "Cooldown active for $senderPhone, $remaining minutes remaining.")
            return
        }

        // Gửi auto-reply
        sendAutoReply(context, senderPhone, prefs.replyMessage, prefs)
    }

    private fun sendAutoReply(
        context: Context,
        phoneNumber: String,
        message: String,
        prefs: PrefsManager
    ) {
        try {
            val smsManager = SmsManager.getDefault()
            // Chia nhỏ nếu tin nhắn dài hơn 160 ký tự
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

            // Ghi lại thời điểm gửi để tính cooldown
            prefs.recordReplySent(phoneNumber)

            Log.d(TAG, "Auto-reply sent to $phoneNumber")

            // Hiện notification
            NotificationHelper.showReplySentNotification(context, phoneNumber, "SMS")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send auto-reply to $phoneNumber: ${e.message}")
        }
    }
}
