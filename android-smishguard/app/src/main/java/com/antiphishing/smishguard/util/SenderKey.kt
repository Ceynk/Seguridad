package com.antiphishing.smishguard.util

object SenderKey {

    private const val MIN_SUFFIX_DIGITS = 7
    private val NUMERIC_CHARS = setOf('+', '-', '(', ')', ' ', '.')

    fun isNumeric(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        var hasDigits = false
        var allValid = true
        for (i in 0 until trimmed.length) {
            val ch = trimmed[i]
            if (ch.isDigit()) {
                hasDigits = true
            } else if (!NUMERIC_CHARS.contains(ch)) {
                allValid = false
            }
        }
        return hasDigits && allValid
    }

    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (isNumeric(trimmed)) {
            val digitsBuilder = StringBuilder()
            for (i in 0 until trimmed.length) {
                val ch = trimmed[i]
                if (ch.isDigit()) {
                    digitsBuilder.append(ch)
                }
            }
            val digits = digitsBuilder.toString()
            return if (digits.startsWith("00")) {
                digits.substring(2)
            } else {
                digits
            }
        } else {
            return trimmed.lowercase()
        }
    }

    fun matches(a: String, b: String): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) {
            return false
        }

        val aIsNum = isNumeric(a)
        val bIsNum = isNumeric(b)
        if (aIsNum != bIsNum) {
            return false
        }

        if (!aIsNum) {
            return na == nb
        }

        if (na == nb) {
            return true
        }

        val shorter = if (na.length <= nb.length) na else nb
        val longer = if (na.length <= nb.length) nb else na
        return shorter.length >= MIN_SUFFIX_DIGITS && longer.endsWith(shorter)
    }

    fun matchesAny(sender: String, candidates: Collection<String>): Boolean {
        for (candidate in candidates) {
            if (matches(sender, candidate)) {
                return true
            }
        }
        return false
    }
}
