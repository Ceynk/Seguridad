package com.antiphishing.smishguard.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationFilterService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Paquetes comunes de mensajería SMS en Android (AOSP, Google, Samsung, Xiaomi, Huawei, Motorola, etc.)
    private val smsPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.sonyericsson.conversations",
        "com.motorola.messaging",
        "com.miui.sms",
        "com.xiaomi.xmsf",
        "com.huawei.message",
        "com.coloros.mms",
        "com.oplus.mms"
    )

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (!smsPackages.contains(packageName)) return

        val app = applicationContext as? SmishGuardApp ?: return
        val settingsRepo = app.settingsRepository
        if (!settingsRepo.isProtectionEnabled || !settingsRepo.isSilentDismissNotifEnabled) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (text.isBlank()) return

        val smsRepo = app.smsRepository

        serviceScope.launch {
            try {
                val isWhitelisted = smsRepo.isSenderWhitelisted(title)
                val customPatterns = smsRepo.getActiveCustomRegexes()

                val analysis = SmishDetectorEngine.analyze(
                    sender = title,
                    body = text,
                    isWhitelisted = isWhitelisted,
                    customBlockPatterns = customPatterns,
                    quarantineThreshold = settingsRepo.quarantineThreshold,
                    aggressiveMode = settingsRepo.isAggressiveModeEnabled
                )

                if (analysis.isQuarantined) {
                    cancelNotification(sbn.key)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
