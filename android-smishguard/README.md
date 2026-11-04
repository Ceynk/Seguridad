# SmishGuard - Antiphishing y SMS Honeypot para Android (Kotlin)

SmishGuard es una aplicacion nativa para Android desarrollada en Kotlin, diseñada para detectar, analizar y neutralizar ataques de Smishing (SMS Phishing), remitentes falsificados (SMS Spoofing) y enlaces maliciosos mediante una Boveda Honeypot local con total privacidad en el dispositivo.

---

## 1. Compromiso de Privacidad y No Invasividad

- Procesamiento local en el dispositivo: Todo el analisis semantico, heuristico, calculo de entropia, deteccion de dominios falsificados y ataques homografos se ejecuta localmente en la CPU del telefono.
- Sin conexion externa ni telemetria: Ningun SMS, numero, remitente ni contenido es transmitido a la nube ni a servidores externos.
- Reporte comunitario limitado: Solo se transmite el indicador normalizado de un enlace, dominio o numero. Los reportes pendientes se reintentan al iniciar la aplicacion.
- Modo Guardian No Invasivo: Puede funcionar como servicio complementario sin obligar al usuario a reemplazar su aplicacion de mensajeria predeterminada. Si detecta phishing, suprime la notificacion peligrosa de forma automatica.
- Boveda Honeypot Aislada: Los mensajes clasificados como amenazas son aislados en un entorno seguro donde los enlaces son neutralizados (hxxp:// y [.]) para imposibilitar clics accidentales.

---

## 2. Motor Heuristico de Deteccion (SmishDetectorEngine)

El motor de deteccion integra multiples capas de analisis forense en tiempo real:

1. Analizador de Enlaces y Dominios (UrlThreatAnalyzer):
   - Dominios de alto riesgo: Deteccion de dominios en extensiones frecuentemente abusadas (.xyz, .top, .icu, .tk, .ru, .work, etc.).
   - Direcciones IP directas: Enlaces que apuntan a direcciones IP crudas (http://192.168...) en lugar de nombres de dominio legitimos.
   - Servicios de alojamiento gratuito y tuneles: Identificacion de enlaces alojados en duckdns.org, ngrok.io, firebaseapp.com, 000webhostapp.com, etc.
   - Suplantacion de marca: Subdominios que imitan bancos o servicios conocidos (santander-seguridad.duckdns.org).
   - Entropia de Shannon: Deteccion de dominios generados por algoritmos aleatorios (DGA).

2. Deteccion de Ataques Homografos y Punycode (HomographDetector):
   - Deteccion de caracteres cirilicos o griegos mezclados con caracteres latinos.
   - Deteccion de prefijos Punycode (xn--...).

3. Analisis de Fraude en Remitente (SenderFraudAnalyzer):
   - Deteccion de remitentes alfanumericos habitualmente suplantados sin verificacion.
   - Deteccion de prefijos telefonicos internacionales con anomalias de riesgo.

4. Motor Semantico de Patrones (PatternRuleEngine):
   - Fraude Bancario (cuentas bloqueadas, tarjetas suspendidas).
   - Paqueteria y Aduanas (Correos, DHL, paquete retenido por tasas).
   - Organismos Publicos (DGT multas, Hacienda reembolsos).
   - Robo de Credenciales y Codigos de Seguridad (solicitud de codigos de confirmacion o contraseñas).
   - Tacticas de Urgencia y Coercion (limites de tiempo perentorios).
   - Suplantacion familiar.

---

## 3. Modulos de la Aplicacion

- Panel de Proteccion: Visualizacion del estado del escudo en tiempo real, conteo de SMS analizados, amenazas bloqueadas y acceso rapido a la boveda.
- Boveda Honeypot: Bandeja aislada donde residen los SMS interceptados. Permite inspeccionar el desglose forense detallado de los factores de riesgo, restaurar falsos positivos o eliminar los mensajes.
- Analizador de Amenazas: Herramienta interactiva para pegar cualquier texto, numero o URL sospechosa y obtener un dictamen instantaneo con puntuacion de 0 a 100%. Incluye plantillas de prueba preconfiguradas.
- Ajustes y Reglas Personalizadas:
  - Ajuste del umbral de sensibilidad de cuarentena (deslizador de 20% a 80%).
  - Gestor de Lista Blanca de contactos y numeros de confianza.
  - Creador de Reglas Propias con soporte para palabras clave o Expresiones Regulares (Regex).
  - Lista Negra Comunitaria con reportes pendientes de confirmacion y clasificacion maliciosa desde cinco reportes.

---

## 4. Contrato del API Comunitaria e inteligencia telefónica

La sincronizacion esta desactivada cuando `COMMUNITY_API_URL` esta vacia. Para activarla, define esa variable al compilar. La lista es global al servidor: no se envia ni se exige ningun identificador del dispositivo.

- `POST /v1/reportes`
  - Cuerpo: `{"indicador":"ejemplo.com","tipo":"dominio"}`
  - Respuesta: `{"indicador":"ejemplo.com","tipo":"dominio","reportes":1,"esMalicioso":false}`
- `GET /v1/lista-negra`
  - Respuesta: arreglo con objetos del mismo formato.
- `GET /v1/lista-negra?desde=<epoch-millis>`
  - Devuelve solamente indicadores actualizados después del cursor indicado.
- `GET /v1/inteligencia?desde=<epoch-millis>`
  - Devuelve registros de inteligencia telefónica colombiana en E.164.
- `GET /v1/estado`
  - Devuelve cantidad de números, países, fuentes y última actualización.
- `POST /v1/reportes` para un número
  - Cuerpo: `{"indicador":"+573001234567","tipo":"numero","categoria":"sospechoso","fuente":"comunidad"}`
  - Solo acepta números colombianos válidos en E.164. Un reporte no bloquea globalmente
    por sí solo; la confirmación requiere evidencia acumulada.

El cliente nunca envia SMS, remitentes, contactos, identificadores del dispositivo ni datos personales. La descarga de la lista no incluye parametros del usuario.

## 5. Servidor comunitario

El servidor esta en `../servidor`. Requiere Node.js 20 o posterior.

1. Ejecutar `npm start` dentro de `../servidor`.
2. El servidor escucha en `http://localhost:8787`.
3. Para conectar la aplicacion, define `COMMUNITY_API_URL` antes de compilar:
   - Emulador Android: `http://10.0.2.2:8787`
   - Dispositivo fisico: usa la direccion IP local del equipo, por ejemplo `http://192.168.1.20:8787`
4. Para una instalacion publica, usa HTTPS y define `ALLOW_ORIGIN` con el origen permitido.

Los datos se guardan en `servidor/datos/lista-negra.json` como un documento versionado
con inteligencia, reportes, historial de cambios y ejecuciones de fuentes. No recibe SMS,
contactos ni identificadores del dispositivo. La matriz de fuentes, sus limitaciones y
el estado real de implementación está en [FUENTES-INTELIGENCIA.md](FUENTES-INTELIGENCIA.md).

La aplicación mantiene el cursor de sincronización local y ejecuta la actualización incremental con WorkManager cada seis horas cuando hay conectividad. La evaluación de llamadas no espera al servidor: usa únicamente Room y la reputación descargada previamente. Una instalación nueva no contiene números precargados ficticios.

## 6. Protección de llamadas

El `CallScreeningService` normaliza números con libphonenumber usando Colombia (`CO`) como región predeterminada y guarda los válidos en formato E.164. La clasificación combina lista negra personal, reportes comunitarios, verificación de red, numeración inválida, reglas personalizadas y patrones Wangiri/tarificación especial. Un número internacional válido no se bloquea solo por ser extranjero.

Las políticas de números privados, inválidos e internacionales se almacenan independientemente en preferencias. El bloqueo requiere que la puntuación de señales alcance el umbral configurado o que exista una confirmación de lista negra. Android debe establecer la aplicación como identificador y filtro de llamadas predeterminado para activar `CallScreeningService`.

## 7. Tecnologias Utilizadas

- Lenguaje: Kotlin 2.0.0
- Interfaz de Usuario: Jetpack Compose y Material 3
- Base de Datos Local: Room Database (smishguard_vault.db)
- Concurrencia: Kotlin Coroutines y Flow / StateFlow
- Arquitectura: Clean Architecture y patron MVVM
- Nivel de API Android: Android 14 (API 34) y Min SDK 26 (Android 8.0+)

---

## 8. Instrucciones para Abrir y Compilar en Android Studio

1. Abrir Android Studio.
2. Seleccionar la opcion Open y abrir la carpeta del proyecto:
   d:\Seguridad\android-smishguard
3. Esperar la sincronizacion de Gradle con el catalogo de dependencias.
4. Ejecutar las pruebas unitarias:
   ./gradlew test
5. Ejecutar la aplicacion en un dispositivo fisico o emulador.
