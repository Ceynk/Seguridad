package com.antiphishing.smishguard

import android.app.Application
import com.antiphishing.smishguard.data.local.AppDatabase
import com.antiphishing.smishguard.data.repository.CallRepository
import com.antiphishing.smishguard.data.repository.RuleCache
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.service.ThreatNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        ruleCache = RuleCache(
            whitelistDao = database.whitelistDao(),
            customRuleDao = database.customRuleDao()
        )
        smsRepository = SmsRepository(
            smsDao = database.smsDao(),
            whitelistDao = database.whitelistDao(),
            customRuleDao = database.customRuleDao(),
            ruleCache = ruleCache
        )
        callRepository = CallRepository(
            callDao = database.callDao(),
            whitelistDao = database.whitelistDao(),
            customRuleDao = database.customRuleDao(),
            ruleCache = ruleCache
        )
        settingsRepository = SecuritySettingsRepository(this)

        ThreatNotificationManager.createNotificationChannels(this)

        appScope.launch {
            try {
                smsRepository.purgeExpiredRecords(30)
                callRepository.purgeExpiredRecords(30)
            } catch (e: Exception) {
            }
        }
    }
}
