package com.antiphishing.smishguard

import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import com.antiphishing.smishguard.data.repository.CommunityIndicatorNormalizer
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.domain.model.ThreatLevel
import org.junit.Assert.assertEquals
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
}
