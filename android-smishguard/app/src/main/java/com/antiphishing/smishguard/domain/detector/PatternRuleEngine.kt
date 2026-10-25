package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor
import java.util.regex.Pattern

object PatternRuleEngine {

    data class ReglaPatron(
        val categoria: String,
        val patron: Pattern,
        val descripcion: String,
        val severidad: Int
    )

    private val REGLAS_DE_ATAQUE = listOf(
        ReglaPatron(
            categoria = "Fraude Bancario y Financiero",
            patron = Pattern.compile(
                """(?i)\b(?:cuenta\s+(?:de\s+ahorros\s+)?(?:ha\s+sido\s+|fue\s+)?(?:bloqueada|suspendida|desactivada|restringida)|tarjeta\s+(?:(?:de\s+cr[eé]dito\s+|d[eé]bito\s+)?(?:ha\s+sido\s+|fue\s+|presenta\s+)?(?:bloqueada|retenida|bloqueo\s+preventivo)|cancelaci[oó]n)|acceso\s+no\s+autorizado|intento\s+de\s+ingreso\s+inusual|movimiento(?:s)?\s+(?:inusual(?:es)?|sospechoso(?:s)?)|actuali(?:ce|zar)\s+sus\s+(?:datos|credenciales)\s+(?:urgente|de\s+inmediato|aqu[ií]|hoy|de\s+seguridad)|reactiv(?:ar|e)\s+su\s+(?:cuenta|clave|acceso|banca)|alerta\s+de\s+seguridad\s+bancaria|verificaci[oó]n\s+bancaria\s+urgente|clave\s+(?:din[aá]mica|virtual)\s+(?:ha\s+vencido|ser[aá]\s+desactivada)|(?:si\s+no\s+la\s+reconoce|si\s+no\s+fue\s+usted)\s+(?:an[uú]lela|cancele)|evite\s+el\s+cobro\s+del?\s+seguro)\b"""
            ),
            descripcion = "Intento de suplantación de entidad bancaria alertando sobre bloqueo de cuenta, cargos no reconocidos o vencimiento de clave.",
            severidad = 35
        ),
        ReglaPatron(
            categoria = "Estafa de Paquetería y Envíos",
            patron = Pattern.compile(
                """(?i)\b(?:(?:paquete|encomienda|env[ií]o)\s+(?:esta\s+|se\s+encuentra\s+)?(?:retenid[oa]|pendiente|bloquead[oa]|no\s+entregad[oa])|tasa\s+(?:de\s+aduana|aduanera)|impuesto\s+de\s+nacionalizaci[oó]n|pagar\s+(?:gastos|aranceles|env[ií]o|impuestos)|direcci[oó]n\s+(?:incompleta|err[oó]nea|incorrecta)|imposible\s+entregar\s+su\s+env[ií]o|intento\s+de\s+entrega\s+fallido|reprograme\s+su\s+(?:paquete|env[ií]o)|actualice\s+(?:su\s+direcci[oó]n|sus\s+datos\s+de\s+env[ií]o)|retenci[oó]n\s+en\s+aduanas?|retiro\s+de\s+paquete\s+en\s+aduanas)\b"""
            ),
            descripcion = "Suplantación de empresas de logística alegando paquetes retenidos o direcciones erróneas para exigir pagos o datos.",
            severidad = 30
        ),
        ReglaPatron(
            categoria = "Suplantación de Organismo Público",
            patron = Pattern.compile(
                """(?i)\b(?:dgt\b.*multa|notificaci[oó]n\s+dgt|aeat\b.*devoluci[oó]n|hacienda\b.*reembolso|seguridad\s+social\b.*tarjeta\s+sanitaria|sanci[oó]n\s+administrativa|[uú]ltimo\s+aviso\s+de\s+(?:pago|embargo)|prosperidad\s+social.*(?:renta\s+ciudadana|bono|giro)|renta\s+ciudadana.*(?:\$\s*\d+|giro))\b"""
            ),
            descripcion = "Notificación falsa de multas estatales, subsidios gubernamentales o reembolsos de impuestos.",
            severidad = 35
        ),
        ReglaPatron(
            categoria = "Extracción Activa de Credenciales o Clave",
            patron = Pattern.compile(
                """(?i)\b(?:introduzc?a?\s+(?:su\s+)?(?:pin|clave|contraseña)\s+en|ingres(?:e|a|ar)\s+(?:su(?:s)?\s+)?(?:credenciales|clave|usuario)\s+(?:en|para)|valid(?:e|a|ar)\s+(?:su(?:s)?\s+)?(?:clave|credenciales|identidad|datos)\s+(?:urgente|inmediatamente|antes\s+de|en)|confirm(?:e|a|ar)\s+tu\s+identidad\s+para\s+reactivar|autorizar\s+transferencia\s+en|c[oó]digo\s+de\s+confirmaci[oó]n.*(?:haga\s+clic|enlace|ingrese\s+a)|(?:d[ií]ctenos|env[ií]e|entregue)\s+(?:o\s+env[ií]e\s+)?(?:por\s+este\s+(?:chat|medio|sms)\s+)?los\s+16\s+d[ií]gitos\s+de\s+su\s+tarjeta)\b"""
            ),
            descripcion = "Solicitud engañosa instando a ingresar o entregar credenciales, numeración de tarjeta o claves personales.",
            severidad = 35
        ),
        ReglaPatron(
            categoria = "Extracción de Token Dinámico u OTP",
            patron = Pattern.compile(
                """(?i)\b(?<!\bno\s)(?<!\bnunca\s)(?:(?:reenv[ií][eé]|copi[eé]|entregu[eé]|comparta|env[ií][eé])\s+(?:de\s+inmediato\s+|inmediatamente\s+)?(?:el\s+|la\s+)?(?:c[oó]digo(?:\s+din[aá]mico|\s+otp|\s+de\s+\d+\s+d[ií]gitos)?|token|otp|clave\s+din[aá]mica)|respond[ae]\s+(?:a\s+)?este\s+(?:mensaje|sms)\s+con\s+(?:tu\s+|su\s+|el\s+|la\s+)?(?:clave|c[oó]digo|pin|token|otp))\b"""
            ),
            descripcion = "Técnica de ingeniería social para capturar códigos de verificación en dos pasos (2FA), claves o tokens.",
            severidad = 40
        ),
        ReglaPatron(
            categoria = "Desvío Telefónico o WhatsApp Fraudulento (Vishing)",
            patron = Pattern.compile(
                """(?i)\b(?:(?:comun[ií]quese|llame|escr[ií]b(?:anos|enos|a))\s+(?:urgente\s+|de\s+inmediato\s+|hoy\s+)?(?:a\s+nuestra\s+l[ií]nea[^0-9]{0,30}|por\s+whatsapp\s+al\s+|al\s+whatsapp\s+|al\s+)(?:\+?57\s*)?3\d{9}|llame\s+(?:ya|urgente|de\s+inmediato)\s+a\s+(?:bloqueos|seguridad|soporte|prevenci[oó]n)\s+al\s+\d{7,10})\b"""
            ),
            descripcion = "Intento de desviar a la víctima hacia una línea telefónica no verificada para suplantar soporte bancario.",
            severidad = 35
        ),
        ReglaPatron(
            categoria = "Tácticas de Urgencia y Coerción",
            patron = Pattern.compile(
                """(?i)\b(?:urgente|de\s+inmediato|inmediatamente|en\s+menos\s+de\s+24\s*h(?:oras)?|antes\s+de\s+24\s*h(?:oras)?|plazo\s+l[ií]mite|evite\s+(?:recargo|suspensi[oó]n|bloqueo|cierre|acciones\s+legales|embargo|sanci[oó]n|inmovilizaci[oó]n)|de\s+lo\s+contrario\s+ser[aá]\s+cancelad[oa])\b"""
            ),
            descripcion = "Lenguaje coercitivo orientado a provocar una respuesta impulsiva y apresurada.",
            severidad = 20
        ),
        ReglaPatron(
            categoria = "Falsas Recompensas o Trabajo Fraudulento",
            patron = Pattern.compile(
                """(?i)\b(?:has\s+sido\s+seleccionad[oa]|usted\s+ha\s+sido\s+seleccionad[oa]|has\s+ganado\s+un|ganaste\s+un\s+premio\s+de|premio\s+de\s+\$|sorteo\s+(?:exclusivo|aniversario|nacional)|reclama\s+tu\s+(?:recompensa|premio)|trabajo\s+a\s+tiempo\s+parcial|empleo\s+remoto\s+ganando|inversi[oó]n\s+garantizada|gana\s+dinero\s+f[aá]cil\s+desde\s+casa|gana\s+\$\s*\d+.*diarios)\b"""
            ),
            descripcion = "Promesas de premios inexistentes o estafas laborales con altas ganancias diarias.",
            severidad = 25
        ),
        ReglaPatron(
            categoria = "Falsos Préstamos y Créditos Rápidos (Gota a Gota)",
            patron = Pattern.compile(
                """(?i)\b(?:(?:cr[eé]dito|pr[eé]stamo)\s+(?:pre-?aprobado|inmediato(?:\s+aprobado)?|sin\s+fiador|sin\s+bur[oó])|desembolso\s+(?:inmediato\s+)?de\s+\$\s*\d+|(?:descargue|instala)\s+(?:el\s+apk|la\s+aplicaci[oó]n(?:\s+apk)?|nuestra\s+aplicaci[oó]n)\s+(?:inmediato|para))\b"""
            ),
            descripcion = "Oferta fraudulenta de créditos rápidos para inducir a la instalación de aplicaciones APK maliciosas.",
            severidad = 35
        ),
        ReglaPatron(
            categoria = "Suplantación de Billetera Digital o Banco (Nequi/Daviplata/Bancolombia)",
            patron = Pattern.compile(
                """(?i)\b(?:nequi|daviplata|bancolombia|davivienda|banco\s+de\s+bogot[aá]|banco\s+popular|scotiabank|colpatria|banco\s+agrario|transfiya|efecty)\b.*(?:bloquead[oa]|suspendid[oa]|inactiv[oa]|restricci[oó]n\s+temporal|desembolso|subsidio|bono\s+de|reactivar\s+cuenta|desbloqueo|(?:abono|transferencia|giro)\s+(?:retenid[oa]|pendiente))"""
            ),
            descripcion = "Suplantación de billeteras móviles o entidades financieras simulando bloqueos, giros retenidos o subsidios.",
            severidad = 40
        ),
        ReglaPatron(
            categoria = "Fraude Fiscal o Cobro Coactivo (DIAN/Tránsito)",
            patron = Pattern.compile(
                """(?i)\b(?:dian\b.*(?:embargo|cuenta\s+embargada|orden\s+de\s+pago|mandamiento\s+de\s+pago|notificaci[oó]n\s+(?:de\s+cobro\s+)?coactiva|evite\s+sanci[oó]n|sanci[oó]n\s+pendiente|devoluci[oó]n\s+de\s+saldo|reembolso\s+tributario)|sanci[oó]n\s+tributaria|fotomulta\b|simit\b.*(?:multa|comparendo|fotomulta)|secretar[ií]a\s+de\s+movilidad.*comparendo|colpass.*peaje.*rechazado)\b"""
            ),
            descripcion = "Intento de engaño suplantando a la autoridad tributaria (DIAN), tránsito (SIMIT) o recaudos viales.",
            severidad = 35
        ),
        ReglaPatron(
            categoria = "Falso Débito o Alerta PSE",
            patron = Pattern.compile(
                """(?i)\b(?:pse\b.*(?:no\s+reconocid[oa]|desconocid[oa]|anulaci[oó]n|anule|an[uú]lela|cancele|reversar|si\s+usted\s+no\s+autoriz[oó]|programado\s+en\s+curso)|alerta\s+de\s+compra\s+no\s+reconocida)\b"""
            ),
            descripcion = "Falsa notificación de transacción bancaria a través de PSE para forzar el acceso a un portal clonado.",
            severidad = 35
        ),
        ReglaPatron(
            categoria = "Suplantación de Operador Móvil / Bloqueo IMEI",
            patron = Pattern.compile(
                """(?i)\b(?:claro|tigo|movistar|wom|enel)\b.*(?:bloqueo\s+de\s+imei|homologaci[oó]n\s+de\s+imei|suspensi[oó]n\s+de\s+(?:l[ií]nea|servicio)|orden\s+de\s+corte|registre\s+su\s+(?:equipo|celular)|actualice\s+sus\s+datos|valide\s+su\s+tarjeta)\b"""
            ),
            descripcion = "Falsa alerta de bloqueo de línea celular, registro de IMEI o corte de servicios públicos.",
            severidad = 30
        ),
        ReglaPatron(
            categoria = "Suplantación Familiar",
            patron = Pattern.compile(
                """(?i)\b(?:se\s+me\s+(?:ha\s+roto|da[ñn][oó]|perdi[oó])\s+el\s+(?:m[oó]vil|celular|tel[eé]fono)|este\s+es\s+mi\s+(?:nuevo\s+n[uú]mero|n[uú]mero\s+nuevo)|cambi[eé]\s+de\s+(?:tel[eé]fono|n[uú]mero)|escr[ií]beme\s+(?:urgente\s+)?(?:a|por|al)\s+whatsapp|hola\s+(?:mam[aá]|pap[aá]|t[ií][oa]|hijo|hija)\b.*(?:nuevo\s+n[uú]mero|n[uú]mero\s+nuevo|emergencia|urgencias|pr[eé]stame|puedes\s+prestar|cons[ií]gname|transfi[eé]reme|transferencia))\b"""
            ),
            descripcion = "Suplantación de identidad donde el atacante finge ser un familiar pidiendo contacto o transferencias urgentes.",
            severidad = 35
        )
    )

    private val PATRON_DEFENSIVO_LEGITIMO = Pattern.compile(
        """(?i)\b(?:no\s+(?:lo\s+|la\s+)?comparta[s]?\s+(?:con\s+nadie|con\s+terceros)?|nunca\s+(?:le\s+)?pedir[aá]\s+este\s+c[oó]digo|nunca\s+l[oa]\s+(?:comparta[s]?|entregue[s]?)|por\s+(?:su|tu)\s+seguridad\s+no\s+l[oa]\s+(?:entregue[s]?|comparta[s]?)|v[aá]lid[oa]\s+por\s+\d+\s+minutos?|tiene\s+vigencia\s+de\s+\d+\s+minutos?|c[oó]digo\s+de\s+un\s+solo\s+uso)\b"""
    )

    private val PATRON_INFORMATIVO_OTP = Pattern.compile(
        """(?i)\b(?:es\s+tu\s+c[oó]digo\s+de\s+verificaci[oó]n|su\s+clave\s+din[aá]mica\s+(?:para\s+.*\s+)?es|su\s+c[oó]digo\s+(?:de\s+confirmaci[oó]n\s+)?es|tu\s+c[oó]digo\s+de\s+(?:seguridad|verificaci[oó]n)\s+(?:para\s+.*\s+)?es|el\s+c[oó]digo\s+de\s+confirmaci[oó]n\s+.*\s+es|su\s+clave\s+token\s+.*\s+es|c[oó]digo\s+de\s+(?:seguridad|whatsapp)[^:]*:\s*\d{3}[\-\s]?\d{3}|G-\d{6}|[0-9]{3}-[0-9]{3}\s+es\s+tu\s+c[oó]digo)\b"""
    )

    private val PATRON_TRANSACCION_INFORMATIVA = Pattern.compile(
        """(?i)\b(?:le\s+informa\s+compra\s+por|compra\s+(?:aprobada|autorizada|exitosa|nacional)\s+por|retiro\s+(?:en\s+cajero\s+autom[aá]tico\s+|completado\s+)?(?:por\s+\$[\d\.]+\s+)?(?:exitoso|en\s+punto)|transferencia\s+(?:exitosa|recibida)\s+por|transacci[oó]n\s+(?:#?[a-z0-9\-]+\s+)?(?:aprobada|exitosa)\s+por|recibiste\s+\$[\d\.]+\s+de\s+[a-z\s]+\s+en\s+tu|pago\s+(?:aprobado|exitoso)\s+por\s+\$|se\s+realiz[oó]\s+el\s+pago\s+(?:de\s+su\s+factura|autom[aá]tico)|hemos\s+recibido\s+el\s+pago\s+de\s+(?:su|tu)\s+factura)\b"""
    )

    private val PATRON_AVISO_PRESENCIAL_LEGITIMO = Pattern.compile(
        """(?i)\b(?:ac[eé]rquese\s+a\s+(?:cualquier\s+|nuestra\s+)?(?:oficina|sucursal)|con\s+su\s+(?:c[eé]dula|documento)\s+original|jam[aá]s\s+(?:solicita|pedir[aá])\s+(?:estos\s+datos|claves|c[oó]digos)|directamente\s+en\s+nuestra\s+app\s+oficial)\b"""
    )

    private val PATRON_SOLICITUD_CREDENCIAL_O_CELULAR = Pattern.compile(
        """(?i)\b(?:(?<!no\s)(?<!nunca\s)(?<!jam[aá]s\s)(?:reenv[ií][eé]|respond[ae]|entregu[eé]|env[ií][eé]|digite|ingrese)\b.*\b(?:clave|c[oó]digo|otp|token|pin|credenciales)|(?:\+?57\s*)?3\d{9}|whatsapp)\b"""
    )

    fun evaluatePatterns(text: String): List<ThreatFactor> {
        return evaluarPatrones(text)
    }

    fun evaluarPatrones(texto: String): List<ThreatFactor> {
        val factores = ArrayList<ThreatFactor>()
        for (regla in REGLAS_DE_ATAQUE) {
            val buscador = regla.patron.matcher(texto)
            if (buscador.find()) {
                factores.add(
                    ThreatFactor(
                        category = regla.categoria,
                        description = regla.descripcion,
                        severityScore = regla.severidad,
                        matchedSnippet = buscador.group()
                    )
                )
            }
        }
        return factores
    }

    fun isLegitimateOtpNotification(text: String, hasMaliciousUrls: Boolean): Boolean {
        return esNotificacionOtpLegitima(text, hasMaliciousUrls)
    }

    fun esNotificacionOtpLegitima(texto: String, tieneEnlacesMaliciosos: Boolean): Boolean {
        if (tieneEnlacesMaliciosos) {
            return false
        }
        val tieneInfoOtp = PATRON_INFORMATIVO_OTP.matcher(texto).find()
        val tieneAvisoDefensivo = tieneAdvertenciaDefensiva(texto)
        return tieneInfoOtp || tieneAvisoDefensivo
    }

    fun hasDefensiveSecurityWarning(text: String): Boolean {
        return tieneAdvertenciaDefensiva(text)
    }

    fun tieneAdvertenciaDefensiva(texto: String): Boolean {
        return PATRON_DEFENSIVO_LEGITIMO.matcher(texto).find()
    }

    fun esNotificacionTransaccionalInformativa(texto: String, tieneEnlacesNoOficiales: Boolean): Boolean {
        if (tieneEnlacesNoOficiales) {
            return false
        }
        return PATRON_TRANSACCION_INFORMATIVA.matcher(texto).find()
    }

    fun esAvisoSeguridadPresencialLegitimo(texto: String, tieneEnlaces: Boolean): Boolean {
        if (tieneEnlaces) {
            return false
        }
        if (PATRON_SOLICITUD_CREDENCIAL_O_CELULAR.matcher(texto).find()) {
            return false
        }
        return PATRON_AVISO_PRESENCIAL_LEGITIMO.matcher(texto).find()
    }
}
