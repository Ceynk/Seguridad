package com.antiphishing.smishguard

import com.antiphishing.smishguard.data.local.dao.CommunityBlacklistDao
import com.antiphishing.smishguard.data.local.dao.CustomRuleDao
import com.antiphishing.smishguard.data.local.dao.SmsDao
import com.antiphishing.smishguard.data.local.dao.UserReportDao
import com.antiphishing.smishguard.data.local.dao.WhitelistDao
import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.SmsRecordEntity
import com.antiphishing.smishguard.data.local.entity.UserReportEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
import com.antiphishing.smishguard.data.repository.CommunityIndicatorNormalizer
import com.antiphishing.smishguard.data.repository.RuleCache
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityIndicatorTest {

    @Test
    fun normalizaDominioSinEnviarContenido() {
        val indicadores = CommunityIndicatorNormalizer.obtener(
            "SMS privado https://www.Ejemplo.com/ruta"
        )

        assertEquals("ejemplo.com", indicadores.first().valor)
        assertEquals("dominio", indicadores.first().tipo)
    }

    @Test
    fun normalizaNumeroColombianoFormatoE164() {
        val indicadores = CommunityIndicatorNormalizer.obtener(
            "Llamar al +57 310 205 4740 o al 3001234567"
        )

        assertEquals("+573102054740", indicadores[0].valor)
        assertEquals("numero", indicadores[0].tipo)
        assertEquals("+573001234567", indicadores[1].valor)
        assertEquals("numero", indicadores[1].tipo)
    }

    @Test
    fun pocosReportesMuestranAvisoComunitario() {
        val resultado = SmishDetectorEngine.analyze(
            sender = "Alerta",
            body = "Visita https://ejemplo.com",
            communityEntries = mapOf(
                "ejemplo.com" to CommunityBlacklistEntity(
                    indicador = "ejemplo.com",
                    tipo = "dominio",
                    reportes = 1,
                    esMalicioso = false
                )
            )
        )

        assertTrue(resultado.factors.any { it.category == "Reporte de la comunidad" })
        assertEquals(ThreatLevel.SAFE, resultado.level)
    }

    @Test
    fun cincoReportesClasificanComoMalicioso() {
        val resultado = SmishDetectorEngine.analyze(
            sender = "Alerta",
            body = "Visita https://ejemplo.com",
            communityEntries = mapOf(
                "ejemplo.com" to CommunityBlacklistEntity(
                    indicador = "ejemplo.com",
                    tipo = "dominio",
                    reportes = 5,
                    esMalicioso = true
                )
            )
        )

        assertTrue(resultado.isQuarantined)
        assertEquals(ThreatLevel.PHISHING, resultado.level)
    }

    @Test
    fun eliminarNumeroNoReapareceAlSincronizarDeNuevo() = runBlocking {
        val communityDao = FakeCommunityBlacklistDao()
        val whitelistDao = FakeWhitelistDao()
        val customRuleDao = FakeCustomRuleDao()
        val ruleCache = RuleCache(whitelistDao, customRuleDao, communityDao)
        val repository = SmsRepository(
            smsDao = FakeSmsDao(),
            whitelistDao = whitelistDao,
            customRuleDao = customRuleDao,
            ruleCache = ruleCache,
            communityBlacklistDao = communityDao,
            userReportDao = FakeUserReportDao()
        )

        val servidorEntries = listOf(
            CommunityBlacklistEntity(
                indicador = "+573134304936",
                tipo = "numero",
                reportes = 1,
                esMalicioso = true
            ),
            CommunityBlacklistEntity(
                indicador = "+573162626266",
                tipo = "numero",
                reportes = 1,
                esMalicioso = true
            )
        )

        repository.saveCommunityEntries(servidorEntries)
        assertEquals(2, repository.getCommunityEntries().first().size)

        repository.eliminarEntradaComunidad("+573134304936", pendienteServidor = true)
        assertEquals(listOf("+573162626266"), repository.getCommunityEntries().first().map { it.indicador })
        assertFalse(repository.getCommunityIndicators().contains("+573134304936"))

        // Simular salir y volver a entrar (sincronización descarga los mismos números del servidor)
        val total = repository.saveCommunityEntries(servidorEntries)
        assertEquals(1, total)
        assertEquals(listOf("+573162626266"), repository.getCommunityEntries().first().map { it.indicador })
        assertFalse(repository.getCommunityIndicators().contains("+573134304936"))
    }

    private class FakeCommunityBlacklistDao : CommunityBlacklistDao {
        private val items = LinkedHashMap<String, CommunityBlacklistEntity>()
        private val flow = MutableStateFlow<List<CommunityBlacklistEntity>>(emptyList())

        private fun refresh() {
            flow.value = items.values.toList()
        }

        override fun obtenerTodos(): Flow<List<CommunityBlacklistEntity>> =
            flow.map { list -> list.filter { it.reportes >= 0 }.sortedByDescending { it.actualizadoEn } }

        override suspend fun obtenerIndicadores(): List<String> =
            items.values.filter { it.reportes >= 0 }.map { it.indicador }

        override suspend fun obtenerTodosAhora(): List<CommunityBlacklistEntity> =
            items.values.filter { it.reportes >= 0 }

        override suspend fun obtenerPendientes(): List<CommunityBlacklistEntity> =
            items.values.filter { it.pendienteSincronizacion && it.reportes >= 0 }

        override suspend fun obtenerEliminados(): List<String> =
            items.values.filter { it.reportes < 0 }.map { it.indicador }

        override suspend fun obtenerEliminadosPendientes(): List<String> =
            items.values.filter { it.reportes < 0 && it.pendienteSincronizacion }.map { it.indicador }

        override suspend fun guardar(entrada: CommunityBlacklistEntity) {
            items[entrada.indicador] = entrada
            refresh()
        }

        override suspend fun guardarTodos(entradas: List<CommunityBlacklistEntity>) {
            for (entrada in entradas) {
                items[entrada.indicador] = entrada
            }
            refresh()
        }

        override suspend fun eliminarPorIndicador(indicador: String) {
            items.remove(indicador)
            refresh()
        }

        override suspend fun eliminarSincronizados() {
            val iterator = items.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next().value
                if (!entry.pendienteSincronizacion && entry.reportes >= 0) {
                    iterator.remove()
                }
            }
            refresh()
        }
    }

    private class FakeWhitelistDao : WhitelistDao {
        override fun getAllWhitelist(): Flow<List<WhitelistEntity>> = MutableStateFlow(emptyList())
        override suspend fun getAllSenders(): List<String> = emptyList()
        override suspend fun isWhitelisted(sender: String): Boolean = false
        override suspend fun insertWhitelist(entry: WhitelistEntity) {}
        override suspend fun deleteBySender(sender: String) {}
    }

    private class FakeCustomRuleDao : CustomRuleDao {
        override fun getAllRules(): Flow<List<CustomRuleEntity>> = MutableStateFlow(emptyList())
        override suspend fun getActiveRules(): List<CustomRuleEntity> = emptyList()
        override suspend fun insertRule(rule: CustomRuleEntity): Long = 1L
        override suspend fun updateRule(rule: CustomRuleEntity) {}
        override suspend fun deleteRule(rule: CustomRuleEntity) {}
    }

    private class FakeSmsDao : SmsDao {
        override fun getAllRecords(): Flow<List<SmsRecordEntity>> = MutableStateFlow(emptyList())
        override fun getQuarantinedRecords(): Flow<List<SmsRecordEntity>> = MutableStateFlow(emptyList())
        override suspend fun getQuarantinedPage(limit: Int, offset: Int): List<SmsRecordEntity> = emptyList()
        override suspend fun getRecordById(id: Long): SmsRecordEntity? = null
        override suspend fun insertRecord(record: SmsRecordEntity): Long = 1L
        override suspend fun updateRecord(record: SmsRecordEntity) {}
        override suspend fun deleteRecord(record: SmsRecordEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() {}
        override suspend fun clearAllQuarantined() {}
        override suspend fun clearNonQuarantined() {}
        override suspend fun deleteNonQuarantinedOlderThan(cutoffTimestamp: Long): Int = 0
        override fun getTotalAnalyzedCount(): Flow<Int> = MutableStateFlow(0)
        override fun getActiveQuarantinedCount(): Flow<Int> = MutableStateFlow(0)
        override fun getPhishingDetectedCount(): Flow<Int> = MutableStateFlow(0)
    }

    private class FakeUserReportDao : UserReportDao {
        override fun getAll(): Flow<List<UserReportEntity>> = MutableStateFlow(emptyList())
        override suspend fun insert(report: UserReportEntity) {}
        override suspend fun deleteById(id: String) {}
        override suspend fun deleteAll() {}
    }
}
