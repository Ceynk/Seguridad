package com.antiphishing.smishguard

import com.antiphishing.smishguard.util.SafeRegex
import org.junit.Assert.*
import org.junit.Test

class SafeRegexTest {

    @Test
    fun pruebaReDosCatastroficoEsAbortadoPorTimeout() {
        // Expresión de anidamiento de cuantificadores sobre entrada extensa
        val vulnerableRegex = Regex("""(\w+\s*)+""")
        val evilInput = "abc ".repeat(2000) + "!"

        val result = SafeRegex.containsMatch(vulnerableRegex, evilInput, timeoutMs = 1)
        assertNull("Debe neutralizar la regex con backtracking o alto consumo retornando null", result)
    }

    @Test
    fun pruebaTimeoutExpiradoRetornaNull() {
        val regex = Regex("banco", RegexOption.IGNORE_CASE)
        val text = "Alerta de seguridad en su cuenta bancaria"

        val result = SafeRegex.containsMatch(regex, text, timeoutMs = -1)
        assertNull("Debe retornar null cuando el plazo expira", result)
    }

    @Test
    fun pruebaCoincidenciaNormalRapida() {
        val regex = Regex("banco|seguridad|cuenta", RegexOption.IGNORE_CASE)
        val text = "Alerta de seguridad en su cuenta bancaria"

        val result = SafeRegex.containsMatch(regex, text, timeoutMs = 100)
        assertEquals(true, result)
    }

    @Test
    fun pruebaSinCoincidenciaRetornaFalse() {
        val regex = Regex("premio|sorteo", RegexOption.IGNORE_CASE)
        val text = "Hola mama, ya voy para la casa"

        val result = SafeRegex.containsMatch(regex, text, timeoutMs = 100)
        assertEquals(false, result)
    }

    @Test
    fun pruebaValidacionRechazaExpresionesVacias() {
        val err = SafeRegex.validateUserRule("", isRegex = false)
        assertNotNull(err)
        assertTrue(err!!.contains("vacía"))
    }

    @Test
    fun pruebaValidacionRechazaPalabrasMuyCortas() {
        val err = SafeRegex.validateUserRule("ab", isRegex = false)
        assertNotNull(err)
        assertTrue(err!!.contains("al menos"))
    }

    @Test
    fun pruebaValidacionRechazaRegexQueBloqueariaTodo() {
        val err = SafeRegex.validateUserRule(".*", isRegex = true)
        assertNotNull(err)
        assertTrue(err!!.contains("bloquearía todo"))
    }

    @Test
    fun pruebaValidacionRechazaRegexConSintaxisInvalida() {
        val err = SafeRegex.validateUserRule("[a-z(", isRegex = true)
        assertNotNull(err)
        assertTrue(err!!.contains("inválida"))
    }

    @Test
    fun pruebaValidacionAceptaReglasSeguras() {
        assertNull(SafeRegex.validateUserRule("promocion", isRegex = false))
        assertNull(SafeRegex.validateUserRule("""\bdescuento\s+exclusivo\b""", isRegex = true))
    }
}
