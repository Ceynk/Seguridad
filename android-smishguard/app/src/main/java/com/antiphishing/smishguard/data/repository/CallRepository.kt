package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.data.local.dao.CallDao
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import com.antiphishing.smishguard.domain.model.CallAnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatFactor
import kotlinx.coroutines.flow.Flow

class CallRepository(
    private val callDao: CallDao,
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao,
    private val ruleCache: RuleCache
) {

    fun getAllCallRecords(): Flow<List<CallRecordEntity>> = callDao.getAllCallRecords()
    fun getBlockedCalls(): Flow<List<CallRecordEntity>> = callDao.getBlockedCalls()
    fun getTotalCallsCount(): Flow<Int> = callDao.getTotalCallsCount()
    fun getBlockedCallsCount(): Flow<Int> = callDao.getBlockedCallsCount()

    suspend fun isNumberWhitelisted(number: String): Boolean = ruleCache.isWhitelisted(number)

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
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
        return callDao.deleteOlderThan(cutoff)
    }

    private fun serializeFactors(factors: List<ThreatFactor>): String {
        return factors.joinToString("|||") { factor ->
            val cat = java.net.URLEncoder.encode(factor.category, "UTF-8")
            val desc = java.net.URLEncoder.encode(factor.description, "UTF-8")
            val snip = java.net.URLEncoder.encode(factor.matchedSnippet ?: "", "UTF-8")
            "$cat:::$desc:::${factor.severityScore}:::$snip"
        }
    }

    fun deserializeFactors(raw: String): List<ThreatFactor> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").mapNotNull { entry ->
            val parts = entry.split(":::")
            if (parts.size >= 3) {
                val cat = try { java.net.URLDecoder.decode(parts[0], "UTF-8") } catch (e: Exception) { parts[0] }
                val desc = try { java.net.URLDecoder.decode(parts[1], "UTF-8") } catch (e: Exception) { parts[1] }
                val score = parts[2].toIntOrNull() ?: 10
                val snip = parts.getOrNull(3)?.let {
                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
                }?.takeIf { it.isNotBlank() }
                ThreatFactor(
                    category = cat,
                    description = desc,
                    severityScore = score,
                    matchedSnippet = snip
                )
            } else null
        }
    }
}
