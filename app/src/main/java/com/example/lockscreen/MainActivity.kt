package com.example.lockscreen

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Только экран настроек: первый запуск и long-press -> "Настройки".
 * Тап по иконке идёт в LockActivity и сюда не попадает.
 * Тихой блокировки через Device Admin здесь нет — именно она показывала
 * кейгард со шторкой и требовала потом графключ вместо отпечатка.
 */
class MainActivity : Activity() {
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val ADMIN_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 100, 48, 48)
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val statusMethod = TextView(this).apply { textSize = 17f; setTextColor(Color.WHITE) }
        val statusAccess = TextView(this).apply { textSize = 15f; setTextColor(Color.WHITE) }
        val statusAdmin = TextView(this).apply { textSize = 14f; setTextColor(Color.LTGRAY) }
        val btnAccess = Button(this).apply { text = getString(R.string.btn_enable_access) }
        val btnAdmin = Button(this).apply { text = getString(R.string.btn_enable_admin) }
        val btnShortcut = Button(this).apply { text = getString(R.string.btn_create_shortcut) }
        val btnLock = Button(this).apply { text = getString(R.string.btn_lock_now) }
        val btnRemoveAdmin = Button(this).apply { text = getString(R.string.btn_remove_admin) }

        root.addView(statusMethod)
        root.addView(statusAccess)
        root.addView(statusAdmin)
        root.addView(btnAccess)
        root.addView(btnAdmin)
        root.addView(btnShortcut)
        root.addView(btnLock)
        root.addView(btnRemoveAdmin)
        setContentView(root)

        fun refresh() {
            val acc = LockAccessibilityService.isEnabled(this)
            val admin = dpm.isAdminActive(adminComponent)
            statusMethod.text = when {
                acc -> getString(R.string.method_access)
                admin -> getString(R.string.method_admin)
                else -> getString(R.string.method_none)
            }
            statusAccess.text = if (acc) getString(R.string.access_active) else getString(R.string.access_inactive)
            statusAdmin.text = if (admin) getString(R.string.admin_active) else getString(R.string.admin_inactive)
            btnAccess.isEnabled = !acc
            btnLock.isEnabled = acc
            btnShortcut.isEnabled = acc || admin
            btnRemoveAdmin.isEnabled = admin
        }

        btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Toast.makeText(this, getString(R.string.hint_enable_access), Toast.LENGTH_LONG).show()
        }
        btnAdmin.setOnClickListener {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.desc_admin))
            }
            startActivityForResult(intent, ADMIN_REQUEST_CODE)
        }
        btnShortcut.setOnClickListener { createShortcut() }
        btnLock.setOnClickListener {
            if (!LockAccessibilityService.requestLock(this)) {
                Toast.makeText(this, getString(R.string.access_inactive), Toast.LENGTH_SHORT).show()
            } else {
                finish()
                overridePendingTransition(0, 0)
            }
        }
        btnRemoveAdmin.setOnClickListener {
            try {
                dpm.removeActiveAdmin(adminComponent)
                Toast.makeText(this, getString(R.string.admin_removed), Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(this, getString(R.string.admin_remove_failed), Toast.LENGTH_SHORT).show()
            }
            refresh()
        }

        refresh()
    }

    @Deprecated("Use Activity Result API on new projects")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADMIN_REQUEST_CODE) recreate()
    }

    private fun createShortcut() {
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val shortcut = android.content.pm.ShortcutInfo.Builder(this, "lock_shortcut")
                    .setShortLabel("Lock")
                    .setLongLabel(getString(R.string.shortcut_lock_long))
                    .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
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
}
