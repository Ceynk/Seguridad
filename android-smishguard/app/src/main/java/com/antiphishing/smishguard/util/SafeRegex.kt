package com.antiphishing.smishguard.util

/**
 * Ejecución acotada en tiempo de expresiones regulares definidas por el usuario.
 *
 * Las reglas personalizadas se evalúan en rutas críticas (recepción de SMS y filtrado de
 * llamadas, donde Android exige respuesta en pocos segundos). Una regex con backtracking
 * catastrófico (ReDoS) podría congelar el receptor y provocar un ANR o que la llamada pase
 * sin filtrar. Esta utilidad envuelve la entrada en un CharSequence que aborta la búsqueda
 * cuando se supera el plazo.
 */
object SafeRegex {

    const val DEFAULT_TIMEOUT_MS = 120L
    const val MAX_PATTERN_LENGTH = 200
    const val MIN_KEYWORD_LENGTH = 3

    private class RegexTimeoutException : RuntimeException(null, null, false, false)

    private class DeadlineCharSequence(
        private val inner: CharSequence,
        private val deadlineNanos: Long
    ) : CharSequence {
        override val length: Int get() = inner.length

        override fun get(index: Int): Char {
            if (System.nanoTime() > deadlineNanos) throw RegexTimeoutException()
            return inner[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            DeadlineCharSequence(inner.subSequence(startIndex, endIndex), deadlineNanos)

        override fun toString(): String = inner.toString()
    }

    /**
     * @return true/false según coincida, o null si se agotó el tiempo o la regex falló.
     */
    fun containsMatch(regex: Regex, input: CharSequence, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean? {
        return try {
            regex.containsMatchIn(DeadlineCharSequence(input, System.nanoTime() + timeoutMs * 1_000_000L))
        } catch (e: RegexTimeoutException) {
            null
        } catch (e: StackOverflowError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compila una regla del usuario. Las palabras clave se escapan para tratarse literalmente.
     */
    fun compileUserRule(pattern: String, isRegex: Boolean): Regex? {
        return try {
            if (isRegex) Regex(pattern, RegexOption.IGNORE_CASE)
            else Regex(Regex.escape(pattern.trim()), RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Valida una regla antes de guardarla.
     * @return mensaje de error legible, o null si la regla es segura.
     */
    fun validateUserRule(pattern: String, isRegex: Boolean): String? {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return "La regla no puede estar vacía."
        if (trimmed.length > MAX_PATTERN_LENGTH) return "La regla supera $MAX_PATTERN_LENGTH caracteres."
        if (!isRegex && trimmed.length < MIN_KEYWORD_LENGTH) {
            return "La palabra clave debe tener al menos $MIN_KEYWORD_LENGTH caracteres."
        }
        val regex = compileUserRule(trimmed, isRegex) ?: return "Expresión regular inválida."

        // Una regex que coincide con la cadena vacía (ej: ".*", "a?") bloquearía TODOS los mensajes y llamadas.
        if (regex.containsMatchIn("")) return "La regla coincide con cualquier texto y bloquearía todo."

        // Prueba de estrés contra entradas típicas de ReDoS.
        val probes = listOf(
            "a".repeat(2500) + "!",
            "1".repeat(2500) + "x",
            "ab ".repeat(800) + "\u0000",
            "aaaa.".repeat(500) + "@"
        )
        for (probe in probes) {
            if (containsMatch(regex, probe, timeoutMs = 80) == null) {
                return "La expresión es potencialmente lenta (riesgo ReDoS). Simplifíquela."
            }
        }
        return null
    }
}
