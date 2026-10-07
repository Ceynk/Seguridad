package com.antiphishing.smishguard.util

object SafeRegex {

    const val DEFAULT_TIMEOUT_MS = 120L
    const val MAX_PATTERN_LENGTH = 200
    const val MIN_KEYWORD_LENGTH = 3

    private class RegexTimeoutException : RuntimeException(null, null, false, false)

    private class DeadlineCharSequence(
        private val inner: CharSequence,
        private val deadlineNanos: Long
    ) : CharSequence {
        override val length: Int
            get() = inner.length

        override fun get(index: Int): Char {
            if (System.nanoTime() > deadlineNanos) {
                throw RegexTimeoutException()
            }
            return inner[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            val sub = inner.subSequence(startIndex, endIndex)
            return DeadlineCharSequence(sub, deadlineNanos)
        }

        override fun toString(): String {
            return inner.toString()
        }
    }

    fun containsMatch(regex: Regex, input: CharSequence, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean? {
        return try {
            val timeoutNanos = timeoutMs * 1_000_000L
            val deadline = System.nanoTime() + timeoutNanos
            val timedSequence = DeadlineCharSequence(input, deadline)
            regex.containsMatchIn(timedSequence)
        } catch (e: RegexTimeoutException) {
            null
        } catch (e: StackOverflowError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun compileUserRule(pattern: String, isRegex: Boolean): Regex? {
        return try {
            if (isRegex) {
                Regex(pattern, RegexOption.IGNORE_CASE)
            } else {
                val escaped = Regex.escape(pattern.trim())
                Regex(escaped, RegexOption.IGNORE_CASE)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun validateUserRule(pattern: String, isRegex: Boolean): String? {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) {
            return "La regla no puede estar vacía."
        }
        if (trimmed.length > MAX_PATTERN_LENGTH) {
            return "La regla supera $MAX_PATTERN_LENGTH caracteres."
        }
        if (!isRegex && trimmed.length < MIN_KEYWORD_LENGTH) {
            return "La palabra clave debe tener al menos $MIN_KEYWORD_LENGTH caracteres."
        }
        val regex = compileUserRule(trimmed, isRegex)
        if (regex == null) {
            return "Expresión regular inválida."
        }

        if (regex.containsMatchIn("")) {
            return "La regla coincide con cualquier texto y bloquearía todo."
        }

        val probes = listOf(
            "a".repeat(2500) + "!",
            "1".repeat(2500) + "x",
            "ab ".repeat(800) + "\u0000",
            "aaaa.".repeat(500) + "@"
        )
        for (probe in probes) {
            val probeMatch = containsMatch(regex, probe, 80)
            if (probeMatch == null) {
                return "La expresión es potencialmente lenta (riesgo ReDoS). Simplifíquela."
            }
        }
        return null
    }
}
