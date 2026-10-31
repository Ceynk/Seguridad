package com.antiphishing.smishguard

import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.domain.model.ThreatLevel
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DatasetEvaluacionTest {

    data class Caso(val id: String, val particion: String, val etiqueta: String, val remitente: String, val mensaje: String)

    private val clases = listOf("LEGITIMO", "SOSPECHOSO", "MALICIOSO")

    private fun cargarCasos(): List<Caso> {
        val flujo = javaClass.classLoader!!.getResourceAsStream("dataset_smishing_sintetico.csv")
        val lineas = flujo.bufferedReader(Charsets.UTF_8).readLines()
        val casos = ArrayList<Caso>()
        for (linea in lineas) {
            if (linea.isBlank() || linea.startsWith("#") || linea.startsWith("id|")) continue
            val partes = linea.split("|", limit = 5)
            casos.add(Caso(partes[0], partes[1], partes[2], partes[3], partes[4]))
        }
        return casos
    }

    private fun etiquetaPredicha(nivel: ThreatLevel): String {
        return when (nivel) {
            ThreatLevel.SAFE -> "LEGITIMO"
            ThreatLevel.SUSPICIOUS -> "SOSPECHOSO"
            ThreatLevel.PHISHING -> "MALICIOSO"
        }
    }

    private fun formato(valor: Double): String = String.format(java.util.Locale.US, "%.2f", valor)

    private fun metricas(pares: List<Pair<String, String>>, positivo: (String) -> Boolean): String {
        var vp = 0
        var fp = 0
        var fn = 0
        for ((real, pred) in pares) {
            if (positivo(pred) && positivo(real)) vp++
            if (positivo(pred) && !positivo(real)) fp++
            if (!positivo(pred) && positivo(real)) fn++
        }
        val precision = if (vp + fp == 0) 0.0 else vp.toDouble() / (vp + fp)
        val recall = if (vp + fn == 0) 0.0 else vp.toDouble() / (vp + fn)
        val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
        return "P=${formato(precision)} R=${formato(recall)} F1=${formato(f1)}"
    }

    private fun informe(nombre: String, pares: List<Pair<String, String>>): String {
        val sb = StringBuilder()
        sb.appendLine("== $nombre (n=${pares.size}) ==")
        sb.appendLine("real\\pred   LEG  SOS  MAL")
        for (real in clases) {
            val fila = clases.map { pred -> pares.count { it.first == real && it.second == pred } }
            sb.appendLine(real.padEnd(11) + fila.joinToString("") { it.toString().padStart(5) })
        }
        for (clase in clases) {
            sb.appendLine("${clase.padEnd(11)} ${metricas(pares) { it == clase }}")
        }
        sb.appendLine("NO_LEGIT    ${metricas(pares) { it != "LEGITIMO" }}")
        val exactos = pares.count { it.first == it.second }
        sb.appendLine("Exactitud 3 clases: ${formato(exactos.toDouble() / pares.size)}")
        return sb.toString()
    }

    @Test
    fun evaluarDataset() {
        val casos = cargarCasos()
        assertTrue(casos.size >= 60)
        val resultados = casos.map { caso ->
            val analisis = SmishDetectorEngine.analyze(sender = caso.remitente, body = caso.mensaje)
            Triple(caso, etiquetaPredicha(analisis.level), analisis)
        }
        val sb = StringBuilder()
        sb.appendLine("Datos sinteticos: las metricas no representan tasas reales.")
        for (particion in listOf("CAL", "VAL")) {
            val pares = resultados.filter { it.first.particion == particion }.map { it.first.etiqueta to it.second }
            sb.append(informe(if (particion == "CAL") "CALIBRACION" else "VALIDACION", pares))
        }
        sb.appendLine("== Errores ==")
        for ((caso, pred, analisis) in resultados) {
            if (caso.etiqueta != pred) {
                val tipo = when {
                    caso.etiqueta == "LEGITIMO" -> "FP"
                    pred == "LEGITIMO" -> "FN"
                    else -> "NIVEL"
                }
                sb.appendLine("$tipo ${caso.id} ${caso.particion} real=${caso.etiqueta} pred=$pred score=${analisis.threatScore} " +
                    analisis.factors.joinToString("; ") { "${it.category}(${it.severityScore})" })
            }
        }
        val destino = File(System.getProperty("metricas.salida") ?: "build/reports/smishing/metricas.txt")
        destino.parentFile?.mkdirs()
        destino.writeText(sb.toString())
    }
}
