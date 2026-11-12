package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.data.local.dao.SmsDao
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.CommunityBlacklistDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.data.local.dao.UserReportDao
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import com.antiphishing.smishguard.data.local.entity.SmsRecordEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
import com.antiphishing.smishguard.data.local.entity.UserReportEntity
import com.antiphishing.smishguard.data.security.FieldCipher
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.util.SafeRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SmsRepository(
    private val smsDao: SmsDao,
    private val whitelistDao: WhitelistDao,
    private val customRuleDao: CustomRuleDao,
    private val ruleCache: RuleCache,
    private val communityBlacklistDao: CommunityBlacklistDao,
    private val userReportDao: UserReportDao
) {

    private data class CachedAnalysis(
        val result: AnalysisResult,
        val createdAt: Long
    )

    private val analysisCache = ConcurrentHashMap<String, CachedAnalysis>()
    private val analysisCacheDurationMs = 5 * 60 * 1000L
    private val syncMutex = Mutex()
    private val dbMutex = Mutex()
    private val modificacionesLocales = AtomicInteger(0)

    fun getAllRecords(): Flow<List<SmsMessageData>> {
        return smsDao.getAllRecords().map { entities ->
            entities.map(::convertEntityToDomain)
        }.flowOn(Dispatchers.Default)
    }

    fun getQuarantinedRecords(): Flow<List<SmsMessageData>> {
        return smsDao.getQuarantinedRecords().map { entities ->
            entities.map(::convertEntityToDomain)
        }.flowOn(Dispatchers.Default)
    }

    fun getRecentQuarantinedRecords(limit: Int = 5): Flow<List<SmsMessageData>> {
        return smsDao.getRecentQuarantinedRecords(limit).map { entities ->
            entities.map(::convertEntityToDomain)
        }.flowOn(Dispatchers.Default)
    }

    suspend fun getQuarantinedPage(limit: Int, offset: Int): List<SmsMessageData> {
        val entities = smsDao.getQuarantinedPage(limit, offset)
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            entities.map(::convertEntityToDomain)
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
        val trimmedName = name?.trim()
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

    fun getUserReports(): Flow<List<UserReportEntity>> {
        return userReportDao.getAll()
    }

    suspend fun saveUserReport(
        assignedLevel: ThreatLevel,
        userLevel: ThreatLevel,
        sender: String,
        body: String,
        keepOriginal: Boolean
    ): UserReportEntity {
        val report = UserReportEntity(
            id = UUID.randomUUID().toString(),
            assignedLevel = assignedLevel.name,
            userLevel = userLevel.name,
            timestamp = System.currentTimeMillis(),
            messageHash = identityFor(sender, body),
            sender = sender.trim(),
            originalBodyEncrypted = if (keepOriginal) FieldCipher.encrypt(body) else null
        )
        userReportDao.insert(report)
        return report
    }

    suspend fun deleteUserReport(id: String) {
        userReportDao.deleteById(id)
    }

    suspend fun clearUserReports() {
        userReportDao.deleteAll()
    }

    suspend fun decryptReportBody(report: UserReportEntity): String? {
        val encrypted = report.originalBodyEncrypted ?: return null
        return when (val result = FieldCipher.decrypt(encrypted)) {
            is FieldCipher.DecryptionResult.Decrypted -> result.value
            is FieldCipher.DecryptionResult.LegacyPlaintext -> result.value
            is FieldCipher.DecryptionResult.Unreadable -> null
        }
    }

    fun getAllCustomRules(): Flow<List<CustomRuleEntity>> {
        return customRuleDao.getAllRules()
    }

    suspend fun getActiveCustomRegexes(): List<Regex> {
        return ruleCache.getCustomRegexes()
    }

    suspend fun getCommunityIndicators(): Set<String> {
        return ruleCache.getCommunityIndicators()
    }

    suspend fun getCommunityEntriesByIndicator(): Map<String, CommunityBlacklistEntity> {
        return ruleCache.getCommunityEntriesByIndicator()
    }

    fun getCachedAnalysis(messageIdentity: String): AnalysisResult? {
        val cached = analysisCache[messageIdentity] ?: return null
        if (System.currentTimeMillis() - cached.createdAt >= analysisCacheDurationMs) {
            analysisCache.remove(messageIdentity)
            return null
        }
        return cached.result
    }

    fun cacheAnalysis(messageIdentity: String, analysis: AnalysisResult) {
        analysisCache[messageIdentity] = CachedAnalysis(analysis, System.currentTimeMillis())
    }

    fun getCommunityEntries(): Flow<List<CommunityBlacklistEntity>> {
        return communityBlacklistDao.obtenerTodos()
    }

    suspend fun saveCommunityEntry(
        entry: CommunityBlacklistEntity,
        indicadorOriginal: String? = null
    ) = dbMutex.withLock {
        modificacionesLocales.incrementAndGet()
        val normalizado = CommunityIndicatorNormalizer.obtener(entry.indicador).firstOrNull()?.valor
        if (normalizado != null && normalizado != entry.indicador) {
            communityBlacklistDao.eliminarPorIndicador(normalizado)
        }
        if (!indicadorOriginal.isNullOrBlank() && indicadorOriginal != entry.indicador) {
            communityBlacklistDao.eliminarPorIndicador(indicadorOriginal)
        }
        communityBlacklistDao.guardar(entry)
        ruleCache.invalidateCommunityIndicators()
    }

    suspend fun saveCommunityEntries(entries: List<CommunityBlacklistEntity>): Int = dbMutex.withLock {
        val actuales = communityBlacklistDao.obtenerTodosAhora()
        val actualesSincronizados = actuales.filter { !it.pendienteSincronizacion }
        val pendientesLocales = actuales.filter { it.pendienteSincronizacion }.map { it.indicador }.toSet()
        val eliminadosPrevios = communityBlacklistDao.obtenerEntradasEliminadas().associateBy { it.indicador }
        val consolidados = entries
            .filter { it.reportes >= 0 }
            .groupBy { it.indicador }
            .mapValues { (_, grupo) ->
                grupo.maxByOrNull { it.actualizadoEn } ?: grupo.first()
            }
        val eliminacionesRemotas = entries
            .filter { it.reportes < 0 }
            .groupBy { it.indicador }
            .mapValues { (_, grupo) ->
                grupo.maxByOrNull { it.actualizadoEn } ?: grupo.first()
            }
        for ((indicador, eliminacionRemota) in eliminacionesRemotas) {
            val activaServidor = consolidados[indicador]
            val previoEliminado = eliminadosPrevios[indicador]
            if (indicador !in pendientesLocales &&
                (activaServidor == null || activaServidor.actualizadoEn <= eliminacionRemota.actualizadoEn) &&
                (previoEliminado == null || previoEliminado.actualizadoEn < eliminacionRemota.actualizadoEn || previoEliminado.pendienteSincronizacion)
            ) {
                communityBlacklistDao.guardar(
                    eliminacionRemota.copy(pendienteSincronizacion = false)
                )
            }
        }
        val eliminadosPorIndicador = communityBlacklistDao.obtenerEntradasEliminadas().associateBy { it.indicador }
        val filtradas = consolidados.values
            .filterNot { entrada ->
                val normalizado = CommunityIndicatorNormalizer.obtener(entrada.indicador).firstOrNull()?.valor
                val eliminadoDirecto = eliminadosPorIndicador[entrada.indicador]
                val eliminadoNormalizado = if (normalizado != null) eliminadosPorIndicador[normalizado] else null
                val marcaEliminacion = listOfNotNull(eliminadoDirecto, eliminadoNormalizado)
                    .maxOfOrNull { it.actualizadoEn }
                (marcaEliminacion != null && entrada.actualizadoEn <= marcaEliminacion) ||
                    entrada.indicador in pendientesLocales
            }
        for (entrada in filtradas) {
            if (entrada.indicador in eliminadosPorIndicador) {
                communityBlacklistDao.eliminarPorIndicador(entrada.indicador)
            }
            val normalizado = CommunityIndicatorNormalizer.obtener(entrada.indicador).firstOrNull()?.valor
            if (normalizado != null && normalizado != entrada.indicador && normalizado in eliminadosPorIndicador) {
                communityBlacklistDao.eliminarPorIndicador(normalizado)
            }
        }
        val sinCambios = actualesSincronizados.size == filtradas.size &&
            actualesSincronizados.associateBy { it.indicador }.let { mapaActual ->
                filtradas.all { nueva ->
                    val actual = mapaActual[nueva.indicador]
                    actual != null &&
                        actual.reportes == nueva.reportes &&
                        actual.esMalicioso == nueva.esMalicioso &&
                        actual.tipo == nueva.tipo &&
                        actual.actualizadoEn == nueva.actualizadoEn
                }
            }
        if (!sinCambios) {
            communityBlacklistDao.eliminarSincronizados()
            if (filtradas.isNotEmpty()) {
                communityBlacklistDao.guardarTodos(filtradas)
            }
            ruleCache.invalidateCommunityIndicators()
        }
        return filtradas.size + pendientesLocales.size
    }

    suspend fun sincronizarConServidor(cliente: CommunityApiClient): Int = syncMutex.withLock {
        for (eliminado in getPendingDeletedCommunityIndicators()) {
            try {
                if (cliente.eliminar(eliminado)) {
                    eliminarEntradaComunidad(eliminado, pendienteServidor = false)
                }
            } catch (_: Exception) {
            }
        }
        for (pendiente in getPendingCommunityEntries()) {
            try {
                val sincronizado = cliente.enviar(
                    CommunityIndicator(pendiente.indicador, pendiente.tipo)
                )
                if (sincronizado != null) {
                    saveCommunityEntry(sincronizado, indicadorOriginal = pendiente.indicador)
                }
            } catch (_: Exception) {
            }
        }
        val versionAntes = modificacionesLocales.get()
        var indicadores = cliente.descargar()
        var inteligencia = runCatching { cliente.descargarInteligencia() }.getOrDefault(emptyList())
        if (modificacionesLocales.get() != versionAntes) {
            indicadores = cliente.descargar()
            inteligencia = runCatching { cliente.descargarInteligencia() }.getOrDefault(emptyList())
        }
        return saveCommunityEntries(indicadores + inteligencia)
    }

    suspend fun eliminarEntradaComunidad(indicador: String, pendienteServidor: Boolean = true) = dbMutex.withLock {
        modificacionesLocales.incrementAndGet()
        val limpio = indicador.trim()
        if (limpio.isBlank()) {
            return@withLock
        }
        val normalizado = CommunityIndicatorNormalizer.obtener(limpio).firstOrNull()?.valor
        val objetivos = buildSet {
            add(limpio)
            if (!normalizado.isNullOrBlank()) {
                add(normalizado)
            }
        }
        val activas = communityBlacklistDao.obtenerTodosAhora().filter { it.indicador in objetivos }
        val eliminadas = communityBlacklistDao.obtenerEntradasEliminadas().filter { it.indicador in objetivos }
        if (!pendienteServidor && activas.isNotEmpty()) {
            return@withLock
        }
        val marcaTiempo = (activas + eliminadas).maxOfOrNull { it.actualizadoEn } ?: System.currentTimeMillis()
        for (objetivo in objetivos) {
            communityBlacklistDao.guardar(
                CommunityBlacklistEntity(
                    indicador = objetivo,
                    tipo = "eliminado",
                    reportes = -1,
                    esMalicioso = false,
                    actualizadoEn = marcaTiempo,
                    pendienteSincronizacion = pendienteServidor
                )
            )
        }
        ruleCache.invalidateCommunityIndicators()
    }

    suspend fun getPendingDeletedCommunityIndicators(): List<String> {
        return communityBlacklistDao.obtenerEliminadosPendientes()
    }

    suspend fun getPendingCommunityEntries(): List<CommunityBlacklistEntity> {
        return communityBlacklistDao.obtenerPendientes()
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

    suspend fun saveAnalysis(
        sender: String,
        rawBody: String,
        analysis: AnalysisResult,
        messageIdentity: String = identityFor(sender, rawBody)
    ): Long {
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
            extractedUrls = analysis.extractedUrls.joinToString(","),
            messageIdentity = messageIdentity
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

    sealed interface OriginalBodyResult {
        data object Missing : OriginalBodyResult
        data class Available(val value: String) : OriginalBodyResult
        data class Unreadable(val reason: FieldCipher.DecryptionResult.FailureReason) : OriginalBodyResult
    }

    suspend fun getDecryptedOriginalBody(id: Long): OriginalBodyResult {
        val record = smsDao.getRecordById(id)
        if (record == null) {
            return OriginalBodyResult.Missing
        }
        return if (record.rawBody.isBlank()) {
            OriginalBodyResult.Available(record.sanitizedBody)
        } else {
            when (val result = FieldCipher.decrypt(record.rawBody)) {
                is FieldCipher.DecryptionResult.Decrypted -> OriginalBodyResult.Available(result.value)
                is FieldCipher.DecryptionResult.LegacyPlaintext -> OriginalBodyResult.Available(result.value)
                is FieldCipher.DecryptionResult.Unreadable -> OriginalBodyResult.Unreadable(result.reason)
            }
        }
    }

    companion object {
        fun identityFor(sender: String, body: String, timestamp: Long? = null): String {
            val normalizedTimestamp = timestamp?.let { it / 60_000L }?.toString().orEmpty()
            val input = listOf(sender.trim(), normalizedTimestamp, body).joinToString("\u001f")
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
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
        val urls = when {
            record.extractedUrls.isBlank() -> emptyList()
            "|||" in record.extractedUrls -> record.extractedUrls.split("|||")
            else -> record.extractedUrls.split(",")
        }

        val level = try {
            ThreatLevel.valueOf(record.threatLevel)
        } catch (e: Exception) {
            ThreatLevel.SAFE
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
