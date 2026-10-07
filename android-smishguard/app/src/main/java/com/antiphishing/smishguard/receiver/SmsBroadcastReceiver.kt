package com.antiphishing.smishguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.service.ThreatNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null) {
            return
        }
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION && action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        val application = context.applicationContext
        if (application !is SmishGuardApp) {
            return
        }
        val settingsRepo = application.settingsRepository
        if (!settingsRepo.isProtectionEnabled) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages == null || messages.isEmpty()) {
            return
        }

        val firstMessage = messages[0]
        val rawSender = firstMessage.originatingAddress
        val sender = if (rawSender != null) {
            rawSender
        } else {
            "Desconocido"
        }

        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            val part = msg.messageBody
            if (part != null) {
                bodyBuilder.append(part)
            }
        }
        val fullBody = bodyBuilder.toString()
        if (fullBody.isBlank()) {
            return
        }

        val isOrdered = isOrderedBroadcast
        val pendingResult = goAsync()
        val smsRepo = application.smsRepository

        receiverScope.launch {
            try {
                val isWhitelisted = smsRepo.isSenderWhitelisted(sender)
                val customPatterns = smsRepo.getActiveCustomRegexes()

                val analysis = SmishDetectorEngine.analyze(
                    sender = sender,
                    body = fullBody,
                    isWhitelisted = isWhitelisted,
                    customBlockPatterns = customPatterns,
                    quarantineThreshold = settingsRepo.quarantineThreshold,
                    aggressiveMode = settingsRepo.isAggressiveModeEnabled
                )

                smsRepo.saveAnalysis(sender = sender, rawBody = fullBody, analysis = analysis)

                if (analysis.isQuarantined) {
                    ThreatNotificationManager.showThreatAlert(context, sender, analysis)

                    if (isOrdered) {
                        try {
                            abortBroadcast()
                        } catch (e: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
