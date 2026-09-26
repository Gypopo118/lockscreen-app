package com.example.lockscreen

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val ADMIN_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // Тап по иконке = сразу гасить экран.
        // Исключение: первый запуск без прав, либо явный заход в настройки
        // (long-press -> "Настройки" передаёт open_settings=true).
        val forceSettings = intent?.getBooleanExtra("open_settings", false) == true
        if (dpm.isAdminActive(adminComponent) && !forceSettings) {
            dpm.lockNow()
            finish()
            overridePendingTransition(0, 0)
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 100, 48, 48)
        }
        val status = TextView(this).apply { textSize = 16f }
        val btnAdmin = Button(this).apply { text = "Выдать права администратора" }
        val btnShortcut = Button(this).apply { text = getString(R.string.btn_create_shortcut) }
        val btnLock = Button(this).apply { text = getString(R.string.btn_lock_now) }

        root.addView(status)
        root.addView(btnAdmin)
        root.addView(btnShortcut)
        root.addView(btnLock)
        setContentView(root)

        fun refreshStatus() {
            val active = dpm.isAdminActive(adminComponent)
            status.text = if (active) getString(R.string.admin_active) else getString(R.string.admin_inactive)
            btnAdmin.isEnabled = !active
            btnShortcut.isEnabled = active
            btnLock.isEnabled = active
        }

        btnAdmin.setOnClickListener {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.desc_admin))
            }
            startActivityForResult(intent, ADMIN_REQUEST_CODE)
        }

        btnShortcut.setOnClickListener { createShortcut() }
        btnLock.setOnClickListener { lockNow() }

        refreshStatus()
    }

    @Deprecated("Use Activity Result API on new projects")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADMIN_REQUEST_CODE) {
            // Права только что выданы обычным запуском — сразу гасим экран.
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                finish()
                overridePendingTransition(0, 0)
            } else {
                recreate()
            }
        }
    }

    private fun createShortcut() {
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val shortcut = ShortcutInfo.Builder(this, "lock_shortcut")
                    .setShortLabel("Lock")
                    .setLongLabel("Блокировка экрана")
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(lockIntent)
                    .build()
                shortcutManager.requestPinShortcut(shortcut, null)
                Toast.makeText(this, "Подтвердите создание ярлыка", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Лаунчер не поддерживает закреплённые ярлыки", Toast.LENGTH_SHORT).show()
            }
        } else {
            val addIntent = Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, lockIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, "Lock")
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this@MainActivity, R.mipmap.ic_launcher))
                action = "com.android.launcher.action.INSTALL_SHORTCUT"
            }
            sendBroadcast(addIntent)
            Toast.makeText(this, "Ярлык создан", Toast.LENGTH_SHORT).show()
        }
    }

    private fun lockNow() {
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        } else {
            Toast.makeText(this, "Нет прав администратора", Toast.LENGTH_SHORT).show()
        }
    }
}
