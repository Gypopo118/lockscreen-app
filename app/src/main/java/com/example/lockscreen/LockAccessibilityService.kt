package com.example.lockscreen

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Основной способ гашения экрана: GLOBAL_ACTION_LOCK_SCREEN (API 28+).
 * В отличие от DevicePolicyManager.lockNow() НЕ требует после этого
 * сильный метод разблокировки — отпечаток/лицо продолжают работать.
 * Device Admin — только последний шанс, если действие не сработало.
 */
class LockAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var attempts = 0
    private var pendingStartId = 0

    override fun onServiceConnected() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_LOCK) {
            attempts = 0
            pendingStartId = startId
            handler.removeCallbacksAndMessages(null)
            tryLock()
        }
        return START_NOT_STICKY
    }

    /**
     * performGlobalAction возвращает false, если сервис ещё не связан
     * с фреймворком (гонка после холодного старта процесса) —
     * повторяем несколько раз, прежде чем сдаться.
     */
    private fun tryLock() {
        if (performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)) {
            handler.removeCallbacksAndMessages(null)
            stopSelf(pendingStartId)
            return
        }
        attempts++
        if (attempts < MAX_ATTEMPTS) {
            handler.postDelayed({ tryLock() }, RETRY_DELAY_MS)
        } else {
            handler.removeCallbacksAndMessages(null)
            fallbackAdminLock()
            stopSelf(pendingStartId)
        }
    }

    private fun fallbackAdminLock() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            try {
                dpm.lockNow()
            } catch (_: SecurityException) {
            }
        }
    }

    companion object {
        const val ACTION_LOCK = "com.example.lockscreen.ACTION_LOCK"
        private const val MAX_ATTEMPTS = 5
        private const val RETRY_DELAY_MS = 150L

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, LockAccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        /** Возвращает true, если команда на блокировку отправлена. */
        fun requestLock(context: Context): Boolean {
            if (!isEnabled(context)) return false
            val intent = Intent(context, LockAccessibilityService::class.java).setAction(ACTION_LOCK)
            context.startService(intent)
            return true
        }
    }
}
