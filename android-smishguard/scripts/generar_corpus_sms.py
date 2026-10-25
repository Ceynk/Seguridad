import difflib
import json
import os
import re
import unicodedata
from collections import Counter

DIRECTORIO_ACTUAL = os.path.dirname(os.path.abspath(__file__))
RUTA_ENTRENAMIENTO = os.path.join(DIRECTORIO_ACTUAL, "datos_entrenamiento_sms.json")
RUTA_VALIDACION = os.path.join(DIRECTORIO_ACTUAL, "datos_validacion_sms.json")
RUTA_PRUEBA = os.path.join(DIRECTORIO_ACTUAL, "datos_prueba_independiente.json")

CLASES_VALIDAS = {"LEGITIMO", "SPAM", "SMISHING"}

DOMINIOS_INSTITUCIONALES_VERIFICADOS = {
    "bancolombia.com", "grupobancolombia.com", "transaccionesbancolombia.com", "bancolombia.com.co",
    "nequi.com.co", "nequi.com", "daviplata.com", "daviplata.com.co",
    "davivienda.com", "davivienda.com.co", "bancodebogota.com", "bancodebogota.com.co",
    "bancoagrario.gov.co", "bbva.es", "bbva.com", "bbva.com.co",
    "santander.es", "santander.com", "bancosantander.es", "caixabank.es", "caixabank.com",
    "scotiabankcolpatria.com", "colpatria.com", "avvillas.com.co", "bancodeoccidente.com.co",
    "bancocajasocial.com", "itau.co", "bancofalabella.com.co", "lulobank.com", "nu.com.co",
    "movii.com.co", "dale.com.co", "transfiya.com.co", "correos.es", "correos.com",
    "dhl.com", "dhl.es", "dhl.com.co", "seur.com", "fedex.com", "mrw.es",
    "dgt.es", "gob.es", "hacienda.gob.es", "agenciatributaria.es", "seg-social.es",
    "dian.gov.co", "gov.co", "pse.com.co", "achcolombia.com.co",
    "servientrega.com", "servientrega.com.co", "coordinadora.com", "interrapidisimo.com",
    "envia.co", "4-72.com.co", "efecty.com.co", "supergiros.com.co",
    "fcm.org.co", "simit.org.co", "runt.com.co", "runt.gov.co",
    "prosperidadsocial.gov.co", "colpensiones.gov.co", "claro.com.co", "miclaro.com.co",
    "movistar.co", "movistar.com.co", "movistar.es", "tigo.com.co", "wom.co",
    "mercadolibre.com", "mercadolibre.com.co", "exito.com", "alkosto.com",
    "falabella.com.co", "rappi.com.co", "google.com", "google.co", "google.es",
    "accounts.google.com", "apple.com", "icloud.com", "whatsapp.com",
    "microsoft.com", "live.com", "office.com", "netflix.com",
    "amazon.com", "amazon.es", "amazon.co", "paypal.com", "paypal.es", "bizum.es"
}

MARCAS_SENSIBLES = {
    "bancolombia", "nequi", "daviplata", "davivienda", "bbva", "bancodebogota",
    "banco de bogota", "dian", "pse", "simit", "servientrega", "coordinadora",
    "interrapidisimo", "4-72", "colpensiones", "prosperidad social", "transfiya"
}

PATRON_URL = re.compile(r"(?i)\b(?:https?://|hxxps?://|www\.)[^\s<>'\"{}|\\^`]+")


def crear_ejemplo(remitente, mensaje, clase, familia_plantilla):
    """Construye un registro sintético etiquetado explícitamente con metadatos de auditoría."""
    return {
        "remitente": remitente,
        "mensaje": mensaje,
        "clase": clase,
        "origen": "sintetico",
        "familia_plantilla": familia_plantilla,
        "requiere_revision": False
    }


def extraer_host(enlace):
    limpio = enlace.lower().strip().rstrip(".,;:)]!?\"'")
    if "://" in limpio:
        limpio = limpio.split("://", 1)[1]
    limpio = limpio.split("/", 1)[0].split(":", 1)[0]
    if limpio.startswith("www."):
        limpio = limpio[4:]
    return limpio


def es_dominio_institucional(dominio):
    for oficial in DOMINIOS_INSTITUCIONALES_VERIFICADOS:
        if dominio == oficial or dominio.endswith("." + oficial):
            return True
    return False


def quitar_acentos(texto):
    normalizado = unicodedata.normalize("NFD", texto)
    return "".join(c for c in normalizado if unicodedata.category(c) != "Mn")


def normalizar_plantilla(texto):
    sin_urls = PATRON_URL.sub(" <url> ", texto.lower())
    sin_acentos = quitar_acentos(sin_urls)
    sin_numeros = re.sub(r"\d+", " <num> ", sin_acentos)
    solo_palabras = re.sub(r"[^a-z<>_\s]", " ", sin_numeros)
    return " ".join(solo_palabras.split())


def construir_datos_entrenamiento():
    e = crear_ejemplo
    return [
        # === LEGITIMO (80 ejemplos) ===
        # Notificaciones bancarias transaccionales sin enlace
        e("Bancolombia", "Bancolombia le informa compra por $45.000 en SUPERMERCADO EXITO el 25/10/2026 con su tarjeta terminada en 1042.", "LEGITIMO", "leg_compra_comercio_1"),
        e("Bancolombia", "Bancolombia: Retiro exitoso por $200.000 en cajero automatico Unicentro. Si no reconoce la operacion llame al 018000912345.", "LEGITIMO", "leg_retiro_cajero_1"),
        e("Bancolombia", "Bancolombia: Transferencia exitosa por $150.000 hacia cuenta de ahorros terminada en 8821.", "LEGITIMO", "leg_transferencia_enviada_1"),
        e("Bancolombia", "Bancolombia: Le recordamos que el pago de su tarjeta de credito vence el 30 de este mes. Consulte en nuestra app oficial.", "LEGITIMO", "leg_recordatorio_vencimiento_1"),
        e("Nequi", "Nequi: Recibiste $80.000 de Laura Martinez en tu cuenta. Ya estan disponibles en tu saldo.", "LEGITIMO", "leg_recepcion_billetera_1"),
        e("Nequi", "Nequi: Enviaste $35.000 a Carlos Gomez desde tu disponible. Referencia M918273.", "LEGITIMO", "leg_envio_billetera_1"),
        e("Nequi", "Nequi: Retiraste $100.000 en corresponsal bancario. Gracias por usar Nequi.", "LEGITIMO", "leg_retiro_corresponsal_1"),
        e("Daviplata", "Daviplata: Recibiste un giro nacional por $120.000 en tu bolsillo principal.", "LEGITIMO", "leg_giro_recibido_1"),
        e("Davivienda", "Davivienda le informa pago exitoso de servicios publicos por $94.200 desde su cuenta de ahorros.", "LEGITIMO", "leg_pago_servicios_1"),
        e("BBVA", "BBVA: Se ha abonado su nomina mensual por valor de $2.450.000 en su cuenta.", "LEGITIMO", "leg_abono_nomina_1"),
        e("PSE", "PSE: Su pago en Empresas Publicas de Medellin por $115.000 fue aprobado con CUS 91827364.", "LEGITIMO", "leg_pago_pse_1"),
        e("Transfiya", "Transfiya: Tu envio de dinero por $40.000 fue aceptado por el destinatario en su entidad bancaria.", "LEGITIMO", "leg_transfiya_aceptado_1"),
        e("Nu", "Nu: Compra aprobada por $19.900 en SPOTIFY con tu tarjeta de credito morada.", "LEGITIMO", "leg_compra_suscripcion_1"),
        e("LuloBank", "Lulo Bank: Recibiste una devolucion de cashback por $4.500 en tu Lulo Cuenta.", "LEGITIMO", "leg_cashback_1"),
        e("AVVillas", "Banco AV Villas: Se realizo el pago automatico de su obligacion financiera por $320.000.", "LEGITIMO", "leg_debito_automatico_1"),
        e("Scotiabank", "Scotiabank Colpatria: Compra nacional aprobada por $78.000 en HOMECENTER. Linea de atencion 6017561616.", "LEGITIMO", "leg_compra_ferreteria_1"),

        # Códigos OTP y 2FA variados (con y sin advertencia, distintos remitentes)
        e("Bancolombia", "Bancolombia le informa que su clave dinamica es 582910. Valida por 5 minutos. Nunca la comparta con nadie.", "LEGITIMO", "leg_otp_clave_dinamica_1"),
        e("Nequi", "Nequi: Tu codigo de verificacion es 491-820 para entrar a la app. No lo compartas con nadie.", "LEGITIMO", "leg_otp_billetera_1"),
        e("Daviplata", "Daviplata: Tu codigo de seguridad es 738291. Por tu seguridad no entregues esta clave a terceros.", "LEGITIMO", "leg_otp_bolsillo_1"),
        e("BancoBogota", "Banco de Bogota: Su clave token para autorizar el pago es 839102. No la comparta con nadie.", "LEGITIMO", "leg_otp_token_1"),
        e("Google", "G-482910 es tu codigo de verificacion de Google. No lo compartas con nadie.", "LEGITIMO", "leg_otp_google_1"),
        e("890123", "Tu codigo de seguridad de Microsoft es 774819.", "LEGITIMO", "leg_otp_corto_sin_aviso_1"),
        e("WhatsApp", "Codigo de WhatsApp: 301-948. No compartas este codigo con nadie.", "LEGITIMO", "leg_otp_mensajeria_1"),
        e("Apple", "Tu codigo de ID de Apple es 550192. No lo compartas con nadie.", "LEGITIMO", "leg_otp_apple_1"),
        e("85432", "Codigo de acceso temporal 4419 para iniciar sesion en Netflix.", "LEGITIMO", "leg_otp_streaming_1"),
        e("Amazon", "883920 es tu contrasena de un solo uso (OTP) de Amazon. No la compartas con nadie.", "LEGITIMO", "leg_otp_ecommerce_1"),
        e("PayPal", "PayPal: Tu codigo de seguridad es 671209. Expira en 10 minutos. No lo compartas.", "LEGITIMO", "leg_otp_pagos_1"),
        e("87900", "Codigo de confirmacion 619044 para firmar documentos electronicos en Camara de Comercio.", "LEGITIMO", "leg_otp_firma_1"),

        # Alertas de seguridad bancarias legítimas (sin enlaces ni solicitud de claves, parecen sospechosas)
        e("Bancolombia", "Bancolombia: Nunca entregues tu usuario, clave principal ni clave dinamica por llamadas o mensajes. El banco jamas solicita estos datos.", "LEGITIMO", "leg_educacion_seguridad_1"),
        e("BBVA", "BBVA informa: Bloqueamos preventivamente su tarjeta en cajero por tres intentos de PIN errado. Solicite reposicion en cualquier sucursal fisica con su documento.", "LEGITIMO", "leg_bloqueo_oficina_1"),
        e("Nequi", "Nequi: Detectamos un inicio de sesion en tu cuenta desde un Xiaomi Redmi. Si fuiste tu, ignora este mensaje; de lo contrario cambia tu clave desde la app.", "LEGITIMO", "leg_alerta_nuevo_dispositivo_1"),
        e("BancoBogota", "Banco de Bogota: Actualizamos con exito su tope diario de transferencias desde la Banca Movil. Si no realizo este ajuste acerquese a una oficina.", "LEGITIMO", "leg_cambio_tope_1"),

        # Enlaces oficiales institucionales
        e("Davivienda", "Davivienda: Consulte su extracto mensual disponible en el portal oficial https://notificaciones.davivienda.com", "LEGITIMO", "leg_url_oficial_extracto_1"),
        e("Bancolombia", "Bancolombia: Consulte sus movimientos de forma segura en https://sucursalpersonas.transaccionesbancolombia.com", "LEGITIMO", "leg_url_oficial_sucursal_1"),
        e("Servientrega", "Servientrega: Su envio con guia 209384712 fue entregado satisfactoriamente en porteria. Rastreelo en https://www.servientrega.com", "LEGITIMO", "leg_url_oficial_guia_1"),
        e("Coordinadora", "Coordinadora: Tu paquete con guia 88192031 esta en reparto hoy. Consulta estado en https://coordinadora.com", "LEGITIMO", "leg_url_oficial_reparto_1"),
        e("Correos", "Correos: Su paquete ha sido entregado hoy. Consulte el seguimiento en https://correos.es", "LEGITIMO", "leg_url_oficial_correos_1"),
        e("MercadoLibre", "Mercado Libre: Tu pedido de audifonos inalambricos llegara hoy entre las 10:00 y las 17:00. Revisa en https://www.mercadolibre.com.co", "LEGITIMO", "leg_url_oficial_pedido_1"),
        e("DIAN", "DIAN: Recuerde presentar su declaracion de renta segun el ultimo digito de su NIT en https://www.dian.gov.co", "LEGITIMO", "leg_url_oficial_dian_1"),
        e("Colpensiones", "Colpensiones: Descargue su certificacion de afiliacion ingresando a la sede electronica https://www.colpensiones.gov.co", "LEGITIMO", "leg_url_oficial_pension_1"),

        # Enlaces cotidianos legítimos NO incluidos en listas bancarias (rompe atajo _enlace_no_oficial_)
        e("+573105551234", "Hola equipo, les comparto el enlace de Zoom para la sustentacion de tesis hoy a las 4 pm: https://zoom.us/j/948172635", "LEGITIMO", "leg_url_videollamada_1"),
        e("+573158899001", "Buenas tardes ingeniero, ya subi los cambios del backend al repositorio https://github.com/equipo-grado/detector-sms", "LEGITIMO", "leg_url_repositorio_1"),
        e("Universidad", "Registro Academico: La inscripcion de asignaturas del semestre inicia manana en https://sia.unal.edu.co", "LEGITIMO", "leg_url_universidad_1"),
        e("Laboratorio", "IDIME: Sus resultados de examenes de sangre ya se encuentran listos para consulta en https://resultados.idime.com.co/portal", "LEGITIMO", "leg_url_salud_1"),
        e("+573004411223", "Amor mira la carta del restaurante italiano para reservar esta noche: https://ilforno.co/menu-digital", "LEGITIMO", "leg_url_restaurante_1"),
        e("+573112299884", "Vecinos, la alcaldia publico el cronograma de racionamiento de agua de esta semana en https://bogota.gov.co/servicios", "LEGITIMO", "leg_url_alcaldia_1"),
        e("+573167788992", "Companeros, por favor llenen la asistencia del laboratorio en el documento compartido https://docs.google.com/forms/d/1xYzAbC", "LEGITIMO", "leg_url_formulario_clase_1"),
        e("+573203344551", "Hermano lee este articulo sobre las becas de posgrado en ingenieria: https://elespectador.com/educacion/becas-2026", "LEGITIMO", "leg_url_prensa_1"),
        e("TuBoleta", "TuBoleta: Tus entradas digitales para el concierto sinfonico del sabado estan listas en https://tuboleta.com/mis-boletas", "LEGITIMO", "leg_url_boletas_1"),
        e("ClinicaVet", "Veterinaria San Roque: El carnet de vacunacion de Toby quedo actualizado en https://sanroquevet.com.co/mascotas", "LEGITIMO", "leg_url_veterinaria_1"),
        e("+573146655221", "Profe buenos dias, le dejo el enlace de la reunion en Meet para revisar el anteproyecto: https://meet.google.com/abc-defg-hij", "LEGITIMO", "leg_url_meet_1"),
        e("Biblioteca", "Red de Bibliotecas: Renueve el prestamo de sus libros de calculo en linea desde https://biblored.gov.co/catalogo", "LEGITIMO", "leg_url_biblioteca_1"),

        # Conversaciones personales, familiares, médicas y laborales variadas (incluyendo palabras ambiguas)
        e("+573104455667", "Hola amor, ya sali de la oficina, paso por el supermercado y llego a la casa en media hora.", "LEGITIMO", "leg_conv_regreso_casa_1"),
        e("+573001122334", "Hola mama, ya llegue bien a la universidad, cuando salga del parcial te llamo.", "LEGITIMO", "leg_conv_saludo_mama_1"),
        e("+573129988112", "Hola papa, me puedes pasar por favor el numero de la cuenta de ahorros para enviarte lo del arriendo?", "LEGITIMO", "leg_conv_cuenta_familiar_1"),
        e("+573145566778", "Amiga, necesito urgente que me pases la clave del wifi de tu apartamento para conectar el portatil.", "LEGITIMO", "leg_conv_urgente_wifi_1"),
        e("+573187766554", "Hijo, acuerdate de pagar urgente el recibo del gas hoy antes de las 5 para evitar reconexion.", "LEGITIMO", "leg_conv_urgente_recibo_1"),
        e("+573013344556", "Hola jefe, el proveedor ya aprobo la orden de compra y confirmo que despacha la mercancia el viernes.", "LEGITIMO", "leg_conv_trabajo_orden_1"),
        e("+573126655443", "Ya deje el paquete con el portero del edificio, cualquier novedad me escribes.", "LEGITIMO", "leg_conv_porteria_1"),
        e("+573164433221", "Nos vemos a las 6 en la estacion de Transmilenio para irnos juntos a clase.", "LEGITIMO", "leg_conv_transporte_1"),
        e("+573045566112", "Te mande al correo institucional las diapositivas de arquitectura de software.", "LEGITIMO", "leg_conv_estudio_1"),
        e("+573137788990", "Prima, muchas gracias por acompanarnos ayer en el grado, pasamos muy feliz.", "LEGITIMO", "leg_conv_agradecimiento_1"),
        e("+573172211009", "Buenas tardes, soy el domiciliario de la drogueria, estoy en la entrada principal de la unidad.", "LEGITIMO", "leg_conv_domiciliario_1"),
        e("+573216549870", "Confirmada la reserva de la cancha sintetica hoy a las 8 pm, traigan petos.", "LEGITIMO", "leg_conv_deporte_1"),
        e("+573209988776", "Vecino buenas noches, dejaron la ventana del carro abierta en el sotano dos.", "LEGITIMO", "leg_conv_vecino_carro_1"),
        e("+573105551122", "Me avisas apenas aterrice el avion para salir a recogerte al aeropuerto.", "LEGITIMO", "leg_conv_aeropuerto_1"),
        e("+573156662233", "No olvides comprar huevos, cafe y queso para el desayuno de manana.", "LEGITIMO", "leg_conv_mercado_1"),
        e("+573007773344", "Voy quince minutos tarde porque cerraron un carril en la avenida Boyaca.", "LEGITIMO", "leg_conv_trafico_1"),
        e("+573201115566", "Hermano, ya te transferi por Nequi los 25 mil pesos de la cuota del regalo.", "LEGITIMO", "leg_conv_cuota_amigos_1"),

        # Servicios públicos, salud, aerolíneas y recordatorios institucionales sin enlace
        e("Sura", "EPS Sura: Recordatorio de cita de medicina general manana a las 08:30 AM en sede Industriales.", "LEGITIMO", "leg_cita_medica_1"),
        e("Sanitas", "EPS Sanitas: Su autorizacion para toma de radiografia fue aprobada y enviada a su correo registrado.", "LEGITIMO", "leg_autorizacion_eps_1"),
        e("Compensar", "Compensar: Su cita odontologica con el Dr. Ramirez quedo confirmada para el jueves a las 3:00 PM.", "LEGITIMO", "leg_cita_odontologia_1"),
        e("Rappi", "Rappi: Tu Rappitendero Andres ya recogio tu pedido en el restaurante y va en camino.", "LEGITIMO", "leg_estado_domicilio_1"),
        e("Avianca", "Avianca: El check-in para tu vuelo AV9512 Bogota - Medellin ya esta abierto en nuestra aplicacion.", "LEGITIMO", "leg_vuelo_checkin_1"),
        e("Enel", "Enel Colombia: Hemos recibido el pago de su factura de energia del mes de octubre. Gracias por estar al dia.", "LEGITIMO", "leg_confirmacion_energia_1"),
        e("EPM", "EPM: Informamos interrupcion programada de acueducto este domingo de 8:00 AM a 2:00 PM en su barrio.", "LEGITIMO", "leg_mantenimiento_servicio_1"),
        e("Claro", "Claro: Hemos recibido el pago de tu factura hogar por $102.000. Gracias por tu puntualidad.", "LEGITIMO", "leg_pago_telecom_1"),
        e("Movistar", "Movistar: Tu recarga de $10.000 fue exitosa. Tu paquete todo incluido tiene vigencia de 7 dias.", "LEGITIMO", "leg_recarga_exitosa_1"),
        e("Tigo", "Tigo: El pago de tu plan pospago fue aplicado correctamente. Consulta tu factura en Mi Tigo.", "LEGITIMO", "leg_pago_pospago_1"),
        e("Interrapidisimo", "Interrapidisimo: Hemos entregado tu envio 700192834 en la direccion registrada a nombre de Juliana Rios.", "LEGITIMO", "leg_entrega_sin_url_1"),

        # === SPAM (80 ejemplos: con y sin enlaces comerciales verificados) ===
        e("Claro", "Promo Claro: Pasate a pospago hoy y recibe 50% de descuento durante 6 meses mas gigas libres. Aplica TyC.", "SPAM", "spam_telecom_pospago_1"),
        e("Claro", "Claro Musica: Escucha tus listas favoritas sin descontar datos de tu plan. Activa el mes de cortesia en la app.", "SPAM", "spam_telecom_musica_1"),
        e("85123", "Hoy en Claro recibe el doble de gigas por tu recarga desde $6.000 en puntos autorizados.", "SPAM", "spam_telecom_recarga_1"),
        e("Movistar", "Movistar: Estrena celular 5G con 30% de descuento en tiendas oficiales Movistar. Unidades limitadas.", "SPAM", "spam_telecom_equipos_1"),
        e("Movistar", "Oferta exclusiva Movistar Fibra: 900 megas simetricas con tarifa preferencial los primeros 4 meses en https://movistar.co", "SPAM", "spam_telecom_fibra_1"),
        e("Tigo", "Tigo: Activa hoy tu paquete ilimitado de fin de semana por solo $5.000 y navega sin parar.", "SPAM", "spam_telecom_finde_1"),
        e("Tigo", "Beneficios Tigo: Disfruta entradas 2x1 en cartelera todos los martes presentando tu factura al dia.", "SPAM", "spam_telecom_cine_1"),
        e("WOM", "WOM: Trae tu linea con portabilidad y lleva el segundo mes sin costo en planes desde $35.000. Info en https://wom.co", "SPAM", "spam_telecom_portabilidad_1"),
        e("Exito", "Aniversario Exito: Aprovecha hasta 40% de descuento en televisores, neveras y lavadoras este fin de semana en https://www.exito.com", "SPAM", "spam_retail_electro_1"),
        e("PuntosCol", "Puntos Colombia: Tienes 2.400 puntos listos para redimir en mercado, combustible y aliados.", "SPAM", "spam_fidelidad_puntos_1"),
        e("Alkosto", "Hiperahorro Alkosto: Hoy sabado lleva llantas y portatiles con precios especiales en https://www.alkosto.com", "SPAM", "spam_retail_alkosto_1"),
        e("Falabella", "Falabella: Madrugon de moda con hasta 50% de descuento en ropa y calzado seleccionado en https://falabella.com.co", "SPAM", "spam_moda_falabella_1"),
        e("CineColombia", "Cine Colombia: Este miercoles disfruta todas las funciones a mitad de precio pagando con tu tarjeta Cineco en https://cinecolombia.com", "SPAM", "spam_entretenimiento_cine_1"),
        e("DominoPizza", "Dominos: Martes de 2x1 en pizzas medianas y grandes pidiendo por nuestra app oficial.", "SPAM", "spam_comida_pizza_1"),
        e("KFC", "KFC: Comparte en familia hoy nuestro Mega Fest con 25% de ahorro en domicilios.", "SPAM", "spam_comida_pollo_1"),
        e("Rappi", "Rappi: Tienes un cupon de envio sin costo para tu proximo pedido en restaurantes aliados hoy.", "SPAM", "spam_delivery_cupon_1"),
        e("Bodytech", "Bodytech: Inscribete este mes al plan anual de entrenamiento con 35% de descuento y valoracion incluida.", "SPAM", "spam_gimnasio_anual_1"),
        e("SmartFit", "Smart Fit: Entrena desde $69.900 al mes sin clausula de permanencia. Conoce sedes en https://smartfit.com.co", "SPAM", "spam_gimnasio_mensual_1"),
        e("Pepeganga", "Pepeganga: Gran feria infantil con descuentos del 30% en jugueteria en tiendas fisicas.", "SPAM", "spam_juguetes_feria_1"),
        e("Panamericana", "Panamericana: Temporada escolar con 20% de descuento en cuadernos, morrales y literatura en https://panamericana.com.co", "SPAM", "spam_papeleria_escolar_1"),
        e("Decameron", "Hoteles Decameron: Reserva tus vacaciones todo incluido en Baru o San Andres con tarifas de preventa en https://decameron.com", "SPAM", "spam_turismo_hotel_1"),
        e("Latam", "LATAM Airlines: Vuela por Colombia desde $99.900 por trayecto comprando en https://latamairlines.com", "SPAM", "spam_viajes_tiquetes_1"),
        e("CruzVerde", "Droguerias Cruz Verde: Hoy 25% de ahorro en dermocosmetica y bloqueadores solares para clientes club.", "SPAM", "spam_farmacia_dermo_1"),
        e("Farmatodo", "Farmatodo: Aprovecha nuestros dias azules con promocion 3x2 en cuidado capilar en https://farmatodo.com.co", "SPAM", "spam_farmacia_3x2_1"),
        e("Homecenter", "Homecenter: Renueva tu sala y cocina con hasta 35% de ahorro en muebles e iluminacion en https://homecenter.com.co", "SPAM", "spam_hogar_muebles_1"),
        e("Totto", "Totto Loco: Descuentos de hasta el 50% en maletas de viaje y morrales universitarios en https://totto.com", "SPAM", "spam_moda_morrales_1"),
        e("ArturoCalle", "Arturo Calle: Nueva coleccion de chaquetas y camisas de lino disponible en tiendas del pais y https://arturocalle.com", "SPAM", "spam_moda_masculina_1"),
        e("StudioF", "Studio F: Fin de semana de rebajas con 30% de descuento en la segunda prenda en centros comerciales.", "SPAM", "spam_moda_femenina_1"),
        e("Auteco", "Auteco Mobility: Estrena motocicleta electrica este mes con bono matricula de $400.000 en concesionarios.", "SPAM", "spam_vehiculos_moto_1"),
        e("DirecTV", "DIRECTV: Suscribete a DGO y vive la liga deportiva con 20% de descuento el primer trimestre.", "SPAM", "spam_streaming_deportes_1"),
        e("OpenEnglish", "Open English: Clases de ingles en vivo las 24 horas con 60% de descuento en tu inscripcion online.", "SPAM", "spam_educacion_idiomas_1"),
        e("Platzi", "Platzi: Impulsa tu perfil profesional en ciencia de datos con tarifa preferencial anual en https://platzi.com", "SPAM", "spam_educacion_tech_1"),
        e("Olimpica", "Supertiendas Olimpica: Miercoles de plaza con precios bajos en frutas, verduras y lacteos frescos.", "SPAM", "spam_supermercado_plaza_1"),
        e("D1", "Tiendas D1: Descubre los productos extraordinarios de cocina de esta quincena en tu tienda mas cercana.", "SPAM", "spam_supermercado_d1_1"),
        e("Ara", "Tiendas Ara: Esta semana ahorra en productos de aseo y despensa con nuestras rebajas imperdibles.", "SPAM", "spam_supermercado_ara_1"),
        e("Colsubsidio", "Supermercados Colsubsidio: Martes de 20% de descuento para afiliados en carnes y granos seleccionados.", "SPAM", "spam_supermercado_caja_1"),
        e("MacCenter", "Mac Center: Lleva tu nuevo iPad o MacBook con 0% de interes a 12 cuotas en tiendas autorizadas.", "SPAM", "spam_tech_apple_1"),
        e("Ktronix", "Ktronix: Quincena tecnologica con precios rebajados en barras de sonido y consolas en https://ktronix.com", "SPAM", "spam_tech_consolas_1"),
        e("Locatel", "Locatel: Semana del bienestar con 20% de ahorro en tensiometros y cuidado en casa.", "SPAM", "spam_salud_equipos_1"),
        e("BurgerKing", "Burger King: Usa tus cupones digitales en mostrador y lleva dos combos Whopper por $34.900.", "SPAM", "spam_comida_hamburguesa_1"),
        e("Crepes", "Crepes y Waffles: Conoce los nuevos helados artesanales de temporada en nuestros puntos a nivel nacional.", "SPAM", "spam_comida_postres_1"),
        e("JuanValdez", "Juan Valdez Cafe: Redime tus puntos Amigos y recibe 15% de ahorro en bebidas frias hoy.", "SPAM", "spam_comida_cafe_1"),
        e("Frisby", "Frisby: Pide hoy nuestra picada familiar apanada con precio de lanzamiento en https://frisby.com.co", "SPAM", "spam_comida_frisby_1"),
        e("Tennis", "Tennis: Liquidacion de mitad de temporada con hasta 40% menos en jeans y camisetas en https://tennis.com.co", "SPAM", "spam_moda_juvenil_1"),
        e("Velez", "Cueros Velez: Recibe un bono regalo del 20% en botas y bolsos de cuero presentando este SMS en tienda.", "SPAM", "spam_moda_cuero_1"),
        e("Koaj", "KOAJ: Ultimos 3 dias de precios azules en chaquetas acolchadas y hoodies desde $59.900 en https://koaj.com.co", "SPAM", "spam_moda_koaj_1"),
        e("Jumbo", "Tiendas Jumbo: Fin de semana de vinos y quesos importados con 30% de descuento pagando con Cencosud.", "SPAM", "spam_supermercado_jumbo_1"),
        e("Carulla", "Carulla FreshMarket: Hoy viernes 25% de ahorro en panaderia artesanal y pescaderia para clientes SuperCliente.", "SPAM", "spam_supermercado_carulla_1"),
        e("Bosi", "Bosi: Nueva linea de calzado urbano ya en vitrinas. Lleva el segundo par con 40% de descuento.", "SPAM", "spam_calzado_bosi_1"),
        e("Gef", "GEF: Arma tu pinta basica con 3x2 en camisetas y medias seleccionadas en tiendas y https://gef.co", "SPAM", "spam_moda_gef_1"),
        e(" LiliPink", "Lili Pink: Dias rosados con ofertas desde $14.900 en cuidado corporal y pijamas en puntos fisicos.", "SPAM", "spam_moda_lilipink_1"),
        e(" Offcorss", "OFFCORSS: Viste a los ninos para vacaciones con 30% de descuento en vestidos de bano en https://offcorss.com", "SPAM", "spam_moda_infantil_1"),
        e("87411", "Centro Comercial Santafe: Este sabado gran trasnochon de marcas con parqueadero sin costo despues de las 6 pm.", "SPAM", "spam_evento_cc_1"),
        e("89201", "Feria del Libro en Corferias: Presenta tu carnet estudiantil en taquilla y obten 25% de descuento en tu boleta.", "SPAM", "spam_evento_feria_1"),
        e("ColchonesElDorado", "Colchones El Dorado: Cambia tu colchon hoy con hasta 45% de descuento mas almohadas de obsequio.", "SPAM", "spam_hogar_colchones_1"),
        e("Tugo", "Tugo: Dias de diseno para tu estudio con 25% menos en escritorios y sillas ergonomicas en https://tugo.co", "SPAM", "spam_hogar_escritorios_1"),
        e("OpticaCol", "Lafam: Agenda tu examen visual sin costo al comprar tu montura con 30% de descuento este mes.", "SPAM", "spam_salud_optica_1"),
        e("DentalPlus", "Sonria: Valoracion de ortodoncia sin costo y hasta 35% de descuento en blanqueamiento laser este mes.", "SPAM", "spam_salud_odontologia_1"),
        e("PepsiPromo", "Destapa y participa en puntos autorizados: reune tres tapas marcadas y canjealas por un vaso coleccionable en tienda.", "SPAM", "spam_canje_tienda_1"),
        e("Spotify", "Spotify Premium: Vuelve a disfrutar musica sin anuncios durante 3 meses por $16.900. Cancela cuando quieras en la app.", "SPAM", "spam_streaming_musica_1"),
        e("HBO", "Max: Suscribete al plan anual y ahorra 33% para ver tus series y peliculas favoritas en casa.", "SPAM", "spam_streaming_series_1"),
        e("PapaJohns", "Papa Johns: Jueves de pizza familiar de un ingrediente con borde de queso y gaseosa con 30% de descuento.", "SPAM", "spam_comida_papajohns_1"),
        e("Subway", "Subway: Aprovecha el Sub del dia de 15 cm mas bebida por solo $14.900 en restaurantes participantes.", "SPAM", "spam_comida_subway_1"),
        e("ElCorral", "El Corral: Prueba nuestra nueva hamburguesa doble en combo con papas rusticas a precio de lanzamiento.", "SPAM", "spam_comida_hamburguesa_2"),
        e("Starbucks", "Starbucks Rewards: Hoy acumulas doble estrella en todas tus bebidas preparadas en barra.", "SPAM", "spam_cafe_estrellas_1"),
        e("Adidas", "Adidas: Miembros adiClub reciben 20% adicional en calzado de running este fin de semana en https://adidas.co", "SPAM", "spam_deporte_calzado_1"),
        e("Decathlon", "Decathlon: Equipa tu salida de camping con carpas y linternas desde $49.900 en https://decathlon.com.co", "SPAM", "spam_deporte_camping_1"),
        e(" LibreriaNacional", "Libreria Nacional: 15% de descuento en novelas historicas durante todo octubre en sedes fisicas.", "SPAM", "spam_libros_novelas_1"),
        e("Chevignon", "Chevignon: Bono de $50.000 redimible en tu compra superior a $250.000 en chaquetas de denim en tienda.", "SPAM", "spam_moda_bono_tienda_1"),
        e(" PullBear", "Pull and Bear: Rebajas de temporada de hasta 30% en abrigos y calzado casual en tiendas seleccionadas.", "SPAM", "spam_moda_casual_1"),
        e("Wingo", "Wingo: Tarifas promocionales para volar a Aruba y Panama desde $420.000 ida y regreso en https://wingo.com", "SPAM", "spam_viajes_caribe_1"),
        e("Ikea", "IKEA Colombia: Descubre ideas para organizar tu habitacion desde $19.900 en tienda y https://ikea.com.co", "SPAM", "spam_hogar_ikea_1"),
        e("89100", "Club Vivamos El Tiempo: Presenta tu tarjeta de suscriptor y recibe 20% de descuento en restaurantes aliados.", "SPAM", "spam_club_suscriptor_1"),
        e("Terpel", "Puntos Colombia y Terpel: Tanquea hoy desde $40.000 y recibe el triple de puntos registrando tu cedula en estacion.", "SPAM", "spam_combustible_puntos_1"),
        e("Texaco", "Estaciones Texaco: Por cada cambio de aceite sintetico reclama gratis un ambientador vehicular en isla.", "SPAM", "spam_combustible_aceite_1"),
        e("Miniso", "Miniso: Conoce la nueva coleccion de organizadores y peluches con precios desde $12.900 en tienda.", "SPAM", "spam_variedades_miniso_1"),
        e("Dollarcity", "Dollarcity: Ya llegaron los articulos decorativos de navidad para tu mesa y sala en todas las sedes.", "SPAM", "spam_hogar_decoracion_1"),
        e("Cinemark", "Cinemark: Compra tu combo crispetas grandes en preventa desde la app y recibe recarga sin costo.", "SPAM", "spam_cine_crispetas_1"),
        e("Colombiana", "Festival Gastronomico en el Parque de la 93 este sabado y domingo: entrada libre para toda la familia.", "SPAM", "spam_evento_gastronomico_1"),
        e("Coursera", "Coursera: Obten 25% de ahorro en tu certificado profesional de gestion de proyectos durante esta semana.", "SPAM", "spam_educacion_certificado_1"),

        # === SMISHING (80 ejemplos: con URLs engañosas variadas, subdominios, acortadores, redactados calmados y SIN enlaces) ===
        # Bancos y billeteras con URLs maliciosas variadas
        e("Bancolombia", "Bancolombia: Su cuenta ha sido bloqueada preventivamente por intentos fallidos. Reactive su clave en https://bancolombia-seguridad.xyz/login", "SMISHING", "smish_banco_bloqueo_xyz_1"),
        e("Bancolombia", "Bancolombia: Detectamos un acceso no autorizado desde otro dispositivo. Verifique su identidad urgente en https://sucursal-virtual-personas.top", "SMISHING", "smish_banco_acceso_top_1"),
        e("+573009182736", "Bancolombia: Se registro una compra por $980.000 en falabella. Si no la reconoce anulela de inmediato en https://anular-transaccion-col.com/login", "SMISHING", "smish_banco_compra_falsa_1"),
        e("+573128374650", "Aviso Bancolombia: Su clave dinamica sera desactivada hoy. Actualice sus datos de seguridad en http://bit.ly/3xBancol", "SMISHING", "smish_banco_acortador_1"),
        e("Bancolombia", "Bancolombia: Evite el cobro del seguro de proteccion de tarjetas por $89.900 cancelando la suscripcion aqui: https://cancelar-seguro-banco.online", "SMISHING", "smish_banco_seguro_1"),
        e("Nequi", "Nequi: Tu cuenta fue suspendida por movimientos inusuales. Ingresa tus datos para desbloquearla en https://nequi-desbloqueo.pages.dev/auth", "SMISHING", "smish_billetera_hosting_1"),
        e("+573154433221", "Nequi: Tienes un abono retenido por $350.000 pendiente de aceptacion. Confirma tu cuenta en https://nequi-recargas-col.xyz/aceptar", "SMISHING", "smish_billetera_abono_1"),
        e("Nequi-Info", "Nequi: Por actualizacion de seguridad debes validar tu clave de 4 digitos antes de 24 horas en https://validacion-nequi.lat", "SMISHING", "smish_billetera_clave4_1"),
        e("+573201928374", "Nequi: Ganaste el bono de fidelidad de $250.000 para usuarios activos. Reclama tu desembolso ya en https://bono-nequi.vercel.app", "SMISHING", "smish_billetera_bono_1"),
        e("Daviplata", "Daviplata: Su bolsillo ha sido bloqueado temporalmente. Ingrese su clave en https://daviplata-verifica.top/login para reactivar.", "SMISHING", "smish_bolsillo_bloqueo_1"),
        e("+573119988223", "Daviplata: Eres beneficiario del subsidio de ingreso solidario por $420.000. Registra tu cedula para el retiro en https://subsidio-daviplata.xyz", "SMISHING", "smish_subsidio_estatal_1"),
        e("Daviplata", "Daviplata: Detectamos un retiro sospechoso en tramite. Si no fue usted cancele la transaccion en http://185.220.101.45/daviplata", "SMISHING", "smish_ip_publica_1"),
        e("Davivienda", "Davivienda: Su tarjeta de credito presenta bloqueo preventivo. Valide sus credenciales inmediatamente en https://davivienda-clientes.duckdns.org/auth", "SMISHING", "smish_tunel_duckdns_1"),
        e("+573147766551", "Davivienda: Alerta de transferencia pendiente por $1.500.000. Para rechazar este debito ingrese a https://portal-seguro-davivienda.site", "SMISHING", "smish_rechazar_debito_1"),
        e("BBVA", "BBVA: Su cuenta ha sido restringida por falta de actualizacion de datos. Evite el cierre ingresando a https://bbva-alerta.duckdns.org/login", "SMISHING", "smish_cierre_cuenta_1"),
        e("+573182233445", "BBVA Colombia: Tienes puntos por vencer equivalentes a $310.000 en efectivo. Redimelos hoy en https://puntos-bbva-col.click", "SMISHING", "smish_puntos_efectivo_1"),
        e("BancoBogota", "Banco de Bogota: Su token de seguridad presenta fallas de sincronizacion. Reactive su banca movil en https://bancobogota-seguridad.xyz", "SMISHING", "smish_token_desincronizado_1"),

        # Subdominios engañosos y typosquatting (redactados de manera corporativa / convincente sin "urgente")
        e("Bancolombia", "Estimado cliente, le invitamos a confirmar la vinculacion de su dispositivo movil en https://bancolombia.com.co.gestion-clientes.net/acceso", "SMISHING", "smish_subdominio_engano_1"),
        e("Davivienda", "Informamos que su certificado digital bancario requiere renovacion periodica a traves de https://davivienda.com.portal-verificacion.org/renovar", "SMISHING", "smish_subdominio_certificado_1"),
        e("Scotiabank", "Scotiabank Colpatria: Tiene una nota credito pendiente de aplicacion en su extracto. Confirme la cuenta destino en https://scotiabank-colpatria-abonos.com/cliente", "SMISHING", "smish_corporativo_nota_1"),
        e("CajaSocial", "Banco Caja Social: Por normativa financiera requerimos confirmar su correo y clave transaccional en https://bancocajasoclal.com/datos", "SMISHING", "smish_typosquatting_1"),
        e("NuColombia", "Hola, notamos una inconsistencia en la facturacion de tu tarjeta Nu. Revisa y ajusta el cobro en https://nu-colombia-ayuda.com/estado", "SMISHING", "smish_tono_amable_nu_1"),
        e("PayPal", "PayPal: Hemos limitado temporalmente algunas funciones de su cuenta comercial. Complete la revision documental en https://paypal-resoluciones.com/cuenta", "SMISHING", "smish_tono_corporativo_paypal_1"),

        # Entidades públicas, impuestos, tránsito y servicios
        e("DIAN-Alerta", "DIAN: Notificacion de cobro coactivo. Su cuenta bancaria presenta orden de embargo por mora tributaria. Consulte resolucion en https://dian-tramites.xyz/pago", "SMISHING", "smish_dian_embargo_1"),
        e("+573018877665", "DIAN: Usted tiene una sancion pendiente por inconsistencias en facturacion electronica. Pague con descuento hoy en https://portal-dian-impuestos.top", "SMISHING", "smish_dian_facturacion_1"),
        e("DIAN", "DIAN: Tienes una devolucion de saldo a favor de IVA por $380.000. Inscribe tu cuenta bancaria para el giro en https://devolucion-dian.pages.dev", "SMISHING", "smish_dian_devolucion_1"),
        e("PSE", "PSE: Transaccion aprobada por $450.000 COP en comercio internacional. Si no reconoce este debito cancele en http://192.168.1.80/pse", "SMISHING", "smish_pse_ip_1"),
        e("PSE-Notifica", "PSE: Se detecto un intento de debito automatico desde su cuenta de ahorros. Anule el cobro en https://pse-cancelacion-pagos.xyz", "SMISHING", "smish_pse_intento_1"),
        e("SIMIT", "SIMIT: Usted registra una fotomulta pendiente de pago con ultimo dia de descuento del 50% antes de embargo. Pague aqui: https://simit-comparendos.top", "SMISHING", "smish_simit_fotomulta_1"),
        e("+573165544332", "Secretaria de Movilidad: Evite la inmovilizacion de su vehiculo y reporte en cobro coactivo pagando su comparendo en https://consulta-multas-col.online", "SMISHING", "smish_movilidad_inmovilizacion_1"),
        e("Prosperidad", "Prosperidad Social: Usted salio favorecido en el nuevo ciclo de Renta Ciudadana por $500.000. Consulte su giro en https://renta-ciudadana-consulta.xyz", "SMISHING", "smish_prosperidad_renta_1"),
        e("DGT", "DGT: Sancion administrativa por exceso de velocidad. Ultimo aviso de pago antes de recargo en http://192.168.1.50/dgt", "SMISHING", "smish_dgt_multa_1"),
        e("Colpass", "Colpass: Su paso por peaje electronico fue rechazado por saldo insuficiente. Pague hoy para evitar comparendo en https://peajes-colpass-pago.xyz", "SMISHING", "smish_peaje_colpass_1"),
        e("Enel-Aviso", "Enel: Orden de corte de energia programada para hoy por factura vencida. Pague de inmediato y evite suspension en https://pagos-enel-col.top", "SMISHING", "smish_servicio_energia_1"),
        e("Acueducto", "Acueducto: Su medidor registra mora de dos periodos y suspension manana. Liquide el saldo pendiente en https://acueducto-pagos-enlinea.site", "SMISHING", "smish_servicio_agua_1"),
        e("Colpensiones", "Colpensiones: Detectamos semanas faltantes en su historial pensional. Corrija sus aportes subiendo su cedula en https://colpensiones-revision.com/formulario", "SMISHING", "smish_pension_semanas_1"),

        # Paquetería, aduanas y operadores móviles
        e("Servientrega", "Servientrega: Su paquete no pudo ser entregado por direccion incompleta. Actualice sus datos y pague el reenvio en https://servientrega-guia.xyz/envio", "SMISHING", "smish_paquete_direccion_1"),
        e("+573024455667", "Coordinadora: Su encomienda se encuentra retenida en bodega central por falta de pago de impuestos. Libere su guia en https://coordinadora-rastreo.top", "SMISHING", "smish_paquete_bodega_1"),
        e("Interrapidisimo", "Interrapidisimo: Intento de entrega fallido hoy. Confirme su direccion y pague el recargo logistico de $6.800 en https://interrapidisimo-entrega.site", "SMISHING", "smish_paquete_recargo_1"),
        e("4-72", "4-72 Envios: Su paquete internacional presenta retencion en aduanas. Pague la tasa aduanera en menos de 24 horas en https://aduanas-472-colombia.xyz", "SMISHING", "smish_aduana_472_1"),
        e("Correos", "Correos: Paquete retenido por falta de pago de tasa de aduana. Realice el pago urgente en https://entrega-correos.xyz/pago", "SMISHING", "smish_aduana_correos_1"),
        e("DHL-Express", "DHL: Su importacion requiere liquidacion de arancel por $18.900 para salir a reparto hoy. Gestione el pago en https://cutt.ly/dhl-arancel", "SMISHING", "smish_aduana_acortador_1"),
        e("Claro", "Claro: Su linea celular sera suspendida hoy por falta de registro de IMEI. Evite el bloqueo registrando su equipo en https://claro-imei.xyz", "SMISHING", "smish_operador_imei_1"),
        e("Movistar", "Movistar: Detectamos un error en el cobro de su factura. Valide su tarjeta para evitar la suspension del servicio en https://movistar-pagos.top", "SMISHING", "smish_operador_factura_1"),
        e("Tigo", "Tigo: Tu equipo celular presenta reporte preventivo de bloqueo. Actualiza tus datos personales urgente en https://registro-tigo-col.online", "SMISHING", "smish_operador_reporte_1"),

        # Plataformas digitales, préstamos APK, empleos falsos, sorteos y evasión homógrafa/leetspeak
        e("Netflix-Cobro", "Netflix: Tu suscripcion ha sido suspendida por rechazo en el medio de pago. Actualiza tu tarjeta de credito en https://netflix-facturacion.xyz", "SMISHING", "smish_streaming_pago_1"),
        e("Efecty", "Efecty: Tienes un giro pendiente por reclamar de $380.000. Confirma tus datos bancarios para abono directo en https://efecty-giros-col.online", "SMISHING", "smish_giro_efecty_1"),
        e("Transfiya", "Transfiya: Recibiste una transferencia de $290.000 que expira en 2 horas. Ingresa tu usuario y clave bancaria para aceptarla en https://recibir-transfiya.xyz", "SMISHING", "smish_transfiya_clave_1"),
        e("+573009988776", "Credito pre-aprobado por $3.000.000 sin fiador ni consulta en centrales de riesgo. Descargue el apk inmediato en https://prestamo-ya.top/app.apk", "SMISHING", "smish_prestamo_apk_1"),
        e("+573198877665", "Desembolso inmediato de $5.000.000 a sola firma sin vida crediticia. Instala nuestra aplicacion para recibir el dinero: https://credito-facil-col.xyz/apk", "SMISHING", "smish_prestamo_apk_2"),
        e("+573223344556", "Has sido seleccionado para un trabajo a tiempo parcial desde casa dando me gusta en videos. Gana $300.000 diarios escribiendo a https://wa.me/23480123456", "SMISHING", "smish_empleo_likes_1"),
        e("+2348099887766", "Recursos Humanos Amazon: Oferta de empleo remoto ganando hasta $450.000 al dia sin experiencia. Contactanos urgente en https://bit.ly/trabajo-rapido", "SMISHING", "smish_empleo_acortador_1"),
        e("Soporte", "Su cuenta b4nc0l0mb14 ha sido bl0qu34d4 por seguridad. Ingrese sus credenciales en https://alerta-cuenta.xyz", "SMISHING", "smish_leetspeak_banco_1"),
        e("Seguridad", "Alerta de acceso: Verifique su cuenta en https://раураl-seguro.top/login para evitar suspension.", "SMISHING", "smish_homografo_cirilico_1"),
        e("+573056677889", "Felicidades! Tu linea fue ganadora de un bono de $2.000.000 y un iPhone en nuestro sorteo aniversario. Reclama tu premio en https://sorteo-ganador.top", "SMISHING", "smish_sorteo_premio_1"),
        e("+573174400221", "Notificacion Contable: Su empresa registra factura electronica rechazada con orden de sancion. Descargue el anexo en https://documentos-tributarios-co.net/anexo", "SMISHING", "smish_documento_contable_1"),
        e("+573189900114", "Departamento de Talento Humano: Su hoja de vida paso a fase final para vacante administrativa. Confirme sus datos bancarios de nomina en https://ingreso-personal-col.org/registro", "SMISHING", "smish_rrhh_nomina_1"),
        e("Apple-Seguridad", "iCloud: Se localizo tu iPhone robado hace 10 minutos. Mira la ubicacion exacta iniciando sesion con tu Apple ID en https://icloud-mapa-buscar.com/login", "SMISHING", "smish_icloud_robo_1"),
        e("MercadoPago", "Mercado Pago: Retuvimos un cobro con QR por $410.000. Ingresa tu clave de seguridad para devolver el dinero en https://tinyurl.com/mpago-reverso", "SMISHING", "smish_qr_acortador_1"),
        e("Google-Alert", "Google: Alguien solicito eliminar tu cuenta de Gmail en 12 horas. Cancela la solicitud confirmando tu contrasena en https://verificacion-cuenta-mail.web.app", "SMISHING", "smish_gmail_webapp_1"),

        # SMISHING SIN ENLACES (Vishing / callback antifraude, robo de OTP, emergencias familiares, transferencias directas) -> 20 ejemplos sin URL!
        e("+573009876543", "Banco: Alerta de seguridad. Para anular la transferencia en curso reenvie de inmediato el codigo dinamico de 6 digitos que acaba de recibir.", "SMISHING", "smish_sin_url_otp_banco_1"),
        e("+573107654321", "Soporte Nequi: Estamos bloqueando un retiro fraudulento en tu cuenta. Responde este mensaje con el codigo OTP que te acabamos de enviar.", "SMISHING", "smish_sin_url_otp_nequi_1"),
        e("+573134567890", "Seguridad Bancaria: Para cancelar la compra por $1.200.000 entregue la clave dinamica al asesor o escribala aqui de inmediato.", "SMISHING", "smish_sin_url_clave_asesor_1"),
        e("+573116540987", "Aviso WhatsApp: Detectamos registro de tu numero en otro celular. Envianos por este medio el codigo de 6 digitos recibido para conservar tu linea.", "SMISHING", "smish_sin_url_otp_whatsapp_1"),
        e("Bancolombia", "Bancolombia Alerta: Compra internacional en proceso por USD 680 en MIAMI. Si no la autorizo comuniquese ya con seguridad bancaria al 6014899210.", "SMISHING", "smish_sin_url_vishing_compra_1"),
        e("Davivienda", "Davivienda: Bloqueo preventivo de su clave virtual por intento de fraude. Llame de inmediato a nuestra central de desbloqueo 3209182736.", "SMISHING", "smish_sin_url_vishing_celular_1"),
        e("BBVA-Seguridad", "BBVA: Se programo retiro sin tarjeta por $900.000. Para detener el desembolso comuniquese urgente con soporte antifraude al 6017901122.", "SMISHING", "smish_sin_url_vishing_retiro_1"),
        e("+573109998877", "Hola mama, se me ha roto el movil y este es mi nuevo numero. Escribeme a whatsapp urgente que tengo una emergencia y necesito pagar algo.", "SMISHING", "smish_sin_url_familiar_roto_1"),
        e("+573142211334", "Hola papa, cambie de telefono porque me robaron el otro. Escribeme urgente a este numero que necesito que me hagas un favor con una transferencia.", "SMISHING", "smish_sin_url_familiar_robo_1"),
        e("+573218877441", "Tia buenas tardes, tuve un choque en el carro y transito me lo va a llevar a patios, prestame urgente $450.000 al Nequi 3194455667 mientras llego.", "SMISHING", "smish_sin_url_choque_patios_1"),
        e("+573159988332", "Primo soy Andres, estoy varado en carretera sin senal de llamadas, consigname por favor $180.000 para la grua a Daviplata 3128899004 y ya te devuelvo.", "SMISHING", "smish_sin_url_grua_carretera_1"),
        e("DIAN-Cobros", "DIAN: Su RUT fue suspendido por mora fiscal. Comuniquese hoy mismo por WhatsApp con el gestor tributario 3167788991 para evitar embargo.", "SMISHING", "smish_sin_url_dian_whatsapp_1"),
        e("+573049911223", "Aduanas Nacional: Su equipaje retenido desde Madrid saldra a remate hoy. Consigne $260.000 de bodegaje a la cuenta ahorros 4419283710 y envie soporte.", "SMISHING", "smish_sin_url_maleta_aduana_1"),
        e("+573191122883", "Mesa de Ayuda Bancaria: Su token digital presenta intento de clonacion. Responda este SMS con su cedula y los 4 digitos de su clave para blindar su cuenta.", "SMISHING", "smish_sin_url_clonacion_clave_1"),
        e("+573027766110", "Fiscalia General: Existe citacion penal urgente a su nombre por transferencia sospechosa. Comuniquese de inmediato con el investigador al 3176655449.", "SMISHING", "smish_sin_url_fiscalia_vishing_1"),
        e("+573184455992", "Financiera Rapida: Tu credito por $8.000.000 fue aprobado. Transfiere $190.000 de poliza de desembolso al Nequi 3108877661 para liberar el giro hoy.", "SMISHING", "smish_sin_url_poliza_credito_1"),
        e("+573123399881", "Soporte PSE: Detectamos doble cobro en su cuenta. Para reembolsar el saldo responda con el numero completo de su tarjeta, fecha de vencimiento y CVV.", "SMISHING", "smish_sin_url_cvv_tarjeta_1"),
        e("+2347011223344", "Convocatoria Internacional: Gana desde $350.000 diarios calificando hoteles desde tu celular. Escribe ahora mismo al WhatsApp +573229988110 para activar tu pago.", "SMISHING", "smish_sin_url_empleo_whatsapp_1"),
        e("Soporte-SIM", "Operador Movil: Recibimos solicitud de cambio de eSIM para tu linea. Si no la pediste responde con el PIN de seguridad que llego por mensaje.", "SMISHING", "smish_sin_url_simswap_pin_1"),
        e("+573015544882", "Juzgado Civil: Orden de retencion salarial radicada hoy contra su cedula. Llame antes de las 5 pm al despacho 6013349980 para suspender la medida.", "SMISHING", "smish_sin_url_juzgado_vishing_1"),
    ]


def construir_datos_validacion():
    """Conjunto de validación exclusivo para ajuste de hiperparámetros y umbrales (familias disjuntas)."""
    e = crear_ejemplo
    return [
        # LEGITIMO (20)
        e("BancoBogota", "Banco de Bogota: Compra aprobada por $68.400 en LIBRERIA LERNER con tarjeta debito *5512.", "LEGITIMO", "val_leg_compra_libreria"),
        e("Davivienda", "Davivienda: Su codigo para confirmar la inscripcion de servicios es 409182. Tiene validez de 4 minutos, no lo revele.", "LEGITIMO", "val_leg_otp_inscripcion"),
        e("90124", "Tu codigo de verificacion de Uber es 8821. Nunca compartas este codigo.", "LEGITIMO", "val_leg_otp_transporte"),
        e("BBVA", "BBVA: Recordamos que nuestras oficinas tendran horario especial este lunes festivo. Operaciones disponibles en BBVA Mobile.", "LEGITIMO", "val_leg_horario_festivo"),
        e("CajaSocial", "Banco Caja Social informa: Se rechazo intento de retiro por fondos insuficientes en cajero Chapinero. Sin cobro aplicado.", "LEGITIMO", "val_leg_fondos_insuficientes"),
        e("Envia", "Envia: La guia 014992831 llego a la ciudad destino y sera entregada en el transcurso del dia. Rastreo en https://envia.co", "LEGITIMO", "val_leg_url_envia"),
        e("BancoAgrario", "Banco Agrario: Consulte el calendario oficial de pagos agricolas en https://www.bancoagrario.gov.co", "LEGITIMO", "val_leg_url_bancoagrario"),
        e("+573108877112", "Hola equipo, subi el acta de la reunion semanal a la carpeta de trabajo https://notion.so/proyecto-redes/acta-octubre", "LEGITIMO", "val_leg_url_notion"),
        e("Universidad", "Admisiones: Consulte la citacion a entrevista de posgrado en el portal oficial https://admisiones.uniandes.edu.co", "LEGITIMO", "val_leg_url_uniandes"),
        e("Clinica", "Laboratorio Synlab: Descargue el informe de sus pruebas medicas con su documento en https://resultados.synlab.co", "LEGITIMO", "val_leg_url_synlab"),
        e("+573154433998", "Hola mama, sali temprano de laboratorio, paso por la panaderia y nos vemos en casa.", "LEGITIMO", "val_leg_conv_panaderia"),
        e("+573019922334", "Hola papa, necesito urgente que me confirmes la placa de la moto para comprar el SOAT en el punto oficial.", "LEGITIMO", "val_leg_conv_placa_moto"),
        e("+573127766001", "Buenas tardes, le hablamos de la optica para avisarle que sus gafas formuladas ya estan listas para recoger.", "LEGITIMO", "val_leg_aviso_optica"),
        e("+573183322119", "Companera, me prestas los apuntes de bases de datos manana antes del quiz?", "LEGITIMO", "val_leg_conv_quiz"),
        e("Vanti", "Vanti: La revision periodica quinquenal de gas quedo agendada para el 14 de noviembre en jornada de la manana.", "LEGITIMO", "val_leg_agenda_gas"),
        e("Avianca", "Avianca: Tu equipaje del vuelo AV8410 saldra por la banda numero 3 en el aeropuerto Jose Maria Cordova.", "LEGITIMO", "val_leg_banda_equipaje"),
        e("+573048811229", "Amor revisa la cartelera del teatro municipal para comprar entradas: https://teatromayor.org/programacion", "LEGITIMO", "val_leg_url_teatro"),
        e("Nequi", "Nequi: Programaste con exito el ahorro automatico de $50.000 quincenales en tu colchon.", "LEGITIMO", "val_leg_meta_ahorro"),
        e("86500", "Codigo 390144 para validar tu correo institucional en el campus virtual.", "LEGITIMO", "val_leg_otp_campus"),
        e("+573205544991", "Vecino buenos dias, hoy a las 7 pm es la asamblea ordinaria de copropietarios en el salon comunal.", "LEGITIMO", "val_leg_asamblea_conjunto"),

        # SPAM (20)
        e("Movistar", "Movistar: Pasa tu linea prepago a plan ilimitado con 35% de ahorro por 6 meses. Conoce mas en https://movistar.co", "SPAM", "val_spam_prepago_plan"),
        e("Exito", "Madrugon Exito: Solo manana sabado hasta 45% menos en colchones y lenceria de hogar en tiendas fisicas.", "SPAM", "val_spam_lenceria_exito"),
        e("Falabella", "CMR Puntos: Acumula el doble de puntos este puente festivo en vestuario y perfumeria en https://falabella.com.co", "SPAM", "val_spam_cmr_perfumeria"),
        e("Rappi", "Rappi Turbo: Recibe tus compras de farmacia y antojos en 10 minutos con tarifa de envio gratis hoy.", "SPAM", "val_spam_turbo_farmacia"),
        e("Cineprocinal", "Procinal: Jueves de estrenos con boletas desde $8.500 en funciones antes de las 4 pm en https://procinal.com.co", "SPAM", "val_spam_procinal_tarde"),
        e("Pepeganga", "Semana del Bebe Pepeganga: Coches, sillas de auto y panales con hasta 25% de descuento en sedes.", "SPAM", "val_spam_semana_bebe"),
        e("CruzVerde", "Club Cruz Verde: Martes de bienestar con 20% de descuento en suplementos nutricionales y colageno.", "SPAM", "val_spam_colageno"),
        e("Homecenter", "Especial Pinturas Homecenter: Lleva 3 galones de vinilo lavable y paga 2 este fin de semana.", "SPAM", "val_spam_pinturas_3x2"),
        e("Totto", "Regreso a la U con Totto: 25% de descuento en loncheras y porta laptops en https://totto.com", "SPAM", "val_spam_loncheras"),
        e("Archies", "Archies: Comparte hoy pasta artesanal y pizza mediana con 20% de descuento cenando en nuestros salones.", "SPAM", "val_spam_pasta_cena"),
        e("StudioF", "Clientes Ten Card: Tienes un bono promocional del 25% en la nueva coleccion de vestidos en tiendas.", "SPAM", "val_spam_tencard_vestidos"),
        e("SmartFit", "Inscribete hoy en Plan Black Smart Fit con primer mes por $19.900 para entrenar en cualquier sede.", "SPAM", "val_spam_plan_black"),
        e("Latam", "Mega Promo LATAM: Tiquetes a Lima, Santiago y Miami con descuentos de temporada en https://latamairlines.com", "SPAM", "val_spam_vuelos_internacionales"),
        e("89440", "Outlet de las Americas: Gran liquidacion de calzado deportivo de marcas originales con hasta 50% menos.", "SPAM", "val_spam_outlet_calzado"),
        e("MacPollo", "Puntos Mac Pollo: Prueba nuestros filetes marinados listos para asar con 15% de descuento hoy viernes.", "SPAM", "val_spam_macpollo"),
        e("Colsubsidio", "Piscilago: Compra tus pasaportes en linea con 20% de ahorro para afiliados categoria A y B.", "SPAM", "val_spam_parque_acuatico"),
        e("Panamericana", "Feria de Tecnologia Panamericana: Impresoras multifuncionales y tabletas con bonos de ahorro este mes.", "SPAM", "val_spam_impresoras"),
        e("Velez", "Semana del Cuero Velez: Mantenimiento de cortesia para tus articulos y 15% en cinturones en tienda.", "SPAM", "val_spam_cinturones"),
        e("Ktronix", "Aniversario Ktronix: Televisores OLED de 55 pulgadas con barra de sonido incluida en https://ktronix.com", "SPAM", "val_spam_tv_oled"),
        e("Udemy", "Udemy: Cursos de Python, Kotlin y seguridad informatica desde $39.900 solo durante 48 horas.", "SPAM", "val_spam_cursos_programacion"),

        # SMISHING (20)
        e("Bancolombia", "Bancolombia: Detectamos intentos de retiro en otra ciudad. Confirme su titularidad para mantener activa su tarjeta en https://validacion-sucursal-col.xyz/acceso", "SMISHING", "val_smish_retiro_ciudad"),
        e("+573114488229", "Nequi: Se genero una orden de debito por $490.000 hacia comercio externo. Cancele el descuento en https://nequi-reversion.pages.dev", "SMISHING", "val_smish_orden_debito"),
        e("Davivienda", "Estimado cliente Davivienda, actualice su perfil biometrico obligatorio antes de medianoche en https://davivienda.com.seguridad-clientes.net/biometria", "SMISHING", "val_smish_subdominio_biometria"),
        e("DIAN", "DIAN: Resolucion sancionatoria por omision de activos. Descargue el expediente y evite intereses moratorios en https://expediente-tributario-co.top", "SMISHING", "val_smish_dian_activos"),
        e("Servientrega", "Servientrega: Paquete detenido en centro logistico por etiqueta ilegible. Confirme nomenclatura y cancele $5.900 en https://guia-entrega-hoy.online", "SMISHING", "val_smish_etiqueta_ilegible"),
        e("FedEx", "FedEx Internacional: Su guia 771829 requiere pago de bodegaje aduanero para entrega hoy. Liquide en https://rb.gy/fedex-pago", "SMISHING", "val_smish_fedex_acortador"),
        e("SIMIT-Cobro", "Transito Municipal: Comparendo electronico proximo a pasar a cobro juridico. Consulte placa y pague con rebaja en https://comparendos-viales-co.site", "SMISHING", "val_smish_transito_placa"),
        e("+573028811994", "Aprobamos tu prestamo libre inversion por $6.000.000 sin papeleo. Baja la app oficial para desembolsar: https://dinero-al-instante.xyz/instalar.apk", "SMISHING", "val_smish_apk_dinero"),
        e("+573169900442", "Coordinacion Contable: Adjuntamos soporte de retencion en la fuente pendiente de firma digital en https://soportes-fiscales-empresa.com/firmar", "SMISHING", "val_smish_retencion_firma"),
        e("WOM-Aviso", "WOM: Tu plan presenta rechazo en el cobro automatico. Registra otra tarjeta hoy para conservar tu numero en https://pagos-wom-linea.click", "SMISHING", "val_smish_wom_tarjeta"),
        e("Microsoft-Sec", "Alerta Microsoft 365: Intento de inicio de sesion desde Moscu. Proteja su buzon corporativo en http://45.142.212.61/office", "SMISHING", "val_smish_office_ip"),
        e("+573197766220", "Seleccion Digital TikTok: Recibe comisiones diarias de $280.000 viendo contenido promocional. Registrate por chat en https://wa.me/573238811009", "SMISHING", "val_smish_tiktok_wame"),
        e("Soporte-Seg", "Su billetera d4v1pl4t4 registra bloqueo preventivo. Restablezca su cl4v3 ahora en https://acceso-bolsillo.lat", "SMISHING", "val_smish_leet_daviplata"),
        e("Amazon-Envios", "Amazon: No pudimos procesar el arancel de importacion de tu pedido. Actualiza tu metodo de pago en https://amazon-aduana-col.com/pedido", "SMISHING", "val_smish_amazon_arancel"),
        e("+573148822771", "Central Antifraude Bancaria: Tenemos retenida una compra en linea por $1.350.000. Responda este mensaje con el codigo de seguridad enviado a su celular para anularla.", "SMISHING", "val_smish_sin_url_otp_antifraude"),
        e("BancoBogota", "Banco de Bogota: Su clave segura fue suspendida por intentos invalidos. Comuniquese urgente con la linea de verificacion 6015149920.", "SMISHING", "val_smish_sin_url_vishing_bogota"),
        e("+573103399118", "Hola hijo, se me apago el celular y estoy desde el telefono de una vecina, necesito urgente que me transfieras $220.000 a este Nequi 3156677889.", "SMISHING", "val_smish_sin_url_vecina_nequi"),
        e("+573175500883", "Secretaria de Hacienda: Su predio registra orden de embargo preventivo. Escriba hoy al funcionario encargado al WhatsApp 3182200991 para acuerdo inmediato.", "SMISHING", "val_smish_sin_url_predial_wa"),
        e("+573221100449", "Soporte Billetera Movil: Para completar la devolucion de $145.000 escriba por este medio su numero de cedula y clave de 4 digitos.", "SMISHING", "val_smish_sin_url_pedir_pin"),
        e("Netflix", "Netflix: No logramos validar tu membresia mensual. Confirma los datos de tu tarjeta para seguir viendo en https://is.gd/netflix_cobro", "SMISHING", "val_smish_netflix_isgd"),
    ]


def construir_datos_prueba_independiente():
    """Conjunto de prueba independiente (105 ejemplos: 35 por clase, familias y estructuras no vistas en train ni val)."""
    e = crear_ejemplo
    return [
        # === LEGITIMO (35 ejemplos) ===
        # Transacciones y extractos sin enlaces con redacción distinta
        e("Bancolombia", "Bancolombia: Se desconto $142.300 de tu cuenta corriente *3109 por pago de convenio Acueducto.", "LEGITIMO", "test_leg_convenio_corriente"),
        e("Nequi", "Nequi: Pagaste $27.900 en Crepes y Waffles con codigo QR desde tu saldo disponible.", "LEGITIMO", "test_leg_pago_qr"),
        e("Daviplata", "Daviplata: Retiro completado por $150.000 en punto Efecty autorizado. Costo de la operacion $0.", "LEGITIMO", "test_leg_retiro_efecty"),
        e("Davivienda", "Davivienda: Abono recibido por $620.000 desde otra entidad vía ACH en su cuenta *7741.", "LEGITIMO", "test_leg_abono_ach"),
        e("BBVA", "BBVA: Pago de cuota de credito hipotecario aplicado exitosamente por $1.180.000.", "LEGITIMO", "test_leg_cuota_hipotecaria"),
        e("PSE", "PSE: Transaccion #8849201 aprobada por $95.000 a favor de Vanti Gas Natural.", "LEGITIMO", "test_leg_pse_vanti"),
        e("Nu", "Nu: Recibimos el abono de $410.000 a tu estado de cuenta de octubre. Tu cupo ya fue liberado.", "LEGITIMO", "test_leg_nu_cupo"),
        e("Itau", "Itau: Compra internacional aprobada por USD 14.99 en ADOBE SYSTEMS con tarjeta credito *9021.", "LEGITIMO", "test_leg_itau_adobe"),

        # Códigos OTP variados
        e("87450", "904182 es tu codigo de confirmacion para ingresar a Mi Claro. Expira en 5 minutos.", "LEGITIMO", "test_leg_otp_miclaro"),
        e("Google", "Clave de acceso unico G-726104 para recuperar tu sesion en Gmail desde Android.", "LEGITIMO", "test_leg_otp_google_alt"),
        e("Signal", "Signal: Tu pin de registro es 612-849. Jamas reveles esta secuencia por chat.", "LEGITIMO", "test_leg_otp_signal"),
        e("89900", "Usa el pin temporal 510928 para completar tu inicio de sesion en GitHub.", "LEGITIMO", "test_leg_otp_github"),
        e("Scotiabank", "Scotiabank Colpatria: Clave OTP 338190 para autorizar inscripcion de cuenta. Nunca la entregue por telefono.", "LEGITIMO", "test_leg_otp_scotiabank"),

        # Avisos preventivos / seguridad bancaria real sin enlace (casos difíciles de falso positivo)
        e("Davivienda", "Davivienda informa que su tarjeta debito fue bloqueada preventivamente en cajero. Acerquese a cualquier oficina con su cedula original.", "LEGITIMO", "test_leg_davivienda_oficina"),
        e("Nequi", "Consejo de seguridad Nequi: Ningun funcionario te pedira instalar aplicaciones remotas ni enviar codigos por chat.", "LEGITIMO", "test_leg_consejo_nequi"),
        e("Bancolombia", "Bancolombia: Cambiaste tu clave principal en la Sucursal Virtual Personas el 25/10 a las 14:20. Si no fuiste tu llama al 018000912345.", "LEGITIMO", "test_leg_cambio_clave_real"),

        # Enlaces oficiales e institucionales
        e("Servientrega", "Servientrega: Tu encomienda 99817263 quedo entregada en recepcion. Ver soporte digital en https://www.servientrega.com", "LEGITIMO", "test_leg_url_servientrega"),
        e("Coordinadora", "Coordinadora: Recibimos tu despacho 4419283 con entrega estimada manana. Estado en https://coordinadora.com", "LEGITIMO", "test_leg_url_coordinadora"),
        e("DHL", "DHL Express: Su guia 55910283 llegara hoy antes de las 18:00. Opciones de entrega en https://dhl.com.co", "LEGITIMO", "test_leg_url_dhl"),
        e("RUNT", "RUNT: La renovacion de su licencia de conduccion fue registrada exitosamente. Verifique en https://www.runt.gov.co", "LEGITIMO", "test_leg_url_runt"),

        # Enlaces cotidianos legítimos NO bancarios (prueba crítica contra falsos positivos por URL desconocida)
        e("+573154328765", "Companero, te dejo el enlace de Teams para conectarnos a la revision de codigo: https://teams.microsoft.com/l/meetup-join/19abc", "LEGITIMO", "test_leg_url_teams"),
        e("Universidad", "Facultad de Ingenieria: El listado de salones de examenes finales se publico en https://ingenieria.udea.edu.co/horarios", "LEGITIMO", "test_leg_url_udea"),
        e("CentroMedico", "Colcan: Descargue sus imagenes diagnosticas ingresando con su orden en https://portal.colcan.com.co/pacientes", "LEGITIMO", "test_leg_url_colcan"),
        e("+573102233991", "Mira la oferta laboral de desarrollador Android que publicaron en el portal oficial: https://elempleo.com/co/ofertas-empleo/kotlin", "LEGITIMO", "test_leg_url_elempleo"),
        e("+573187711220", "Familia, aqui esta la ubicacion de la finca para el almuerzo del domingo: https://maps.google.com/?q=4.7110,-74.0721", "LEGITIMO", "test_leg_url_maps"),
        e("Atrapalo", "Atrapalo: Tu reserva para dos personas en la obra de teatro de esta noche esta en https://atrapalo.com.co/reservas", "LEGITIMO", "test_leg_url_atrapalo"),

        # Mensajes cotidianos de remitentes desconocidos y con palabras ambiguas
        e("+573109871234", "Hola mama, ya llegue bien a Medellin, apenas me instale en el hotel te marco.", "LEGITIMO", "test_leg_hola_mama_viaje"),
        e("+573006543219", "Amor, necesito urgente que revises si ya depositaron la quincena para ir a mercar.", "LEGITIMO", "test_leg_urgente_quincena"),
        e("+573123459876", "Buenas tardes, escribimos del consultorio dental para confirmar su control de ortodoncia manana a las 10 AM.", "LEGITIMO", "test_leg_control_dental"),
        e("+573187651234", "Hola papa, me pasas por favor la clave del decodificador de la sala para ver el partido?", "LEGITIMO", "test_leg_hola_papa_deco"),
        e("+573114455990", "Buenas noches vecina, dejaron un sobre de mensajeria para usted en conserjeria porque no habia nadie en el apto.", "LEGITIMO", "test_leg_conserjeria"),
        e("+573149900887", "Ingeniero, el servidor de pruebas ya quedo desplegado y pasando todos los tests unitarios.", "LEGITIMO", "test_leg_despliegue_tests"),
        e("+573021199443", "Recuerda traer la cedula original manana para el ingreso al edificio empresarial.", "LEGITIMO", "test_leg_cedula_edificio"),
        e("Acueducto", "EAAB: La lectura de su medidor del bimestre actual finalizo sin novedades. Consumo normal registrado.", "LEGITIMO", "test_leg_lectura_medidor"),
        e("+573196655002", "Hola tia, cambie el turno de la clinica para el sabado, entonces si alcanzo a ir al cumpleanos del abuelo.", "LEGITIMO", "test_leg_turno_clinica"),

        # === SPAM (35 ejemplos) ===
        e("Claro", "Claro: Renueva hoy tu smartphone con hasta 40% de descuento en referencias seleccionadas en tiendas Claro.", "SPAM", "test_spam_renueva_claro"),
        e("Movistar", "Movistar: Por tus recargas desde $8.000 hoy recibes gigas dobles por 7 dias para redes y video.", "SPAM", "test_spam_recarga_video"),
        e("Tigo", "Hogar Tigo: Contrata 500 megas de internet fibra mas TV HD con descuento los primeros 3 meses en https://tigo.com.co", "SPAM", "test_spam_hogar_tigo"),
        e("Exito", "Dias de Precios Especiales Exito: Ahorra hasta 35% en despensa, aseo del hogar y pequenos electrodomesticos.", "SPAM", "test_spam_dias_especiales"),
        e("Alkosto", "Quinceazo Alkosto: Precios rebajados en portatiles gamer, monitores y audifonos en tiendas y https://alkosto.com", "SPAM", "test_spam_quinceazo"),
        e("CineColombia", "Cine Colombia: Vive el estreno de la semana en salas IMAX reservando tu combo en https://cinecolombia.com", "SPAM", "test_spam_estreno_imax"),
        e("Rappi", "Rappi Restaurantes: Pide tu almuerzo hoy con hasta 30% de ahorro en platos seleccionados de tu zona.", "SPAM", "test_spam_almuerzo_rappi"),
        e("Homecenter", "Feria del Constructor Homecenter: Hasta 25% menos en taladros, escaleras y cerraduras en puntos fisicos.", "SPAM", "test_spam_constructor"),
        e("CruzVerde", "Jueves de Salud Cruz Verde: 20% de ahorro en vitaminas y cuidado articular presentando tu cedula en caja.", "SPAM", "test_spam_jueves_vitaminas"),
        e("Avianca", "Promocion Avianca: Vuela a Cartagena, Cali o Bucaramanga desde $109.900 por trayecto comprando en nuestra app.", "SPAM", "test_spam_vuelos_nacionales"),
        e("Falabella", "Cierre de temporada Falabella: Hasta 45% de descuento en tenis y chaquetas en https://falabella.com.co", "SPAM", "test_spam_cierre_falabella"),
        e("SmartFit", "Conoce la nueva sede Smart Fit de tu barrio: matricula sin costo e invitacion para un amigo este mes.", "SPAM", "test_spam_nueva_sede_gym"),
        e("PromoCalzado", "Cliente preferencial: Tienes un bono de descuento del 30% en nuestra tienda fisica de calzado del centro comercial.", "SPAM", "test_spam_bono_calzado_fisico"),
        e("Farmatodo", "Fin de semana Farmatodo: 25% menos en protectores solares y dermolimpiadores pidiendo en https://farmatodo.com.co", "SPAM", "test_spam_solares"),
        e("Panamericana", "Festival del Libro Panamericana: Lleva 3 novelas seleccionadas y paga 2 en todas nuestras librerias.", "SPAM", "test_spam_festival_libro"),
        e("JuanValdez", "Tardes Juan Valdez: Acompana tu nevado de cafe con pastel de pollo con 20% de descuento de 3 a 6 pm.", "SPAM", "test_spam_tardes_cafe"),
        e("Bodytech", "Reto de verano Bodytech: 3 meses de gimnasio con acceso a zonas humedas con 30% de ahorro.", "SPAM", "test_spam_zonas_humedas"),
        e("Totto", "Viaja ligero con Totto: Maletas de cabina rigidas con 35% de descuento este puente en https://totto.com", "SPAM", "test_spam_maletas_cabina"),
        e("Olimpica", "Viernes de carnes en Olimpica: 20% de ahorro en cortes seleccionados de res y cerdo con Tarjeta Plata.", "SPAM", "test_spam_viernes_carnes"),
        e("DominoPizza", "Noche de partido con Dominos: Pide dos pizzas familiares con 35% de descuento desde la app oficial.", "SPAM", "test_spam_noche_partido"),
        e("87320", " Feria del Automovil en Corferias: Conoce los lanzamientos hibridos y electricos del 10 al 20 de noviembre.", "SPAM", "test_spam_salon_automovil"),
        e("ArturoCalle", "Celebracion Dia del Padre en Arturo Calle: Polos y sueteres desde $69.900 en tiendas a nivel nacional.", "SPAM", "test_spam_polos_padre"),
        e("KFC", "Miercoles de alitas picantes en KFC: Lleva 12 unidades mas papas medianas a precio promocional.", "SPAM", "test_spam_alitas_kfc"),
        e("DisneyPlus", "Disney+: Disfruta estrenos de cine, deportes en vivo y documentales con nuestra tarifa anual especial.", "SPAM", "test_spam_disney_anual"),
        e("Koaj", "Nueva capsula urbana KOAJ: Jeans cargo y camisetas estampadas desde $49.900 en https://koaj.com.co", "SPAM", "test_spam_jeans_cargo"),
        e("Decameron", "Escapada al Eje Cafetero con Decameron Panaca: ninos pagan mitad de tarifa reservando este mes.", "SPAM", "test_spam_eje_cafetero"),
        e("Pepeganga", "Sabado de Lego en Pepeganga: 20% de ahorro en sets de construccion seleccionados en tienda.", "SPAM", "test_spam_lego"),
        e("Adidas", "Temporada de maraton Adidas: Ropa deportiva transpirable con 25% de descuento en https://adidas.co", "SPAM", "test_spam_ropa_maraton"),
        e("Locatel", "Alquiler y venta de sillas de ruedas y caminadores con 15% de ahorro esta semana en Locatel.", "SPAM", "test_spam_movilidad_locatel"),
        e("Ikea", "Renueva tu comedor en IKEA: Mesas de madera y juegos de vajilla en oferta en https://ikea.com.co", "SPAM", "test_spam_comedor_ikea"),
        e("Starbucks", "Feliz semana del cafe: Lleva tu vaso reutilizable a Starbucks y recibe descuento en tu americano.", "SPAM", "test_spam_vaso_cafe"),
        e("Jumbo", "Especial mascotas Jumbo: Concentrados premium para perro y gato con 25% de ahorro hoy sabado.", "SPAM", "test_spam_mascotas_jumbo"),
        e("DirecTV", "Fin de semana de clasicos europeos: Activa el canal deportivo premium con 50% de descuento el primer mes.", "SPAM", "test_spam_canal_deportivo"),
        e("Platzi", "Estudia ingles y desarrollo movil en Platzi con beca parcial de aniversario en https://platzi.com", "SPAM", "test_spam_beca_platzi"),
        e("Crepes", "Domingos en familia en Crepes y Waffles: Conoce nuestros nuevos brunch desde las 9 am en sedes.", "SPAM", "test_spam_brunch_crepes"),

        # === SMISHING (35 ejemplos: campañas nuevas, con enlaces engañosos, sin enlaces y remitentes desconocidos) ===
        e("Bancolombia", "Bancolombia: Registramos un intento de cambio de correo desde un navegador desconocido. Confirme su identidad en https://bancolombia-proteccion.xyz/validar", "SMISHING", "test_smish_cambio_correo"),
        e("+573004455661", "Alerta Bancolombia: Cargo en proceso por $740.000 en tienda virtual. Si usted no realizo la compra cancelela en https://reversion-pagos-col.com/anular", "SMISHING", "test_smish_cargo_tienda"),
        e("Nequi", "Nequi: Por politicas de seguridad tu bolsillo quedo congelado temporalmente. Restablece tu acceso en https://nequi-verificacion.top/acceso", "SMISHING", "test_smish_bolsillo_congelado"),
        e("+573148899002", "Nequi: Tienes una remesa internacional retenida por $680.000. Valida tu clave para abonar el dinero en https://abonos-nequi-col.online/recibir", "SMISHING", "test_smish_remesa_nequi"),
        e("Daviplata", "Daviplata: Saliste priorizado en el apoyo alimentario municipal de $360.000. Inscribe tu documento en https://ayuda-bolsillo-social.xyz/cobro", "SMISHING", "test_smish_apoyo_municipal"),
        e("+573117766550", "Davivienda: Su certificado de seguridad movil expiro hoy. Evite restricciones operativas renovandolo en https://acceso-clientes-davi.site", "SMISHING", "test_smish_certificado_movil"),
        e("BBVA", "BBVA: Detectamos cobro duplicado en su tarjeta de credito. Solicite el reintegro inmediato en https://bbva-reintegros.pages.dev/formulario", "SMISHING", "test_smish_reintegro_bbva"),
        e("BancoBogota", "Banco de Bogota: Su clave de avances fue utilizada hace 5 minutos. Si desconoce el movimiento bloqueelo en https://seguridad-bogota-virtual.xyz", "SMISHING", "test_smish_clave_avances"),
        e("DIAN", "DIAN: Mandamiento ejecutivo radicado sobre sus productos financieros por obligacion vencida. Revise el auto en https://mandamiento-fiscal-co.top/auto", "SMISHING", "test_smish_mandamiento_auto"),
        e("DIAN-Info", "DIAN: Saldo a favor liquidado en su declaracion por $415.000. Registre su cuenta bancaria para desembolso en https://liquidacion-saldo-co.com/abono", "SMISHING", "test_smish_liquidacion_saldo"),
        e("PSE", "PSE: Pago programado en curso por $590.000 hacia aerolinea internacional. Anule el descuento no reconocido en http://192.168.10.45/pse-anular", "SMISHING", "test_smish_pse_aerolinea_ip"),
        e("SIMIT", "Notificacion Vial SIMIT: Comparendo por camara salvavidas proximo a embargo. Obtenga 50% de rebaja pagando en https://recaudo-comparendos-co.xyz", "SMISHING", "test_smish_camara_salvavidas"),
        e("Servientrega", "Servientrega: Nomenclatura inconsistente en su guia 884120. Corrija la direccion de entrega hoy en https://actualizacion-guia-envio.top", "SMISHING", "test_smish_nomenclatura_guia"),
        e("4-72", "Operador Postal 4-72: Su paquete del exterior requiere pago de IVA de importacion por $14.200 en https://nacionalizacion-postal-co.online", "SMISHING", "test_smish_iva_postal"),
        e("Coordinadora", "Coordinadora: No encontramos receptor en el domicilio. Reprograme el segundo despacho cancelando $4.800 en https://reprogramar-despacho-col.com/guia", "SMISHING", "test_smish_segundo_despacho"),
        e("Claro", "Claro Colombia: Su equipo presenta inconsistencia de homologacion ante CRC. Evite bloqueo de senal registrandolo en https://homologacion-equipo-movil.xyz", "SMISHING", "test_smish_homologacion_crc"),
        e("+573205544331", "Financiamos hasta $4.500.000 hoy mismo aunque estes reportado. Obten el instalador directo en https://cupo-inmediato.top/credito.apk", "SMISHING", "test_smish_cupo_apk"),
        e("+573192233110", "Tu linea pospago gano un bono de mercado de $1.500.000 en el sorteo nacional de clientes. Solicita la entrega en https://entrega-premio-linea.xyz", "SMISHING", "test_smish_premio_pospago"),
        e("Soporte", "Su acceso n3qu1 fue r3str1ng1d0 preventivamente. Confirme su cl4v3 personal en https://verificacion-movil.top", "SMISHING", "test_smish_leet_acceso"),
        e("Movii-Pagos", "MOVii: Hay una consignacion pendiente de $320.000 asociada a tu celular. Digita tus credenciales bancarias para liberarla en https://abono-inmediato-col.com/login", "SMISHING", "test_smish_abono_entrante"),
        e("+573152200119", "Estimado contribuyente, su reporte exogeno del trimestre presenta glosas pendientes. Descargue el detalle en https://revision-contable-co.net/detalle", "SMISHING", "test_smish_reporte_exogeno"),
        e("+573178800991", "Area de Contratacion: Su perfil profesional fue aprobado para vacante remota administrativa. Complete su afiliacion bancaria en https://vinculacion-talento-co.org/ficha", "SMISHING", "test_smish_vinculacion_remota"),

        # Subdominios engañosos, acortadores y tono corporativo sin urgencia explícita
        e("BancoCajaSocial", "Banco Caja Social: Le invitamos a validar su token empresarial actualizado ingresando a https://bancocajasocial.com.portal-empresas.net/token", "SMISHING", "test_smish_subdominio_cajasocial"),
        e("Santander", "Banco Santander: Se requiere confirmacion de su firma electronica para mantener operativa su cuenta en https://t.ly/santander-firma", "SMISHING", "test_smish_acortador_santander"),
        e("SaludEPS", "Afiliado EPS: Tiene un reembolso medico autorizado por $185.000. Indique la cuenta y clave para transferencia en https://reembolsos-salud-col.com/datos", "SMISHING", "test_smish_reembolso_eps"),
        e("Seguridad-Cloud", "Alerta ID: Detectamos acceso a tus fotos en la nube desde otro pais. Revisa la sesion en https://аpple-soporte.top/id", "SMISHING", "test_smish_homografo_apple"),
        e("+573219900445", "Agencia de Mercadeo Digital: Gana $320.000 al dia calificando comercios desde tu telefono. Inicia hoy en https://wa.me/22997112233", "SMISHING", "test_smish_agencia_wame"),

        # SMISHING SIN ENLACES en prueba independiente (8 casos variados: OTP, vishing, familiar, extorsión/adelanto)
        e("+573012233449", "Seguridad Bancaria: Para frenar el retiro no reconocido en curso reenvie inmediatamente el codigo OTP de 6 digitos que acaba de llegarle.", "SMISHING", "test_smish_sin_url_frenar_retiro"),
        e("+573169988112", "Hola mama, se me dano la pantalla del celular y te hablo desde otro numero. Escribeme urgente a WhatsApp que necesito pedirte una transferencia.", "SMISHING", "test_smish_sin_url_pantalla_familiar"),
        e("Bancolombia-Seg", "Bancolombia: Registramos compra presencial sospechosa por $2.100.000 en Joyeria Inter. Si no fue usted llame ya a bloqueos al 6014408819.", "SMISHING", "test_smish_sin_url_joyeria_vishing"),
        e("+573138800112", "Hola tio, la policia de carreteras me inmovilizo la moto por un comparendo viejo, pasame urgente $380.000 al Daviplata 3147700992 para sacarla.", "SMISHING", "test_smish_sin_url_tio_moto"),
        e("Soporte-Cuenta", "Centro de Verificacion: Para cancelar el cambio de correo electronico en tu billetera digital responde este SMS con tu clave actual de 4 digitos.", "SMISHING", "test_smish_sin_url_cambio_correo_pin"),
        e("+573186600334", "DIAN Cobranzas: Su proceso de embargo bancario inicia hoy a las 4 pm. Comuniquese urgente por WhatsApp al 3207711445 con el abogado asignado.", "SMISHING", "test_smish_sin_url_abogado_dian"),
        e("+573054411889", "Cooperativa Solidaria: Su prestamo de $10.000.000 esta listo para giro. Consigne $210.000 de estudio de seguridad a la cuenta 0918273645 hoy.", "SMISHING", "test_smish_sin_url_estudio_credito"),
        e("Mesa-Fraudes", "Alerta PSE: Hay un pago retenido desde su cuenta de ahorros. Dictenos o envie por este chat los 16 digitos de su tarjeta y codigo trasero para anular.", "SMISHING", "test_smish_sin_url_16_digitos"),
    ]


def auditar_consistencia_dominios(registros):
    """Marca para revisión cualquier ejemplo etiquetado como LEGITIMO que mencione una marca sensible con un dominio no verificado."""
    marcados = 0
    for item in registros:
        enlaces = PATRON_URL.findall(item["mensaje"])
        if item["clase"] == "LEGITIMO" and len(enlaces) > 0:
            texto_bajo = quitar_acentos(f"{item['remitente']} {item['mensaje']}".lower())
            menciona_marca_sensible = any(marca in texto_bajo for marca in MARCAS_SENSIBLES)
            if menciona_marca_sensible:
                for enlace in enlaces:
                    host = extraer_host(enlace)
                    if not es_dominio_institucional(host):
                        item["requiere_revision"] = True
                        marcados += 1
    return marcados


def verificar_calidad_y_fugas(entrenamiento, validacion, prueba):
    conjuntos = [
        ("entrenamiento", entrenamiento),
        ("validacion", validacion),
        ("prueba", prueba)
    ]

    # 1. Campos obligatorios, clases válidas y no vacíos
    for nombre, datos in conjuntos:
        for idx, item in enumerate(datos):
            for campo in ("remitente", "mensaje", "clase", "origen", "familia_plantilla"):
                if campo not in item or not str(item[campo]).strip():
                    raise ValueError(f"Falta campo '{campo}' en {nombre}[{idx}]")
            if item["clase"] not in CLASES_VALIDAS:
                raise ValueError(f"Clase inválida '{item['clase']}' en {nombre}[{idx}]")
            if item.get("requiere_revision", False):
                raise ValueError(f"El registro {nombre}[{idx}] quedó marcado para revisión manual de dominio.")

    # 2. Sin duplicados internos exactos ni normalizados
    todos = [("entrenamiento", x) for x in entrenamiento] + [("validacion", x) for x in validacion] + [("prueba", x) for x in prueba]
    vistos_exactos = {}
    vistos_plantilla = {}
    familias_por_particion = {}

    for particion, item in todos:
        clave_exacta = item["mensaje"].strip().lower()
        if clave_exacta in vistos_exactos:
            raise ValueError(f"Mensaje duplicado exacto entre {vistos_exactos[clave_exacta]} y {particion}: {item['mensaje']}")
        vistos_exactos[clave_exacta] = particion

        clave_norm = normalizar_plantilla(item["mensaje"])
        if clave_norm in vistos_plantilla:
            raise ValueError(f"Plantilla normalizada duplicada entre {vistos_plantilla[clave_norm]} y {particion}: {item['mensaje']}")
        vistos_plantilla[clave_norm] = particion

        familia = item["familia_plantilla"]
        if familia in familias_por_particion and familias_por_particion[familia] != particion:
            raise ValueError(f"Fuga de familia_plantilla '{familia}' entre {familias_por_particion[familia]} y {particion}")
        familias_por_particion[familia] = particion

    # 3. Verificación de cuasi-duplicados estructurales entre entrenamiento/validación y prueba
    base_desarrollo = [(x["mensaje"], normalizar_plantilla(x["mensaje"])) for x in (entrenamiento + validacion)]
    base_desarrollo_prep = [(msg, norm, set(norm.split())) for msg, norm in base_desarrollo]
    max_similitud = 0.0
    par_mas_parecido = None

    for item_p in prueba:
        norm_p = normalizar_plantilla(item_p["mensaje"])
        tokens_p = set(norm_p.split())
        comparador = difflib.SequenceMatcher(None, norm_p, "")
        for msg_d, norm_d, tokens_d in base_desarrollo_prep:
            comparador.set_seq2(norm_d)
            if comparador.quick_ratio() >= 0.70:
                sim_secuencia = comparador.ratio()
            else:
                sim_secuencia = 0.0
            union = len(tokens_p | tokens_d)
            sim_jaccard = (len(tokens_p & tokens_d) / union) if union > 0 else 0.0

            if sim_secuencia > max_similitud:
                max_similitud = sim_secuencia
                par_mas_parecido = (msg_d, item_p["mensaje"])

            if sim_secuencia >= 0.72 or sim_jaccard >= 0.65:
                raise ValueError(
                    f"Posible fuga estructural detectada (seq={sim_secuencia:.2f}, jaccard={sim_jaccard:.2f}):\n"
                    f"  Desarrollo: {msg_d}\n"
                    f"  Prueba:     {item_p['mensaje']}"
                )

    return max_similitud, par_mas_parecido


def guardar_archivos():
    entrenamiento = construir_datos_entrenamiento()
    validacion = construir_datos_validacion()
    prueba = construir_datos_prueba_independiente()

    for datos in (entrenamiento, validacion, prueba):
        auditar_consistencia_dominios(datos)

    max_sim, _ = verificar_calidad_y_fugas(entrenamiento, validacion, prueba)

    for ruta, datos in (
        (RUTA_ENTRENAMIENTO, entrenamiento),
        (RUTA_VALIDACION, validacion),
        (RUTA_PRUEBA, prueba),
    ):
        with open(ruta, "w", encoding="utf-8", newline="\n") as f:
            json.dump(datos, f, indent=2, ensure_ascii=False)

    print("Corpus sintético generado y verificado correctamente (NOTA: ejemplos sintéticos, no tráfico real de operador).")
    for nombre, datos in (("Entrenamiento", entrenamiento), ("Validacion", validacion), ("Prueba independiente", prueba)):
        conteo = dict(Counter(x["clase"] for x in datos))
        con_enlace = sum(1 for x in datos if len(PATRON_URL.findall(x["mensaje"])) > 0)
        print(f"  - {nombre:20s}: total={len(datos):3d} | clases={conteo} | con_enlace={con_enlace} | sin_enlace={len(datos) - con_enlace}")
    print(f"  - Similitud estructural máxima entre desarrollo y prueba independiente: {max_sim:.3f}")


if __name__ == "__main__":
    guardar_archivos()
