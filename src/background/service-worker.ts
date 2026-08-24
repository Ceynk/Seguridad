import { MotorRiesgo } from "../analyzer/motor-riesgo";
import { ServicioAlmacenamiento, CONFIGURACION_PREDETERMINADA } from "../storage/servicio-almacenamiento";
import { ResultadoAnalisisUrl } from "../types/analisis";
import { ConfiguracionExtension } from "../types/configuracion";

/**
 * Service Worker de segundo plano (Manifest V3).
 * Intercepta la navegación web, coordina el análisis de riesgo y aplica bloqueos/inspecciones.
 */

// Instancia única del motor de riesgo
const motorRiesgo = new MotorRiesgo();

// Caché en memoria de la configuración para respuesta instantánea (cero latencia de storage en navegación)
let configuracionEnMemoria: ConfiguracionExtension = CONFIGURACION_PREDETERMINADA;

// Conjunto en memoria de dominios con excepción temporal concedida por el usuario en la sesión
const dominiosExcepcionTemporal = new Set<string>();

// Caché en memoria del último resultado por pestaña para respuesta inmediata a la interfaz
const mapaResultadosPorPestana = new Map<number, ResultadoAnalisisUrl>();

// Conjunto para evitar bucles de redirección por pestaña
const pestanasEnProcesoBloqueo = new Set<number>();

/**
 * Actualiza el indicador visual (Badge) en el icono de la extensión para una pestaña específica.
 */
function actualizarInsigniaPestana(idPestana: number, resultado: ResultadoAnalisisUrl): void {
  if (typeof chrome === "undefined" || !chrome.action) return;

  switch (resultado.clasificacion) {
    case "MALICIOSO":
      chrome.action.setBadgeText({ tabId: idPestana, text: "ALERTA" });
      chrome.action.setBadgeBackgroundColor({ tabId: idPestana, color: "#DC2626" });
      break;
    case "SOSPECHOSO":
      chrome.action.setBadgeText({ tabId: idPestana, text: "RIESGO" });
      chrome.action.setBadgeBackgroundColor({ tabId: idPestana, color: "#D97706" });
      break;
    case "SEGURO":
      chrome.action.setBadgeText({ tabId: idPestana, text: "OK" });
      chrome.action.setBadgeBackgroundColor({ tabId: idPestana, color: "#16A34A" });
      break;
  }
}

/**
 * Determina si una URL debe ser excluida del análisis.
 */
function esUrlExcluida(urlTexto: string): boolean {
  if (!urlTexto) return true;
  return (
    urlTexto.startsWith("chrome://") ||
    urlTexto.startsWith("chrome-extension://") ||
    urlTexto.startsWith("edge://") ||
    urlTexto.startsWith("about:") ||
    urlTexto.startsWith("devtools://") ||
    urlTexto.startsWith("view-source:") ||
    urlTexto.startsWith("data:") ||
    urlTexto.startsWith("blob:")
  );
}

/**
 * Construye la URL de la página de bloqueo local.
 */
function construirUrlPaginaBloqueo(urlOriginal: string, host: string, resultado: ResultadoAnalisisUrl): string {
  const motivos = resultado.reglasCoincidentes.map((r) => ({
    nombre: r.nombre,
    descripcion: r.descripcion,
    puntuacion: r.puntuacion,
    severidad: r.severidad,
    tecnicaMitre: r.tecnicaMitre,
  }));

  return chrome.runtime.getURL(
    `src/pages/bloqueado/index.html?url=${encodeURIComponent(
      urlOriginal
    )}&puntuacion=${resultado.puntuacionRiesgo}&host=${encodeURIComponent(
      host
    )}&motivos=${encodeURIComponent(JSON.stringify(motivos))}`
  );
}

/**
 * Función centralizada y ultra-rápida de inspección y bloqueo de navegación.
 */
async function procesarNavegacion(idPestana: number, urlTexto: string): Promise<boolean> {
  if (esUrlExcluida(urlTexto)) return false;
  if (!configuracionEnMemoria.proteccionHabilitada) return false;
  if (pestanasEnProcesoBloqueo.has(idPestana)) return false;

  try {
    let urlObj: URL;
    try {
      urlObj = new URL(urlTexto);
    } catch {
      return false;
    }

    const host = urlObj.hostname.toLowerCase();
    if (dominiosExcepcionTemporal.has(host)) return false;

    // Ejecutar análisis heurístico y de reputación
    const resultado = await motorRiesgo.analizarUrl(urlTexto);
    mapaResultadosPorPestana.set(idPestana, resultado);
    actualizarInsigniaPestana(idPestana, resultado);

    // Registro asíncrono en segundo plano (sin retrasar el bloqueo)
    ServicioAlmacenamiento.registrarAnalisis({
      url: urlTexto,
      nombreHost: host,
      puntuacionRiesgo: resultado.puntuacionRiesgo,
      clasificacion: resultado.clasificacion,
      marcaTemporal: resultado.marcaTemporal,
    }).catch(() => {});

    // Si es MALICIOSO y el bloqueo automático está activo, REDIRIGIR INMEDIATAMENTE
    if (
      resultado.clasificacion === "MALICIOSO" &&
      configuracionEnMemoria.bloquearMaliciososAutomaticamente
    ) {
      pestanasEnProcesoBloqueo.add(idPestana);
      const urlBloqueo = construirUrlPaginaBloqueo(urlTexto, host, resultado);

      chrome.tabs.update(idPestana, { url: urlBloqueo }, () => {
        setTimeout(() => {
          pestanasEnProcesoBloqueo.delete(idPestana);
        }, 1000);
      });
      return true;
    }
  } catch {
    // Silencioso ante errores
  }

  return false;
}

/**
 * Inicializa la configuración y eventos del Service Worker.
 */
async function inicializarServiceWorker(): Promise<void> {
  try {
    const config = await ServicioAlmacenamiento.obtenerConfiguracion();
    configuracionEnMemoria = config;
    motorRiesgo.actualizarConfiguracion(config);
  } catch {
    configuracionEnMemoria = CONFIGURACION_PREDETERMINADA;
  }
}

inicializarServiceWorker();

/**
 * Interceptor 1: webNavigation.onBeforeNavigate (Captura antes de la solicitud de red)
 */
if (typeof chrome !== "undefined" && chrome.webNavigation && chrome.webNavigation.onBeforeNavigate) {
  chrome.webNavigation.onBeforeNavigate.addListener((detalles) => {
    if (detalles.frameId === 0) {
      procesarNavegacion(detalles.tabId, detalles.url);
    }
  });
}

/**
 * Interceptor 2: tabs.onUpdated (Captura cambios de URL en la barra de direcciones o enlaces)
 */
if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.onUpdated) {
  chrome.tabs.onUpdated.addListener((idPestana, cambioInfo, pestana) => {
    if (cambioInfo.url) {
      procesarNavegacion(idPestana, cambioInfo.url);
    } else if (cambioInfo.status === "loading" && pestana.url) {
      procesarNavegacion(idPestana, pestana.url);
    }
  });
}

/**
 * Limpieza de recursos al cerrar pestañas.
 */
if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.onRemoved) {
  chrome.tabs.onRemoved.addListener((idPestana) => {
    mapaResultadosPorPestana.delete(idPestana);
    pestanasEnProcesoBloqueo.delete(idPestana);
  });
}

/**
 * Mensajería entre componentes (Popup, Opciones, Bloqueo y Service Worker).
 */
if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.onMessage) {
  chrome.runtime.onMessage.addListener((mensaje, _remitente, responder) => {
    (async () => {
      switch (mensaje.tipo) {
        case "OBTENER_ANALISIS_PESTANA_ACTUAL": {
          const idPestana = mensaje.idPestana;
          const enCache = mapaResultadosPorPestana.get(idPestana);
          if (enCache) {
            responder({ exito: true, datos: enCache });
            return;
          }

          if (mensaje.url && !esUrlExcluida(mensaje.url)) {
            const resultado = await motorRiesgo.analizarUrl(mensaje.url);
            mapaResultadosPorPestana.set(idPestana, resultado);
            actualizarInsigniaPestana(idPestana, resultado);
            responder({ exito: true, datos: resultado });
            return;
          }

          responder({ exito: false, mensaje: "Página del navegador no analizable." });
          break;
        }

        case "PERMITIR_DOMINIO_TEMPORALMENTE": {
          if (mensaje.dominio) {
            dominiosExcepcionTemporal.add(mensaje.dominio.toLowerCase());
            responder({ exito: true });
          }
          break;
        }

        case "ACTUALIZAR_CONFIGURACION": {
          const nuevaConfig = mensaje.configuracion as ConfiguracionExtension;
          if (nuevaConfig) {
            configuracionEnMemoria = nuevaConfig;
            await ServicioAlmacenamiento.guardarConfiguracion(nuevaConfig);
            motorRiesgo.actualizarConfiguracion(nuevaConfig);
            responder({ exito: true });
          }
          break;
        }

        case "PROBAR_BLOQUEO_EN_VIVO": {
          if (mensaje.url) {
            const resultado = await motorRiesgo.analizarUrl(mensaje.url);
            const urlObj = new URL(mensaje.url.startsWith("http") ? mensaje.url : `http://${mensaje.url}`);
            const urlBloqueo = construirUrlPaginaBloqueo(mensaje.url, urlObj.hostname, resultado);
            chrome.tabs.create({ url: urlBloqueo });
            responder({ exito: true });
          }
          break;
        }

        default:
          responder({ exito: false, mensaje: "Tipo de mensaje desconocido" });
      }
    })();

    return true;
  });
}
