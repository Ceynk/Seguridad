package com.antiphishing.smishguard.domain.detector

import java.util.regex.Pattern

object HomographDetector {

    private val CONFUSABLE_MAPPING = mapOf(
        'а' to 'a', 'А' to 'A',
        'с' to 'c', 'С' to 'C',
        'е' to 'e', 'Е' to 'E',
        'о' to 'o', 'О' to 'O',
        'р' to 'p', 'Р' to 'P',
        'ѕ' to 's', 'Ѕ' to 'S',
        'х' to 'x', 'Х' to 'X',
        'у' to 'y', 'У' to 'Y',
        'і' to 'i', 'І' to 'I',
        'ј' to 'j', 'Ј' to 'J',
        'ԁ' to 'd', 'ԛ' to 'q',
        'α' to 'a', 'Α' to 'A',
        'ο' to 'o', 'Ο' to 'O',
        'ν' to 'v', 'Ν' to 'N',
        'ρ' to 'p', 'Ρ' to 'P',
        'τ' to 't', 'Τ' to 'T',
        'ո' to 'n',
        'ѵ' to 'v',
        'ѡ' to 'w',
        'ӏ' to 'l', 'Ӏ' to 'I',
        'В' to 'B', 'Н' to 'H', 'К' to 'K', 'М' to 'M', 'Т' to 'T'
    )

    private val INVISIBLE_AND_BIDI_CHARS = setOf(
        '\u200B',
        '\u200C',
        '\u200D',
        '\u200E',
        '\u200F',
        '\u202A',
        '\u202B',
        '\u202C',
        '\u202D',
        '\u202E',
        '\u2060',
        '\uFEFF',
        '\u00AD',
        '\u034F'
    )

    private val PUNYCODE_PATTERN = Pattern.compile("(?i)xn--[a-z0-9]+")

    private val ARTIFICIAL_SPACING_REGEX = Regex("""(?i)\b([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])(?:\s+([a-z0-9]))*\b""")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    private val LEET_REPLACEMENTS = listOf(
        Regex("""(?i)\bb4nc0\b""") to "banco",
        Regex("""(?i)\bb4nc0l0mb14\b""") to "bancolombia",
        Regex("""(?i)\bn3qu1\b""") to "nequi",
        Regex("""(?i)\bd4v1pl4t4\b""") to "daviplata",
        Regex("""(?i)\bd14n\b""") to "dian",
        Regex("""(?i)\bbl0qu34d[ao]\b""") to "bloqueada",
        Regex("""(?i)\bcl4v3\b""") to "clave",
        Regex("""(?i)\bcr3d1t0\b""") to "credito",
        Regex("""(?i)\bp4qu3t3\b""") to "paquete",
        Regex("""(?i)\bs3gur1d4d\b""") to "seguridad",
        Regex("""(?i)\bv3r1f1c4r\b""") to "verificar",
        Regex("""(?i)\b4ctu4l1z4r\b""") to "actualizar"
    )

    private val LEET_DETECTION_PATTERNS = listOf(
        Regex("""(?i)\bb4nc0"""),
        Regex("""(?i)\bn3qu1"""),
        Regex("""(?i)\bd4v1pl4t4"""),
        Regex("""(?i)\bd14n\b"""),
        Regex("""(?i)\bbl0qu"""),
        Regex("""(?i)\bcl4v3""")
    )

    private val SENSITIVE_KEYWORDS = setOf(
        "banco", "nequi", "daviplata", "bancolombia", "clave", "bloqueo", "bloqueada",
        "tarjeta", "dian", "aduana", "multa", "urgente", "pse", "dinero", "premio"
    )

    fun findHomoglyphs(text: String): List<String> {
        val detected = mutableListOf<String>()

        val punyMatcher = PUNYCODE_PATTERN.matcher(text)
        while (punyMatcher.find()) {
            detected.add("Punycode detectado: ${punyMatcher.group()}")
        }

        val hasInvisible = text.any(INVISIBLE_AND_BIDI_CHARS::contains)
        if (hasInvisible) {
            detected.add("Caracteres invisibles o modificadores de dirección detectados (intento de evasión)")
        }

        var hasLatin = false
        var hasNonLatinConfusable = false
        val foundChars = StringBuilder()

        for (ch in text) {
            if ((ch in 'a'..'z') || (ch in 'A'..'Z')) {
                hasLatin = true
            } else if (CONFUSABLE_MAPPING.containsKey(ch)) {
                hasNonLatinConfusable = true
                val replacement = CONFUSABLE_MAPPING.getValue(ch)
                foundChars.append(ch).append(" imita a '").append(replacement).append("', ")
            }
        }

        if (hasLatin && hasNonLatinConfusable) {
            val cleanedChars = foundChars.trimEnd(' ', ',').toString()
            detected.add("Ataque homógrafo detectado: Caracteres mixtos no latinos -> $cleanedChars")
        }

        if (containsLeetspeakKeywords(text)) {
            detected.add("Ofuscación alfanumérica (Leetspeak) detectada en palabras sensibles")
        }

        for (matchResult in ARTIFICIAL_SPACING_REGEX.findAll(text)) {
            val match = matchResult.value
            val collapsed = match.replace(WHITESPACE_REGEX, "")
            if (isSensitiveKeyword(collapsed.lowercase())) {
                detected.add("Espaciado artificial detectado para eludir filtros ($match)")
                break
            }
        }

        return detected
    }

    fun normalizeHomoglyphs(text: String): String {
        val cleanText = buildString {
            for (ch in text) {
                if (ch !in INVISIBLE_AND_BIDI_CHARS) {
                    append(CONFUSABLE_MAPPING[ch] ?: ch)
                }
            }
        }
        var processed = cleanText

        processed = ARTIFICIAL_SPACING_REGEX.replace(processed) { matchResult ->
            val candidate = matchResult.value.replace(WHITESPACE_REGEX, "")
            if (isSensitiveKeyword(candidate.lowercase())) {
                candidate
            } else {
                matchResult.value
            }
        }

        for (pair in LEET_REPLACEMENTS) {
            processed = processed.replace(pair.first, pair.second)
        }

        return processed
    }

    private fun containsLeetspeakKeywords(input: String): Boolean {
        for (pattern in LEET_DETECTION_PATTERNS) {
            if (pattern.containsMatchIn(input)) {
                return true
            }
        }
        return false
    }

    private fun isSensitiveKeyword(word: String): Boolean {
        return SENSITIVE_KEYWORDS.contains(word)
    }
}
