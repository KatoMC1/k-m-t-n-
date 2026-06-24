package com.autoreply.app

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_AUTO_REPLY_ENABLED = "auto_reply_enabled"
        const val KEY_REPLY_MESSAGE = "reply_message"
        const val KEY_REPLY_ON_SMS = "reply_on_sms"
        const val KEY_REPLY_ON_CALL = "reply_on_call"
        const val KEY_COOLDOWN_HOURS = "cooldown_hours"
        const val DEFAULT_MESSAGE = "Xin chào! Cảm ơn bạn đã liên hệ. Chúng tôi sẽ phản hồi sớm nhất có thể. Trân trọng!"
        const val DEFAULT_COOLDOWN_HOURS = 5L
        private const val PREFIX_COOLDOWN = "cooldown_"
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_REPLY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_REPLY_ENABLED, value).apply()

    var replyMessage: String
        get() = prefs.getString(KEY_REPLY_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE
        set(value) = prefs.edit().putString(KEY_REPLY_MESSAGE, value).apply()

    var replyOnSms: Boolean
        get() = prefs.getBoolean(KEY_REPLY_ON_SMS, true)
        set(value) = prefs.edit().putBoolean(KEY_REPLY_ON_SMS, value).apply()

    var replyOnCall: Boolean
        get() = prefs.getBoolean(KEY_REPLY_ON_CALL, true)
        set(value) = prefs.edit().putBoolean(KEY_REPLY_ON_CALL, value).apply()

    var cooldownHours: Long
        get() = prefs.getLong(KEY_COOLDOWN_HOURS, DEFAULT_COOLDOWN_HOURS)
        set(value) = prefs.edit().putLong(KEY_COOLDOWN_HOURS, value).apply()

    /**
     * Kiểm tra xem số điện thoại này có đang trong cooldown không.
     * Trả về true nếu CÓ thể gửi (không trong cooldown), false nếu đang cooldown.
     */
    fun canSendReply(phoneNumber: String): Boolean {
        val key = PREFIX_COOLDOWN + normalizePhone(phoneNumber)
        val lastSentTime = prefs.getLong(key, 0L)
        if (lastSentTime == 0L) return true

        val cooldownMs = cooldownHours * 60 * 60 * 1000
        val elapsed = System.currentTimeMillis() - lastSentTime
        return elapsed >= cooldownMs
    }

    /**
     * Ghi lại thời điểm vừa gửi reply cho số điện thoại này.
     */
    fun recordReplySent(phoneNumber: String) {
        val key = PREFIX_COOLDOWN + normalizePhone(phoneNumber)
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }

    /**
     * Lấy thời gian còn lại của cooldown (tính bằng phút), trả về 0 nếu đã hết.
     */
    fun getRemainingCooldownMinutes(phoneNumber: String): Long {
        val key = PREFIX_COOLDOWN + normalizePhone(phoneNumber)
        val lastSentTime = prefs.getLong(key, 0L)
        if (lastSentTime == 0L) return 0L

        val cooldownMs = cooldownHours * 60 * 60 * 1000
        val elapsed = System.currentTimeMillis() - lastSentTime
        val remaining = cooldownMs - elapsed
        return if (remaining <= 0) 0L else remaining / 60000
    }

    /**
     * Xóa cooldown của một số điện thoại (để test hoặc reset thủ công).
     */
    fun clearCooldown(phoneNumber: String) {
        val key = PREFIX_COOLDOWN + normalizePhone(phoneNumber)
        prefs.edit().remove(key).apply()
    }

    /**
     * Xóa toàn bộ cooldown.
     */
    fun clearAllCooldowns() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(PREFIX_COOLDOWN) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    /**
     * Chuẩn hoá số điện thoại (bỏ khoảng trắng, dấu +, dấu -)
     */
    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "").takeLast(10)
    }
}
