package com.antiphishing.smishguard.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationFilterService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        if (sbn == null) {
            return
        }

        val packageName = sbn.packageName
        if (!smsPackages.contains(packageName)) {
            return
        }

        val application = applicationContext
        if (application !is SmishGuardApp) {
            return
        }
        val settingsRepo = application.settingsRepository
        if (!settingsRepo.isProtectionEnabled || !settingsRepo.isSilentDismissNotifEnabled) {
            return
        }

        val notification = sbn.notification
        val extras = notification.extras
        if (extras == null) {
            return
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        if (text.isBlank()) {
            return
        }

        val smsRepo = application.smsRepository

        serviceScope.launch {
            try {
                val isWhitelisted = smsRepo.isSenderWhitelisted(title)
                val customPatterns = smsRepo.getActiveCustomRegexes()
                val communityEntries = smsRepo.getCommunityEntriesByIndicator()

                val analysis = SmishDetectorEngine.analyze(
                    sender = title,
                    body = text,
                    isWhitelisted = isWhitelisted,
                    customBlockPatterns = customPatterns,
                    quarantineThreshold = settingsRepo.quarantineThreshold,
                    aggressiveMode = settingsRepo.isAggressiveModeEnabled,
                    communityEntries = communityEntries
                )

                val identity = SmsRepository.identityFor(
                    sender = title,
                    body = text,
                    timestamp = sbn.postTime
                )
                val recordId = smsRepo.saveAnalysis(
                    sender = title,
                    rawBody = text,
                    analysis = analysis,
                    messageIdentity = identity
                )

                if (recordId != -1L && analysis.isQuarantined) {
                    cancelNotification(sbn.key)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al analizar la notificación recibida", e)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationFilterService"
    }
}
