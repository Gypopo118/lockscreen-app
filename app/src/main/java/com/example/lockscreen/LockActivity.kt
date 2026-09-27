package com.example.lockscreen

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Launcher-вход: никакого окна не рисует (Theme.Lock.NoDisplay —
 * без preview, сплеша и анимаций). Только команда сервису и выход.
 * Тихого отката на Device Admin здесь НЕТ: именно lockNow() показывал
 * кейгард со шторкой и требовал потом графключ.
 */
class LockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView НЕ вызываем намеренно.
        if (!LockAccessibilityService.requestLock(this)) {
            // Доступ не включён — один раз показываем настройки, ничего не блокируем.
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("open_settings", true)
            })
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
