# PhishGuard Colombia 🛡️

**Extensión de Navegador para Detección, Advertencia y Bloqueo de Sitios Web Fraudulentos y Campañas de Phishing en Colombia**  
*(Manifest V3 • TypeScript • React • Vite)*

---

## 📋 Tabla de Contenidos
1. [Descripción y Problema que Resuelve](#-descripción-y-problema-que-resuelve)
2. [Objetivo del MVP](#-objetivo-del-mvp)
3. [Arquitectura del Sistema](#-arquitectura-del-sistema)
4. [Tecnologías Utilizadas](#-tecnologías-utilizadas)
5. [Estructura del Proyecto](#-estructura-del-proyecto)
6. [Instalación y Puesta en Marcha](#-instalación-y-puesta-en-marcha)
7. [Cómo Cargar la Extensión en Chrome](#-cómo-cargar-la-extensión-en-chrome)
8. [Motor de Riesgo y Heurística](#-motor-de-riesgo-y-heurística)
9. [Cómo Añadir Nuevas Reglas de Detección](#-cómo-añadir-nuevas-reglas-de-detección)
10. [Cómo Añadir Nuevos Indicadores y Marcas](#-cómo-añadir-nuevos-indicadores-y-marcas)
11. [Ejecución de Pruebas Unitarias](#-ejecución-de-pruebas-unitarias)
12. [Justificación de Permisos (Manifest V3)](#-justificación-de-permisos-manifest-v3)
13. [Privacidad y Seguridad por Diseño](#-privacidad-y-seguridad-por-diseño)
14. [Limitaciones Actuales del MVP](#-limitaciones-actuales-del-mvp)
15. [Roadmap de Futuras Versiones](#-roadmap-de-futuras-versiones)

---

## 🎯 Descripción y Problema que Resuelve

El phishing y la suplantación de identidad en entornos web representan una de las mayores amenazas de fraude financiero y robo de credenciales en Colombia y Latinoamérica. Los cibercriminales despliegan diariamente campañas dirigidas simulando canales de entidades bancarias (*Bancolombia, Nequi, Daviplata, Davivienda, Banco de Bogotá, PSE*), entidades gubernamentales (*DIAN, SIMIT/Fotomultas, RUNT*) y empresas de paquetería (*4-72, Servientrega*).

**PhishGuard Colombia** es el primer componente de un ecosistema de ciberseguridad integral. Consiste en una extensión de navegador ligera, rápida y privada que intercepta y analiza las URLs antes de su carga, calculando un índice de riesgo basado en heurística léxica, detección de homoglifos/typosquatting, evaluación estructural y reputación local.

---

## 🚀 Objetivo del MVP

La extensión evalúa las páginas que el usuario navega y clasifica cada sitio en uno de **tres estados**:

* 🟢 **SEGURO (0 - 29 puntos):** Sitio aparentemente legítimo o sin indicadores de riesgo.
* 🟡 **SOSPECHOSO (30 - 69 puntos):** Existen anomalías estructurales o léxicas moderadas, pero insuficientes para un bloqueo automático. Se notifica con advertencia en el icono y panel.
* 🔴 **MALICIOSO (70 - 100 puntos):** Existe evidencia contundente de fraude, suplantación o amenaza conocida. La navegación se interrumpe inmediatamente y se redirige a una pantalla de bloqueo segura con explicación técnica.

---

## 🏛️ Arquitectura del Sistema

La arquitectura está diseñada bajo los principios de **Modularidad**, **Bajo Acoplamiento** y **Privacidad por Diseño (Privacy by Design)**:

```mermaid
flowchart TD
    A[Usuario navega a una URL] --> B[chrome.webNavigation.onBeforeNavigate]
    B --> C[Background Service Worker]
    C --> D[Motor de Riesgo]
    D --> E[Proveedor de Reputación Local]
    E -- Coincidencia en Lista Blanca --> F[Clasificación: SEGURO (Score 0)]
    E -- Coincidencia en Lista Negra --> G[Clasificación: MALICIOSO (Score 95)]
    E -- Dominio Desconocido --> H[Extractor de Características]
    H --> I[Registro de Reglas Heurísticas]
    I --> J[Regla Suplantación Marca + Homoglifos]
    I --> K[Regla IP Directa]
    I --> L[Regla Seguridad Protocolo HTTP]
    I --> M[Regla Punycode / IDN]
    I --> N[Regla Palabras Sospechosas]
    I --> O[Regla TLD de Alto Riesgo]
    I --> P[Regla Estructura y Anomalías URL]
    J & K & L & M & N & O & P --> Q[Acumulador Ponderado de Riesgo]
    Q --> R{Evaluación de Score}
    R -- 0 a 29 --> S[Badge OK Verde]
    R -- 30 a 69 --> T[Badge RIESGO Naranja]
    R -- 70 a 100 --> U[Redirección a Página de Bloqueo src/pages/bloqueado]
```

### Componentes Clave:
1. **Service Worker (`src/background/service-worker.ts`):** Orquestador en segundo plano que intercepta eventos de navegación, aplica redirecciones defensivas y actualiza insignias en la barra de herramientas.
2. **Extractor de Características (`src/analyzer/extractor-caracteristicas.ts`):** Parsea componentes sintácticos (subdominios, TLDs, longitud, caracteres `@`, puertos, IPs IPv4/IPv6, codificación hexadecimal).
3. **Módulo de Homoglifos y Typosquatting (`src/analyzer/homoglifos.ts`):** Implementa distancia Levenshtein optimizada y desofuscación de sustituciones de caracteres visuales engañosos (`0` por `o`, `1` por `l`, `rn` por `m`, etc.).
4. **Registro de Reglas (`src/rules/`):** Clases desacopladas e independientes que implementan la interfaz `ReglaRiesgo`.
5. **Proveedor de Reputación (`src/reputation/`):** Abstracción desacoplada que permite consultar listas locales y que está preparada para conectarse en fases futuras a APIs remotas de Threat Intelligence.
6. **Almacenamiento Local Seguro (`src/storage/`):** Gestiona `chrome.storage.local` garantizando que **no se guarde historial de navegación permanente**.
7. **Interfaces de Usuario (`src/pages/`):**
   - **Popup:** Estado en tiempo real, índice de riesgo, métricas de sesión y reporte.
   - **Bloqueado (Blocked Page):** Explicación clara de la amenaza, motivos técnicos, desglose de reglas y reporte de falso positivo.
   - **Opciones (Settings & Sandbox):** Configuración de sensibilidad, listas personalizadas y probador de URLs en vivo.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** TypeScript 5.7 (Modo estricto, variables y tipos 100% en español).
* **Navegador:** Google Chrome Extensions Manifest V3 (Compatible con navegadores basados en Chromium).
* **Bundler & Build Tool:** Vite 6 + `@vitejs/plugin-react`.
* **Interfaz de Usuario:** React 18 + HTML5 + CSS3 + Lucide Icons (Solo en Popup, Bloqueo y Opciones).
* **Testing:** Vitest 3 (Suite automatizada de pruebas unitarias).
* **Calidad de Código:** ESLint 9 + Prettier.

---

## 📁 Estructura del Proyecto

```
d:/Seguridad/
├── .gitignore
├── .prettierrc
├── package.json
├── tsconfig.json
├── vite.config.ts
├── vitest.config.ts
├── public/
│   ├── manifest.json
│   └── icons/
│       ├── icon-16.png
│       ├── icon-32.png
│       ├── icon-48.png
│       └── icon-128.png
├── scripts/
│   └── generar-iconos.js
├── src/
│   ├── analyzer/
│   │   ├── extractor-caracteristicas.ts
│   │   ├── homoglifos.ts
│   │   └── motor-riesgo.ts
│   ├── background/
│   │   └── service-worker.ts
│   ├── content/
│   │   └── content-script.ts
│   ├── pages/
│   │   ├── bloqueado/
│   │   │   ├── index.html
│   │   │   ├── bloqueado.tsx
│   │   │   └── bloqueado.css
│   │   ├── opciones/
│   │   │   ├── index.html
│   │   │   ├── opciones.tsx
│   │   │   └── opciones.css
│   │   └── popup/
│   │       ├── index.html
│   │       ├── popup.tsx
│   │       └── popup.css
│   ├── reputation/
│   │   └── reputacion-local.ts
│   ├── rules/
│   │   ├── base-regla.ts
│   │   ├── regla-estructura-url.ts
│   │   ├── regla-ip-host.ts
│   │   ├── regla-palabras-sospechosas.ts
│   │   ├── regla-punycode-homografo.ts
│   │   ├── regla-seguridad-protocolo.ts
│   │   ├── regla-suplantacion-marca.ts
│   │   ├── regla-tld-sospechoso.ts
│   │   └── registro-reglas.ts
│   ├── storage/
│   │   └── servicio-almacenamiento.ts
│   ├── types/
│   │   ├── analisis.ts
│   │   ├── configuracion.ts
│   │   ├── reglas.ts
│   │   └── reputacion.ts
│   └── utils/
│       └── normalizador-url.ts
└── tests/
    ├── extractor-caracteristicas.test.ts
    ├── falsos-positivos.test.ts
    ├── homoglifos.test.ts
    ├── motor-riesgo.test.ts
    ├── reglas.test.ts
    └── reputacion.test.ts
```

---

## 📦 Instalación y Puesta en Marcha

### Prerrequisitos
* Node.js v18+ (probado en Node v25.4.0)
* npm v9+ (probado en npm 11.7.0)

### Pasos de Instalación:
```bash
# 1. Clonar o ingresar al directorio del proyecto
cd d:/Seguridad

# 2. Instalar dependencias
npm install

# 3. Compilar el proyecto para producción
npm run build
```

El comando generará la carpeta compilada lista para desplegar en: `d:/Seguridad/dist/`.

---

## 🌐 Cómo Cargar la Extensión en Chrome

1. Abre Google Chrome o cualquier navegador compatible con Chromium (Brave, Edge, Opera).
2. Dirígete a la URL de extensiones: `chrome://extensions`
3. En la esquina superior derecha, activa el interruptor **"Modo de desarrollador"** (Developer mode).
4. Haz clic en el botón **"Cargar descomprimida"** (Load unpacked).
5. Selecciona la carpeta `dist` ubicada en `d:\Seguridad\dist`.
6. La extensión **"PhishGuard Colombia - Detector de Phishing y Fraude"** quedará instalada y activa.

---

## 🧮 Motor de Riesgo y Heurística

El motor de riesgo calcula una puntuación escalar de **0 a 100** evaluando múltiples señales en paralelo:

| Indicador Heurístico | Severidad | Puntuación Base | Detalle de la Detección |
| :--- | :--- | :--- | :--- |
| **Suplantación de Marca / Typosquatting** | CRÍTICA | 45 - 55 pts | Detección de marcas bancarias/gubernamentales en dominios no oficiales u homoglifos (`banc0l0mbia`, `nequ1`). |
| **Dirección IP Directa en Host** | ALTA | 35 - 55 pts | Uso de IPv4 o IPv6 directa en lugar de dominio registrado. |
| **Dominio Punycode (IDN)** | ALTA | 35 - 60 pts | Prefijo `xn--` utilizado para suplantar caracteres en alfabetos cirílicos o griegos. |
| **Anomalías Estructurales / `@` en URL** | ALTA | 20 - 45 pts | Presencia de `@` en la URL, subdominios anidados (≥ 3), exceso de guiones o URLs largas. |
| **TLD de Alto Riesgo Abusivo** | MEDIA | 20 - 45 pts | Extensiones como `.xyz`, `.top`, `.tk`, `.click`, `.fit` combinadas con términos sensibles. |
| **Léxico de Urgencia e Ingeniería Social** | MEDIA | 10 - 30 pts | Combinación de palabras de autenticación (`login`, `clave`, `token`) con urgencia (`fotomulta`, `embargo`, `bloqueo`). |
| **Protocolo HTTP No Cifrado** | MEDIA | 15 - 35 pts | Ausencia de HTTPS en páginas que solicitan credenciales bancarias o trámites. |

### Factor de Amplificación Multi-Señal:
* Si se activan **2 reglas simultáneas**: Se aplica un multiplicador de sinergia de **+10%**.
* Si se activan **3 o más reglas simultáneas**: Se aplica un multiplicador de **+25%**.

---

## 🧩 Cómo Añadir Nuevas Reglas de Detección

1. Crea un nuevo archivo en `src/rules/regla-[nombre].ts` que herede de `BaseReglaRiesgo`:

```typescript
import { BaseReglaRiesgo } from "./base-regla";
import { CaracteristicasUrl, SeveridadRegla } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";

export class ReglaNuevaDeteccion extends BaseReglaRiesgo {
  readonly id = "REGLA_NUEVA_DETECCION";
  readonly nombre = "Nombre Descriptivo de la Regla";
  readonly descripcion = "Explicación clara del patrón detectado.";
  readonly severidad: SeveridadRegla = "MEDIA";
  readonly puntuacionBase = 25;

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    if (/* condición sospechosa */) {
      return this.crearResultadoActivado(this.puntuacionBase, [
        {
          campo: "campoEvaluado",
          valorEncontrado: "valor",
          descripcion: "Detalle de la evidencia encontrada",
        },
      ]);
    }
    return this.crearResultadoNoActivado();
  }
}
```

2. Registra la nueva regla en `src/rules/registro-reglas.ts`:
```typescript
this.registrarRegla(new ReglaNuevaDeteccion());
```

---

## 🏢 Cómo Añadir Nuevos Indicadores y Marcas

Para añadir entidades colombianas o internacionales adicionales:
1. Abre `src/analyzer/homoglifos.ts`.
2. Agrega la nueva entidad al arreglo `MARCAS_OBJETIVO`:
```typescript
{
  nombre: "nueva-entidad",
  dominioLegitimo: "nuevaentidad.com.co",
  nombresAlternativos: ["entidad-app", "portal-entidad"]
}
```

---

## 🧪 Ejecución de Pruebas Unitarias

El proyecto cuenta con una suite completa de pruebas unitarias automatizadas con Vitest:

```bash
# Ejecutar todas las pruebas unitarias
npm test

# Ejecutar en modo observación (watch)
npm run test:watch
```

### Cobertura de Pruebas:
* `tests/homoglifos.test.ts`: Validación de distancia Levenshtein, desofuscación de caracteres engañosos y detección de suplantación.
* `tests/extractor-caracteristicas.test.ts`: Extracción sintáctica de URLs, IPs, Punycode, TLDs y diccionarios léxicos.
* `tests/reglas.test.ts`: Evaluación individual de cada regla heurística.
* `tests/reputacion.test.ts`: Verificación de listas de reputación locales y listas personalizadas.
* `tests/motor-riesgo.test.ts`: Puntuación agregada, clasificación y niveles de confianza.
* `tests/falsos-positivos.test.ts`: Verificación estricta de que sitios legítimos oficiales (`google.com`, `bancolombia.com`, `nequi.com.co`, `dian.gov.co`, `github.com`, etc.) mantengan clasificación `SEGURO` sin bloqueos erróneos.

---

## 🔐 Justificación de Permisos (Manifest V3)

PhishGuard aplica el principio de **Mínimo Privilegio**. Solo solicita los siguientes permisos estrictamente necesarios:

| Permiso | Justificación Técnica |
| :--- | :--- |
| `webNavigation` | Requerido para inspeccionar el evento `onBeforeNavigate` del marco principal y evaluar la URL antes de que el navegador descargue código o contenido web malicioso. |
| `storage` | Requerido para almacenar de manera local (`chrome.storage.local`) las configuraciones del usuario (modo de protección, umbrales) y las métricas efímeras de la sesión. |
| `tabs` | Requerido para actualizar la insignia visual (Badge) en la pestaña analizada y para redirigir la pestaña a la página de bloqueo local cuando se detecta un sitio `MALICIOSO`. |

---

## 🛡️ Privacidad y Seguridad por Diseño

* **Sin Almacenamiento de Historial:** La extensión **NO** registra de forma persistente las URLs visitadas ni crea un historial de navegación en disco. Solo almacena métricas numéricas agregadas de la sesión y el último host inspeccionado.
* **Procesamiento 100% Local:** El análisis heurístico, léxico y de reputación se ejecuta completamente en el dispositivo del usuario. No se transmiten URLs a servidores externos en esta fase.
* **Protección contra XSS:** No se utiliza `innerHTML` ni funciones inseguras tipo `eval()`. Todas las interfaces React escapan el texto automáticamente.
* **Content Security Policy (CSP):** Se aplica una estricta política de seguridad: `script-src 'self'; object-src 'self';`.

---

## ⚠️ Limitaciones Actuales del MVP

1. **Reputación Local Estática:** La base de datos de reputación del MVP es local en memoria; no consume en tiempo real feeds de Threat Intelligence dinámicos globales.
2. **Análisis de Contenido Dinámico:** La detección se enfoca principalmente en la capa de URL y sintaxis del dominio. No ejecuta análisis del DOM en profundidad ni detección de formularios de captura de credenciales por JavaScript dinámico.
3. **Entornos de Red Corporativa (Proxies/Captive Portals):** Redirecciones internas en redes Wi-Fi públicas con portales cautivos pueden requerir agregar el host local a la lista blanca personalizada.

---

## 🗺️ Roadmap de Futuras Versiones

En las siguientes fases del proyecto integral de ciberseguridad se desarrollarán:

* ☕ **Backend con Spring Boot & Java:** Microservicios de alta concurrencia y API REST para ingesta y telemetría anónima de amenazas.
* 🐘 **Base de Datos PostgreSQL:** Almacenamiento relacional de campañas de phishing, patrones de ataque y reportes consolidados.
* ⚡ **Caché Distribuida con Redis:** Consultas de reputación de dominios de ultra-baja latencia (< 5ms).
* 🌐 **Integración con Threat Intelligence Externa:** Conectores con VirusTotal, OpenPhish, PhishTank y feeds del CSIRT / ColCERT.
* 🧠 **Modelo de Machine Learning en Python:** Clasificador avanzado con modelos basados en árboles de decisión (XGBoost/LightGBM) o Transformers para detección de patrones semánticos complejos.
* 📱 **Aplicación Móvil Android (Kotlin):** Protección integral para dispositivos móviles contra enlaces fraudulentos.
* 💬 **Detección de Smishing y Mensajería Fraudulenta:** Análisis de mensajes SMS y aplicaciones de mensajería para identificar campañas de estafa financiera en Colombia.
* 📞 **Detección de Números Telefónicos Sospechosos:** Base comunitaria de números reportados por vishing y fraude bancario.
* 👥 **Sistema Colectivo de Reportes Comunitarios:** Plataforma descentralizada para reportar y validar incidentes de ciberseguridad.
