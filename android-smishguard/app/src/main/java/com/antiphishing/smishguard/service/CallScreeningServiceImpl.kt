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
        val rawNumber = handle?.schemeSpecificPart ?: ""

        val app = applicationContext as? SmishGuardApp
        val settingsRepo = app?.settingsRepository

        // Si la aplicación o ajustes no están disponibles, o la protección está apagada por el usuario:
        // Responder inmediatamente permitiendo la llamada según el contrato de Android Telecom
        if (settingsRepo == null || !settingsRepo.isProtectionEnabled) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val callRepo = app.callRepository
        val smsRepo = app.smsRepository

        // Android 11+ (API 30+): Verificar autenticidad del identificador (STIR/SHAKEN de red)
        val networkVerificationFailed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED
        } else false

        serviceScope.launch {
            var responseBuilder = CallResponse.Builder()
            try {
                val isWhitelisted = if (rawNumber.isNotBlank()) callRepo.isNumberWhitelisted(rawNumber) else false
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

                if (analysis.action == CallAction.BLOCK) {
                    responseBuilder = CallResponse.Builder().apply {
                        setDisallowCall(true)
                        setRejectCall(true)
                        setSkipNotification(true)
                        setSkipCallLog(false)
                    }
                    ThreatNotificationManager.showCallThreatAlert(
                        context = applicationContext,
                        phoneNumber = if (rawNumber.isBlank()) "Número Oculto" else rawNumber,
                        analysis = analysis
                    )
                } else if (analysis.action == CallAction.WARN) {
                    ThreatNotificationManager.showCallThreatAlert(
                        context = applicationContext,
                        phoneNumber = if (rawNumber.isBlank()) "Número Oculto" else rawNumber,
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
