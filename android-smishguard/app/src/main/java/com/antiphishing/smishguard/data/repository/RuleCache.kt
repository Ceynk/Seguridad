package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.CommunityBlacklistDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.util.SafeRegex
import com.antiphishing.smishguard.util.SenderKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity

class RuleCache(
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao,
    private val communityBlacklistDao: CommunityBlacklistDao
) {
    private val mutex = Mutex()

    @Volatile private var whitelist: List<String>? = null
    @Volatile private var customRegexes: List<Regex>? = null
    @Volatile private var communityIndicators: Set<String>? = null
    @Volatile private var communityEntries: Map<String, CommunityBlacklistEntity>? = null
    @Volatile private var communityEntriesAt: Long = 0L

    private val communityCacheDurationMs = 10 * 60 * 1000L

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

    suspend fun getCommunityIndicators(): Set<String> {
        val cached = communityIndicators
        if (cached != null) {
            return cached
        }
        return mutex.withLock {
            val current = communityIndicators
            if (current != null) {
                current
            } else {
                val fetched = communityBlacklistDao.obtenerIndicadores().toSet()
                communityIndicators = fetched
                fetched
            }
        }
    }

    fun invalidateCommunityIndicators() {
        communityIndicators = null
        communityEntries = null
        communityEntriesAt = 0L
    }

    suspend fun getCommunityEntriesByIndicator(): Map<String, CommunityBlacklistEntity> {
        val now = System.currentTimeMillis()
        val cached = communityEntries
        if (cached != null && now - communityEntriesAt < communityCacheDurationMs) {
            return cached
        }
        return mutex.withLock {
            val current = communityEntries
            val currentTime = System.currentTimeMillis()
            if (current != null && currentTime - communityEntriesAt < communityCacheDurationMs) {
                current
            } else {
                val fetched = communityBlacklistDao.obtenerTodosAhora().associateBy { it.indicador }
                communityEntries = fetched
                communityEntriesAt = currentTime
                fetched
            }
        }
    }
}
