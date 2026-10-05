package com.antiphishing.smishguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.domain.detector.CallDetectorEngine
import com.antiphishing.smishguard.domain.model.CallAction
import com.antiphishing.smishguard.service.ThreatNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        @Suppress("DEPRECATION")
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
        if (incomingNumber.isBlank()) return

        val app = context.applicationContext as? SmishGuardApp ?: return
        val settingsRepo = app.settingsRepository
        if (!settingsRepo.isProtectionEnabled) return

        val callRepo = app.callRepository
        val smsRepo = app.smsRepository

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                val isWhitelisted = callRepo.isNumberWhitelisted(incomingNumber)
                val customPatterns = smsRepo.getActiveCustomRegexes()

                val analysis = CallDetectorEngine.analyzeCall(
                    incomingNumber = incomingNumber,
                    isWhitelisted = isWhitelisted,
                    customBlockPatterns = customPatterns,
                    blockThreshold = settingsRepo.quarantineThreshold,
                    aggressiveMode = settingsRepo.isAggressiveModeEnabled
                )

                callRepo.saveCallAnalysis(analysis)

                if (analysis.action == CallAction.BLOCK || analysis.action == CallAction.WARN) {
                    ThreatNotificationManager.showCallThreatAlert(
                        context = context,
                        phoneNumber = incomingNumber,
                        analysis = analysis
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
