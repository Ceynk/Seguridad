package com.antiphishing.smishguard

import com.antiphishing.smishguard.domain.detector.CallDetectorEngine
import com.antiphishing.smishguard.domain.model.CallAction
import com.antiphishing.smishguard.domain.model.ThreatLevel
import org.junit.Assert.*
import org.junit.Test

class CallDetectorEngineTest {

    @Test
    fun pruebaDeteccionLlamadaWangiriIslaAscension() {
        val incoming = "+247551234"
        val result = CallDetectorEngine.analyzeCall(incoming)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertEquals(CallAction.BLOCK, result.action)
        assertTrue(result.isBlocked)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.factors.any { it.category.contains("Wangiri") })
        assertNotNull(result.countryOrOrigin)
    }

    @Test
    fun pruebaDeteccionLlamadaWangiriBenin() {
        val incoming = "+22997123456"
        val result = CallDetectorEngine.analyzeCall(incoming)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertEquals(CallAction.BLOCK, result.action)
        assertTrue(result.isBlocked)
        assertTrue(result.factors.any { it.category.contains("Wangiri") })
    }

    @Test
    fun pruebaDeteccionLlamadaTarificacionEspecialEspana() {
        val incoming = "+34905123456"
        val result = CallDetectorEngine.analyzeCall(incoming)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertEquals(CallAction.BLOCK, result.action)
        assertTrue(result.isBlocked)
        assertTrue(result.factors.any { it.category.contains("Tarificación Adicional") })
    }

    @Test
    fun pruebaDeteccionLlamadaTarificacionEspecialColombia() {
        val incoming = "+5701900123456"
        val result = CallDetectorEngine.analyzeCall(incoming)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertEquals(CallAction.BLOCK, result.action)
        assertTrue(result.isBlocked)
        assertTrue(result.factors.any { it.category.contains("Tarificación Adicional") })
    }

    @Test
    fun pruebaNumeroEmergenciaEsSeguro() {
        val emergencyNumbers = listOf("123", "112", "911")
        for (num in emergencyNumbers) {
            val result = CallDetectorEngine.analyzeCall(num)
            assertEquals(ThreatLevel.SAFE, result.level)
            assertEquals(CallAction.ALLOW, result.action)
            assertEquals(0, result.threatScore)
            assertFalse(result.isBlocked)
        }
    }

    @Test
    fun pruebaNumeroEnListaBlancaEsSeguro() {
        val incoming = "+247551234" // Wangiri, pero en lista blanca
        val result = CallDetectorEngine.analyzeCall(incoming, isWhitelisted = true)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(CallAction.ALLOW, result.action)
        assertEquals(0, result.threatScore)
        assertFalse(result.isBlocked)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun pruebaDeteccionNeighborSpoofing() {
        val userNumber = "+573001234567"
        val incoming = "+573001239999" // Mismo operador y prefijo local

        val result = CallDetectorEngine.analyzeCall(incoming, userNumber = userNumber)

        assertTrue(result.factors.any { it.category.contains("Neighbor Spoofing") })
        assertTrue(result.threatScore >= 25)
    }

    @Test
    fun pruebaDeteccionNumeroMalformadoConVariosSignosMas() {
        val incoming = "++57300++1234"
        val result = CallDetectorEngine.analyzeCall(incoming)

        assertTrue(result.factors.any { it.category.contains("Malformado") })
        assertTrue(result.threatScore >= 35)
    }

    @Test
    fun pruebaLlamadaNormalNacionalEsSegura() {
        val incoming = "+573105551234"
        val result = CallDetectorEngine.analyzeCall(incoming)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(CallAction.ALLOW, result.action)
        assertFalse(result.isBlocked)
        assertEquals(0, result.threatScore)
    }

    @Test
    fun pruebaReglaPersonalizadaDeLlamadaBloquea() {
        val customRule = listOf(Regex("""^018000.*"""))
        val incoming = "018000998877"

        val result = CallDetectorEngine.analyzeCall(
            incomingNumber = incoming,
            customBlockPatterns = customRule,
            blockThreshold = 50
        )

        assertTrue(result.factors.any { it.category.contains("Personalizada") })
        assertTrue(result.isBlocked)
    }

    @Test
    fun pruebaLlamadaNumeroOcultoEnModoAgresivoBloquea() {
        val result = CallDetectorEngine.analyzeCall(
            incomingNumber = "",
            aggressiveMode = true,
            blockThreshold = 60
        )

        assertTrue(result.isBlocked)
        assertEquals(CallAction.BLOCK, result.action)
        assertTrue(result.factors.any { it.category.contains("Oculto") })
    }

    @Test
    fun pruebaFalloVerificacionRedStirShakenSumaRiesgo() {
        val incoming = "+573001234567"
        val result = CallDetectorEngine.analyzeCall(
            incomingNumber = incoming,
            networkVerificationFailed = true
        )

        assertTrue(result.threatScore >= 50)
        assertTrue(result.factors.any { it.category.contains("Falsificado") || it.category.contains("STIR/SHAKEN") })
    }

    @Test
    fun pruebaReglaPersonalizadaConRiesgoReDosEsManejadaSegura() {
        // Expresión exponencial ReDoS proporcionada como regla
        val badRule = listOf(Regex("^(a+)+$"))
        val evilNumber = "a".repeat(30) + "!"

        val result = CallDetectorEngine.analyzeCall(
            incomingNumber = evilNumber,
            customBlockPatterns = badRule
        )

        assertNotNull(result)
    }
}
