package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.BuildConfig
import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class CommunityApiClient(
    private val urlBase: String = obtenerUrlPredeterminada()
) {

    fun enviar(indicador: CommunityIndicator): CommunityBlacklistEntity? {
        if (urlBase.isBlank()) {
            return null
        }
        val normalizado = CommunityIndicatorNormalizer.obtener(indicador.valor).firstOrNull()
            ?: indicador
        val conexion = enviarPostReporte(normalizado.valor, normalizado.tipo)
        if (conexion.responseCode in 200..299) {
            return leerEntrada(conexion.inputStream.bufferedReader().use { it.readText() })
        }
        if (conexion.responseCode == 400 && normalizado.tipo == "numero") {
            val aliasDominio = codificarNumeroComoDominio(normalizado.valor)
            if (aliasDominio != null) {
                val conexionAlterna = enviarPostReporte(aliasDominio, "dominio")
                if (conexionAlterna.responseCode in 200..299) {
                    return leerEntrada(conexionAlterna.inputStream.bufferedReader().use { it.readText() })
                }
            }
        }
        throw IllegalStateException("El API comunitaria respondió ${conexion.responseCode}")
    }

    private fun enviarPostReporte(valor: String, tipo: String): HttpURLConnection {
        val conexion = abrir("/v1/reportes")
        conexion.requestMethod = "POST"
        conexion.doOutput = true
        conexion.setRequestProperty("Content-Type", "application/json")
        val cuerpo = JSONObject()
            .put("indicador", valor)
            .put("tipo", tipo)
            .toString()
        conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
        return conexion
    }

    fun eliminar(indicador: String): Boolean {
        if (urlBase.isBlank()) {
            return false
        }
        val limpio = indicador.trim()
        val codificado = URLEncoder.encode(limpio, "UTF-8")
        val conexion = abrir("/v1/reportes?indicador=$codificado")
        conexion.requestMethod = "DELETE"
        if (conexion.responseCode == 404) {
            return true
        }
        if (conexion.responseCode !in 200..299) {
            throw IllegalStateException("El API comunitaria respondió ${conexion.responseCode}")
        }
        val aliasDominio = codificarNumeroComoDominio(limpio)
        if (aliasDominio != null) {
            runCatching {
                val codificadoAlias = URLEncoder.encode(aliasDominio, "UTF-8")
                val conexionAlias = abrir("/v1/reportes?indicador=$codificadoAlias")
                conexionAlias.requestMethod = "DELETE"
                conexionAlias.responseCode
            }
        }
        return true
    }

    fun descargar(desde: Long = 0L): List<CommunityBlacklistEntity> {
        if (urlBase.isBlank()) {
            return emptyList()
        }
        val ruta = if (desde > 0L) "/v1/lista-negra?desde=$desde" else "/v1/lista-negra"
        val conexion = abrir(ruta)
        conexion.requestMethod = "GET"
        if (conexion.responseCode !in 200..299) {
            throw IllegalStateException("El API comunitaria respondió ${conexion.responseCode}")
        }
        val arreglo = JSONArray(conexion.inputStream.bufferedReader().use { it.readText() })
        val resultado = ArrayList<CommunityBlacklistEntity>(arreglo.length())
        for (indice in 0 until arreglo.length()) {
            leerEntradaObjeto(arreglo.getJSONObject(indice))?.let { resultado.add(it) }
        }
        return resultado
    }

    fun descargarInteligencia(desde: Long = 0L): List<CommunityBlacklistEntity> {
        if (urlBase.isBlank()) {
            return emptyList()
        }
        val ruta = if (desde > 0L) "/v1/inteligencia?desde=$desde" else "/v1/inteligencia"
        val conexion = abrir(ruta)
        conexion.requestMethod = "GET"
        if (conexion.responseCode !in 200..299) {
            throw IllegalStateException("El API de inteligencia respondió ${conexion.responseCode}")
        }
        val arreglo = JSONArray(conexion.inputStream.bufferedReader().use { it.readText() })
        val resultado = mutableListOf<CommunityBlacklistEntity>()
        for (indice in 0 until arreglo.length()) {
            val objeto = arreglo.getJSONObject(indice)
            val numero = objeto.optString("numeroE164").ifBlank { objeto.optString("indicador") }.trim()
            if (numero.isBlank()) {
                continue
            }
            val reportes = objeto.optInt("reportesIndependientes", objeto.optInt("reportes", 0))
            val confirmada = objeto.optString("estadoVerificacion") == "confirmado"
            val actualizado = parseFecha(objeto.optString("actualizadoEn"))
            resultado.add(
                CommunityBlacklistEntity(
                    indicador = numero,
                    tipo = "numero",
                    reportes = reportes,
                    esMalicioso = confirmada || reportes >= 1,
                    actualizadoEn = actualizado
                )
            )
        }
        return resultado
    }

    private fun abrir(ruta: String): HttpURLConnection {
        val base = urlBase.trimEnd('/')
        return (URL("$base$ruta").openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
        }
    }

    private fun leerEntrada(texto: String): CommunityBlacklistEntity? {
        return leerEntradaObjeto(JSONObject(texto))
    }

    private fun leerEntradaObjeto(objeto: JSONObject): CommunityBlacklistEntity? {
        val indicadorCrudo = objeto.optString("indicador").ifBlank { objeto.optString("numeroE164") }.trim()
        val numeroDecodificado = decodificarNumeroDesdeDominio(indicadorCrudo)
        val indicador = numeroDecodificado ?: indicadorCrudo
        val tipo = if (numeroDecodificado != null) {
            "numero"
        } else {
            objeto.optString("tipo").ifBlank { if (indicador.startsWith("+")) "numero" else "" }.trim()
        }
        if (indicador.isBlank() || tipo.isBlank()) {
            return null
        }
        val reportes = objeto.optInt("reportes", objeto.optInt("reportesIndependientes", 0))
        val confirmada = objeto.optString("estadoVerificacion") == "confirmado"
        val actualizado = parseFecha(objeto.optString("actualizadoEn"))
        return CommunityBlacklistEntity(
            indicador = indicador,
            tipo = tipo,
            reportes = reportes,
            esMalicioso = objeto.optBoolean("esMalicioso", confirmada || reportes >= 1),
            actualizadoEn = actualizado
        )
    }

    private fun codificarNumeroComoDominio(valor: String): String? {
        val limpio = valor.trim()
        val tieneMas = limpio.startsWith("+")
        val digitos = limpio.filter { it.isDigit() }
        if (digitos.length !in 8..15) {
            return null
        }
        val prefijo = if (tieneMas) "plus$digitos" else digitos
        return "$prefijo.tel"
    }

    private fun decodificarNumeroDesdeDominio(valor: String): String? {
        val limpio = valor.trim().lowercase()
        if (!limpio.endsWith(".tel")) {
            return null
        }
        val etiqueta = limpio.removeSuffix(".tel")
        if (etiqueta.startsWith("plus")) {
            val digitos = etiqueta.removePrefix("plus")
            if (digitos.length in 8..15 && digitos.all { it.isDigit() }) {
                return "+$digitos"
            }
        } else if (etiqueta.length in 8..15 && etiqueta.all { it.isDigit() }) {
            return etiqueta
        }
        return null
    }

    private fun parseFecha(valor: String): Long {
        return valor.toLongOrNull() ?: runCatching {
            java.time.Instant.parse(valor).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    }

    companion object {
        private const val URL_RENDER = "https://smishguard-servidor.onrender.com"

        private fun obtenerUrlPredeterminada(): String {
            val configurada = BuildConfig.COMMUNITY_API_URL.trim()
            if (configurada.isBlank() || configurada.indexOf("http", 4) != -1) {
                return URL_RENDER
            }
            return configurada
        }
    }
}
