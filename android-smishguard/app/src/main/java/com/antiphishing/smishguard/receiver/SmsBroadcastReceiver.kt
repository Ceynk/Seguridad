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
        val action = intent.action ?: return
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION && action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        val app = context.applicationContext as? SmishGuardApp ?: return
        val settingsRepo = app.settingsRepository
        if (!settingsRepo.isProtectionEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: "Desconocido"
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            msg.messageBody?.let { bodyBuilder.append(it) }
        }
        val fullBody = bodyBuilder.toString()
        if (fullBody.isBlank()) return

        val isOrdered = isOrderedBroadcast
        val pendingResult = goAsync()
        val smsRepo = app.smsRepository

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

                    // Si es una difusión ordenada (SMS_DELIVER cuando es app predeterminada, o SMS_RECEIVED con prioridad)
                    // abortamos la propagación para que el mensaje no alerte al usuario ni contamine la bandeja de entrada
                    if (isOrdered) {
                        try {
                            abortBroadcast()
                        } catch (e: Exception) {
                            // Si la difusión ya fue finalizada por el sistema
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
