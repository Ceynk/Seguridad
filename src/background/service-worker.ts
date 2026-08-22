import { MotorRiesgo } from "../analyzer/motor-riesgo";
import { ServicioAlmacenamiento } from "../storage/servicio-almacenamiento";
import { ResultadoAnalisisUrl } from "../types/analisis";
import { ConfiguracionExtension } from "../types/configuracion";

/**
 * Service Worker de segundo plano (Manifest V3).
 * Intercepta la navegación web, coordina el análisis de riesgo y aplica bloqueos/inspecciones.
 */

// Instancia única del motor de riesgo para el ciclo de vida del Service Worker
const motorRiesgo = new MotorRiesgo();

// Conjunto en memoria de dominios con excepción temporal concedida por el usuario en la sesión
const dominiosExcepcionTemporal = new Set<string>();

// Caché en memoria del último resultado por pestaña para respuesta inmediata a la interfaz
const mapaResultadosPorPestana = new Map<number, ResultadoAnalisisUrl>();

/**
 * Actualiza el indicador visual (Badge) en el icono de la extensión para una pestaña específica.
 */
function actualizarInsigniaPestana(idPestana: number, resultado: ResultadoAnalisisUrl): void {
  if (typeof chrome === "undefined" || !chrome.action) return;

  switch (resultado.clasificacion) {
    case "MALICIOSO":
      chrome.action.setBadgeText({ tabId: idPestana, text: "ALERTA" });
      chrome.action.setBadgeBackgroundColor({ tabId: idPestana, color: "#DC2626" }); // Rojo
      break;
    case "SOSPECHOSO":
      chrome.action.setBadgeText({ tabId: idPestana, text: "RIESGO" });
      chrome.action.setBadgeBackgroundColor({ tabId: idPestana, color: "#D97706" }); // Naranja
      break;
    case "SEGURO":
      chrome.action.setBadgeText({ tabId: idPestana, text: "OK" });
      chrome.action.setBadgeBackgroundColor({ tabId: idPestana, color: "#16A34A" }); // Verde
      break;
  }
}

/**
 * Determina si una URL debe ser excluida del análisis (recursos internos, extensiones o esquemas especiales).
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
 * Inicializa la configuración y eventos del Service Worker.
 */
async function inicializarServiceWorker(): Promise<void> {
  const configuracion = await ServicioAlmacenamiento.obtenerConfiguracion();
  motorRiesgo.actualizarConfiguracion(configuracion);
}

// Cargar configuración al iniciar
inicializarServiceWorker();

/**
 * Interceptor de navegación antes de la carga de la página.
 */
if (typeof chrome !== "undefined" && chrome.webNavigation && chrome.webNavigation.onBeforeNavigate) {
  chrome.webNavigation.onBeforeNavigate.addListener(async (detalles) => {
    // Analizar únicamente el marco principal (frameId === 0)
    if (detalles.frameId !== 0) return;

    const url = detalles.url;
    if (esUrlExcluida(url)) return;

    try {
      const configuracion = await ServicioAlmacenamiento.obtenerConfiguracion();
      if (!configuracion.proteccionHabilitada) return;

      const urlObj = new URL(url);
      const host = urlObj.hostname.toLowerCase();

      // Si el usuario autorizó temporalmente este dominio en esta sesión, permitir paso
      if (dominiosExcepcionTemporal.has(host)) return;

      // Realizar análisis de riesgo de la URL
      const resultado = await motorRiesgo.analizarUrl(url);

      // Guardar en caché para consulta rápida del Popup
      mapaResultadosPorPestana.set(detalles.tabId, resultado);

      // Registrar métrica de navegación de forma segura (sin historial persistente)
      await ServicioAlmacenamiento.registrarAnalisis({
        url,
        nombreHost: host,
        puntuacionRiesgo: resultado.puntuacionRiesgo,
        clasificacion: resultado.clasificacion,
        marcaTemporal: resultado.marcaTemporal,
      });

      // Actualizar badge visual en la barra de herramientas
      actualizarInsigniaPestana(detalles.tabId, resultado);

      // Bloquear navegación si la URL es MALICIOSA y el bloqueo automático está activado
      if (
        resultado.clasificacion === "MALICIOSO" &&
        configuracion.bloquearMaliciososAutomaticamente
      ) {
        const urlPaginaBloqueo = chrome.runtime.getURL(
          `src/pages/bloqueado/index.html?url=${encodeURIComponent(
            url
          )}&puntuacion=${resultado.puntuacionRiesgo}&host=${encodeURIComponent(
            host
          )}&motivos=${encodeURIComponent(
            JSON.stringify(
              resultado.reglasCoincidentes.map((r) => ({
                nombre: r.nombre,
                descripcion: r.descripcion,
                puntuacion: r.puntuacion,
                severidad: r.severidad,
              }))
            )
          )}`
        );

        chrome.tabs.update(detalles.tabId, { url: urlPaginaBloqueo });
      }
    } catch {
      // Manejo silencioso ante errores de navegación para no interrumpir al usuario
    }
  });
}

/**
 * Limpieza de recursos al cerrar pestañas.
 */
if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.onRemoved) {
  chrome.tabs.onRemoved.addListener((idPestana) => {
    mapaResultadosPorPestana.delete(idPestana);
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
            await ServicioAlmacenamiento.guardarConfiguracion(nuevaConfig);
            motorRiesgo.actualizarConfiguracion(nuevaConfig);
            responder({ exito: true });
          }
          break;
        }

        case "ANALIZAR_URL_MANUAL": {
          if (mensaje.url) {
            const resultado = await motorRiesgo.analizarUrl(mensaje.url);
            responder({ exito: true, datos: resultado });
          } else {
            responder({ exito: false, mensaje: "URL no proporcionada" });
          }
          break;
        }

        default:
          responder({ exito: false, mensaje: "Tipo de mensaje desconocido" });
      }
    })();

    return true; // Respuesta asíncrona
  });
}
