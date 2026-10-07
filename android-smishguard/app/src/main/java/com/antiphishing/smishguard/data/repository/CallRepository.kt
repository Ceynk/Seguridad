package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.data.local.dao.CallDao
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import com.antiphishing.smishguard.domain.model.CallAnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatFactor
import kotlinx.coroutines.flow.Flow
import java.net.URLDecoder
import java.net.URLEncoder

class CallRepository(
    private val callDao: CallDao,
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao,
    private val ruleCache: RuleCache
) {

    fun getAllCallRecords(): Flow<List<CallRecordEntity>> {
        return callDao.getAllCallRecords()
    }

    fun getBlockedCalls(): Flow<List<CallRecordEntity>> {
        return callDao.getBlockedCalls()
    }

    fun getTotalCallsCount(): Flow<Int> {
        return callDao.getTotalCallsCount()
    }

    fun getBlockedCallsCount(): Flow<Int> {
        return callDao.getBlockedCallsCount()
    }

    suspend fun isNumberWhitelisted(number: String): Boolean {
        return ruleCache.isWhitelisted(number)
    }

    suspend fun saveCallAnalysis(analysis: CallAnalysisResult): Long {
        val entity = CallRecordEntity(
            phoneNumber = analysis.phoneNumber,
            timestamp = System.currentTimeMillis(),
            threatScore = analysis.threatScore,
            threatLevel = analysis.level.name,
            isBlocked = analysis.isBlocked,
            action = analysis.action.name,
            countryOrOrigin = analysis.countryOrOrigin,
            factorsJson = serializeFactors(analysis.factors),
            summaryReason = analysis.summaryReason
        )
        return callDao.insertCallRecord(entity)
    }

    suspend fun deleteCallRecord(id: Long) {
        callDao.deleteById(id)
    }

    suspend fun clearAllCallRecords() {
        callDao.clearAll()
    }

    suspend fun purgeExpiredRecords(retentionDays: Int = 30): Int {
        val daysInMillis = retentionDays.toLong() * 24L * 60L * 60L * 1000L
        val cutoff = System.currentTimeMillis() - daysInMillis
        return callDao.deleteOlderThan(cutoff)
    }

    private fun serializeFactors(factors: List<ThreatFactor>): String {
        val items = mutableListOf<String>()
        for (factor in factors) {
            val cat = URLEncoder.encode(factor.category, "UTF-8")
            val desc = URLEncoder.encode(factor.description, "UTF-8")
            val snippet = if (factor.matchedSnippet != null) factor.matchedSnippet else ""
            val snip = URLEncoder.encode(snippet, "UTF-8")
            items.add("$cat:::$desc:::${factor.severityScore}:::$snip")
        }
        return items.joinToString("|||")
    }

    fun deserializeFactors(raw: String): List<ThreatFactor> {
        if (raw.isBlank()) {
            return emptyList()
        }
        val result = mutableListOf<ThreatFactor>()
        val entries = raw.split("|||")
        for (entry in entries) {
            val parts = entry.split(":::")
            if (parts.size >= 3) {
                var cat = parts[0]
                try {
                    cat = URLDecoder.decode(parts[0], "UTF-8")
                } catch (e: Exception) {
                    cat = parts[0]
                }

                var desc = parts[1]
                try {
                    desc = URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) {
                    desc = parts[1]
                }

                val parsedScore = parts[2].toIntOrNull()
                val score = if (parsedScore != null) parsedScore else 10

                var snip: String? = null
                if (parts.size > 3) {
                    try {
                        snip = URLDecoder.decode(parts[3], "UTF-8")
                    } catch (e: Exception) {
                        snip = parts[3]
                    }
                }

                val cleanedSnip = if (snip != null && snip.isNotBlank()) {
                    snip
                } else {
                    null
                }

                result.add(ThreatFactor(cat, desc, score, cleanedSnip))
            }
        }
        return result
    }
}
