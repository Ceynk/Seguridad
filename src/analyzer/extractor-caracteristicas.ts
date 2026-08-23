import { CaracteristicasUrl, VectorIngenieriaSocial } from "../types/analisis";
import { normalizarYAnalizarUrl } from "../utils/normalizador-url";
import { calcularEntropiaShannon } from "../utils/cripto-forense";
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
    return textoLimpio.includes(palabra);
  });
}

/**
 * Analiza el contexto de la URL para identificar los vectores psicológicos de ingeniería social (Cialdini).
 */
export function clasificarVectoresIngenieriaSocial(
  contieneAutenticacion: string[],
  contieneBancarias: string[],
  contieneUrgencia: string[]
): VectorIngenieriaSocial[] {
  const vectores: VectorIngenieriaSocial[] = [];

  if (
    contieneUrgencia.some((t) =>
      ["multa", "fotomulta", "comparendo", "simit", "embargo", "dian", "suspension", "suspendida"].includes(t)
    )
  ) {
    vectores.push({
      principioCoercion: "MIEDO_SANCION",
      titulo: "Miedo a Sanciones / Coerción Legal",
      descripcion:
        "El atacante induce miedo mediante supuestos embargos, multas o suspensiones judiciales para forzar una acción apresurada.",
      terminosIdentificados: contieneUrgencia.filter((t) =>
        ["multa", "fotomulta", "comparendo", "simit", "embargo", "dian", "suspension", "suspendida"].includes(t)
      ),
    });
  }

  if (
    contieneUrgencia.some((t) =>
      ["urgente", "inmediato", "alerta", "bloqueo", "desbloquear"].includes(t)
    )
  ) {
    vectores.push({
      principioCoercion: "URGENCIA_TEMPORAL",
      titulo: "Presión de Urgencia Temporal",
      descripcion:
        "Intenta reducir la capacidad crítica de la víctima imponiendo una sensación de límite temporal inmediato.",
      terminosIdentificados: contieneUrgencia.filter((t) =>
        ["urgente", "inmediato", "alerta", "bloqueo", "desbloquear"].includes(t)
      ),
    });
  }

  if (contieneBancarias.length > 0) {
    vectores.push({
      principioCoercion: "AUTORIDAD_INSTITUCIONAL",
      titulo: "Suplantación de Autoridad Financiera",
      descripcion:
        "Se aprovecha de la confianza del usuario hacia entidades bancarias o pasarelas de pago consolidadas.",
      terminosIdentificados: contieneBancarias,
    });
  }

  if (
    contieneUrgencia.some((t) =>
      ["premio", "ganador", "sorteo", "reembolso", "devolucion", "cupon"].includes(t)
    )
  ) {
    vectores.push({
      principioCoercion: "RECOMPENSA_FINANCIERA",
      titulo: "Cebo de Recompensa Financiera (Baiting)",
      descripcion:
        "Atrae a la víctima con falsas promesas de dinero, premios o devoluciones pendientes.",
      terminosIdentificados: contieneUrgencia.filter((t) =>
        ["premio", "ganador", "sorteo", "reembolso", "devolucion", "cupon"].includes(t)
      ),
    });
  }

  if (contieneAutenticacion.length > 0) {
    vectores.push({
      principioCoercion: "ACCESO_CRITICO",
      titulo: "Captura de Credenciales y Factores de Autenticación",
      descripcion:
        "Punto de recolección de credenciales sensibles, contraseñas, claves dinámicas o tokens.",
      terminosIdentificados: contieneAutenticacion,
    });
  }

  return vectores;
}

/**
 * Extrae todas las características estáticas, léxicas y forenses de una URL dada.
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
  const entropiaShannon = calcularEntropiaShannon(nombreHost);
  const vectoresIngenieriaSocial = clasificarVectoresIngenieriaSocial(
    contienePalabrasAutenticacion,
    contienePalabrasBancarias,
    contienePalabrasUrgencia
  );

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
    entropiaShannon,
    vectoresIngenieriaSocial,
  };
}
