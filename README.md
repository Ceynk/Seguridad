# PhishGuard Colombia

Extensión de navegador para detección, advertencia y bloqueo de sitios fraudulentos y phishing en Colombia (Manifest V3, TypeScript, React, Vite).

## Contenido
1. [Descripción](#descripción)
2. [Arquitectura](#arquitectura)
3. [Tecnologías](#tecnologías)
4. [Estructura del Proyecto](#estructura-del-proyecto)
5. [Instalación y Compilación](#instalación-y-compilación)
6. [Instalación en Navegador](#instalación-en-navegador)
7. [Motor de Riesgo y Heurística](#motor-de-riesgo-y-heurística)
8. [Pruebas Unitarias](#pruebas-unitarias)
9. [Permisos Manifest V3](#permisos-manifest-v3)
10. [Privacidad y Seguridad](#privacidad-y-seguridad)

---

## Descripción

PhishGuard Colombia intercepta y evalúa URLs antes de su carga en el navegador, calculando un índice de riesgo basado en heurística contextual, detección de homoglifos, análisis estructural y reputación local.

Clasificación de riesgo:
- **SEGURO (0 - 29 puntos):** Sin indicadores de riesgo detectados.
- **SOSPECHOSO (30 - 69 puntos):** Anomalías léxicas o estructurales moderadas.
- **MALICIOSO (70 - 100 puntos):** Coincidencia con patrones de fraude o suplantación de identidad; se redirige a la pantalla de bloqueo local.

---

## Arquitectura

```
Navegación -> chrome.webNavigation.onBeforeNavigate -> Service Worker -> MotorRiesgo
                                                                             |
      +-----------------------+----------------------+-----------------------+
      |                       |                      |                       |
Reputación Local      Extractor URL          Registro Reglas        Módulo Homoglifos
(Listas blanca/negra) (Sintaxis y tokens)    (Reglas Heurísticas)   (Levenshtein / Typos)
```

### Componentes Principales:
- **Trabajador de Servicio (`src/segundo-plano/trabajador-servicio.ts`):** Intercepta la navegación web, coordina el análisis en segundo plano y aplica redirecciones defensivas.
- **Inspector de Documento (`src/contenido/inspector-documento.ts`):** Detecta formularios sensibles y campos de contraseña en el documento activo.
- **Extractor de Características (`src/analizador/extractor-caracteristicas.ts`):** Extrae segmentos, TLDs, longitud, caracteres especiales, direcciones IP y métricas léxicas.
- **Módulo de Homoglifos (`src/analizador/homoglifos.ts`):** Detecta sustituciones de caracteres visualmente similares y similitud tipográfica frente a marcas conocidas.
- **Motor de Riesgo (`src/analizador/motor-riesgo.ts`):** Evalúa reglas heurísticas combinadas con reputación local.
- **Reputación Local (`src/reputacion/reputacion-local.ts`):** Consulta listas locales de dominios legítimos y listas de excepción configuradas por el usuario.
- **Almacenamiento (`src/almacenamiento/servicio-almacenamiento.ts`):** Adaptador tipado sobre `chrome.storage.local`.

---

## Tecnologías

- TypeScript 5.7
- Google Chrome Extensions Manifest V3
- Vite 6 + React 18
- Vitest 3
- Lucide React

---

## Estructura del Proyecto

```
├── public/
│   ├── manifest.json
│   └── icons/
├── scripts/
│   ├── generar-iconos.js
│   └── procesar-csv.js
├── src/
│   ├── almacenamiento/
│   │   └── servicio-almacenamiento.ts
│   ├── analizador/
│   │   ├── extractor-caracteristicas.ts
│   │   ├── homoglifos.ts
│   │   └── motor-riesgo.ts
│   ├── contenido/
│   │   └── inspector-documento.ts
│   ├── paginas/
│   │   ├── estilos-base.css
│   │   ├── bloqueado/
│   │   ├── emergente/
│   │   └── opciones/
│   ├── reputacion/
│   │   └── reputacion-local.ts
│   ├── segundo-plano/
│   │   └── trabajador-servicio.ts
│   ├── tipos/
│   │   ├── analisis.ts
│   │   ├── configuracion.ts
│   │   └── reputacion.ts
│   └── utilidades/
│       ├── cripto-forense.ts
│       └── normalizador-url.ts
└── pruebas/
```

---

## Instalación y Compilación

```bash
# Instalar dependencias
npm install

# Compilar para producción
npm run build
```

Los artefactos de producción se generan en la carpeta `dist/`.

---

## Instalación en Navegador

1. Abrir `chrome://extensions` en Google Chrome o navegador basado en Chromium.
2. Activar la opción **Modo de desarrollador**.
3. Hacer clic en **Cargar descomprimida**.
4. Seleccionar la carpeta `dist` del proyecto.

---

## Motor de Riesgo y Heurística

| Indicador | Severidad | Puntuación Base | Descripción |
| :--- | :--- | :--- | :--- |
| **Suplantación de Marca** | CRÍTICA | 50 pts | Detección de marcas en etiquetas de dominio no oficiales u homoglifos (`banc0l0mbia`, `nequ1`). |
| **Host IP Directo** | ALTA | 40 pts | Uso de IPv4 o IPv6 directa en lugar de nombre de dominio. |
| **Punycode / IDN** | ALTA | 60 pts | Prefijo `xn--` o caracteres confusables en nombres de dominio. |
| **Anomalías Estructurales** | MEDIA | 5 - 35 pts | Presencia de `@`, subdominios profundos, etiquetas compuestas o codificación elevada. |
| **TLD de abuso frecuente** | MEDIA | 20 pts | Extensiones con alta tasa histórica de abuso (`.xyz`, `.top`, `.click`, `.tk`). |
| **Contexto de ingeniería social** | ALTA | 25 - 30 pts | Solo puntúa cuando combina autenticación, contexto financiero o presión temporal. |
| **Protocolo HTTP** | BAJA | 5 pts | Solo puntúa junto con un contexto sensible de ingeniería social. |

Las señales aisladas no provocan bloqueo automático. La puntuación se combina con reputación local y contexto; las listas blancas oficiales y personalizadas se evalúan antes de las reglas heurísticas.

El bloqueo automático requiere una entrada de lista negra o evidencia heurística fuerte. Las señales moderadas se muestran como sospechosas y pueden solicitar una confirmación documental local cuando la página contiene un formulario sensible. La versión actual no envía URLs, hashes ni datos de navegación a servicios externos.

---

## Pruebas Unitarias

```bash
npm test
```

Suite de pruebas:
- `pruebas/homoglifos.test.ts`: Distancia Levenshtein, desofuscación y marcas.
- `pruebas/extractor-caracteristicas.test.ts`: Extracción sintáctica y diccionarios.
- `pruebas/reputacion.test.ts`: Listas de reputación locales y del usuario.
- `pruebas/motor-riesgo.test.ts`: Puntuación acumulada y clasificación.
- `pruebas/falsos-positivos.test.ts`: Validación en dominios oficiales legítimos.
- `pruebas/cripto-forense.test.ts`: Entropía de Shannon, hash SHA-256 y ficha técnica.

---

## Permisos Manifest V3

- `webNavigation`: Intercepción de navegación en `onBeforeNavigate` para evaluar la URL antes de la descarga de contenido.
- `storage`: Almacenamiento local de configuración y contadores de sesión en `chrome.storage.local`.
- `tabs`: Actualización de la insignia en la barra de herramientas y redirección a la pantalla de bloqueo ante sitios maliciosos.

---

## Privacidad y Seguridad

- **Procesamiento Local:** Las evaluaciones de riesgo se ejecutan localmente en el dispositivo. No se envían URLs a servidores externos.
- **Sin Historial Persistente:** No se registra historial de navegación en disco.
- **Content Security Policy:** `script-src 'self'; object-src 'self';`
