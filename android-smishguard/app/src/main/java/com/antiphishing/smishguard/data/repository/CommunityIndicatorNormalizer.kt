package com.antiphishing.smishguard.data.repository

import java.net.URI
import java.util.Locale

data class CommunityIndicator(
    val valor: String,
    val tipo: String
)

object CommunityIndicatorNormalizer {

    private val numeroRegex = Regex("""\+?\d[\d\s().-]{6,}\d""")
    private val urlsRegex = Regex("""(?i)(?:https?://|www\.)[^\s<>'"]+|(?:[a-z0-9-]+\.)+[a-z]{2,}(?:/[^\s<>'"]*)?""")

    fun obtener(texto: String): List<CommunityIndicator> {
        val resultado = LinkedHashMap<String, CommunityIndicator>()
        val urls = urlsRegex.findAll(texto)

        for (coincidencia in urls) {
            val indicador = normalizarUrl(coincidencia.value)
            if (indicador != null) {
                resultado[indicador.valor] = indicador
            }
        }

        for (coincidencia in numeroRegex.findAll(texto)) {
            val numero = coincidencia.value.filter { it.isDigit() }
            if (numero.length >= 8) {
                resultado[numero] = CommunityIndicator(numero, "numero")
            }
        }
        return resultado.values.toList()
    }

    fun normalizarUrl(valor: String): CommunityIndicator? {
        val limpio = valor.trim().trimEnd('.', ',', ';', ':', ')', ']', '!')
        val uri = try {
            URI(
                when {
                    limpio.startsWith("www.", true) -> "https://$limpio"
                    "://" in limpio -> limpio
                    else -> "https://$limpio"
                }
            )
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return null
        if (host.isBlank() || !host.contains('.')) {
            return null
        }
        return CommunityIndicator(host, "dominio")
    }
}
