package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.SmsDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.SmsRecordEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
import com.antiphishing.smishguard.data.security.FieldCipher
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.util.SafeRegex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.net.URLEncoder

class SmsRepository(
    private val smsDao: SmsDao,
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao,
    private val ruleCache: RuleCache
) {

    fun getAllRecords(): Flow<List<SmsMessageData>> {
        return smsDao.getAllRecords().map { entities ->
            val domainList = mutableListOf<SmsMessageData>()
            for (entity in entities) {
                domainList.add(convertEntityToDomain(entity))
            }
            domainList
        }
    }

    fun getQuarantinedRecords(): Flow<List<SmsMessageData>> {
        return smsDao.getQuarantinedRecords().map { entities ->
            val domainList = mutableListOf<SmsMessageData>()
            for (entity in entities) {
                domainList.add(convertEntityToDomain(entity))
            }
            domainList
        }
    }

    fun getTotalAnalyzedCount(): Flow<Int> {
        return smsDao.getTotalAnalyzedCount()
    }

    fun getActiveQuarantinedCount(): Flow<Int> {
        return smsDao.getActiveQuarantinedCount()
    }

    fun getPhishingDetectedCount(): Flow<Int> {
        return smsDao.getPhishingDetectedCount()
    }

    suspend fun isSenderWhitelisted(sender: String): Boolean {
        return ruleCache.isWhitelisted(sender)
    }

    fun getAllWhitelist(): Flow<List<WhitelistEntity>> {
        return whitelistDao.getAllWhitelist()
    }

    suspend fun addToWhitelist(sender: String, name: String? = null) {
        val trimmedSender = sender.trim()
        val trimmedName = if (name != null) name.trim() else null
        val entry = WhitelistEntity(
            senderOrNumber = trimmedSender,
            contactName = trimmedName
        )
        whitelistDao.insertWhitelist(entry)
        ruleCache.invalidateWhitelist()
    }

    suspend fun removeFromWhitelist(sender: String) {
        val trimmedSender = sender.trim()
        whitelistDao.deleteBySender(trimmedSender)
        ruleCache.invalidateWhitelist()
    }

    fun getAllCustomRules(): Flow<List<CustomRuleEntity>> {
        return customRuleDao.getAllRules()
    }

    suspend fun getActiveCustomRegexes(): List<Regex> {
        return ruleCache.getCustomRegexes()
    }

    suspend fun addCustomRule(pattern: String, isRegex: Boolean, description: String): String? {
        val error = SafeRegex.validateUserRule(pattern, isRegex)
        if (error != null) {
            return error
        }

        val rule = CustomRuleEntity(
            pattern = pattern.trim(),
            isRegex = isRegex,
            description = description.trim()
        )
        customRuleDao.insertRule(rule)
        ruleCache.invalidateRules()
        return null
    }

    suspend fun updateCustomRule(rule: CustomRuleEntity) {
        customRuleDao.updateRule(rule)
        ruleCache.invalidateRules()
    }

    suspend fun deleteCustomRule(rule: CustomRuleEntity) {
        customRuleDao.deleteRule(rule)
        ruleCache.invalidateRules()
    }

    suspend fun saveAnalysis(sender: String, rawBody: String, analysis: AnalysisResult): Long {
        val storedRawBody = if (analysis.isQuarantined) {
            FieldCipher.encrypt(rawBody)
        } else {
            ""
        }

        val entity = SmsRecordEntity(
            sender = sender,
            rawBody = storedRawBody,
            sanitizedBody = analysis.sanitizedBody,
            timestamp = System.currentTimeMillis(),
            threatScore = analysis.threatScore,
            threatLevel = analysis.level.name,
            isQuarantined = analysis.isQuarantined,
            factorsJson = serializeFactors(analysis.factors),
            extractedUrls = analysis.extractedUrls.joinToString(",")
        )
        return smsDao.insertRecord(entity)
    }

    suspend fun restoreQuarantinedMessage(id: Long) {
        val record = smsDao.getRecordById(id)
        if (record == null) {
            return
        }
        val updatedRecord = record.copy(wasRestored = true)
        smsDao.updateRecord(updatedRecord)
    }

    suspend fun getDecryptedOriginalBody(id: Long): String? {
        val record = smsDao.getRecordById(id)
        if (record == null) {
            return null
        }
        return if (record.rawBody.isBlank()) {
            record.sanitizedBody
        } else {
            FieldCipher.decrypt(record.rawBody)
        }
    }

    suspend fun deleteRecord(id: Long) {
        smsDao.deleteById(id)
    }

    suspend fun clearAllRecords() {
        smsDao.clearAll()
    }

    suspend fun clearAllQuarantined() {
        smsDao.clearAllQuarantined()
    }

    suspend fun clearNonQuarantined() {
        smsDao.clearNonQuarantined()
    }

    suspend fun purgeExpiredRecords(retentionDays: Int = 30): Int {
        val daysInMillis = retentionDays.toLong() * 24L * 60L * 60L * 1000L
        val cutoff = System.currentTimeMillis() - daysInMillis
        return smsDao.deleteNonQuarantinedOlderThan(cutoff)
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

    private fun deserializeFactors(raw: String): List<ThreatFactor> {
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

    private fun convertEntityToDomain(record: SmsRecordEntity): SmsMessageData {
        val urls = if (record.extractedUrls.isBlank()) {
            emptyList()
        } else if (record.extractedUrls.contains("|||")) {
            record.extractedUrls.split("|||")
        } else {
            record.extractedUrls.split(",")
        }

        var level = ThreatLevel.SAFE
        try {
            level = ThreatLevel.valueOf(record.threatLevel)
        } catch (e: Exception) {
            level = ThreatLevel.SAFE
        }

        return SmsMessageData(
            id = record.id,
            sender = record.sender,
            body = record.sanitizedBody,
            timestamp = record.timestamp,
            threatScore = record.threatScore,
            threatLevel = level,
            isQuarantined = record.isQuarantined,
            threatFactors = deserializeFactors(record.factorsJson),
            extractedUrls = urls,
            wasRestored = record.wasRestored
        )
    }
}
