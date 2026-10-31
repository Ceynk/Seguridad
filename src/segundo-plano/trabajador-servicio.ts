import { MotorRiesgo } from "../analizador/motor-riesgo";
import { decodificarDominioPunycode } from "../analizador/homoglifos";
import { coincideDominio } from "../reputacion/reputacion-local";
import {
  CONFIGURACION_PREDETERMINADA,
  ServicioAlmacenamiento,
} from "../almacenamiento/servicio-almacenamiento";
import { ClasificacionRiesgo, ResultadoAnalisisUrl } from "../tipos/analisis";
import { ConfiguracionExtension } from "../tipos/configuracion";

const motorRiesgo = new MotorRiesgo();

let configuracionEnMemoria: ConfiguracionExtension = CONFIGURACION_PREDETERMINADA;
const dominiosExcepcionTemporal = new Set<string>();
const resultadosPorPestana = new Map<number, ResultadoAnalisisUrl>();
const pestanasEnBloqueo = new Set<number>();

const INSIGNIAS: Record<ClasificacionRiesgo, { texto: string; color: string }> = {
  MALICIOSO: { texto: "ALERTA", color: "#DC2626" },
  SOSPECHOSO: { texto: "RIESGO", color: "#D97706" },
  SEGURO: { texto: "OK", color: "#16A34A" },
};

const PREFIJOS_EXCLUIDOS = [
  "chrome://",
  "chrome-extension://",
  "edge://",
  "about:",
  "devtools://",
  "view-source:",
  "data:",
  "blob:",
];

function actualizarInsigniaPestana(idPestana: number, clasificacion: ClasificacionRiesgo): void {
  if (typeof chrome === "undefined" || !chrome.action) return;
  const { texto, color } = INSIGNIAS[clasificacion];
  chrome.action.setBadgeText({ tabId: idPestana, text: texto });
  chrome.action.setBadgeBackgroundColor({ tabId: idPestana, color });
}

function esUrlExcluida(urlTexto: string): boolean {
  return !urlTexto || PREFIJOS_EXCLUIDOS.some((prefijo) => urlTexto.startsWith(prefijo));
}

function construirUrlPaginaBloqueo(
  urlOriginal: string,
  anfitrion: string,
  resultado: ResultadoAnalisisUrl
): string {
  const motivos = resultado.reglasCoincidentes.map((r) => ({
    nombre: r.nombre,
    descripcion: r.descripcion,
    puntuacion: r.puntuacion,
    severidad: r.severidad,
    tecnicaMitre: r.tecnicaMitre,
  }));

  const consulta = new URLSearchParams({
    url: urlOriginal,
    puntuacion: String(resultado.puntuacionRiesgo),
    host: anfitrion,
    motivos: JSON.stringify(motivos),
  });

  return chrome.runtime.getURL(`src/paginas/bloqueado/index.html?${consulta.toString()}`);
}

function esDominioExcluidoTemporalmente(anfitrion: string): boolean {
  for (const dominio of dominiosExcepcionTemporal) {
    if (coincideDominio(anfitrion, dominio)) return true;
  }
  return false;
}

function redirigirABloqueo(
  idPestana: number,
  urlOriginal: string,
  anfitrion: string,
  resultado: ResultadoAnalisisUrl
): void {
  pestanasEnBloqueo.add(idPestana);
  const urlBloqueo = construirUrlPaginaBloqueo(urlOriginal, anfitrion, resultado);
  chrome.tabs.update(idPestana, { url: urlBloqueo }, () => {
    setTimeout(() => pestanasEnBloqueo.delete(idPestana), 1000);
  });
}

const inicializacion = ServicioAlmacenamiento.obtenerConfiguracion()
  .then((configuracion) => {
    configuracionEnMemoria = configuracion;
    motorRiesgo.actualizarConfiguracion(configuracion);
  })
  .catch(() => {
    configuracionEnMemoria = CONFIGURACION_PREDETERMINADA;
  });

async function procesarNavegacion(idPestana: number, urlTexto: string): Promise<boolean> {
  if (esUrlExcluida(urlTexto)) return false;
  await inicializacion;
  if (!configuracionEnMemoria.proteccionHabilitada || pestanasEnBloqueo.has(idPestana)) {
    return false;
  }

  try {
    const objetoUrl = new URL(urlTexto);
    const anfitrion = objetoUrl.hostname.toLowerCase();
    if (esDominioExcluidoTemporalmente(anfitrion)) return false;

    const resultado = await motorRiesgo.analizarUrl(urlTexto);
    resultadosPorPestana.set(idPestana, resultado);
    actualizarInsigniaPestana(idPestana, resultado.clasificacion);

    ServicioAlmacenamiento.registrarAnalisis({
      url: urlTexto,
      nombreHost: anfitrion,
      puntuacionRiesgo: resultado.puntuacionRiesgo,
      clasificacion: resultado.clasificacion,
      marcaTemporal: resultado.marcaTemporal,
    }).catch(() => {});

    if (
      resultado.clasificacion === "MALICIOSO" &&
      resultado.esBloqueoAltaConfianza &&
      configuracionEnMemoria.bloquearMaliciososAutomaticamente
    ) {
      redirigirABloqueo(idPestana, urlTexto, anfitrion, resultado);
      return true;
    }
  } catch {}

  return false;
}

if (typeof chrome !== "undefined") {
  chrome.webNavigation?.onBeforeNavigate?.addListener((detalles) => {
    if (detalles.frameId === 0) {
      void procesarNavegacion(detalles.tabId, detalles.url);
    }
  });

  chrome.tabs?.onUpdated?.addListener((idPestana, cambio, pestana) => {
    const urlObjetivo = cambio.url || (cambio.status === "loading" ? pestana.url : undefined);
    if (urlObjetivo) {
      void procesarNavegacion(idPestana, urlObjetivo);
    }
  });

  chrome.tabs?.onRemoved?.addListener((idPestana) => {
    resultadosPorPestana.delete(idPestana);
    pestanasEnBloqueo.delete(idPestana);
  });

  chrome.runtime?.onMessage?.addListener((mensaje, remitente, responder) => {
    (async () => {
      switch (mensaje.tipo) {
        case "OBTENER_ANALISIS_PESTANA_ACTUAL": {
          const enMemoria = resultadosPorPestana.get(mensaje.idPestana);
          if (enMemoria) {
            responder({ exito: true, datos: enMemoria });
            return;
          }
          if (mensaje.url && !esUrlExcluida(mensaje.url)) {
            const resultado = await motorRiesgo.analizarUrl(mensaje.url);
            resultadosPorPestana.set(mensaje.idPestana, resultado);
            actualizarInsigniaPestana(mensaje.idPestana, resultado.clasificacion);
            responder({ exito: true, datos: resultado });
            return;
          }
          responder({ exito: false, mensaje: "Página del navegador no analizable." });
          break;
        }

        case "PERMITIR_DOMINIO_TEMPORALMENTE": {
          if (mensaje.dominio) {
            const limpio = String(mensaje.dominio).toLowerCase();
            dominiosExcepcionTemporal.add(limpio);
            try {
              dominiosExcepcionTemporal.add(decodificarDominioPunycode(limpio).toLowerCase());
            } catch {}
            responder({ exito: true });
          }
          break;
        }

        case "ACTUALIZAR_CONFIGURACION": {
          const nuevaConfiguracion = mensaje.configuracion as ConfiguracionExtension | undefined;
          if (nuevaConfiguracion) {
            configuracionEnMemoria = nuevaConfiguracion;
            await ServicioAlmacenamiento.guardarConfiguracion(nuevaConfiguracion);
            motorRiesgo.actualizarConfiguracion(nuevaConfiguracion);
            responder({ exito: true });
          }
          break;
        }

        case "PROBAR_BLOQUEO_EN_VIVO": {
          if (mensaje.url) {
            const resultado = await motorRiesgo.analizarUrl(mensaje.url);
            const conProtocolo = mensaje.url.startsWith("http") ? mensaje.url : `http://${mensaje.url}`;
            const objetoUrl = new URL(conProtocolo);
            chrome.tabs.create({
              url: construirUrlPaginaBloqueo(mensaje.url, objetoUrl.hostname, resultado),
            });
            responder({ exito: true });
          }
          break;
        }

        case "ANALIZAR_ENLACE": {
          if (mensaje.url) {
            responder({ exito: true, datos: await motorRiesgo.analizarUrl(mensaje.url) });
          }
          break;
        }

        case "ANALIZAR_DOCUMENTO": {
          const idPestana = typeof mensaje.idPestana === "number" ? mensaje.idPestana : remitente.tab?.id;
          if (typeof idPestana === "number") {
            const resultado = resultadosPorPestana.get(idPestana);
            if (resultado && resultado.clasificacion === "MALICIOSO" && Boolean(mensaje.tieneFormularioSensible)) {
              resultado.esBloqueoAltaConfianza = true;
              resultado.requiereConfirmacionDocumento = false;
              resultadosPorPestana.set(idPestana, resultado);
              const urlOriginal = remitente.tab?.url;
              if (
                urlOriginal &&
                configuracionEnMemoria.bloquearMaliciososAutomaticamente &&
                !pestanasEnBloqueo.has(idPestana)
              ) {
                redirigirABloqueo(idPestana, urlOriginal, resultado.nombreHost, resultado);
              }
            }
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
