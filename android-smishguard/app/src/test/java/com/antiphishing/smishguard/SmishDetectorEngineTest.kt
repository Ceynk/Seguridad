package com.antiphishing.smishguard

import com.antiphishing.smishguard.domain.detector.HomographDetector
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.domain.model.ThreatLevel
import org.junit.Assert.*
import org.junit.Test

class SmishDetectorEngineTest {

    @Test
    fun palabrasGenericasPorSiSolasNoPonenEnCuarentena() {
        val result = SmishDetectorEngine.analyze(
            sender = "Contacto",
            body = "Mensaje urgente: recuerda verificar tu cuenta y revisar el pago."
        )

        assertTrue(result.threatScore in 0..49)
        assertFalse(result.isQuarantined)
    }

    @Test
    fun mensajeBancarioConUrlMaliciosaYSolicitudDeCredencialesSeAisla() {
        val result = SmishDetectorEngine.analyze(
            sender = "BBVA",
            body = "Su cuenta fue bloqueada. Ingrese sus credenciales en https://bbva-alerta.duckdns.org/login para reactivarla."
        )

        assertTrue(result.isQuarantined)
        assertTrue(result.threatScore >= 60)
    }

    @Test
    fun mensajeDePaqueteriaLegitimoNoSeAisla() {
        val result = SmishDetectorEngine.analyze(
            sender = "Correos",
            body = "Su paquete fue entregado hoy. Consulte el seguimiento en https://correos.es"
        )

        assertFalse(result.isQuarantined)
        assertTrue(result.threatScore < 50)
    }

    @Test
    fun mensajeDePaqueteriaFraudulentoSeAisla() {
        val result = SmishDetectorEngine.analyze(
            sender = "Correos",
            body = "Paquete retenido: pague la tasa de aduana en https://correos-pago.xyz/entrega"
        )

        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Paquetería") })
    }

    @Test
    fun pruebaDeteccionPhishingBancarioConUrlMaliciosa() {
        val sender = "BBVA"
        val body = "BBVA: Su cuenta ha sido bloqueada temporalmente. Ingrese a https://bbva-seguridad.duckdns.org/login para evitar la cancelacion de su tarjeta."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Bancario") })
        assertTrue(result.factors.any { it.category.contains("Alojamiento") || it.category.contains("Marca") })
    }

    @Test
    fun pruebaDeteccionEstafaPaqueteriaConTldDeRiesgo() {
        val sender = "+34600123456"
        val body = "CORREOS: Su paquete no pudo ser entregado por falta de pago de tasa de aduana (2.64 EUR). Realice el pago urgente en: https://entrega-correos.xyz/pago"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.factors.any { it.category.contains("Paquetería") })
        assertTrue(result.factors.any { it.category.contains("Extensión de Dominio Sospechosa") })
    }

    @Test
    fun pruebaDeteccionAtaqueHomografoConCaracteresCirlicos() {
        val rawText = "Inicie sesion en https://раураl.top/auth para validar su cuenta"
        val homoglyphs = HomographDetector.findHomoglyphs(rawText)

        assertTrue(homoglyphs.isNotEmpty())

        val result = SmishDetectorEngine.analyze(sender = "Soporte", body = rawText)
        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.factors.any { it.category.contains("Homógrafo") || it.category.contains("Homografo") })
    }

    @Test
    fun pruebaDeteccionFalsaMultaDgtConIpDirecta() {
        val sender = "DGT"
        val body = "DGT: Sancion administrativa por exceso de velocidad. Ultimo aviso de pago antes de recargo: http://192.168.1.50/dgt"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.factors.any { it.category.contains("Organismo Público") })
        assertTrue(result.factors.any { it.category.contains("Dirección IP Directa") })
    }

    @Test
    fun pruebaDeteccionEstafaSuplantacionFamiliar() {
        val sender = "+34699887766"
        val body = "Hola mama, se me ha roto el movil y este es mi nuevo numero. Escribeme a whatsapp urgente que tengo una emergencia"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertTrue(result.level == ThreatLevel.PHISHING || result.level == ThreatLevel.SUSPICIOUS)
        assertTrue(result.factors.any { it.category.contains("Familiar") })
    }

    @Test
    fun pruebaSmsLegitimoGoogleOtpEsSeguro() {
        val sender = "Google"
        val body = "G-918234 es tu codigo de verificacion de Google."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertFalse(result.isQuarantined)
    }

    @Test
    fun pruebaSmsLegitimoBancolombiaClaveDinamicaSinFalsoPositivo() {
        val sender = "Bancolombia"
        val body = "Bancolombia le informa que su clave dinamica es 849201. Valida por 5 minutos. No la comparta con nadie."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(0, result.threatScore)
        assertFalse(result.isQuarantined)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun pruebaSmsLegitimoNequiOtpSinFalsoPositivo() {
        val sender = "Nequi"
        val body = "Nequi: Tu codigo de verificacion es 391-482 para ingresar a tu cuenta. No compartas este codigo con nadie."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(0, result.threatScore)
        assertFalse(result.isQuarantined)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun pruebaSmsLegitimoDaviviendaConSubdominioOficialSinFalsoPositivo() {
        val sender = "Davivienda"
        val body = "Davivienda: Realice sus consultas de seguridad en nuestro portal oficial https://notificaciones.davivienda.com"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertFalse(result.isQuarantined)
        assertFalse(result.factors.any { it.category.contains("Suplantación de Marca") })
    }

    @Test
    fun pruebaSmsLegitimoBancolombiaConSubdominioOficialSinFalsoPositivo() {
        val sender = "Bancolombia"
        val body = "Bancolombia: Ingrese a la sucursal virtual https://sucursalpersonas.transaccionesbancolombia.com para consultar su saldo."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertFalse(result.isQuarantined)
        assertFalse(result.factors.any { it.category.contains("Suplantación de Marca") })
    }

    @Test
    fun pruebaDeteccionUrlMaliciosaSinProtocoloYSinBarraFinal() {
        val sender = "Alerta"
        val body = "Su cuenta ha sido bloqueada preventivamente. Ingrese a bancolombia-reactivar.xyz para recuperarla urgente."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.extractedUrls.isNotEmpty())
        assertTrue(result.factors.any { it.category.contains("Extensión de Dominio Sospechosa") || it.category.contains("Marca") })
    }

    @Test
    fun pruebaDeteccionLeetspeakEnPalabrasSensibles() {
        val sender = "Soporte"
        val body = "Su cuenta b4nc0l0mb14 ha sido bl0qu34d4. Ingrese a https://alerta-cuenta.xyz"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
    }

    @Test
    fun pruebaDeteccionEspaciadoArtificialEvasivo() {
        val sender = "Soporte"
        val body = "b a n c o l o m b i a : c u e n t a   b l o q u e a d a . Visite https://portal-falso.top"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
    }

    @Test
    fun pruebaDeteccionFalsoPrestamoGotaAGota() {
        val sender = "+573009988776"
        val body = "Credito pre-aprobado por $3.000.000 sin fiador ni buro. Descargue el apk inmediato en: https://prestamo-ya.top/app.apk"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Préstamos") || it.category.contains("Prestamo") })
    }

    @Test
    fun pruebaDeteccionSuplantacionOperadorMovilImei() {
        val sender = "Claro"
        val body = "Claro: Su linea sera suspendida por falta de registro de IMEI. Evite el bloqueo y registre su equipo en: https://claro-imei.xyz"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Operador Móvil") || it.category.contains("Extensión") })
    }

    @Test
    fun pruebaCelularPrivadoEnviandoAlertaBancariaEsSospechoso() {
        val sender = "+573108889900"
        val body = "Bancolombia: Notificacion de seguridad. Ingrese a https://bancolombia-login.top para verificar su estado."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.factors.any { it.category.contains("Celular Privada") })
    }

    @Test
    fun pruebaRemitenteEnListaBlancaRetornaSeguro() {
        val sender = "+34611223344"
        val body = "Hola! Te envio este link con descuento: http://oferta-loca.xyz/promo"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body, isWhitelisted = true)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(0, result.threatScore)
        assertFalse(result.isQuarantined)
    }

    @Test
    fun pruebaReglaPersonalizadaActivaCuarentena() {
        val sender = "Promo"
        val body = "Gana dinero facil desde casa sin esfuerzo"
        val customRule = listOf(Regex("(?i)gana dinero facil"))

        val result = SmishDetectorEngine.analyze(
            sender = sender,
            body = body,
            customBlockPatterns = customRule
        )

        assertTrue(result.factors.any { it.category.contains("Personalizada") })
        assertTrue(result.threatScore >= 40)
    }

    @Test
    fun pruebaDeteccionPhishingNequiConAlojamientoAbusado() {
        val sender = "Nequi"
        val body = "Nequi: Su cuenta ha sido bloqueada preventivamente por movimientos inusuales. Reactívela aquí: https://nequi-desbloqueo.pages.dev/auth"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Nequi") || it.category.contains("Billetera") })
        assertTrue(result.factors.any { it.category.contains("Alojamiento") || it.category.contains("Marca") })
    }

    @Test
    fun pruebaDeteccionFraudeDianConEmbargo() {
        val sender = "DIAN-Alerta"
        val body = "DIAN: Mandamiento de pago. Su cuenta bancaria presenta embargo por mora tributaria. Descargue la resolucion en: https://dian-tramites.xyz/pago"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.factors.any { it.category.contains("Fiscal") || it.category.contains("DIAN") })
        assertTrue(result.factors.any { it.category.contains("Extensión de Dominio Sospechosa") })
    }

    @Test
    fun pruebaDeteccionFalsoDebitoPseConIpDirecta() {
        val sender = "PSE"
        val body = "PSE: Transaccion aprobada por $450.000 COP. Si no reconoce este debito cancele de inmediato en: http://192.168.1.80/pse"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.factors.any { it.category.contains("PSE") || it.category.contains("Débito") })
        assertTrue(result.factors.any { it.category.contains("Dirección IP Directa") })
    }

    @Test
    fun pruebaDeteccionRoboTokenOtpDinamico() {
        val sender = "+573009876543"
        val body = "Banco: Alerta de seguridad. Para anular transferencia reenvié de inmediato el código dinámico de 6 dígitos que acaba de recibir."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertTrue(result.level == ThreatLevel.PHISHING || result.level == ThreatLevel.SUSPICIOUS)
        assertTrue(result.factors.any { it.category.contains("Token") || it.category.contains("OTP") })
    }

    @Test
    fun pruebaDeteccionEvasionConCaracteresInvisiblesAnchoCero() {
        val body = "N\u200Be\u200Bq\u200Bu\u200Bi: Su cuenta ha sido b\u200Bl\u200Bo\u200Bq\u200Bu\u200Be\u200Ba\u200Bd\u200Ba. Entre en: https://nequi-login.top/auth"

        val result = SmishDetectorEngine.analyze(sender = "Soporte", body = body)

        assertTrue(result.factors.any { it.category.contains("Homógrafo") || it.category.contains("Homografo") })
        assertEquals(ThreatLevel.PHISHING, result.level)
    }

    @Test
    fun pruebaDefangCompletoDeUrlsIpsYDominios() {
        val rawText = "Visite http://192.168.1.100 y https://banco-fraude.xyz/login para verificar."
        val extracted = listOf("http://192.168.1.100", "https://banco-fraude.xyz/login")

        val defanged = SmishDetectorEngine.defangText(rawText, extracted)

        assertFalse(defanged.contains("http://"))
        assertFalse(defanged.contains("https://"))
        assertTrue(defanged.contains("hxxp://") || defanged.contains("hxxps://"))
        assertTrue(defanged.contains("192[.]168[.]1[.]100"))
        assertTrue(defanged.contains("banco-fraude[.]xyz/login"))
    }

    @Test
    fun pruebaSinEmojisEnResultadosDelMotor() {
        val sender = "Nequi"
        val body = "Nequi: Cuenta bloqueada. Ingrese a https://nequi-desbloqueo.pages.dev"
        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        val emojiPattern = Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u2600-\u27BF]""")
        assertFalse("summaryReason tiene emoji: ${result.summaryReason}", emojiPattern.containsMatchIn(result.summaryReason))
        for (factor in result.factors) {
            assertFalse("category tiene emoji: ${factor.category}", emojiPattern.containsMatchIn(factor.category))
            assertFalse("description tiene emoji: '${factor.description}'", emojiPattern.containsMatchIn(factor.description))
        }
    }

    @Test
    fun pruebaNoFalsoPositivoMarcasCortasIngYPse() {
        val legitimateUrls = listOf(
            "https://booking.com/hotel/123",
            "https://hosting.com/pricing",
            "https://billing.example.com",
            "https://shopping.test.org",
            "https://eclipse.org/download",
            "https://synapse.org/info"
        )

        for (url in legitimateUrls) {
            val factors = com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.analyzeUrls(listOf(url))
            val brandFactors = factors.filter { it.category.contains("Suplantación de Marca") }
            assertTrue("URL $url no debería activar factor de suplantación de marca: $brandFactors", brandFactors.isEmpty())
        }
    }

    @Test
    fun pruebaDeteccionSuplantacionMarcaCortaCuandoEstaDelimitada() {
        val maliciousUrls = listOf(
            "https://ing-direct-alerta.xyz/login",
            "https://banco-ing.xyz/auth",
            "https://pse-pagos-enlinea.top/verificar"
        )

        for (url in maliciousUrls) {
            val factors = com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.analyzeUrls(listOf(url))
            assertTrue(
                "URL $url debería activar factor de suplantación de marca",
                factors.any { it.category.contains("Suplantación de Marca") }
            )
        }
    }

    @Test
    fun pruebaDeteccionMultipleEspaciadoArtificial() {
        val text = "Estimado cliente, b a n c o l o m b i a : c u e n t a  b l o q u e a d a . Visite https://alerta.xyz"
        val homoglyphs = HomographDetector.findHomoglyphs(text)

        assertTrue(
            "Debe detectar espaciado artificial en texto con múltiples palabras",
            homoglyphs.any { it.contains("Espaciado artificial") }
        )
    }

    @Test
    fun pruebaAtaqueConOtpFalsoYIngenieriaSocialNoSeNeutraliza() {
        val body = "Su cuenta Nequi ha sido bloqueada. Ingrese a https://nequi-desbloqueo.xyz para recuperar acceso. Codigo de seguridad 9182. Valido por 5 minutos, no lo comparta con nadie."
        val result = SmishDetectorEngine.analyze(sender = "Nequi", body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.threatScore >= 60)
    }

    @Test
    fun pruebaReglaPersonalizadaAislaMensajePorSiSola() {
        val customRule = listOf(Regex("""\bpremio\s+exclusivo\b""", RegexOption.IGNORE_CASE))
        val body = "Felicidades, ha ganado un premio exclusivo por su fidelidad. Reclame hoy."

        val result = SmishDetectorEngine.analyze(
            sender = "+573001112233",
            body = body,
            customBlockPatterns = customRule,
            quarantineThreshold = 50
        )

        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Personalizada") })
    }

    @Test
    fun pruebaDefangTextNeutralizaIpsYProtocolosCompletamente() {
        val raw = "Visite http://192.168.1.10/login y www.malware.com/payload"
        val urls = listOf("http://192.168.1.10/login", "www.malware.com/payload")
        val defanged = SmishDetectorEngine.defangText(raw, urls)

        assertFalse("No debe contener http://", defanged.contains("http://"))
        assertFalse("No debe contener www.", defanged.contains("www."))
        assertTrue("Debe contener hxxp://", defanged.contains("hxxp://"))
        assertTrue("Debe neutralizar la IP con corchetes", defanged.contains("192[.]168[.]1[.]10"))
    }

    @Test
    fun pruebaClasificadorMlDistingueLegitimoSpamYSmishing() {
        val resultadoLegitimo = com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms.clasificarMensaje(
            remitente = "Bancolombia",
            mensaje = "Bancolombia le informa compra por $125.000 en ALMACENES EXITO con tarjeta debito terminada en 9012."
        )
        assertEquals("LEGITIMO", resultadoLegitimo.categoriaPredicha)

        val resultadoSpam = com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms.clasificarMensaje(
            remitente = "Claro",
            mensaje = "Claro: Aprovecha hoy 40% de descuento en equipos seleccionados renovando tu plan pospago en tiendas."
        )
        assertEquals("SPAM", resultadoSpam.categoriaPredicha)

        val resultadoSmishing = com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms.clasificarMensaje(
            remitente = "Nequi",
            mensaje = "Nequi: Tu cuenta presenta restriccion temporal. Confirma tu identidad en https://nequi-soporte-oficial.top/login"
        )
        assertEquals("SMISHING", resultadoSmishing.categoriaPredicha)
        assertTrue(resultadoSmishing.probabilidadSmishing > 0.70)
    }

    @Test
    fun pruebaNotificacionBancariaCompraRealSinEnlaceEsSegura() {
        val resultado = SmishDetectorEngine.analizarMensaje(
            remitente = "Bancolombia",
            mensaje = "Bancolombia le informa compra por $125.000 en ALMACENES EXITO con tarjeta debito terminada en 9012."
        )

        assertEquals(ThreatLevel.SAFE, resultado.level)
        assertFalse(resultado.isQuarantined)
        assertEquals("LEGITIMO", resultado.categoriaMensaje)
    }

    @Test
    fun pruebaPromocionComercialSeClasificaComoSpamSinCuarentena() {
        val resultado = SmishDetectorEngine.analizarMensaje(
            remitente = "Claro",
            mensaje = "Claro: Aprovecha hoy 40% de descuento en equipos seleccionados renovando tu plan pospago en tiendas."
        )

        assertEquals(ThreatLevel.SAFE, resultado.level)
        assertFalse(resultado.isQuarantined)
        assertEquals("SPAM", resultado.categoriaMensaje)
    }

    @Test
    fun pruebaSmishingColombianoConDominioComNoOficialSeDetecta() {
        val resultado = SmishDetectorEngine.analizarMensaje(
            remitente = "+573004455661",
            mensaje = "Bancolombia: Se registro una compra por $740.000 en MercadoLibre. Si no la reconoce cancele el cobro aqui: https://seguridad-sucursal-col.com/anular"
        )

        assertEquals(ThreatLevel.PHISHING, resultado.level)
        assertTrue(resultado.isQuarantined)
        assertEquals("SMISHING", resultado.categoriaMensaje)
    }

    @Test
    fun pruebaSmishingPaqueteriaServientregaY472SeDetecta() {
        val resultadoServientrega = SmishDetectorEngine.analizarMensaje(
            remitente = "Servientrega",
            mensaje = "Servientrega: Su paquete presenta direccion erronea y no pudo entregarse. Actualice sus datos de envio en https://rastreo-servientrega.top/guia"
        )
        assertTrue(resultadoServientrega.isQuarantined)
        assertEquals(ThreatLevel.PHISHING, resultadoServientrega.level)

        val resultado472 = SmishDetectorEngine.analizarMensaje(
            remitente = "4-72",
            mensaje = "4-72: Su encomienda internacional esta retenida en aduana. Pague el impuesto de nacionalizacion en https://envios-472-aduana.online"
        )
        assertTrue(resultado472.isQuarantined)
        assertEquals(ThreatLevel.PHISHING, resultado472.level)
    }

    data class CasoPruebaIndependiente(
        val remitente: String,
        val mensaje: String,
        val claseReal: String
    )

    private fun obtenerConjuntoPruebaIndependiente(): List<CasoPruebaIndependiente> {
        return listOf(
            CasoPruebaIndependiente("Bancolombia", "Bancolombia le informa compra por $125.000 en ALMACENES EXITO con tarjeta debito terminada en 9012.", "LEGITIMO"),
            CasoPruebaIndependiente("Bancolombia", "Bancolombia: Su clave dinamica es 304918. Tiene vigencia de 5 minutos. Por su seguridad no la comparta con nadie.", "LEGITIMO"),
            CasoPruebaIndependiente("Bancolombia", "Bancolombia: Se realizo el pago de su factura de EPM por $142.300 desde su cuenta de ahorros.", "LEGITIMO"),
            CasoPruebaIndependiente("Nequi", "Nequi: Tu codigo de seguridad para confirmar el ingreso es 817-294. Nunca lo compartas con nadie.", "LEGITIMO"),
            CasoPruebaIndependiente("Nequi", "Nequi: Recibiste $65.000 de Andres Felipe en tu disponible. Consulta tus movimientos en la app.", "LEGITIMO"),
            CasoPruebaIndependiente("Daviplata", "Daviplata: Compra exitosa por $32.000 en RESTAURANTE EL CORRAL con tu tarjeta virtual.", "LEGITIMO"),
            CasoPruebaIndependiente("Daviplata", "Daviplata: El codigo de confirmacion para tu retiro es 940123. Valido por 5 minutos, no lo entregues a nadie.", "LEGITIMO"),
            CasoPruebaIndependiente("Davivienda", "Davivienda: Retiro en cajero automatico por $300.000 exitoso. Si requiere asistencia comuniquese al #338.", "LEGITIMO"),
            CasoPruebaIndependiente("BBVA", "BBVA: Transferencia recibida por $480.000 en su cuenta de ahorros terminada en 4410.", "LEGITIMO"),
            CasoPruebaIndependiente("Google", "G-726104 es tu codigo de verificacion de Google. Por seguridad no lo compartas.", "LEGITIMO"),
            CasoPruebaIndependiente("Servientrega", "Servientrega: Tu paquete con guia 99817263 fue entregado exitosamente. Consulta el comprobante en https://www.servientrega.com", "LEGITIMO"),
            CasoPruebaIndependiente("PSE", "PSE: Pago aprobado por $95.000 en Vanti Gas Natural con codigo CUS 44556677.", "LEGITIMO"),
            CasoPruebaIndependiente("+573109871234", "Hola ma, ya llegue bien a Medellin, apenas me instale en el hotel te marco.", "LEGITIMO"),
            CasoPruebaIndependiente("+573154328765", "Companero, me puedes compartir el enlace del documento de Google Drive para revisar la presentacion?", "LEGITIMO"),
            CasoPruebaIndependiente("+573006543219", "Amor, necesito urgente que revises si ya pagaron la nomina en tu cuenta para hacer el mercado.", "LEGITIMO"),
            CasoPruebaIndependiente("+573123459876", "Buenas tardes, le escribimos del consultorio odontologico para recordar su cita de limpieza manana a las 10 AM.", "LEGITIMO"),
            CasoPruebaIndependiente("+573187651234", "Pa, me pasas por favor la clave del televisor de la sala para poner el partido?", "LEGITIMO"),
            CasoPruebaIndependiente("Coordinadora", "Coordinadora: Hemos recibido su envio 4419283 y llegara manana a su destino. Seguimiento en https://coordinadora.com", "LEGITIMO"),
            CasoPruebaIndependiente("WhatsApp", "Tu codigo de WhatsApp Business es 612-849. No compartas este codigo con terceros.", "LEGITIMO"),
            CasoPruebaIndependiente("Nu", "Nu: El pago de tu estado de cuenta por $410.000 fue recibido exitosamente. Gracias.", "LEGITIMO"),
            CasoPruebaIndependiente("Davivienda", "Davivienda informa que su tarjeta debito fue bloqueada preventivamente en cajero. Acerquese a cualquier oficina con su cedula original.", "LEGITIMO"),
            CasoPruebaIndependiente("+573114455990", "Hola vecina, dejaron un paquete para usted en la porteria porque la direccion del apartamento no se leia bien.", "LEGITIMO"),

            CasoPruebaIndependiente("Claro", "Claro: Aprovecha hoy 40% de descuento en equipos seleccionados renovando tu plan pospago en tiendas.", "SPAM"),
            CasoPruebaIndependiente("Movistar", "Movistar: Recarga $8.000 o mas hoy y recibe el doble de gigas para navegar toda la semana.", "SPAM"),
            CasoPruebaIndependiente("Tigo", "Promo Tigo Hogar: Internet de 500 megas mas television HD con descuento especial durante 3 meses.", "SPAM"),
            CasoPruebaIndependiente("Exito", "Dias de Precios Especiales Exito: Hasta 35% de ahorro en mercado, aseo y electrodomesticos este fin de semana.", "SPAM"),
            CasoPruebaIndependiente("Alkosto", "Alkosto: Aprovecha el quinceazo con descuentos increibles en computadores, celulares y sonido.", "SPAM"),
            CasoPruebaIndependiente("CineColombia", "Cine Colombia: No te pierdas el estreno de la semana en nuestras salas IMAX con combo promocional.", "SPAM"),
            CasoPruebaIndependiente("Rappi", "Rappi: Pide hoy tu almuerzo favorito con hasta 30% de descuento en restaurantes aliados de tu zona.", "SPAM"),
            CasoPruebaIndependiente("Homecenter", "Homecenter: Feria de herramientas y construccion con hasta 25% de descuento en tiendas fisicas.", "SPAM"),
            CasoPruebaIndependiente("CruzVerde", "Cruz Verde: Hoy jueves ahorra 20% en medicamentos seleccionados y vitaminas para toda la familia.", "SPAM"),
            CasoPruebaIndependiente("Avianca", "Avianca: Compra tus tiquetes en promocion para volar a destinos nacionales desde $109.900 por trayecto.", "SPAM"),
            CasoPruebaIndependiente("Falabella", "Falabella: Ultimos dias de descuentos de temporada con hasta 45% en vestuario y tenis en https://falabella.com.co", "SPAM"),
            CasoPruebaIndependiente("SmartFit", "Smart Fit: Ven a entrenar en nuestras nuevas sedes con promocion de primer mes a mitad de precio.", "SPAM"),
            CasoPruebaIndependiente("PromoClub", "Has sido seleccionado para ganar un bono de descuento en nuestra tienda de calzado. Reclama tu premio hoy en tienda.", "SPAM"),

            CasoPruebaIndependiente("Bancolombia", "Bancolombia: Hemos detectado un intento de ingreso inusual. Su cuenta fue bloqueada, valide sus datos en https://bancolombia-verifica.xyz/acceso", "SMISHING"),
            CasoPruebaIndependiente("+573004455661", "Bancolombia: Se registro una compra por $740.000 en MercadoLibre. Si no la reconoce cancele el cobro aqui: https://seguridad-sucursal-col.com/anular", "SMISHING"),
            CasoPruebaIndependiente("Nequi", "Nequi: Tu cuenta presenta restriccion temporal de seguridad. Confirma tu identidad para reactivarla en https://nequi-soporte-oficial.top/login", "SMISHING"),
            CasoPruebaIndependiente("+573148899002", "Nequi: Tienes una transferencia retenida por $280.000. Ingresa tu clave para liberar el saldo en https://portal-ayuda-nequi.co/validar", "SMISHING"),
            CasoPruebaIndependiente("Daviplata", "Daviplata: Usted ha sido seleccionado para el bono de apoyo economico de $360.000. Solicite el desembolso en https://daviplata-bonos.xyz/cobro", "SMISHING"),
            CasoPruebaIndependiente("+573117766550", "Davivienda: Su clave virtual ha vencido. Actualice sus credenciales hoy para evitar el bloqueo de sus productos en https://clientes-davivienda.online", "SMISHING"),
            CasoPruebaIndependiente("BBVA", "BBVA: Alerta por movimiento sospechoso con su tarjeta de credito. Verifique y bloquee el cargo en https://bbva-validacion.pages.dev/seguridad", "SMISHING"),
            CasoPruebaIndependiente("BancoBogota", "Banco de Bogota: Su cuenta de ahorros ha sido suspendida preventivamente. Reactive su acceso inmediato en https://portal-bancobogota.xyz", "SMISHING"),
            CasoPruebaIndependiente("DIAN", "DIAN: Ultimo aviso de embargo sobre sus cuentas bancarias por deuda fiscal. Descargue el mandamiento de pago en https://dian-resolucion.top/embargo", "SMISHING"),
            CasoPruebaIndependiente("DIAN-Info", "DIAN: Tienes un reembolso tributario aprobado por $415.000. Registra tu cuenta para el abono en https://reembolsos-impuestos-col.com/validar", "SMISHING"),
            CasoPruebaIndependiente("PSE", "PSE: Debito automatico en proceso por $590.000. Si usted no autorizo esta transaccion anulela inmediatamente en http://192.168.10.45/pse-cancelar", "SMISHING"),
            CasoPruebaIndependiente("SIMIT", "SIMIT: Notificacion de fotomulta pendiente. Evite sanciones y embargo vehicular pagando con 50% de descuento en https://pagos-simit-transito.xyz", "SMISHING"),
            CasoPruebaIndependiente("Servientrega", "Servientrega: Su paquete presenta direccion erronea y no pudo entregarse. Actualice sus datos de envio en https://rastreo-servientrega.top/guia", "SMISHING"),
            CasoPruebaIndependiente("4-72", "4-72: Su encomienda internacional esta retenida en aduana. Pague el impuesto de nacionalizacion en https://envios-472-aduana.online", "SMISHING"),
            CasoPruebaIndependiente("Coordinadora", "Coordinadora: Segundo intento de entrega fallido. Reprograme su paquete y cancele el sobrecosto en https://entrega-guia-col.com/reprogramar", "SMISHING"),
            CasoPruebaIndependiente("Claro", "Claro: Aviso de suspension de linea por falta de homologacion de IMEI. Registre su celular urgente en https://claro-registro-equipo.xyz", "SMISHING"),
            CasoPruebaIndependiente("+573012233449", "Seguridad Bancaria: Para detener el retiro no autorizado de su cuenta reenvie inmediatamente el codigo OTP de 6 digitos que acaba de llegarle.", "SMISHING"),
            CasoPruebaIndependiente("+573205544331", "Prestamo inmediato aprobado por $4.000.000 sin fiador ni consulta en Datacredito. Descargue la aplicacion APK en https://credito-rapido.top/app.apk", "SMISHING"),
            CasoPruebaIndependiente("+573169988112", "Hola mama, se me dano el celular y este es mi numero nuevo. Escribeme a WhatsApp urgente que tengo una emergencia y necesito una transferencia.", "SMISHING"),
            CasoPruebaIndependiente("+573192233110", "Ganaste un premio de $1.500.000 y un bono de mercado en nuestro sorteo nacional. Reclama tu recompensa hoy en https://premio-sorteo-col.xyz", "SMISHING"),
            CasoPruebaIndependiente("Soporte", "Su cuenta n3qu1 ha sido bl0qu34d4 por seguridad. Valide su cl4v3 urgente en https://acceso-seguro.top", "SMISHING"),
            CasoPruebaIndependiente("Transfiya", "Transfiya: Tienes un giro pendiente por $320.000 que vencera pronto. Ingresa tu clave bancaria para recibirlo en https://aceptar-giro-col.com/login", "SMISHING"),
            CasoPruebaIndependiente("+573152200119", "Estimado usuario, su declaracion presenta inconsistencias contables del periodo actual. Revise el documento adjunto en https://consulta-documentos-co.net/archivo", "SMISHING"),
            CasoPruebaIndependiente("+573178800991", "Hola, te escribo de recursos humanos porque tu perfil fue preseleccionado para vacante remota. Agenda tu entrevista hoy en https://seleccion-personal-co.org/agenda", "SMISHING")
        )
    }

    private fun clasificarConReglasAnteriores(remitente: String, mensaje: String): String {
        val reglasAntiguas = listOf(
            Regex("""(?i)\b(?:cuenta\s+(?:ha\s+sido\s+)?(?:bloqueada|suspendida|desactivada|restringida)|tarjeta\s+(?:(?:ha\s+sido\s+)?(?:bloqueada|retenida)|cancelaci[oó]n)|acceso\s+no\s+autorizado|actuali(?:ce|zar)\s+sus\s+datos\s+(?:urgente|de\s+inmediato|aqu[ií])|reactiv(?:ar|e)\s+su\s+cuenta|alerta\s+de\s+seguridad\s+bancaria|verificaci[oó]n\s+bancaria\s+urgente)\b""") to 35,
            Regex("""(?i)\b(?:paquete\s+(?:retenido|pendiente|bloqueado|no\s+entregado)|tasa\s+de\s+aduana|pagar\s+(?:gastos|aranceles|env[ií]o)|direcci[oó]n\s+incompleta|imposible\s+entregar\s+su\s+env[ií]o|actualice\s+su\s+direcci[oó]n\s+en|retiro\s+de\s+paquete\s+en\s+aduanas)\b""") to 30,
            Regex("""(?i)\b(?:dgt\b.*multa|notificaci[oó]n\s+dgt|aeat\b.*devoluci[oó]n|hacienda\b.*reembolso|seguridad\s+social\b.*tarjeta\s+sanitaria|sanci[oó]n\s+administrativa|último\s+aviso\s+de\s+pago)\b""") to 35,
            Regex("""(?i)\b(?:introduzc?a?\s+(?:su\s+)?(?:pin|clave|contraseña)\s+en|ingres(?:e|ar)\s+(?:sus\s+)?credenciales\s+en|autorizar\s+transferencia\s+en|c[oó]digo\s+de\s+confirmaci[oó]n.*(?:haga\s+clic|enlace|ingrese\s+a))\b""") to 35,
            Regex("""(?i)\b(?<!\bno\s)(?<!\bnunca\s)(?:reenv[ií][eé]|copi[eé]|entregu[eé]|comparta|env[ií][eé])\s+(?:de\s+inmediato\s+)?(?:el\s+)?(?:c[oó]digo(?:\s+din[aá]mico)?|token|otp|clave\s+din[aá]mica)\b""") to 40,
            Regex("""(?i)\b(?:urgente|de\s+inmediato|en\s+menos\s+de\s+24\s*h(?:oras)?|plazo\s+l[ií]mite|evite\s+(?:recargo|suspensi[oó]n|bloqueo|cierre|acciones\s+legales|embargo)|de\s+lo\s+contrario\s+ser[aá]\s+cancelad[oa])\b""") to 20,
            Regex("""(?i)\b(?:has\s+sido\s+seleccionad[oa]|has\s+ganado\s+un|premio\s+de\s+\$|sorteo\s+exclusivo|reclama\s+tu\s+recompensa|trabajo\s+a\s+tiempo\s+parcial.*gana\s+\$|inversi[oó]n\s+garantizada|gana\s+dinero\s+f[aá]cil\s+desde\s+casa|gana\s+\$\s*\d+.*diarios)\b""") to 25,
            Regex("""(?i)\b(?:cr[eé]dito\s+(?:pre-?aprobado|inmediato|sin\s+fiador|sin\s+bur[oó])|desembolso\s+de\s+\$\s*\d+|descargue\s+(?:el\s+apk|la\s+aplicaci[oó]n)\s+para\s+su\s+pr[eé]stamo)\b""") to 35,
            Regex("""(?i)\b(?:nequi|daviplata|bancolombia|davivienda|banco\s+de\s+bogot[aá]|banco\s+popular|scotiabank|colpatria|banco\s+agrario)\b.*(?:bloquead[oa]|suspendid[oa]|inactiv[oa]|desembolso|subsidio|reactivar\s+cuenta|desbloqueo)""") to 40,
            Regex("""(?i)\b(?:dian\b.*(?:embargo|cuenta\s+embargada|orden\s+de\s+pago|notificaci[oó]n\s+coactiva|evite\s+sanci[oó]n)|sanci[oó]n\s+tributaria|fotomulta\b|simit\b.*multa)\b""") to 35,
            Regex("""(?i)\b(?:pse\b.*(?:d[eé]bito|transacci[oó]n|compra|pago\s+aprobado|no\s+reconocid[oa]|desconocid[oa]|anulaci[oó]n)|alerta\s+de\s+compra\s+no\s+reconocida)\b""") to 35,
            Regex("""(?i)\b(?:claro|tigo|movistar|wom)\b.*(?:bloqueo\s+de\s+imei|suspensi[oó]n\s+de\s+l[ií]nea|registre\s+su\s+equipo|actualice\s+sus\s+datos\s+para\s+no\s+perder\s+su\s+l[ií]nea)\b""") to 30,
            Regex("""(?i)\b(?:hola\s+mam[aá]|hola\s+pap[aá]|se\s+me\s+ha\s+roto\s+el\s+m[oó]vil|este\s+es\s+mi\s+nuevo\s+n[uú]mero|escr[ií]beme\s+a\s+whatsapp|tengo\s+una\s+emergencia)\b""") to 35
        )

        val homoglifos = HomographDetector.findHomoglyphs(mensaje)
        val normalizado = HomographDetector.normalizeHomoglyphs(mensaje)
        var puntaje = if (homoglifos.isNotEmpty()) 35 else 0

        val enlaces = com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.detectarEnlaces(normalizado)
        var puntajeUrl = 0
        for (enlace in enlaces) {
            val dominio = com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.extraerDominioDeEnlace(enlace)
            val ext = dominio.substringAfterLast(".", "")
            if (Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""").matches(dominio)) puntajeUrl += 35
            if (setOf("xyz", "top", "online", "site", "click", "lat").contains(ext)) puntajeUrl += 25
            if (dominio.endsWith("pages.dev") || dominio.endsWith("duckdns.org")) puntajeUrl += 30
            for (marca in listOf("bancolombia", "nequi", "daviplata", "davivienda", "bbva", "dian", "pse", "servientrega", "claro")) {
                if (dominio.contains(marca) && !com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.esDominioOficialGeneral(dominio)) {
                    puntajeUrl += 35
                    break
                }
            }
            for (kw in listOf("login", "verificar", "actualizar", "seguridad", "bloqueo", "reactivar", "validar", "confirmar", "tarjeta", "clave", "token", "embargo", "multa", "pago", "retiro")) {
                if (enlace.lowercase().contains(kw) && !com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.esDominioOficialGeneral(dominio)) {
                    puntajeUrl += 20
                    break
                }
            }
        }
        puntaje += puntajeUrl

        var puntajePatrones = 0
        for ((regex, sev) in reglasAntiguas) {
            if (regex.containsMatchIn(normalizado)) {
                puntajePatrones += sev
            }
        }
        puntaje += puntajePatrones

        val mencionaEntidadAntigua = Regex("""(?i)\b(?:banco|bancolombia|nequi|daviplata|davivienda|bbva|santander|caixa|dian|pse|scotiabank|colpatria|bancoagrario)\b""").containsMatchIn(normalizado)
        val esCelular = Regex("""^(?:\+?57)?3[0-9]{9}$""").matches(remitente.trim())
        if (esCelular && mencionaEntidadAntigua && puntajeUrl > 0) {
            puntaje += 35
        }
        if (puntajeUrl > 0 && puntajePatrones > 0) {
            puntaje = (puntaje * 1.35).toInt()
        }

        val tieneOtpAntiguo = Regex("""(?i)\b(?:no\s+(?:lo\s+)?comparta|v[aá]lid[oa]\s+por\s+\d+\s+minutos?|es\s+tu\s+c[oó]digo|su\s+clave\s+din[aá]mica\s+es)\b""").containsMatchIn(normalizado)
        if (tieneOtpAntiguo && puntajeUrl == 0 && puntajePatrones == 0) {
            puntaje = 0
        }

        return if (puntaje >= 35) "SMISHING" else "LEGITIMO"
    }

    @Test
    fun pruebaEvaluacionComparativaConjuntoIndependiente() {
        val casos = obtenerConjuntoPruebaIndependiente()
        val clases = listOf("LEGITIMO", "SPAM", "SMISHING")

        val matrizAnterior = Array(3) { IntArray(3) }
        val matrizNueva = Array(3) { IntArray(3) }

        for (caso in casos) {
            val indiceReal = clases.indexOf(caso.claseReal)

            val predAnterior = clasificarConReglasAnteriores(caso.remitente, caso.mensaje)
            val indicePredAnterior = clases.indexOf(predAnterior)
            matrizAnterior[indiceReal][indicePredAnterior]++

            val resultadoNuevo = SmishDetectorEngine.analizarMensaje(
                remitente = caso.remitente,
                mensaje = caso.mensaje
            )
            val predNuevo = resultadoNuevo.categoriaMensaje
            val indicePredNuevo = clases.indexOf(predNuevo)
            matrizNueva[indiceReal][indicePredNuevo]++
        }

        var sumaF1Anterior = 0.0
        var sumaF1Nuevo = 0.0

        println("=== EVALUACION SISTEMA ANTERIOR ===")
        for (i in 0 until 3) {
            val verdaderosPositivos = matrizAnterior[i][i].toDouble()
            var falsosPositivos = 0.0
            var falsosNegativos = 0.0
            for (j in 0 until 3) {
                if (j != i) {
                    falsosPositivos += matrizAnterior[j][i].toDouble()
                    falsosNegativos += matrizAnterior[i][j].toDouble()
                }
            }
            val precision = if (verdaderosPositivos + falsosPositivos > 0.0) {
                verdaderosPositivos / (verdaderosPositivos + falsosPositivos)
            } else {
                0.0
            }
            val recall = if (verdaderosPositivos + falsosNegativos > 0.0) {
                verdaderosPositivos / (verdaderosPositivos + falsosNegativos)
            } else {
                0.0
            }
            val f1 = if (precision + recall > 0.0) {
                (2.0 * precision * recall) / (precision + recall)
            } else {
                0.0
            }
            sumaF1Anterior += f1
            println("Clase ${clases[i]}: Precision=${"%.4f".format(precision)}, Recall=${"%.4f".format(recall)}, F1=${"%.4f".format(f1)}")
        }
        val macroF1Anterior = sumaF1Anterior / 3.0
        println("Macro-F1 Anterior: ${"%.4f".format(macroF1Anterior)}")

        println("=== EVALUACION SISTEMA NUEVO ===")
        for (i in 0 until 3) {
            val verdaderosPositivos = matrizNueva[i][i].toDouble()
            var falsosPositivos = 0.0
            var falsosNegativos = 0.0
            for (j in 0 until 3) {
                if (j != i) {
                    falsosPositivos += matrizNueva[j][i].toDouble()
                    falsosNegativos += matrizNueva[i][j].toDouble()
                }
            }
            val precision = if (verdaderosPositivos + falsosPositivos > 0.0) {
                verdaderosPositivos / (verdaderosPositivos + falsosPositivos)
            } else {
                0.0
            }
            val recall = if (verdaderosPositivos + falsosNegativos > 0.0) {
                verdaderosPositivos / (verdaderosPositivos + falsosNegativos)
            } else {
                0.0
            }
            val f1 = if (precision + recall > 0.0) {
                (2.0 * precision * recall) / (precision + recall)
            } else {
                0.0
            }
            sumaF1Nuevo += f1
            println("Clase ${clases[i]}: Precision=${"%.4f".format(precision)}, Recall=${"%.4f".format(recall)}, F1=${"%.4f".format(f1)}")
        }
        val macroF1Nuevo = sumaF1Nuevo / 3.0
        println("Macro-F1 Nuevo: ${"%.4f".format(macroF1Nuevo)}")

        assertTrue("El Macro-F1 nuevo ($macroF1Nuevo) debe superar sustancialmente al anterior ($macroF1Anterior)", macroF1Nuevo > macroF1Anterior)
        assertTrue("El recall de SMISHING debe ser superior en el nuevo detector", matrizNueva[2][2] > matrizAnterior[2][2])
    }

    private fun extraerObjetosJsonPlano(contenidoJson: String): List<Map<String, String>> {
        val objetos = ArrayList<Map<String, String>>()
        val patronObjeto = Regex("""\{([^{}]+)\}""")
        val patronCampo = Regex(""""([^"]+)"\s*:\s*(?:"((?:\\.|[^"\\])*)"|([-+]?\d+(?:\.\d+)?))""")

        for (coincidenciaObj in patronObjeto.findAll(contenidoJson)) {
            val cuerpo = coincidenciaObj.groupValues[1]
            val mapa = HashMap<String, String>()
            for (coincidenciaCampo in patronCampo.findAll(cuerpo)) {
                val clave = coincidenciaCampo.groupValues[1]
                val valorTexto = coincidenciaCampo.groupValues[2]
                val valorNum = coincidenciaCampo.groupValues[3]
                val valorCrudo = if (valorNum.isNotEmpty()) valorNum else valorTexto
                val valorLimpio = valorCrudo
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                mapa[clave] = valorLimpio
            }
            if (mapa.isNotEmpty()) {
                objetos.add(mapa)
            }
        }
        return objetos
    }

    @Test
    fun pruebaConsistenciaEquivalentePythonYKotlin() {
        val flujo = javaClass.getResourceAsStream("/casos_consistencia_python_kotlin.json")
            ?: javaClass.classLoader?.getResourceAsStream("casos_consistencia_python_kotlin.json")
        assertNotNull("Debe existir casos_consistencia_python_kotlin.json en test resources", flujo)

        val contenido = flujo!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val casos = extraerObjetosJsonPlano(contenido)
        assertEquals("Se esperan 20 casos de consistencia exportados desde Python", 20, casos.size)

        for ((indice, caso) in casos.withIndex()) {
            val remitente = caso["remitente"] ?: ""
            val mensaje = caso["mensaje"] ?: ""
            val categoriaPython = caso["categoria_python"] ?: ""
            val probLegPython = caso["prob_legitimo"]?.toDouble() ?: 0.0
            val probSpamPython = caso["prob_spam"]?.toDouble() ?: 0.0
            val probSmishPython = caso["prob_smishing"]?.toDouble() ?: 0.0

            val resultadoKotlin = com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms.clasificarMensaje(
                remitente = remitente,
                mensaje = mensaje
            )

            assertEquals(
                "Discrepancia de clase en caso #$indice ($remitente)",
                categoriaPython,
                resultadoKotlin.categoriaPredicha
            )
            assertEquals(
                "Discrepancia en probLegitimo caso #$indice",
                probLegPython,
                resultadoKotlin.probabilidadLegitimo,
                1e-4
            )
            assertEquals(
                "Discrepancia en probSpam caso #$indice",
                probSpamPython,
                resultadoKotlin.probabilidadSpam,
                1e-4
            )
            assertEquals(
                "Discrepancia en probSmishing caso #$indice",
                probSmishPython,
                resultadoKotlin.probabilidadSmishing,
                1e-4
            )
        }
    }

    @Test
    fun pruebaMensajesLegitimosConEnlacesCotidianosSinFalsoPositivo() {
        val mensajesLegitimosConEnlace = listOf(
            "UNAL" to "Universidad Nacional: Recuerde su clase sincrona hoy a las 4 PM en https://zoom.us/j/981273645",
            "UNAL" to "Universidad Nacional: Consulta tu horario de matricula en https://registro.unal.edu.co/estudiante",
            "Sura" to "EPS Sura: Recuerda tu cita de medicina general manana 9:20 AM. Detalles en https://epssura.com/citas",
            "Restaurante" to "Tu mesa para 4 personas hoy a las 8 PM esta confirmada. Ver menu en https://crepesywaffles.com/menu",
            "CentroMedico" to "Colcan: Descargue sus imagenes diagnosticas ingresando con su orden en https://portal.colcan.com.co/pacientes"
        )

        for ((remitente, mensaje) in mensajesLegitimosConEnlace) {
            val resultado = SmishDetectorEngine.analizarMensaje(remitente = remitente, mensaje = mensaje)
            assertFalse("No debe poner en cuarentena enlace cotidiano legitimo: $mensaje", resultado.isQuarantined)
            assertEquals("Debe mantener nivel SAFE en enlace cotidiano legitimo: $mensaje", ThreatLevel.SAFE, resultado.level)
            assertNotEquals("No debe clasificarse como SMISHING: $mensaje", "SMISHING", resultado.categoriaMensaje)
        }
    }

    @Test
    fun pruebaAvisosCotidianosYFamiliaresRealesSinFalsoPositivo() {
        val saludoMama = SmishDetectorEngine.analizarMensaje(
            remitente = "+573104455667",
            mensaje = "Hola mamá, salgo urgente para la casa, nos vemos en 20 minutos."
        )
        assertFalse(saludoMama.isQuarantined)
        assertEquals(ThreatLevel.SAFE, saludoMama.level)
        assertEquals("LEGITIMO", saludoMama.categoriaMensaje)

        val avisoOficinaDavivienda = SmishDetectorEngine.analizarMensaje(
            remitente = "Davivienda",
            mensaje = "Davivienda informa que su tarjeta debito fue bloqueada preventivamente en cajero. Acerquese a cualquier oficina con su cedula original."
        )
        assertFalse(avisoOficinaDavivienda.isQuarantined)
        assertEquals(ThreatLevel.SAFE, avisoOficinaDavivienda.level)
        assertEquals(0, avisoOficinaDavivienda.threatScore)
        assertEquals("LEGITIMO", avisoOficinaDavivienda.categoriaMensaje)
    }

    @Test
    fun pruebaSmishingSinEnlacesYRemitentesDesconocidosSeDetectan() {
        val casosMaliciosos = listOf(
            "+573145566778" to "Alerta Bancolombia: Detectamos un retiro en proceso por $850.000. Comuniquese urgente a nuestra linea de seguridad 3145566778 para bloquearlo.",
            "Soporte-Cuenta" to "Centro de Verificacion: Para cancelar el cambio de correo electronico en tu billetera digital responde este SMS con tu clave actual de 4 digitos.",
            "Mesa-Fraudes" to "Alerta PSE: Hay un pago retenido desde su cuenta de ahorros. Dictenos o envie por este chat los 16 digitos de su tarjeta y codigo trasero para anular.",
            "RemitenteNuevo99" to "Banco Caja Social: Le invitamos a validar su token empresarial actualizado ingresando a https://bancocajasocial.com.portal-empresas.net/token",
            "+573219900445" to "Agencia de Mercadeo Digital: Gana $320.000 al dia calificando comercios desde tu telefono. Inicia hoy en https://wa.me/22997112233"
        )

        for ((remitente, mensaje) in casosMaliciosos) {
            val resultado = SmishDetectorEngine.analizarMensaje(remitente = remitente, mensaje = mensaje)
            assertEquals("Debe clasificar como SMISHING: $mensaje", "SMISHING", resultado.categoriaMensaje)
            assertTrue(
                "Debe marcar al menos SUSPICIOUS o PHISHING: $mensaje",
                resultado.level == ThreatLevel.SUSPICIOUS || resultado.level == ThreatLevel.PHISHING
            )
        }
    }

    @Test
    fun pruebaRendimientoInferenciaLocalMenorACincoMilisegundos() {
        com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms.precargarModelo()
        assertTrue(com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms.estaModeloCargado())

        val muestras = obtenerConjuntoPruebaIndependiente()
        for (i in 0 until 10) {
            val caso = muestras[i % muestras.size]
            SmishDetectorEngine.analizarMensaje(caso.remitente, caso.mensaje)
        }

        val iteraciones = 200
        val inicioNs = System.nanoTime()
        for (i in 0 until iteraciones) {
            val caso = muestras[i % muestras.size]
            SmishDetectorEngine.analizarMensaje(caso.remitente, caso.mensaje)
        }
        val duracionTotalMs = (System.nanoTime() - inicioNs) / 1_000_000.0
        val promedioMs = duracionTotalMs / iteraciones
        println("Latencia promedio SmishDetectorEngine (Kotlin JVM): ${"%.4f".format(promedioMs)} ms/SMS")

        assertTrue("La inferencia local debe tardar menos de 5 ms por SMS (actual: $promedioMs ms)", promedioMs < 5.0)
    }

    @Test
    fun pruebaEvaluacionCompleta105MensajesIndependientesEnMotorHibrido() {
        val archivoPrueba = java.io.File("../scripts/datos_prueba_independiente.json")
        assertTrue("Debe existir ../scripts/datos_prueba_independiente.json", archivoPrueba.exists())

        val casosJson = extraerObjetosJsonPlano(archivoPrueba.readText(Charsets.UTF_8))
        assertEquals(105, casosJson.size)

        val clases = listOf("LEGITIMO", "SPAM", "SMISHING")
        val matrizMlKotlin = Array(3) { IntArray(3) }
        val matrizHibrida = Array(3) { IntArray(3) }
        val patronMascaraOtp = Regex("""\bG-\d{4,8}\b|\b\d{3}-\d{3}\b|\b\d{4,8}\b""")

        for (caso in casosJson) {
            val remitente = caso["remitente"] ?: ""
            val mensaje = caso["mensaje"] ?: ""
            val claseReal = caso["clase"] ?: "LEGITIMO"
            val idxReal = clases.indexOf(claseReal)

            val resMl = com.antiphishing.smishguard.domain.detector.ClasificadorMensajesSms.clasificarMensaje(remitente, mensaje)
            matrizMlKotlin[idxReal][clases.indexOf(resMl.categoriaPredicha)]++

            val resHibrido = SmishDetectorEngine.analizarMensaje(remitente, mensaje)
            matrizHibrida[idxReal][clases.indexOf(resHibrido.categoriaMensaje)]++
            if (resHibrido.categoriaMensaje != claseReal) {
                val enmascarado = patronMascaraOtp.replace(mensaje, "[CODIGO_OCULTO]")
                println("  [Dif Hibrido] Real=$claseReal -> Pred=${resHibrido.categoriaMensaje} (score=${resHibrido.threatScore}) | $remitente: $enmascarado")
            }
        }

        fun imprimirYCalcularMacroF1(titulo: String, matriz: Array<IntArray>): Double {
            println("=== $titulo (105 casos independientes) ===")
            var sumaF1 = 0.0
            var aciertos = 0
            for (i in 0 until 3) {
                aciertos += matriz[i][i]
                val vp = matriz[i][i].toDouble()
                val fp = (0 until 3).filter { it != i }.sumOf { matriz[it][i] }.toDouble()
                val fn = (0 until 3).filter { it != i }.sumOf { matriz[i][it] }.toDouble()
                val prec = if (vp + fp > 0) vp / (vp + fp) else 0.0
                val rec = if (vp + fn > 0) vp / (vp + fn) else 0.0
                val f1 = if (prec + rec > 0) 2.0 * prec * rec / (prec + rec) else 0.0
                sumaF1 += f1
                println("  Fila ${clases[i]}: ${matriz[i].contentToString()} | P=${"%.4f".format(prec)} R=${"%.4f".format(rec)} F1=${"%.4f".format(f1)}")
            }
            val macro = sumaF1 / 3.0
            println("  Correctas: $aciertos/105 | Macro-F1: ${"%.4f".format(macro)}")
            return macro
        }

        val macroMl = imprimirYCalcularMacroF1("SOLO MODELO ML EN KOTLIN", matrizMlKotlin)
        val macroHibrido = imprimirYCalcularMacroF1("MOTOR HIBRIDO COMPLETO EN KOTLIN", matrizHibrida)

        assertEquals("El modelo ML en Kotlin debe coincidir exactamente con las 100/105 predicciones de Python", 100, matrizMlKotlin[0][0] + matrizMlKotlin[1][1] + matrizMlKotlin[2][2])
        assertTrue("El Macro-F1 del motor hibrido ($macroHibrido) debe ser >= 0.94", macroHibrido >= 0.94)
        assertTrue("El recall de SMISHING en el motor hibrido debe ser >= 34/35", matrizHibrida[2][2] >= 34)
        assertEquals("Cero mensajes LEGITIMOS clasificados como SMISHING en el motor hibrido", 0, matrizHibrida[0][2])
    }
}
