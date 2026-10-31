package com.antiphishing.smishguard

import com.antiphishing.smishguard.domain.detector.DomainReputationSource
import com.antiphishing.smishguard.domain.detector.ReputacionDominio
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer
import com.antiphishing.smishguard.domain.model.ThreatLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CasoBancolombiaCardifTest {

    private val remitente = "MiBmacolombia"
    private val mensaje = "MiBmacolombia: Tu inscripcion al servicio CARDI'F quedo efectuada el 07/08/2026 " +
        "por 287.896. Desconoces este movimiento? Anulalo aqui: svaglfm.org"

    @Test
    fun smsDePruebaNoEsLegitimoYReconoceSenalesObligatorias() {
        val resultado = SmishDetectorEngine.analyze(sender = remitente, body = mensaje)

        assertNotEquals(ThreatLevel.SAFE, resultado.level)
        assertEquals(ThreatLevel.PHISHING, resultado.level)
        assertTrue(resultado.isQuarantined)
        assertTrue(resultado.extractedUrls.contains("svaglfm.org"))

        val categorias = resultado.factors.map { it.category }
        val descripciones = resultado.factors.joinToString(" | ") { it.description.lowercase() }

        assertTrue(categorias.any { it.contains("Suplantación de Marca en Remitente") })
        assertTrue(categorias.any { it.contains("Dominio No Oficial para Entidad Mencionada") })
        assertTrue(categorias.contains(UrlThreatAnalyzer.CATEGORIA_SIN_ESQUEMA))
        assertTrue(categorias.contains(UrlThreatAnalyzer.CATEGORIA_DOMINIO_ALEATORIO))
        assertTrue(categorias.contains("Combinación de Riesgo"))
        assertTrue(descripciones.contains("suscripcion") || descripciones.contains("suscripción"))
        assertTrue(descripciones.contains("anular"))
        assertTrue(descripciones.contains("financiera"))
        assertFalse(resultado.summaryReason.contains("%"))
    }

    @Test
    fun senalesDeLegitimidadNoAnulanCombinacionCritica() {
        val conPieDefensivoFalso = "$mensaje Nunca compartas tu clave con nadie."
        val resultado = SmishDetectorEngine.analyze(sender = remitente, body = conPieDefensivoFalso)

        assertEquals(ThreatLevel.PHISHING, resultado.level)
        assertTrue(resultado.isQuarantined)
    }

    @Test
    fun palabraSueltaYUrlLargaDesconocidaNoBloqueanSolas() {
        val soloPalabra = SmishDetectorEngine.analyze(sender = "Contacto", body = "Por favor anula la cita del viernes.")
        assertEquals(ThreatLevel.SAFE, soloPalabra.level)
        assertFalse(soloPalabra.isQuarantined)

        val urlLargaDesconocida = SmishDetectorEngine.analyze(
            sender = "VitalFit",
            body = "Hola, consulta el horario de clases grupales de noviembre en " +
                "https://vitalfitgym.com.co/sedes/medellin/horarios-clases-grupales-2026?mes=noviembre&sede=poblado"
        )
        assertEquals(ThreatLevel.SAFE, urlLargaDesconocida.level)
        assertFalse(urlLargaDesconocida.isQuarantined)
    }

    @Test
    fun dominioRegistrableConSufijosColombianosYFuenteReputacion() {
        assertEquals("epm.com.co", UrlThreatAnalyzer.obtenerDominioRegistrable("pagos.clientes.epm.com.co"))
        assertEquals("simit.org.co", UrlThreatAnalyzer.obtenerDominioRegistrable("consulta.simit.org.co"))
        assertEquals("dian.gov.co", UrlThreatAnalyzer.obtenerDominioRegistrable("muisca.dian.gov.co"))

        val fuentePrueba = object : DomainReputationSource {
            override fun consultar(dominio: String): ReputacionDominio {
                return if (dominio == "portal-desconocido.co") ReputacionDominio.MALICIOSO else ReputacionDominio.DESCONOCIDO
            }
        }
        val resultado = SmishDetectorEngine.analyze(
            sender = "Info",
            body = "Revisa el documento en https://portal-desconocido.co/doc",
            reputationSource = fuentePrueba
        )
        assertEquals(ThreatLevel.PHISHING, resultado.level)
    }
}
