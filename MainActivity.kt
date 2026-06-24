package com.autoreply.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager

    // UI elements
    private lateinit var switchMaster: Switch
    private lateinit var switchSms: Switch
    private lateinit var switchCall: Switch
    private lateinit var editMessage: EditText
    private lateinit var btnSaveMessage: Button
    private lateinit var btnClearCooldowns: Button
    private lateinit var tvCharCount: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etCooldownHours: EditText
    private lateinit var btnSaveCooldown: Button

    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
    ).also {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            it.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsManager(this)

        initViews()
        loadSettings()
        setupListeners()
        checkPermissions()
    }

    private fun initViews() {
        switchMaster = findViewById(R.id.switchMaster)
        switchSms = findViewById(R.id.switchSms)
        switchCall = findViewById(R.id.switchCall)
        editMessage = findViewById(R.id.editMessage)
        btnSaveMessage = findViewById(R.id.btnSaveMessage)
        btnClearCooldowns = findViewById(R.id.btnClearCooldowns)
        tvCharCount = findViewById(R.id.tvCharCount)
        tvStatus = findViewById(R.id.tvStatus)
        etCooldownHours = findViewById(R.id.etCooldownHours)
        btnSaveCooldown = findViewById(R.id.btnSaveCooldown)
    }

    private fun loadSettings() {
        switchMaster.isChecked = prefs.isEnabled
        switchSms.isChecked = prefs.replyOnSms
        switchCall.isChecked = prefs.replyOnCall
        editMessage.setText(prefs.replyMessage)
        etCooldownHours.setText(prefs.cooldownHours.toString())
        updateStatusText()
        updateCharCount(prefs.replyMessage)
    }

    private fun setupListeners() {
        // Toggle tổng
        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
            updateStatusText()
            toggleService(isChecked)
            Toast.makeText(
                this,
                if (isChecked) "✅ Đã bật tự động trả lời" else "⛔ Đã tắt tự động trả lời",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Toggle SMS
        switchSms.setOnCheckedChangeListener { _, isChecked ->
            prefs.replyOnSms = isChecked
        }

        // Toggle cuộc gọi nhỡ
        switchCall.setOnCheckedChangeListener { _, isChecked ->
            prefs.replyOnCall = isChecked
        }

        // Đếm ký tự
        editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharCount(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Lưu tin nhắn
        btnSaveMessage.setOnClickListener {
            val msg = editMessage.text.toString().trim()
            if (msg.isEmpty()) {
                Toast.makeText(this, "⚠️ Tin nhắn không được để trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.replyMessage = msg
            Toast.makeText(this, "✅ Đã lưu tin nhắn!", Toast.LENGTH_SHORT).show()
        }

        // Lưu cooldown
        btnSaveCooldown.setOnClickListener {
            val hours = etCooldownHours.text.toString().toLongOrNull()
            if (hours == null || hours < 1 || hours > 72) {
                Toast.makeText(this, "⚠️ Nhập số giờ từ 1 đến 72", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.cooldownHours = hours
            Toast.makeText(this, "✅ Đã lưu cooldown: $hours tiếng", Toast.LENGTH_SHORT).show()
        }

        // Xóa cooldown
        btnClearCooldowns.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa toàn bộ cooldown?")
                .setMessage("Tất cả số điện thoại sẽ nhận được tin nhắn tự động ngay lần liên hệ tiếp theo.")
                .setPositiveButton("Xóa") { _, _ ->
                    prefs.clearAllCooldowns()
                    Toast.makeText(this, "✅ Đã xóa toàn bộ cooldown", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun toggleService(enable: Boolean) {
        val serviceIntent = Intent(this, AutoReplyService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            stopService(serviceIntent)
        }
    }

    private fun updateStatusText() {
        tvStatus.text = if (prefs.isEnabled) {
            "🟢 Đang hoạt động"
        } else {
            "🔴 Đang tắt"
        }
    }

    private fun updateCharCount(text: String) {
        val len = text.length
        tvCharCount.text = "$len ký tự"
    }

    private fun checkPermissions() {
        val missingPerms = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPerms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPerms.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val denied = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            if (denied.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Thiếu quyền truy cập")
                    .setMessage("App cần các quyền sau để hoạt động:\n\n${denied.joinToString("\n") { "• ${it.substringAfterLast(".")}" }}\n\nVui lòng cấp quyền trong Cài đặt.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}
