package com.antiphishing.smishguard.domain.detector

object NormalizadorTexto {

    private val PATRON_ESPACIOS = Regex("""\s+""")
    private val PATRON_SIGNOS_REPETIDOS = Regex("""([!?.,:;¡¿*_\-])\1+""")
    private val PATRON_PALABRA = Regex("""[a-z0-9@$]+""")

    private val SUSTITUCIONES = mapOf(
        '0' to 'o', '1' to 'l', '3' to 'e', '4' to 'a',
        '5' to 's', '7' to 't', '@' to 'a', '$' to 's'
    )

    fun normalizar(texto: String): String {
        val sinHomoglifos = HomographDetector.normalizeHomoglyphs(texto)
        val sinTildes = ClasificadorMensajesSms.quitarAcentos(sinHomoglifos.lowercase())
        val sinOfuscacion = PATRON_PALABRA.replace(sinTildes) { coincidencia ->
            desofuscarPalabra(coincidencia.value)
        }
        val sinSignosRepetidos = PATRON_SIGNOS_REPETIDOS.replace(sinOfuscacion, "$1")
        return PATRON_ESPACIOS.replace(sinSignosRepetidos, " ").trim()
    }

    private fun desofuscarPalabra(palabra: String): String {
        var letras = 0
        var simbolos = 0
        for (caracter in palabra) {
            if (caracter in 'a'..'z') letras++
            if (SUSTITUCIONES.containsKey(caracter)) simbolos++
        }
        if (letras < 2 || simbolos == 0 || simbolos > letras) {
            return palabra
        }
        val resultado = StringBuilder(palabra.length)
        for (caracter in palabra) {
            resultado.append(SUSTITUCIONES[caracter] ?: caracter)
        }
        return resultado.toString()
    }

    fun soloLetras(texto: String): String {
        val resultado = StringBuilder(texto.length)
        for (caracter in normalizar(texto)) {
            if (caracter in 'a'..'z') resultado.append(caracter)
        }
        return resultado.toString()
    }
}
