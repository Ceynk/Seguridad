package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.util.SafeRegex
import com.antiphishing.smishguard.util.SenderKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RuleCache(
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao
) {
    private val mutex = Mutex()

    @Volatile private var whitelist: List<String>? = null
    @Volatile private var customRegexes: List<Regex>? = null

    suspend fun isWhitelisted(senderOrNumber: String): Boolean {
        if (senderOrNumber.isBlank()) {
            return false
        }
        val currentWhitelist = getWhitelist()
        return SenderKey.matchesAny(senderOrNumber, currentWhitelist)
    }

    suspend fun getWhitelist(): List<String> {
        val cached = whitelist
        if (cached != null) {
            return cached
        }
        return mutex.withLock {
            val current = whitelist
            if (current != null) {
                current
            } else {
                val fetched = whitelistDao.getAllSenders()
                whitelist = fetched
                fetched
            }
        }
    }

    suspend fun getCustomRegexes(): List<Regex> {
        val cached = customRegexes
        if (cached != null) {
            return cached
        }
        return mutex.withLock {
            val current = customRegexes
            if (current != null) {
                current
            } else {
                val activeRules = customRuleDao.getActiveRules()
                val compiledList = mutableListOf<Regex>()
                for (rule in activeRules) {
                    val regex = SafeRegex.compileUserRule(rule.pattern, rule.isRegex)
                    if (regex != null) {
                        compiledList.add(regex)
                    }
                }
                customRegexes = compiledList
                compiledList
            }
        }
    }

    fun invalidateWhitelist() {
        whitelist = null
    }

    fun invalidateRules() {
        customRegexes = null
    }
}
