# Fuentes de inteligencia telefónica para Colombia

Fecha de verificación: 2026-11-04.

## Fuentes oficiales

| Fuente | Acceso público | API o descarga automática | Datos colombianos | Uso en SmishGuard |
|---|---|---|---|---|
| [CRC](https://www.crcom.gov.co/es) | Sí, portal institucional y documentos públicos | No se identificó un API público general de reputación telefónica | Sí, normativa y recursos de identificación | Referencia normativa; no se importa automáticamente |
| SIGRI y numeración CRC ([portal CRC](https://www.crcom.gov.co/es)) | La información pública depende del trámite o consulta publicada | No se identificó una descarga pública universal de asignaciones | Sí | No se usa para inferir fraude ni para afirmar que un número no asignado |
| DNO CRC | No; acceso restringido a operadores autorizados | No | Sí | No se consulta ni se intenta eludir su control de acceso |
| Resolución CRC 8308 de 2026 y modificaciones | Debe consultarse desde el repositorio normativo oficial de la CRC | No es una fuente de números por sí misma | Sí | Referencia normativa pendiente de revisión jurídica de cada modificación vigente |
| [SIC](https://www.sic.gov.co/) | Sí, información institucional y trámites | No se identificó un API público de números fraudulentos | Sí | Referencia de protección de datos, publicidad y reportes |
| Registro de Números Excluidos (RNE) | Consulta y trámite sujetos al canal definido por la SIC | No se identificó una descarga pública autorizada | Sí | No se trata como lista de fraude; solo expresa oposición a publicidad |
| [MinTIC](https://www.mintic.gov.co/portal/inicio/) | Sí, portal institucional y datos publicados | Depende de cada conjunto o publicación | Sí | Contexto sectorial; no se importa como reputación telefónica |

La ausencia de un número en una fuente pública no prueba que sea inválido, no asignado,
legítimo o seguro. La numeración no asignada solo puede marcarse cuando exista una
consulta o archivo autorizado que lo establezca.

## Comparación de fuentes verificadas

| Fuente | Cobertura CO comprobable | Registros observables | Actualización | Licencia y redistribución | Precio/API | Decisión |
|---|---|---:|---|---|---|---|
| [tellows Colombia](https://www.tellows.co/) | Sí: sus estadísticas mostraron números con prefijo `+57` el 2026-11-04 | La página publica evaluaciones por número; no publica un conteo exclusivo de Colombia. La red global declara 12.066.197 evaluaciones y 563.731 usuarios | Continua | La web permite consulta, pero el API es un programa comercial; no se encontró licencia abierta para copiar y redistribuir la base | Comercial, cotización | No importar por scraping; solicitar contrato |
| [Should I Answer Colombia](https://co.shouldianswer.net/) | Tiene portal localizado para Colombia | No publica el total colombiano | Comunitaria; fecha no publicada en el portal | No se encontró licencia abierta ni feed de exportación | API y precio no publicados | No importar sin autorización escrita |
| [SpamCalls.net](https://spamcalls.net/en/) | Servicio mundial y reportes por país; no ofrece un conteo colombiano verificable | Muestra reportes y consultas por número, no un dataset colombiano descargable | Continua | Sus páginas regulan el servicio; no autorizan redistribución masiva | API pública y precio no identificados | No importar por scraping |
| [Truecaller](https://www.truecaller.com/) | Ofrece reputación global, pero no publica cobertura ni cantidad para Colombia | No publicado | Continua | Datos y API sujetos a contrato; no se encontró licencia de redistribución | Comercial, cotización | Evaluar solo mediante contrato |
| [Hiya](https://www.hiya.com/) | Producto global; cobertura colombiana no publicada | No publicado | Continua | Datos/API comerciales, sin permiso abierto de redistribución | Comercial, cotización | Evaluar solo mediante contrato |
| CRC, SIC, RNE y MinTIC | Colombia | No son listas públicas de fraude; no hay cantidad importable verificada | Según publicación o trámite | Fuentes institucionales; el RNE expresa oposición a publicidad, no fraude | Público; API de reputación no identificada | Usar como contexto, nunca como lista de fraude |

Las cantidades son las realmente publicadas; “no publicado” no se sustituye por una
estimación. Una evaluación de tellows tampoco equivale necesariamente a fraude:
incluye empresas, cobranza, publicidad, llamadas confiables y otros tipos.

## Fuentes abiertas y proveedores

Los reportes de usuarios, repositorios abiertos y proveedores comerciales pueden ser
incorporados únicamente después de verificar sus términos de uso, licencia, cobertura
colombiana, método de actualización y derecho de redistribución. El backend acepta
fuentes identificadas en el campo `fuentes`, pero no incluye una fuente externa
preconfigurada ni descarga datos sin autorización. Esto evita convertir una función de
consulta en una fuente declarada o redistribuir datos con licencia incompatible.

## Importador de feeds autorizados

Se implementó `servidor/fuentes/importador.mjs` y el comando `npm run fuentes:actualizar`.
No contiene números de ejemplo ni una URL de proveedor: exige `pais: "CO"`, licencia y
`permiteRedistribucion: true`. Acepta JSON o CSV con `numeroE164` o `numero`,
categoría, confianza, reportes y un `id` estable.

Ejemplo de configuración, sin datos ficticios:

```powershell
$env:SMISHGUARD_FUENTES = '[{"id":"proveedor-con-contrato","url":"https://fuente-autorizada.example/co.json","formato":"json","pais":"CO","licencia":"contrato-2026-01","permiteRedistribucion":true,"categoriaPredeterminada":"sospechoso","confianzaPredeterminada":30}]'
npm run fuentes:actualizar
```

Cada observación conserva fuente, licencia, fecha de consulta, fecha del dato, categoría,
confianza y cantidad de reportes. La huella estable evita duplicados. Una fuente fallida
queda registrada en `ejecucionesFuentes` y no modifica sus registros anteriores; el
archivo se reemplaza atómicamente después de procesar el lote. El proceso debe
ejecutarse con un programador externo con la periodicidad permitida por el contrato.

## Estrategia comunitaria colombiana

Mientras no exista un feed con licencia compatible, la base propia debe crecer con
reportes explícitos desde la aplicación y el endpoint `POST /v1/reportes`. Se debe
conservar el tipo exacto (`fraude`, `robocall`, `spam` o `publicidad_no_deseada`),
exigir E.164 `+57`, aplicar una huella por usuario y fecha, limitar un reporte por
fuente independiente y permitir retractaciones. La etiqueta publicitaria no debe
elevarse automáticamente a fraude.

La confirmación existente requiere cinco reportes y confianza mínima de 50. Conviene
revisar manualmente las entradas con múltiples fuentes, separar números corporativos
legítimos y publicar solo indicadores agregados, sin identidad del reportante. Así se
construye una colección colombiana propia sin copiar bases comerciales.

## Estado de implementación

- La base central se guarda en `SMISHGUARD_DATA_FILE` como un documento versionado.
- Cada registro de inteligencia contiene `numeroE164`, indicativo, país, categoría,
  reportes independientes, primera y última fecha, fuentes, confianza, estado de
  verificación, estado activo y fecha de actualización.
- Los cambios se conservan en `cambios`; no se almacenan nombres, contactos ni SMS.
- Los reportes comunitarios empiezan en `sospecha`. Solo alcanzan `confirmado` con
  al menos cinco reportes y confianza mínima de 50.
- No se cargan números iniciales ficticios. En una instalación nueva la cantidad de
  números importados es cero hasta que se incorpore una fuente autorizada o existan
  reportes comunitarios.

## Endpoints

- `GET /v1/inteligencia?desde=<epoch-millis>`: cambios incrementales de números colombianos.
- `GET /v1/estado`: cantidad, países, fuentes y última actualización.
- `POST /v1/reportes`: recibe `indicador`, `tipo`, y para números opcionalmente
  `categoria` y `fuente`.
- `GET /v1/lista-negra`: contrato legado para dominios e indicadores antiguos.

La aplicación Android ejecuta la sincronización con WorkManager cada seis horas cuando
hay conectividad. El filtrado de llamadas consulta exclusivamente la copia local.
Los fallos de red se registran como reintentos y no convierten un número en seguro.
