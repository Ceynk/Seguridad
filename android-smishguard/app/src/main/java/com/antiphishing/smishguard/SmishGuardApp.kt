package com.antiphishing.smishguard

import android.app.Application
import android.util.Log
import com.antiphishing.smishguard.data.local.AppDatabase
import com.antiphishing.smishguard.data.repository.CallRepository
import com.antiphishing.smishguard.data.repository.CommunityApiClient
import com.antiphishing.smishguard.data.repository.CommunityIndicator
import com.antiphishing.smishguard.data.repository.RuleCache
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms
import com.antiphishing.smishguard.service.ThreatNotificationManager
import com.antiphishing.smishguard.sync.CommunitySyncWorker
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SmishGuardApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var ruleCache: RuleCache
        private set

    lateinit var smsRepository: SmsRepository
        private set

    lateinit var callRepository: CallRepository
        private set

    lateinit var settingsRepository: SecuritySettingsRepository
        private set

    lateinit var communityApiClient: CommunityApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        ruleCache = RuleCache(
            whitelistDao = database.whitelistDao(),
            customRuleDao = database.customRuleDao(),
            communityBlacklistDao = database.communityBlacklistDao()
        )
        smsRepository = SmsRepository(
            smsDao = database.smsDao(),
            whitelistDao = database.whitelistDao(),
            customRuleDao = database.customRuleDao(),
            ruleCache = ruleCache,
            communityBlacklistDao = database.communityBlacklistDao(),
            userReportDao = database.userReportDao()
        )
        callRepository = CallRepository(
            callDao = database.callDao(),
            ruleCache = ruleCache,
            personalBlacklistDao = database.personalBlacklistDao()
        )
        settingsRepository = SecuritySettingsRepository(this)
        communityApiClient = CommunityApiClient()
        if (settingsRepository.isCommunitySyncEnabled) {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "community-blacklist-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CommunitySyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()
            )
        }

        ThreatNotificationManager.createNotificationChannels(this)

        appScope.launch {
            ClasificadorMensajesSms.precargarModelo()
            try {
                smsRepository.purgeExpiredRecords(30)
                callRepository.purgeExpiredRecords(30)
                if (settingsRepository.isCommunitySyncEnabled) {
                    for (eliminado in smsRepository.getPendingDeletedCommunityIndicators()) {
                        try {
                            if (communityApiClient.eliminar(eliminado)) {
                                smsRepository.eliminarEntradaComunidad(eliminado, pendienteServidor = false)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo sincronizar la eliminación del indicador comunitario", e)
                        }
                    }
                    for (pendiente in smsRepository.getPendingCommunityEntries()) {
                        try {
                            val sincronizado = communityApiClient.enviar(
                                CommunityIndicator(pendiente.indicador, pendiente.tipo)
                            )
                            if (sincronizado != null) {
                                smsRepository.saveCommunityEntry(sincronizado, indicadorOriginal = pendiente.indicador)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo sincronizar el indicador comunitario", e)
                        }
                    }
                    val indicadores = communityApiClient.descargar()
                    val inteligencia = communityApiClient.descargarInteligencia()
                    smsRepository.saveCommunityEntries(indicadores + inteligencia)
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudieron purgar los registros expirados", e)
            }
        }
    }

    companion object {
        private const val TAG = "SmishGuardApp"
    }
}
