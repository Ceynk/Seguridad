package com.antiphishing.smishguard.domain.detector

import java.text.Normalizer
import java.util.regex.Pattern

object HomographDetector {

    // Caracteres cirílicos, griegos y otros alfabetos que imitan al alfabeto latino
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

    // Caracteres invisibles, de ancho cero y modificadores de dirección de texto (Bidi)
    private val INVISIBLE_AND_BIDI_CHARS = setOf(
        '\u200B', // Zero width space
        '\u200C', // Zero width non-joiner
        '\u200D', // Zero width joiner
        '\u200E', // Left-to-Right mark
        '\u200F', // Right-to-Left mark
        '\u202A', // Left-to-Right embedding
        '\u202B', // Right-to-Left embedding
        '\u202C', // Pop directional formatting
        '\u202D', // Left-to-Right override
        '\u202E', // Right-to-Left override
        '\u2060', // Word joiner
        '\uFEFF', // Zero width no-break space (BOM)
        '\u00AD', // Soft hyphen
        '\u034F'  // Combining grapheme joiner
    )

    private val PUNYCODE_PATTERN = Pattern.compile("(?i)xn--[a-z0-9]+")

    // Detección de patrones de espaciado artificial para evadir filtros (ej: "b a n c o", "c u e n t a")
    private val ARTIFICIAL_SPACING_REGEX = Regex("""(?i)\b([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])(?:\s+([a-z0-9]))*\b""")

    fun findHomoglyphs(text: String): List<String> {
        val detected = mutableListOf<String>()

        val punyMatcher = PUNYCODE_PATTERN.matcher(text)
        while (punyMatcher.find()) {
            detected.add("Punycode detectado: ${punyMatcher.group()}")
        }

        val hasInvisible = text.any { INVISIBLE_AND_BIDI_CHARS.contains(it) }
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
                foundChars.append(ch).append(" imita a '").append(CONFUSABLE_MAPPING[ch]).append("', ")
            }
        }

        if (hasLatin && hasNonLatinConfusable) {
            detected.add("Ataque homógrafo detectado: Caracteres mixtos no latinos -> ${foundChars.trimEnd(' ', ',')}")
        }

        // Detección de Leetspeak agresivo en palabras clave
        if (containsLeetspeakKeywords(text)) {
            detected.add("Ofuscación alfanumérica (Leetspeak) detectada en palabras sensibles")
        }

        // Detección de espaciado artificial evasivo (evaluando todas las ocurrencias)
        for (matchResult in ARTIFICIAL_SPACING_REGEX.findAll(text)) {
            val match = matchResult.value
            val collapsed = match.replace("\\s+".toRegex(), "")
            if (isSensitiveKeyword(collapsed.lowercase())) {
                detected.add("Espaciado artificial detectado para eludir filtros ($match)")
                break
            }
        }

        return detected
    }

    /**
     * Normaliza el texto eliminando caracteres invisibles, sustituyendo confusables,
     * descomponiendo acentos y traduciendo leetspeak común para facilitar el análisis heurístico.
     */
    fun normalizeHomoglyphs(text: String): String {
        // 1. Eliminar caracteres invisibles y bidi
        val cleanSb = StringBuilder()
        for (ch in text) {
            if (!INVISIBLE_AND_BIDI_CHARS.contains(ch)) {
                cleanSb.append(CONFUSABLE_MAPPING[ch] ?: ch)
            }
        }
        var processed = cleanSb.toString()

        // 2. Colapsar espaciados artificiales sospechosos ("b a n c o" -> "banco")
        processed = ARTIFICIAL_SPACING_REGEX.replace(processed) { matchResult ->
            val candidate = matchResult.value.replace("\\s+".toRegex(), "")
            if (isSensitiveKeyword(candidate.lowercase())) candidate else matchResult.value
        }

        // 3. Normalizar Leetspeak si se detectan términos financieros ofuscados
        processed = normalizeLeetspeak(processed)

        return processed
    }

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

    private fun normalizeLeetspeak(input: String): String {
        var result = input
        for ((regex, replacement) in LEET_REPLACEMENTS) {
            result = result.replace(regex, replacement)
        }
        return result
    }

    private fun containsLeetspeakKeywords(input: String): Boolean {
        return LEET_DETECTION_PATTERNS.any { it.containsMatchIn(input) }
    }

    private fun isSensitiveKeyword(word: String): Boolean {
        return SENSITIVE_KEYWORDS.contains(word)
    }
}
