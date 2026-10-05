package com.antiphishing.smishguard

import com.antiphishing.smishguard.util.SenderKey
import org.junit.Assert.*
import org.junit.Test

class SenderKeyTest {

    @Test
    fun pruebaCoincidenciaNumeroConYPrefijoDePais() {
        // En Colombia (+57)
        val storedInWhitelist = "3001234567"
        val incomingFromNetwork = "+57 300 123 4567"

        assertTrue(SenderKey.matches(storedInWhitelist, incomingFromNetwork))
        assertTrue(SenderKey.matches(incomingFromNetwork, storedInWhitelist))
    }

    @Test
    fun pruebaCoincidenciaConDiferenteFormatoYEspacios() {
        val a = "+34 (600) 12-34-56"
        val b = "34600123456"

        assertTrue(SenderKey.matches(a, b))
    }

    @Test
    fun pruebaRemitenteAlfanumericoEsCaseInsensitive() {
        val stored = "Bancolombia"
        val incoming = "BANCOLOMBIA"

        assertTrue(SenderKey.matches(stored, incoming))
    }

    @Test
    fun pruebaNumerosDiferentesNoCoinciden() {
        val a = "+573001234567"
        val b = "+573009998877"

        assertFalse(SenderKey.matches(a, b))
    }

    @Test
    fun pruebaMatchesAnyFuncionaCorrectamente() {
        val whitelist = listOf("Mama", "+573001234567", "Banco Bogota")

        assertTrue(SenderKey.matchesAny("3001234567", whitelist))
        assertTrue(SenderKey.matchesAny("mama", whitelist))
        assertFalse(SenderKey.matchesAny("+573105559988", whitelist))
    }
}
