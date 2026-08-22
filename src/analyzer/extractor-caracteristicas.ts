import { CaracteristicasUrl } from "../types/analisis";
import { normalizarYAnalizarUrl } from "../utils/normalizador-url";
import { detectarSimilitudMarcas } from "./homoglifos";

/**
 * Diccionarios de palabras clave frecuentemente utilizadas en ataques de ingeniería social y phishing.
 */
export const PALABRAS_AUTENTICACION = [
  "login",
  "iniciar-sesion",
  "iniciarsesion",
  "ingreso",
  "ingresar",
  "autenticacion",
  "verificar",
  "verificacion",
  "password",
  "clave",
  "claves",
  "actualizar-datos",
  "actualizardatos",
  "recuperar-cuenta",
  "desbloquear",
  "seguridad",
  "portal",
  "acceso",
  "token",
  "confirmar",
  "validacion",
];

export const PALABRAS_BANCARIAS = [
  "banco",
  "bancolombia",
  "nequi",
  "daviplata",
  "davivienda",
  "bbva",
  "pse",
  "transfiya",
  "tarjeta",
  "credito",
  "debito",
  "transferencia",
  "pagos",
  "saldo",
  "retiro",
  "ahorros",
  "colpatria",
  "avvillas",
  "cajasocial",
  "popular",
  "sucursal",
  "virtual",
];

export const PALABRAS_URGENCIA_Y_ENGANO = [
  "multa",
  "fotomulta",
  "comparendo",
  "simit",
  "dian",
  "factura",
  "embargo",
  "urgente",
  "bloqueo",
  "suspension",
  "suspendida",
  "premio",
  "ganador",
  "sorteo",
  "paquete",
  "rastreo",
  "entrega-pendiente",
  "reembolso",
  "devolucion",
  "cupon",
  "alerta",
  "inmediato",
  "4-72",
  "servientrega",
];

export const TLDS_SOSPECHOSOS = new Set([
  "top",
  "xyz",
  "tk",
  "ml",
  "ga",
  "cf",
  "gq",
  "fit",
  "surf",
  "click",
  "cam",
  "work",
  "rest",
  "icu",
  "monster",
  "buzz",
  "pw",
  "country",
  "stream",
  "gdn",
  "kim",
  "science",
]);

/**
 * Determina si una cadena contiene caracteres de Punycode (IDN).
 */
export function verificarEsPunycode(texto: string): boolean {
  return texto.toLowerCase().includes("xn--");
}

/**
 * Calcula el porcentaje de caracteres codificados en porcentaje (%XX) en una URL.
 */
export function calcularPorcentajeCodificacion(urlTexto: string): number {
  const coincidencias = urlTexto.match(/%[0-9a-fA-F]{2}/g);
  if (!coincidencias || coincidencias.length === 0) return 0;
  const caracteresCodificados = coincidencias.length * 3;
  return Math.round((caracteresCodificados / urlTexto.length) * 100);
}

/**
 * Extrae la extensión TLD final de un dominio.
 */
export function extraerExtensionTld(dominioPrincipal: string): string {
  const partes = dominioPrincipal.toLowerCase().split(".");
  if (partes.length < 2) return "";
  return partes[partes.length - 1];
}

/**
 * Busca ocurrencias de un conjunto de palabras clave en un texto.
 */
export function buscarPalabrasClave(texto: string, listaPalabras: string[]): string[] {
  const textoLimpio = texto.toLowerCase();
  return listaPalabras.filter((palabra) => {
    // Coincidencia exacta de substring o separada por guiones/puntos
    return textoLimpio.includes(palabra);
  });
}

/**
 * Extrae todas las características estáticas y léxicas de una URL dada.
 */
export function extraerCaracteristicasUrl(urlEntrada: string): CaracteristicasUrl {
  const urlNormalizada = normalizarYAnalizarUrl(urlEntrada);
  const urlCompleta = urlNormalizada.urlSanitizada;
  const nombreHost = urlNormalizada.nombreHost;
  const dominioPrincipal = urlNormalizada.dominioPrincipal;
  const ruta = urlNormalizada.ruta;
  const extensionTld = extraerExtensionTld(dominioPrincipal);

  // Detección de caracteres especiales en el dominio
  const cantidadGuiones = (nombreHost.match(/-/g) || []).length;
  const cantidadPuntos = (nombreHost.match(/\./g) || []).length;
  const caracteresEspecialesSospechosos = (nombreHost.match(/[@_~$!=+]/g) || []).length;

  const textoParaBuscarPalabras = `${nombreHost}${ruta}`;

  const contienePalabrasAutenticacion = buscarPalabrasClave(textoParaBuscarPalabras, PALABRAS_AUTENTICACION);
  const contienePalabrasBancarias = buscarPalabrasClave(textoParaBuscarPalabras, PALABRAS_BANCARIAS);
  const contienePalabrasUrgencia = buscarPalabrasClave(textoParaBuscarPalabras, PALABRAS_URGENCIA_Y_ENGANO);

  const similitudMarcas = detectarSimilitudMarcas(nombreHost, dominioPrincipal);

  return {
    urlCompleta,
    protocolo: urlNormalizada.protocolo,
    nombreHost,
    dominioPrincipal,
    subdominios: urlNormalizada.subdominios,
    cantidadSubdominios: urlNormalizada.subdominios.length,
    longitudTotal: urlCompleta.length,
    longitudDominio: nombreHost.length,
    longitudRuta: ruta.length,
    ruta,
    parametros: urlNormalizada.parametros,
    cantidadParametros: Object.keys(urlNormalizada.parametros).length,
    esIpDirecta: urlNormalizada.esIp,
    versionIp: urlNormalizada.versionIp,
    esPunycode: verificarEsPunycode(nombreHost),
    tieneArroba: urlEntrada.includes("@"),
    porcentajeCodificacionUrl: calcularPorcentajeCodificacion(urlCompleta),
    cantidadGuiones,
    cantidadPuntos,
    caracteresEspecialesSospechosos,
    extensionTld,
    tldEsSospechoso: TLDS_SOSPECHOSOS.has(extensionTld),
    contienePalabrasAutenticacion,
    contienePalabrasBancarias,
    contienePalabrasUrgencia,
    similitudMarcas,
  };
}
