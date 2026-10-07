package com.antiphishing.smishguard.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import androidx.annotation.RequiresApi
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.domain.detector.CallDetectorEngine
import com.antiphishing.smishguard.domain.model.CallAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
class CallScreeningServiceImpl : CallScreeningService() {

    companion object {
        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val rawNumber = if (handle != null && handle.schemeSpecificPart != null) {
            handle.schemeSpecificPart
        } else {
            ""
        }

        val application = applicationContext
        if (application !is SmishGuardApp) {
            val defaultResponse = CallResponse.Builder().build()
            respondToCall(callDetails, defaultResponse)
            return
        }

        val settingsRepo = application.settingsRepository
        if (!settingsRepo.isProtectionEnabled) {
            val defaultResponse = CallResponse.Builder().build()
            respondToCall(callDetails, defaultResponse)
            return
        }

        val callRepo = application.callRepository
        val smsRepo = application.smsRepository

        val networkVerificationFailed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED
        } else {
            false
        }

        serviceScope.launch {
            var responseBuilder = CallResponse.Builder()
            try {
                val isWhitelisted = if (rawNumber.isNotBlank()) {
                    callRepo.isNumberWhitelisted(rawNumber)
                } else {
                    false
                }
                val customPatterns = smsRepo.getActiveCustomRegexes()

                val analysis = CallDetectorEngine.analyzeCall(
                    incomingNumber = rawNumber,
                    isWhitelisted = isWhitelisted,
                    customBlockPatterns = customPatterns,
                    blockThreshold = settingsRepo.quarantineThreshold,
                    aggressiveMode = settingsRepo.isAggressiveModeEnabled,
                    networkVerificationFailed = networkVerificationFailed
                )

                callRepo.saveCallAnalysis(analysis)

                val displayPhone = if (rawNumber.isBlank()) {
                    "Número Oculto"
                } else {
                    rawNumber
                }

                if (analysis.action == CallAction.BLOCK) {
                    val blockBuilder = CallResponse.Builder()
                    blockBuilder.setDisallowCall(true)
                    blockBuilder.setRejectCall(true)
                    blockBuilder.setSkipNotification(true)
                    blockBuilder.setSkipCallLog(false)
                    responseBuilder = blockBuilder

                    ThreatNotificationManager.showCallThreatAlert(
                        context = applicationContext,
                        phoneNumber = displayPhone,
                        analysis = analysis
                    )
                } else if (analysis.action == CallAction.WARN) {
                    ThreatNotificationManager.showCallThreatAlert(
                        context = applicationContext,
                        phoneNumber = displayPhone,
                        analysis = analysis
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    respondToCall(callDetails, responseBuilder.build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
