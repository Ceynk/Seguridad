package com.antiphishing.smishguard.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log
import androidx.annotation.RequiresApi
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.domain.detector.CallDetectorEngine
import com.antiphishing.smishguard.domain.detector.PhoneNumberNormalizer
import com.antiphishing.smishguard.domain.model.CallAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
class CallScreeningServiceImpl : CallScreeningService() {

    companion object {
        private const val TAG = "CallScreeningService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
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
                val normalizedNumber = PhoneNumberNormalizer.normalize(rawNumber).e164
                val isWhitelisted = if (rawNumber.isNotBlank()) {
                    callRepo.isNumberWhitelisted(normalizedNumber ?: rawNumber)
                } else {
                    false
                }
                val isPersonallyBlocked = normalizedNumber?.let {
                    callRepo.isPersonallyBlocked(it)
                } == true
                val customPatterns = smsRepo.getActiveCustomRegexes()
                val communityEntry = normalizedNumber?.let {
                    smsRepo.getCommunityEntriesByIndicator()[it]
                }

                val analysis = CallDetectorEngine.analyzeCall(
                    incomingNumber = rawNumber,
                    isWhitelisted = isWhitelisted,
                    customBlockPatterns = customPatterns,
                    blockThreshold = settingsRepo.quarantineThreshold,
                    aggressiveMode = settingsRepo.isAggressiveModeEnabled,
                    networkVerificationFailed = networkVerificationFailed,
                    communityReports = communityEntry?.reportes ?: 0,
                    blockPrivateNumbers = settingsRepo.blockPrivateCalls,
                    blockInvalidNumbers = settingsRepo.blockInvalidCalls,
                    internationalPolicy = settingsRepo.internationalCallPolicy,
                    communityConfirmed = communityEntry?.esMalicioso == true || isPersonallyBlocked
                )

                val recordId = callRepo.saveCallAnalysis(analysis)

                val displayPhone = if (rawNumber.isBlank()) {
                    "Número Oculto"
                } else {
                    rawNumber
                }

                if (recordId != -1L && analysis.action == CallAction.BLOCK) {
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
                } else if (recordId != -1L && analysis.action == CallAction.WARN) {
                    ThreatNotificationManager.showCallThreatAlert(
                        context = applicationContext,
                        phoneNumber = displayPhone,
                        analysis = analysis
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al analizar la llamada recibida", e)
            } finally {
                try {
                    respondToCall(callDetails, responseBuilder.build())
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo responder al servicio de filtrado de llamadas", e)
                }
            }
        }
    }

}
