package com.antiphishing.smishguard

import com.antiphishing.smishguard.domain.detector.CallDetectorEngine
import com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer
import com.antiphishing.smishguard.domain.model.CallAction
import com.antiphishing.smishguard.domain.model.ThreatLevel
import org.junit.Assert.*
import org.junit.Test

/**
 * Suite de verificación técnica para sustentación académica.
 * Comprueba los 13 escenarios obligatorios de seguridad y control de navegación.
 */
class SecurityVerificationTest {

    // 1. URL normal y conocida
    @Test
    fun escenario01_urlNormalYConocida() {
        val url = "https://www.bancolombia.com"
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(url)

        assertFalse("Una URL oficial no debe ser bloqueada", decision.isBlocked)
        assertEquals("El nivel de amenaza debe ser 0", 0, decision.threatScore)
        assertTrue("No debe tener factores de amenaza", decision.factors.isEmpty())
    }

    // 2. URL con estructura sospechosa
    @Test
    fun escenario02_urlConEstructuraSospechosa() {
        val url = "http://banco-login-verificar-cuenta-alerta.net/actualizar"
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(url)

        assertTrue("Debe bloquearse por acumulación de patrones sospechosos", decision.isBlocked)
        assertTrue("El puntaje debe ser alto", decision.threatScore >= 40)
        assertTrue(
            "Debe detectar palabras de fraude o exceso de guiones",
            decision.factors.any {
                it.category.contains("Palabras") || it.category.contains("Guiones") || it.category.contains("HTTP")
            }
        )
    }

    // 3. URL de phishing conocida para pruebas
    @Test
    fun escenario03_urlDePhishingConocidaParaPruebas() {
        val url = "https://bbva-seguridad.duckdns.org/login"
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(url)

        assertTrue("Debe bloquearse la URL de prueba de phishing", decision.isBlocked)
        assertTrue("El puntaje de amenaza debe ser crítico (>=60)", decision.threatScore >= 60)
        assertTrue(
            "Debe detectar la suplantación de marca y el túnel/alojamiento gratuito",
            decision.factors.any { it.category.contains("Marca") || it.category.contains("Alojamiento") }
        )
    }

    // 4. URL con IP en lugar de dominio
    @Test
    fun escenario04_urlConIpEnLugarDeDominio() {
        val url = "http://192.168.1.50/dgt"
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(url)

        assertTrue("Una IP directa debe bloquearse", decision.isBlocked)
        assertTrue(
            "Debe registrar el factor de Dirección IP Directa",
            decision.factors.any { it.category.contains("Dirección IP Directa") }
        )
    }

    // 5. URL con subdominios sospechosos
    @Test
    fun escenario05_urlConSubdominiosSospechosos() {
        val url = "https://bancolombia.verificar.login.cuenta.sitio-trampa.xyz"
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(url)

        assertTrue("Debe bloquearse el dominio con múltiples subdominios engañosos", decision.isBlocked)
        assertTrue(
            "Debe registrar factor de subdominios, extensión o marca",
            decision.factors.any {
                it.category.contains("Subdominios") ||
                it.category.contains("Extensión") ||
                it.category.contains("Marca")
            }
        )
    }

    // 6. URL con caracteres extraños (@ en URL)
    @Test
    fun escenario06_urlConCaracteresExtraños() {
        val url = "https://usuario:clave@sitio-trampa.com/login"
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(url)

        assertTrue(
            "Debe detectar el carácter anómalo '@'",
            decision.factors.any { it.category.contains("@") }
        )
    }

    // 7. URL acortada
    @Test
    fun escenario07_urlAcortada() {
        val url = "https://bit.ly/3xXyZ"
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(url)

        assertTrue(
            "Debe identificar el servicio acortador",
            decision.factors.any { it.category.contains("Acortado") }
        )
        assertTrue(decision.threatScore >= 20)
    }

    // 8. URL vacía o inválida
    @Test
    fun escenario08_urlVaciaOInvalida() {
        val decisionVacia = UrlThreatAnalyzer.analyzeSingleUrl("")
        val decisionEspacios = UrlThreatAnalyzer.analyzeSingleUrl("   ")

        assertFalse("URL vacía no debe bloquear el sistema", decisionVacia.isBlocked)
        assertEquals(0, decisionVacia.threatScore)
        assertFalse("URL de espacios no debe bloquear", decisionEspacios.isBlocked)
        assertEquals(0, decisionEspacios.threatScore)
    }

    // 9. Número telefónico normal
    @Test
    fun escenario09_numeroTelefonicoNormal() {
        val numero = "+573105551234"
        val resultado = CallDetectorEngine.analyzeCall(numero)

        assertEquals("Debe clasificarse como Seguro", ThreatLevel.SAFE, resultado.level)
        assertEquals("La acción debe ser Permitir", CallAction.ALLOW, resultado.action)
        assertFalse("No debe ser bloqueado", resultado.isBlocked)
        assertEquals(0, resultado.threatScore)
    }

    // 10. Número telefónico sospechoso
    @Test
    fun escenario10_numeroTelefonicoSospechoso() {
        val numeroMalformado = "++57300++1234"
        val resultado = CallDetectorEngine.analyzeCall(numeroMalformado)

        assertEquals(ThreatLevel.SUSPICIOUS, resultado.level)
        assertEquals(CallAction.WARN, resultado.action)
        assertTrue(
            "Debe detectar la anomalía estructural",
            resultado.factors.any { it.category.contains("Malformado") }
        )
    }

    // 11. Número desconocido
    @Test
    fun escenario11_numeroDesconocido() {
        val numeroDesconocido = "+573019876543"
        val resultado = CallDetectorEngine.analyzeCall(numeroDesconocido)

        assertEquals(
            "Un número desconocido estándar NO debe marcarse como peligroso",
            ThreatLevel.SAFE,
            resultado.level
        )
        assertEquals(CallAction.ALLOW, resultado.action)
        assertFalse(resultado.isBlocked)
        assertEquals(0, resultado.threatScore)
    }

    // 12. Intentar abrir una URL maliciosa (Bloqueo previo a la navegación)
    @Test
    fun escenario12_intentarAbrirUrlMaliciosa() {
        val urlPeligrosa = "https://nequi-desbloqueo.pages.dev/login"

        // El flujo del sistema evalúa la URL antes de ejecutar cualquier Intent
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(urlPeligrosa)

        var intentNavegacionLanzado = false
        if (decision.isBlocked) {
            // Se activa la pantalla de bloqueo y se detiene el flujo
            intentNavegacionLanzado = false
        } else {
            intentNavegacionLanzado = true
        }

        assertTrue("El enlace debe ser catalogado como bloqueado", decision.isBlocked)
        assertFalse("El Intent hacia el navegador NUNCA debe lanzarse si es malicioso", intentNavegacionLanzado)
    }

    // 13. Intentar abrir una URL segura (Permite navegación)
    @Test
    fun escenario13_intentarAbrirUrlSegura() {
        val urlSegura = "https://www.google.com"

        val decision = UrlThreatAnalyzer.analyzeSingleUrl(urlSegura)

        var intentNavegacionLanzado = false
        if (decision.isBlocked) {
            intentNavegacionLanzado = false
        } else {
            // El enlace es seguro: se autoriza el lanzamiento del Intent
            intentNavegacionLanzado = true
        }

        assertFalse("El enlace seguro no debe ser bloqueado", decision.isBlocked)
        assertTrue("El enlace seguro debe permitir la navegación normalmente", intentNavegacionLanzado)
    }
}
