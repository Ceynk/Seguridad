package com.antiphishing.smishguard.data.repository

import com.antiphishing.smishguard.BuildConfig
import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CommunityApiClient(
    private val urlBase: String = BuildConfig.COMMUNITY_API_URL
) {

    fun enviar(indicador: CommunityIndicator): CommunityBlacklistEntity? {
        if (urlBase.isBlank()) {
            return null
        }
        val conexion = abrir("/v1/reportes")
        conexion.requestMethod = "POST"
        conexion.doOutput = true
        conexion.setRequestProperty("Content-Type", "application/json")
        val cuerpo = JSONObject()
            .put("indicador", indicador.valor)
            .put("tipo", indicador.tipo)
            .toString()
        conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
        if (conexion.responseCode !in 200..299) {
            throw IllegalStateException("El API comunitaria respondió ${conexion.responseCode}")
        }
        return leerEntrada(conexion.inputStream.bufferedReader().use { it.readText() })
    }

    fun descargar(): List<CommunityBlacklistEntity> {
        if (urlBase.isBlank()) {
            return emptyList()
        }
        val conexion = abrir("/v1/lista-negra")
        conexion.requestMethod = "GET"
        if (conexion.responseCode !in 200..299) {
            throw IllegalStateException("El API comunitaria respondió ${conexion.responseCode}")
        }
        val arreglo = JSONArray(conexion.inputStream.bufferedReader().use { it.readText() })
        val resultado = mutableListOf<CommunityBlacklistEntity>()
        for (indice in 0 until arreglo.length()) {
            leerEntrada(arreglo.getJSONObject(indice).toString())?.let { resultado.add(it) }
        }
        return resultado
    }

    private fun abrir(ruta: String): HttpURLConnection {
        val base = urlBase.trimEnd('/')
        return URL("$base$ruta").openConnection() as HttpURLConnection
    }

    private fun leerEntrada(texto: String): CommunityBlacklistEntity? {
        val objeto = JSONObject(texto)
        val indicador = objeto.optString("indicador").trim()
        val tipo = objeto.optString("tipo").trim()
        if (indicador.isBlank() || tipo.isBlank()) {
            return null
        }
        val reportes = objeto.optInt("reportes", 0)
        return CommunityBlacklistEntity(
            indicador = indicador,
            tipo = tipo,
            reportes = reportes,
            esMalicioso = objeto.optBoolean("esMalicioso", reportes >= 5)
        )
    }
}
