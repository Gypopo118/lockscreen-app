package com.example.lockscreen

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Никакого окна не рисует (Theme.NoDisplay): команда на гашение уходит
 * до первой отрисовки — без вспышки рабочего стола.
 */
class LockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView НЕ вызываем намеренно.
        if (!LockAccessibilityService.requestLock(this)) {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) {
                try {
                    dpm.lockNow()
                } catch (_: SecurityException) {
                }
            } else {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("open_settings", true)
                })
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
