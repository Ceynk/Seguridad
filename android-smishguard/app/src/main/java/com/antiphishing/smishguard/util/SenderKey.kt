package com.antiphishing.smishguard.util

/**
 * Normalización y comparación de remitentes/números.
 *
 * Un mismo número llega en formatos distintos ("+57 300 123 4567", "3001234567", "0057300...").
 * La lista blanca original comparaba cadenas exactas, por lo que un contacto de confianza
 * guardado en un formato no coincidía cuando la red lo entregaba en otro.
 */
object SenderKey {

    private const val MIN_SUFFIX_DIGITS = 7
    private val NUMERIC_CHARS = setOf('+', '-', '(', ')', ' ', '.')

    fun isNumeric(raw: String): Boolean {
        val t = raw.trim()
        return t.isNotEmpty() && t.any { it.isDigit() } && t.all { it.isDigit() || it in NUMERIC_CHARS }
    }

    /** Clave canónica: solo dígitos para números, minúsculas para remitentes alfanuméricos. */
    fun normalize(raw: String): String {
        val t = raw.trim()
        return if (isNumeric(t)) t.filter { it.isDigit() }.removePrefix("00") else t.lowercase()
    }

    /**
     * Dos números coinciden si uno es sufijo del otro con al menos 7 dígitos significativos
     * (cubre la presencia/ausencia de prefijo de país). Los alfanuméricos coinciden sin distinguir mayúsculas.
     */
    fun matches(a: String, b: String): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        val aNum = isNumeric(a)
        val bNum = isNumeric(b)
        if (aNum != bNum) return false
        if (!aNum) return na == nb
        if (na == nb) return true
        val shorter = if (na.length <= nb.length) na else nb
        val longer = if (na.length <= nb.length) nb else na
        return shorter.length >= MIN_SUFFIX_DIGITS && longer.endsWith(shorter)
    }

    fun matchesAny(sender: String, candidates: Collection<String>): Boolean =
        candidates.any { matches(sender, it) }
}
