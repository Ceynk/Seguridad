package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor
import java.util.regex.Pattern

object PatternRuleEngine {

    data class RuleDefinition(
        val category: String,
        val pattern: Pattern,
        val description: String,
        val severity: Int
    )

    // Patrones de ataque malicioso refinados (sin falsos positivos de frases de advertencia legítimas)
    private val ATTACK_RULES = listOf(
        RuleDefinition(
            category = "Fraude Bancario y Financiero",
            pattern = Pattern.compile(
                """(?i)\b(?:cuenta\s+(?:ha\s+sido\s+)?(?:bloqueada|suspendida|desactivada|restringida)|tarjeta\s+(?:(?:ha\s+sido\s+)?(?:bloqueada|retenida)|cancelaci[oó]n)|acceso\s+no\s+autorizado|actuali(?:ce|zar)\s+sus\s+datos\s+(?:urgente|de\s+inmediato|aqu[ií])|reactiv(?:ar|e)\s+su\s+cuenta|alerta\s+de\s+seguridad\s+bancaria|verificaci[oó]n\s+bancaria\s+urgente)\b"""
            ),
            description = "Intento de suplantación de entidad bancaria alertando sobre bloqueo de cuenta o tarjeta.",
            severity = 35
        ),
        RuleDefinition(
            category = "Estafa de Paquetería y Envíos",
            pattern = Pattern.compile(
                """(?i)\b(?:paquete\s+(?:retenido|pendiente|bloqueado|no\s+entregado)|tasa\s+de\s+aduana|pagar\s+(?:gastos|aranceles|env[ií]o)|direcci[oó]n\s+incompleta|imposible\s+entregar\s+su\s+env[ií]o|actualice\s+su\s+direcci[oó]n\s+en|retiro\s+de\s+paquete\s+en\s+aduanas)\b"""
            ),
            description = "Suplantación de empresas de logística alegando paquetes retenidos para exigir pagos o datos personales.",
            severity = 30
        ),
        RuleDefinition(
            category = "Suplantación de Organismo Público",
            pattern = Pattern.compile(
                """(?i)\b(?:dgt\b.*multa|notificaci[oó]n\s+dgt|aeat\b.*devoluci[oó]n|hacienda\b.*reembolso|seguridad\s+social\b.*tarjeta\s+sanitaria|sanci[oó]n\s+administrativa|último\s+aviso\s+de\s+pago)\b"""
            ),
            description = "Notificación falsa de multas de tráfico o reembolsos de impuestos estatales.",
            severity = 35
        ),
        RuleDefinition(
            category = "Extracción Activa de Credenciales o Clave",
            pattern = Pattern.compile(
                """(?i)\b(?:introduzc?a?\s+(?:su\s+)?(?:pin|clave|contraseña)\s+en|ingres(?:e|ar)\s+(?:sus\s+)?credenciales\s+en|autorizar\s+transferencia\s+en|c[oó]digo\s+de\s+confirmaci[oó]n.*(?:haga\s+clic|enlace|ingrese\s+a))\b"""
            ),
            description = "Solicitud engañosa instando a ingresar credenciales o claves personales en un enlace.",
            severity = 35
        ),
        RuleDefinition(
            category = "Extracción de Token Dinámico u OTP",
            pattern = Pattern.compile(
                """(?i)\b(?<!\bno\s)(?<!\bnunca\s)(?:reenv[ií][eé]|copi[eé]|entregu[eé]|comparta|env[ií][eé])\s+(?:de\s+inmediato\s+)?(?:el\s+)?(?:c[oó]digo(?:\s+din[aá]mico)?|token|otp|clave\s+din[aá]mica)\b"""
            ),
            description = "Técnica de ingeniería social para capturar códigos de verificación en dos pasos (2FA).",
            severity = 40
        ),
        RuleDefinition(
            category = "Tácticas de Urgencia y Coerción",
            pattern = Pattern.compile(
                """(?i)\b(?:urgente|de\s+inmediato|en\s+menos\s+de\s+24\s*h(?:oras)?|plazo\s+l[ií]mite|evite\s+(?:recargo|suspensi[oó]n|bloqueo|cierre|acciones\s+legales|embargo)|de\s+lo\s+contrario\s+ser[aá]\s+cancelad[oa])\b"""
            ),
            description = "Lenguaje coercitivo orientado a provocar una respuesta impulsiva y apresurada.",
            severity = 20
        ),
        RuleDefinition(
            category = "Falsas Recompensas o Trabajo Fraudulento",
            pattern = Pattern.compile(
                """(?i)\b(?:has\s+sido\s+seleccionad[oa]|has\s+ganado\s+un|premio\s+de\s+\$|sorteo\s+exclusivo|reclama\s+tu\s+recompensa|trabajo\s+a\s+tiempo\s+parcial.*gana\s+\$|inversi[oó]n\s+garantizada|gana\s+dinero\s+f[aá]cil\s+desde\s+casa|gana\s+\$\s*\d+.*diarios)\b"""
            ),
            description = "Promesas de premios inexistentes o estafas laborales con altas ganancias diarias.",
            severity = 25
        ),
        RuleDefinition(
            category = "Falsos Préstamos y Créditos Rápidos (Gota a Gota)",
            pattern = Pattern.compile(
                """(?i)\b(?:cr[eé]dito\s+(?:pre-?aprobado|inmediato|sin\s+fiador|sin\s+bur[oó])|desembolso\s+de\s+\$\s*\d+|descargue\s+(?:el\s+apk|la\s+aplicaci[oó]n)\s+para\s+su\s+pr[eé]stamo)\b"""
            ),
            description = "Oferta fraudulenta de créditos rápidos para inducir a la instalación de malware espía o extorsión.",
            severity = 35
        ),
        RuleDefinition(
            category = "Suplantación de Billetera Digital o Banco (Nequi/Daviplata/Bancolombia)",
            pattern = Pattern.compile(
                """(?i)\b(?:nequi|daviplata|bancolombia|davivienda|banco\s+de\s+bogot[aá]|banco\s+popular|scotiabank|colpatria|banco\s+agrario)\b.*(?:bloquead[oa]|suspendid[oa]|inactiv[oa]|desembolso|subsidio|reactivar\s+cuenta|desbloqueo)"""
            ),
            description = "Suplantación de billeteras móviles o entidades financieras simulando bloqueos o beneficios inexistentes.",
            severity = 40
        ),
        RuleDefinition(
            category = "Fraude Fiscal o Cobro Coactivo (DIAN/Tránsito)",
            pattern = Pattern.compile(
                """(?i)\b(?:dian\b.*(?:embargo|cuenta\s+embargada|orden\s+de\s+pago|notificaci[oó]n\s+coactiva|evite\s+sanci[oó]n)|sanci[oó]n\s+tributaria|fotomulta\b|simit\b.*multa)\b"""
            ),
            description = "Intento de extorsión o engaño suplantando a la autoridad tributaria o de tránsito.",
            severity = 35
        ),
        RuleDefinition(
            category = "Falso Débito o Alerta PSE",
            pattern = Pattern.compile(
                """(?i)\b(?:pse\b.*(?:d[eé]bito|transacci[oó]n|compra|pago\s+aprobado|no\s+reconocid[oa]|desconocid[oa]|anulaci[oó]n)|alerta\s+de\s+compra\s+no\s+reconocida)\b"""
            ),
            description = "Falsa notificación de transacción bancaria a través de PSE para forzar el acceso a un portal clonado.",
            severity = 35
        ),
        RuleDefinition(
            category = "Suplantación de Operador Móvil / Bloqueo IMEI",
            pattern = Pattern.compile(
                """(?i)\b(?:claro|tigo|movistar|wom)\b.*(?:bloqueo\s+de\s+imei|suspensi[oó]n\s+de\s+l[ií]nea|registre\s+su\s+equipo|actualice\s+sus\s+datos\s+para\s+no\s+perder\s+su\s+l[ií]nea)\b"""
            ),
            description = "Falsa alerta de bloqueo de línea celular o registro obligatorio de IMEI para robo de datos.",
            severity = 30
        ),
        RuleDefinition(
            category = "Suplantación Familiar",
            pattern = Pattern.compile(
                """(?i)\b(?:hola\s+mam[aá]|hola\s+pap[aá]|se\s+me\s+ha\s+roto\s+el\s+m[oó]vil|este\s+es\s+mi\s+nuevo\s+n[uú]mero|escr[ií]beme\s+a\s+whatsapp|tengo\s+una\s+emergencia)\b"""
            ),
            description = "Suplantación de identidad donde el atacante finge ser un familiar pidiendo contacto o transferencias urgentes.",
            severity = 35
        )
    )

    // Patrón de advertencias defensivas legítimas emitidas por bancos y servicios en OTPs
    private val LEGITIMATE_DEFENSIVE_PATTERN = Pattern.compile(
        """(?i)\b(?:no\s+(?:lo\s+)?comparta\s+(?:con\s+nadie)?|nunca\s+(?:le\s+)?pedir[aá]\s+este\s+c[oó]digo|por\s+su\s+seguridad\s+no\s+lo\s+entregue|v[aá]lid[oa]\s+por\s+\d+\s+minutos?|c[oó]digo\s+de\s+un\s+solo\s+uso)\b"""
    )

    // Patrón para identificar códigos OTP informativos legítimos
    private val OTP_INFORMATIVE_PATTERN = Pattern.compile(
        """(?i)\b(?:es\s+tu\s+c[oó]digo\s+de\s+verificaci[oó]n|su\s+clave\s+din[aá]mica\s+es|su\s+c[oó]digo\s+es|c[oó]digo\s+de\s+seguridad:\s*\d{4,8}|G-\d{6}|[0-9]{3}-[0-9]{3}\s+es\s+tu\s+c[oó]digo)\b"""
    )

    fun evaluatePatterns(text: String): List<ThreatFactor> {
        val matchedFactors = mutableListOf<ThreatFactor>()

        for (rule in ATTACK_RULES) {
            val matcher = rule.pattern.matcher(text)
            if (matcher.find()) {
                val snippet = matcher.group()
                matchedFactors.add(
                    ThreatFactor(
                        category = rule.category,
                        description = rule.description,
                        severityScore = rule.severity,
                        matchedSnippet = snippet
                    )
                )
            }
        }

        return matchedFactors
    }

    /**
     * Determina si el mensaje corresponde a una notificación transaccional legítima (ej. OTP de banco)
     * que contiene advertencias defensivas y un código, sin órdenes de redirección a páginas externas.
     */
    fun isLegitimateOtpNotification(text: String, hasMaliciousUrls: Boolean): Boolean {
        if (hasMaliciousUrls) return false
        val hasOtpInfo = OTP_INFORMATIVE_PATTERN.matcher(text).find()
        val hasDefensiveAdvice = LEGITIMATE_DEFENSIVE_PATTERN.matcher(text).find()

        // Si tiene código OTP o advertencia defensiva oficial y no contiene enlaces maliciosos
        return hasOtpInfo || hasDefensiveAdvice
    }

    /**
     * Verifica si el texto contiene advertencias defensivas emitidas por una entidad oficial.
     */
    fun hasDefensiveSecurityWarning(text: String): Boolean {
        return LEGITIMATE_DEFENSIVE_PATTERN.matcher(text).find()
    }
}
