package com.antiphishing.smishguard.domain.detector

object DetectorMarcas {

    data class MarcaDetectada(val marca: String, val esExacta: Boolean)

    private val MARCAS_CON_TOLERANCIA = listOf(
        "bancolombia", "davivienda", "daviplata", "bancodebogota", "scotiabank", "colpatria",
        "avvillas", "falabella", "lulobank", "transfiya", "netflix", "mercadolibre",
        "servientrega", "movistar", "paypal"
    )

    private val PALABRAS_COMUNES_IGNORADAS = setOf(
        "dale", "envia", "exito", "correos", "ing", "hacienda", "whatsapp"
    )

    private val PATRON_SEPARADOR = Regex("""[^a-z]+""")

    fun buscarEnRemitente(remitente: String): MarcaDetectada? {
        val compacto = NormalizadorTexto.soloLetras(remitente)
        if (compacto.isEmpty()) return null
        return buscarEnPalabras(listOf(compacto), compacto)
    }

    fun buscarEnTexto(texto: String): MarcaDetectada? {
        val normalizado = NormalizadorTexto.normalizar(texto)
        val palabras = normalizado.split(PATRON_SEPARADOR).filter { it.isNotEmpty() }
        val compacto = palabras.joinToString("")
        return buscarEnPalabras(palabras, compacto)
    }

    private fun buscarEnPalabras(palabras: List<String>, compacto: String): MarcaDetectada? {
        for (marca in UrlThreatAnalyzer.DOMINIOS_OFICIALES_POR_MARCA.keys) {
            if (PALABRAS_COMUNES_IGNORADAS.contains(marca)) continue
            if (marca.length >= 6 && compacto.contains(marca)) {
                return MarcaDetectada(marca, true)
            }
            if (marca.length < 6 && palabras.contains(marca)) {
                return MarcaDetectada(marca, true)
            }
        }
        for (marca in MARCAS_CON_TOLERANCIA) {
            val tolerancia = if (marca.length >= 9) 2 else 1
            val minLen = marca.length - 1
            for (palabra in palabras) {
                if (palabra.length < minLen) continue
                if (contieneParecido(palabra, marca, tolerancia)) {
                    return MarcaDetectada(marca, false)
                }
            }
        }
        return null
    }

    private fun contieneParecido(palabra: String, marca: String, tolerancia: Int): Boolean {
        val anterior = IntArray(marca.length + 1)
        val actual = IntArray(marca.length + 1)
        for (tamano in (marca.length - 1)..(marca.length + 1)) {
            if (tamano > palabra.length) continue
            for (inicio in 0..(palabra.length - tamano)) {
                if (distanciaEdicionVentana(palabra, inicio, tamano, marca, tolerancia, anterior, actual)) {
                    return true
                }
            }
        }
        return false
    }

    private fun distanciaEdicionVentana(
        palabra: String,
        inicio: Int,
        tamano: Int,
        marca: String,
        tolerancia: Int,
        anterior: IntArray,
        actual: IntArray
    ): Boolean {
        val mLen = marca.length
        for (j in 0..mLen) anterior[j] = j
        for (i in 1..tamano) {
            actual[0] = i
            var minFila = i
            val charA = palabra[inicio + i - 1]
            for (j in 1..mLen) {
                val costo = if (charA == marca[j - 1]) 0 else 1
                val valor = minOf(actual[j - 1] + 1, anterior[j] + 1, anterior[j - 1] + costo)
                actual[j] = valor
                if (valor < minFila) minFila = valor
            }
            if (minFila > tolerancia) return false
            for (j in 0..mLen) anterior[j] = actual[j]
        }
        return anterior[mLen] <= tolerancia
    }

    fun distanciaEdicion(a: String, b: String): Int {
        val anterior = IntArray(b.length + 1) { it }
        val actual = IntArray(b.length + 1)
        for (i in 1..a.length) {
            actual[0] = i
            for (j in 1..b.length) {
                val costo = if (a[i - 1] == b[j - 1]) 0 else 1
                actual[j] = minOf(actual[j - 1] + 1, anterior[j] + 1, anterior[j - 1] + costo)
            }
            for (j in 0..b.length) anterior[j] = actual[j]
        }
        return anterior[b.length]
    }

    fun dominioEsOficialParaMarca(dominio: String, marca: String): Boolean {
        val oficiales = UrlThreatAnalyzer.DOMINIOS_OFICIALES_POR_MARCA[marca] ?: return false
        return UrlThreatAnalyzer.esDominioOficial(dominio, oficiales)
    }
}
