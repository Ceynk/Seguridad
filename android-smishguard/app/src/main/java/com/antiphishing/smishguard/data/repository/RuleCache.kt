package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.util.SafeRegex
import com.antiphishing.smishguard.util.SenderKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caché en memoria de la lista blanca y de las reglas personalizadas ya compiladas.
 *
 * Antes, cada SMS/llamada entrante consultaba la BD y recompilaba todas las regex.
 * En la ruta de filtrado de llamadas (presupuesto de ~5 s impuesto por Android) eso es
 * latencia innecesaria. La caché se invalida explícitamente al modificar reglas o lista blanca.
 */
class RuleCache(
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao
) {
    private val mutex = Mutex()

    @Volatile private var whitelist: List<String>? = null
    @Volatile private var customRegexes: List<Regex>? = null

    suspend fun isWhitelisted(senderOrNumber: String): Boolean {
        if (senderOrNumber.isBlank()) return false
        return SenderKey.matchesAny(senderOrNumber, getWhitelist())
    }

    suspend fun getWhitelist(): List<String> {
        whitelist?.let { return it }
        return mutex.withLock {
            whitelist ?: whitelistDao.getAllSenders().also { whitelist = it }
        }
    }

    suspend fun getCustomRegexes(): List<Regex> {
        customRegexes?.let { return it }
        return mutex.withLock {
            customRegexes ?: customRuleDao.getActiveRules()
                .mapNotNull { SafeRegex.compileUserRule(it.pattern, it.isRegex) }
                .also { customRegexes = it }
        }
    }

    fun invalidateWhitelist() {
        whitelist = null
    }

    fun invalidateRules() {
        customRegexes = null
    }
}
