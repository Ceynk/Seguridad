package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel

object AnalizadorContextual {

    enum class Senal(val descripcion: String, val patron: Regex?) {
        OPERACION_FINANCIERA(
            "menciona un cobro, pago, compra o movimiento de dinero",
            Regex("""\b(cobro|cargo|debito|debitado|debitamos|retiro|movimientos?|transaccion|compra|pago|transferencia|reintegro|reembolso)\b|\$\s?\d|\bpor \d{1,3}(\.\d{3})+\b""")
        ),
        OPERACION_DESCONOCIDA(
            "pregunta si desconoces o no reconoces una operacion",
            Regex("""\b(desconoces|desconoce|no (la |lo )?reconoces|no (la |lo )?reconoce|no (la |lo )?hiciste|no fuiste tu|no fue usted|no (lo |la )?autorizaste|no (lo |la )?autorizo|no (la |lo )?solicitaste|no (la |lo )?solicito)\b""")
        ),
        SUSCRIPCION_NO_RECONOCIDA(
            "informa una inscripcion, suscripcion, seguro o cargo automatico",
            Regex("""\b(inscripcion|suscripcion|afiliacion|membresia|seguro|renovacion automatica|pago automatico|cargo automatico|debito automatico|activamos|se programo)\b|\b(inscripcion|suscripcion|afiliacion|membresia|seguro|cobro|cargo|servicio)\b.{0,45}\b(quedo|fue|ha sido) (efectuad|activad|registrad|confirmad|inscrit|programad)[oa]\b""")
        ),
        ACCION_ANULAR(
            "pide cancelar, anular o revertir la operacion",
            Regex("""\b(anul\w*|cancela|cancele|cancelar|cancelalo|cancelala|cancelarlo|cancelarla|revierte|revertir|reversar|desistir|detenlo|detenla|detener|gestiona|gestionalo|rechazalo|rechazala)\b""")
        ),
        AMENAZA_BLOQUEO(
            "amenaza con bloqueo, suspension, embargo, reporte o retencion",
            Regex("""\b(bloquead[oa]|bloqueo|suspendid[oa]|suspension|suspender|desactivad[oa]|desactivar|inhabilitad[oa]|embargo|embargad[oa]|reporte|centrales de riesgo|retenid[oa]|no pudo ser entregad[oa]|entrega fallida|direccion incompleta|saldo pendiente|mensaje de voz pendiente)\b""")
        ),
        SOLICITUD_VERIFICACION(
            "pide verificar, validar, reprogramar o actualizar datos o claves",
            Regex("""\b(verifica|verifique|verificar|valida|valide|validar|confirma|confirme|confirmar|actualiza|actualice|actualizar|actualizacion)\b.{0,40}\b(datos|identidad|clave|cuenta|tarjeta|medio de pago|informacion|usuario|cedula|direccion|envio)\b|\b(ingresa|ingrese|ingresando|digita)\b.{0,30}\b(clave|cedula|usuario|contrasena)\b|\bcon tu clave\b|\b(reprograme|reprogramar|responde si)\b""")
        ),
        PREMIO_FALSO(
            "ofrece un premio, bono, descuento o beneficio enganoso",
            Regex("""\b(ganaste|has ganado|ganador|premios?|bono|reintegro|reembolso|saldo a favor|seleccionad[oa]|felicidades|sorteo|concurso|participa|rifa|favorecid[oa]|puntos|descuento|barato|soat|gana dinero|invierte|credito aprobado|sin codeudor)\b|\bsalario de \$\s*\d|\b\d{1,2}\s*%\b""")
        ),
        URGENCIA(
            "usa presion de tiempo",
            Regex("""\b(hoy|urgente|inmediat\w*|24 horas|ultimo dia|ultimo aviso|hasta hoy|cupos limitados|compralo ya|hazlo ya|ya mismo|vencen hoy)\b""")
        ),
        SOLICITUD_CODIGO(
            "pide reenviar un codigo recibido por SMS",
            Regex("""\bcodigo\b.{0,60}\b(reenvi\w*|me lo (envias|pasas|mandas|das)|enviamelo|pasamelo|dictame)\b|\b(reenviame|enviame|pasame|dictame)\b.{0,30}\bcodigo\b""")
        ),
        CONTACTO_CELULAR(
            "pide comunicarse con un numero celular particular",
            Regex("""(?<!\d)(\+?57)?3\d{9}(?!\d)|\bwa\.me\b|\bt\.me\b""")
        ),
        ENLACE_EXTERNO("invita a abrir un enlace externo no verificado", null),
        SUPLANTACION_ENTIDAD("menciona una entidad pero el enlace no pertenece a ella", null),
        REMITENTE_IMITA_MARCA("el remitente imita el nombre de una entidad", null),
        DOMINIO_RIESGO_TECNICO("el dominio es aleatorio, IP, punycode o subdominio enganoso", null),
        ACORTADOR_DESDE_CELULAR("enlace acortado enviado desde una linea celular", null)
    }

    data class Entrada(
        val textoNormalizado: String,
        val tieneEnlaceExterno: Boolean,
        val entidadSuplantada: String?,
        val remitenteImitaMarca: String?,
        val tieneRiesgoTecnicoDominio: Boolean,
        val acortadorDesdeCelular: Boolean
    )

    data class Resultado(
        val senales: Set<Senal>,
        val factores: List<ThreatFactor>,
        val nivelMinimo: ThreatLevel,
        val combinacionCritica: Boolean
    )

    private const val CATEGORIA_COMBINACION = "Combinación de Riesgo"
    private const val CATEGORIA_SENALES = "Señales de Contexto"

    fun analizar(entrada: Entrada): Resultado {
        val senales = LinkedHashSet<Senal>()
        for (senal in Senal.values()) {
            val patron = senal.patron ?: continue
            if (patron.containsMatchIn(entrada.textoNormalizado)) {
                senales.add(senal)
            }
        }
        if (entrada.tieneEnlaceExterno) senales.add(Senal.ENLACE_EXTERNO)
        if (entrada.entidadSuplantada != null) senales.add(Senal.SUPLANTACION_ENTIDAD)
        if (entrada.remitenteImitaMarca != null) senales.add(Senal.REMITENTE_IMITA_MARCA)
        if (entrada.tieneRiesgoTecnicoDominio) senales.add(Senal.DOMINIO_RIESGO_TECNICO)
        if (entrada.acortadorDesdeCelular) senales.add(Senal.ACORTADOR_DESDE_CELULAR)

        val factores = ArrayList<ThreatFactor>()
        val motivosCombinados = ArrayList<String>()
        var puntajeMaximoCombinacion = 0
        var nivelMinimo = ThreatLevel.SAFE

        fun regla(descripcion: String, puntos: Int, nivel: ThreatLevel) {
            motivosCombinados.add(descripcion)
            if (puntos > puntajeMaximoCombinacion) puntajeMaximoCombinacion = puntos
            if (nivel.ordinal > nivelMinimo.ordinal) nivelMinimo = nivel
        }

        val enlace = Senal.ENLACE_EXTERNO in senales
        val operacionNoReconocida = Senal.OPERACION_DESCONOCIDA in senales || Senal.SUSCRIPCION_NO_RECONOCIDA in senales
        val engano = senales.any {
            it == Senal.OPERACION_FINANCIERA || it == Senal.OPERACION_DESCONOCIDA ||
                it == Senal.SUSCRIPCION_NO_RECONOCIDA || it == Senal.ACCION_ANULAR ||
                it == Senal.AMENAZA_BLOQUEO || it == Senal.SOLICITUD_VERIFICACION ||
                it == Senal.PREMIO_FALSO || it == Senal.URGENCIA
        }
        val suplantacion = Senal.SUPLANTACION_ENTIDAD in senales || Senal.REMITENTE_IMITA_MARCA in senales

        if (operacionNoReconocida && Senal.ACCION_ANULAR in senales && enlace) {
            regla("Operación o suscripción desconocida con llamado a anular y enlace externo.", 45, ThreatLevel.PHISHING)
        }
        if (Senal.SOLICITUD_CODIGO in senales) {
            regla("Pide reenviar un código recibido por SMS (posible robo de cuenta).", 45, ThreatLevel.PHISHING)
        }
        if (suplantacion && enlace) {
            val nivel = if (engano) ThreatLevel.PHISHING else ThreatLevel.SUSPICIOUS
            val puntos = if (engano) 40 else 30
            regla("La entidad mencionada o el remitente no coinciden con el dominio del enlace.", puntos, nivel)
        } else if (Senal.REMITENTE_IMITA_MARCA in senales && engano) {
            regla("El remitente imita a una entidad y el texto trata temas de dinero o seguridad.", 30, ThreatLevel.SUSPICIOUS)
        }
        if (Senal.AMENAZA_BLOQUEO in senales &&
            (Senal.SOLICITUD_VERIFICACION in senales || enlace || Senal.CONTACTO_CELULAR in senales)
        ) {
            regla("Amenaza de bloqueo, retención o reporte junto con enlace o acción solicitada.", 35, ThreatLevel.SUSPICIOUS)
        }
        if (Senal.SOLICITUD_VERIFICACION in senales && enlace) {
            regla("Solicitud de verificar, actualizar o reprogramar en un enlace externo.", 30, ThreatLevel.SUSPICIOUS)
        }
        if (Senal.PREMIO_FALSO in senales && (enlace || Senal.CONTACTO_CELULAR in senales)) {
            regla("Premio, oferta o beneficio que dirige a un enlace o número no verificado.", 30, ThreatLevel.SUSPICIOUS)
        }
        if (Senal.OPERACION_FINANCIERA in senales &&
            (Senal.URGENCIA in senales || Senal.OPERACION_DESCONOCIDA in senales) && enlace
        ) {
            regla("Preocupación financiera con urgencia explícita o implícita y enlace externo.", 30, ThreatLevel.SUSPICIOUS)
        }
        if (Senal.URGENCIA in senales && enlace) {
            regla("Presión de tiempo para abrir un enlace no verificado.", 15, ThreatLevel.SAFE)
        }
        if (Senal.DOMINIO_RIESGO_TECNICO in senales && engano) {
            val nivel = if (suplantacion) ThreatLevel.PHISHING else ThreatLevel.SUSPICIOUS
            val puntos = if (suplantacion) 40 else 30
            regla("Dominio de riesgo técnico combinado con ingeniería social.", puntos, nivel)
        }
        if (Senal.OPERACION_DESCONOCIDA in senales && Senal.OPERACION_FINANCIERA in senales &&
            Senal.CONTACTO_CELULAR in senales
        ) {
            regla("Operación no reconocida que pide contactar un celular particular.", 35, ThreatLevel.SUSPICIOUS)
        }
        if (Senal.ACORTADOR_DESDE_CELULAR in senales) {
            regla("Enlace acortado enviado desde una línea celular particular.", 15, ThreatLevel.SUSPICIOUS)
        }

        if (motivosCombinados.isNotEmpty()) {
            factores.add(
                ThreatFactor(
                    category = CATEGORIA_COMBINACION,
                    description = motivosCombinados.joinToString(" "),
                    severityScore = puntajeMaximoCombinacion,
                    matchedSnippet = "nivel mínimo: ${nivelMinimo.label}"
                )
            )
        }

        val senalesTexto = senales.filter { it.patron != null }
        if (senalesTexto.isNotEmpty() && motivosCombinados.isEmpty()) {
            factores.add(
                ThreatFactor(
                    category = CATEGORIA_SENALES,
                    description = "El mensaje " + senalesTexto.joinToString("; ") { it.descripcion } + ".",
                    severityScore = minOf(10, senalesTexto.size * 5),
                    matchedSnippet = senalesTexto.joinToString(", ") { it.name.lowercase() }
                )
            )
        }

        return Resultado(
            senales = senales,
            factores = factores,
            nivelMinimo = nivelMinimo,
            combinacionCritica = nivelMinimo == ThreatLevel.PHISHING
        )
    }
}
