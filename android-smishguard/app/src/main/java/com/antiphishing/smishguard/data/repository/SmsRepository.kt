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

class SmsRepository(
    private val smsDao: SmsDao,
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao,
    private val ruleCache: RuleCache
) {

    fun getAllRecords(): Flow<List<SmsMessageData>> {
        return smsDao.getAllRecords().map { list -> list.map { it.toDomain() } }
    }

    fun getQuarantinedRecords(): Flow<List<SmsMessageData>> {
        return smsDao.getQuarantinedRecords().map { list -> list.map { it.toDomain() } }
    }

    fun getTotalAnalyzedCount(): Flow<Int> = smsDao.getTotalAnalyzedCount()
    fun getActiveQuarantinedCount(): Flow<Int> = smsDao.getActiveQuarantinedCount()
    fun getPhishingDetectedCount(): Flow<Int> = smsDao.getPhishingDetectedCount()

    suspend fun isSenderWhitelisted(sender: String): Boolean = ruleCache.isWhitelisted(sender)

    fun getAllWhitelist(): Flow<List<WhitelistEntity>> = whitelistDao.getAllWhitelist()

    suspend fun addToWhitelist(sender: String, name: String? = null) {
        whitelistDao.insertWhitelist(WhitelistEntity(senderOrNumber = sender.trim(), contactName = name?.trim()))
        ruleCache.invalidateWhitelist()
    }

    suspend fun removeFromWhitelist(sender: String) {
        whitelistDao.deleteBySender(sender.trim())
        ruleCache.invalidateWhitelist()
    }

    fun getAllCustomRules(): Flow<List<CustomRuleEntity>> = customRuleDao.getAllRules()

    suspend fun getActiveCustomRegexes(): List<Regex> = ruleCache.getCustomRegexes()

    /**
     * Valida y agrega una regla personalizada de bloqueo.
     * @return Mensaje de error legible si la regla es inválida o peligrosa, o null si fue exitosa.
     */
    suspend fun addCustomRule(pattern: String, isRegex: Boolean, description: String): String? {
        val error = SafeRegex.validateUserRule(pattern, isRegex)
        if (error != null) return error

        customRuleDao.insertRule(
            CustomRuleEntity(
                pattern = pattern.trim(),
                isRegex = isRegex,
                description = description.trim()
            )
        )
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
        // Principio de minimización y protección de datos:
        // Si el SMS está en cuarentena, ciframos el cuerpo original con AES-GCM (Keystore)
        // para prevenir exfiltración en caso de extracción forense no autorizada.
        // Si no está en cuarentena y es seguro, almacenamos el texto desarmado sin secretos.
        val storedRawBody = if (analysis.isQuarantined) {
            FieldCipher.encrypt(rawBody)
        } else {
            "" // Minimización: no retener el texto original en claro de mensajes no maliciosos
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
        val record = smsDao.getRecordById(id) ?: return
        smsDao.updateRecord(record.copy(wasRestored = true))
    }

    suspend fun getDecryptedOriginalBody(id: Long): String? {
        val record = smsDao.getRecordById(id) ?: return null
        return if (record.rawBody.isBlank()) record.sanitizedBody else FieldCipher.decrypt(record.rawBody)
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

    /**
     * Limpieza automática por política de retención local (elimina registros antiguos para optimizar almacenamiento).
     */
    suspend fun purgeExpiredRecords(retentionDays: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
        return smsDao.deleteNonQuarantinedOlderThan(cutoff)
    }

    private fun serializeFactors(factors: List<ThreatFactor>): String {
        return factors.joinToString("|||") { factor ->
            val cat = java.net.URLEncoder.encode(factor.category, "UTF-8")
            val desc = java.net.URLEncoder.encode(factor.description, "UTF-8")
            val snip = java.net.URLEncoder.encode(factor.matchedSnippet ?: "", "UTF-8")
            "$cat:::$desc:::${factor.severityScore}:::$snip"
        }
    }

    private fun deserializeFactors(raw: String): List<ThreatFactor> {
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

    private fun SmsRecordEntity.toDomain(): SmsMessageData {
        return SmsMessageData(
            id = id,
            sender = sender,
            body = sanitizedBody,
            timestamp = timestamp,
            threatScore = threatScore,
            threatLevel = try { ThreatLevel.valueOf(threatLevel) } catch (e: Exception) { ThreatLevel.SAFE },
            isQuarantined = isQuarantined,
            threatFactors = deserializeFactors(factorsJson),
            extractedUrls = if (extractedUrls.isBlank()) emptyList()
                else if (extractedUrls.contains("|||")) extractedUrls.split("|||")
                else extractedUrls.split(","),
            wasRestored = wasRestored
        )
    }
}
