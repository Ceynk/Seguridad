import { CaracteristicasUrl, PrincipioCoercion, VectorIngenieriaSocial } from "../tipos/analisis";
import { normalizarYAnalizarUrl } from "../utilidades/normalizador-url";
import { calcularEntropiaShannon } from "../utilidades/cripto-forense";
import { detectarSimilitudMarcas, detectarLetrasRarasYAlfabetoMixto } from "./homoglifos";

export const PALABRAS_AUTENTICACION = [
  "login", "iniciar-sesion", "iniciarsesion", "ingreso", "ingresar", "autenticacion",
  "verificar", "verificacion", "password", "clave", "claves", "actualizar-datos",
  "actualizardatos", "recuperar-cuenta", "desbloquear", "seguridad", "portal",
  "acceso", "token", "confirmar", "validacion",
];

export const PALABRAS_BANCARIAS = [
  "banco", "bancolombia", "nequi", "daviplata", "davivienda", "bbva", "pse",
  "transfiya", "tarjeta", "credito", "debito", "transferencia", "pagos",
  "saldo", "retiro", "ahorros", "colpatria", "avvillas", "cajasocial",
  "popular", "sucursal", "virtual",
];

export const PALABRAS_URGENCIA_Y_ENGANO = [
  "multa", "fotomulta", "comparendo", "simit", "dian", "factura", "embargo",
  "urgente", "bloqueo", "suspension", "suspendida", "premio", "ganador",
  "sorteo", "paquete", "rastreo", "entrega-pendiente", "reembolso",
  "devolucion", "cupon", "alerta", "inmediato", "4-72", "servientrega",
];

export const TLDS_SOSPECHOSOS = new Set([
  "top", "xyz", "tk", "ml", "ga", "cf", "gq", "fit", "surf", "click", "cam",
  "work", "rest", "icu", "monster", "buzz", "pw", "country", "stream", "gdn", "kim", "science",
]);

export function verificarEsPunycode(texto: string): boolean {
  return texto.toLowerCase().includes("xn--");
}

export function calcularPorcentajeCodificacion(urlTexto: string): number {
  const coincidencias = urlTexto.match(/%[0-9a-fA-F]{2}/g);
  if (!coincidencias?.length) return 0;
  return Math.round(((coincidencias.length * 3) / urlTexto.length) * 100);
}

export function extraerExtensionTld(dominioPrincipal: string): string {
  const partes = dominioPrincipal.toLowerCase().split(".");
  return partes.length >= 2 ? partes[partes.length - 1] : "";
}

export function buscarPalabrasClave(texto: string, listaPalabras: string[]): string[] {
  const limpio = texto.toLowerCase();
  return listaPalabras.filter((palabra) => limpio.includes(palabra));
}

const DEFINICIONES_VECTORES: {
  principioCoercion: PrincipioCoercion;
  titulo: string;
  descripcion: string;
  origen: "urgencia" | "banca" | "autenticacion";
  terminos?: Set<string>;
}[] = [
  {
    principioCoercion: "MIEDO_SANCION",
    titulo: "Miedo a Sanciones / Coerción Legal",
    descripcion:
      "El atacante induce miedo mediante supuestos embargos, multas o suspensiones judiciales para forzar una acción apresurada.",
    origen: "urgencia",
    terminos: new Set(["multa", "fotomulta", "comparendo", "simit", "embargo", "dian", "suspension", "suspendida"]),
  },
  {
    principioCoercion: "URGENCIA_TEMPORAL",
    titulo: "Presión de Urgencia Temporal",
    descripcion:
      "Intenta reducir la capacidad crítica de la víctima imponiendo una sensación de límite temporal inmediato.",
    origen: "urgencia",
    terminos: new Set(["urgente", "inmediato", "alerta", "bloqueo", "desbloquear"]),
  },
  {
    principioCoercion: "AUTORIDAD_INSTITUCIONAL",
    titulo: "Suplantación de Autoridad Financiera",
    descripcion:
      "Se aprovecha de la confianza del usuario hacia entidades bancarias o pasarelas de pago consolidadas.",
    origen: "banca",
  },
  {
    principioCoercion: "RECOMPENSA_FINANCIERA",
    titulo: "Cebo de Recompensa Financiera",
    descripcion:
      "Atrae a la víctima con falsas promesas de dinero, premios o devoluciones pendientes.",
    origen: "urgencia",
    terminos: new Set(["premio", "ganador", "sorteo", "reembolso", "devolucion", "cupon"]),
  },
  {
    principioCoercion: "ACCESO_CRITICO",
    titulo: "Captura de Credenciales y Factores de Autenticación",
    descripcion:
      "Punto de recolección de credenciales sensibles, contraseñas, claves dinámicas o códigos.",
    origen: "autenticacion",
  },
];

export function clasificarVectoresIngenieriaSocial(
  contieneAutenticacion: string[],
  contieneBancarias: string[],
  contieneUrgencia: string[]
): VectorIngenieriaSocial[] {
  const fuentes = {
    autenticacion: contieneAutenticacion,
    banca: contieneBancarias,
    urgencia: contieneUrgencia,
  };

  return DEFINICIONES_VECTORES.flatMap(({ principioCoercion, titulo, descripcion, origen, terminos }) => {
    const identificados = terminos
      ? fuentes[origen].filter((termino) => terminos.has(termino))
      : fuentes[origen];

    return identificados.length > 0
      ? [{ principioCoercion, titulo, descripcion, terminosIdentificados: identificados }]
      : [];
  });
}

export function extraerCaracteristicasUrl(urlEntrada: string): CaracteristicasUrl {
  const normalizada = normalizarYAnalizarUrl(urlEntrada);
  const { urlSanitizada: urlCompleta, nombreHost, dominioPrincipal, ruta, subdominios, parametros } = normalizada;
  const extensionTld = extraerExtensionTld(dominioPrincipal);

  let tieneArroba = false;
  try {
    const objetoUrl = new URL(urlCompleta);
    tieneArroba = Boolean(objetoUrl.username || objetoUrl.password);
  } catch {}

  const textoBusqueda = `${nombreHost}${ruta}`;
  const contienePalabrasAutenticacion = buscarPalabrasClave(textoBusqueda, PALABRAS_AUTENTICACION);
  const contienePalabrasBancarias = buscarPalabrasClave(textoBusqueda, PALABRAS_BANCARIAS);
  const contienePalabrasUrgencia = buscarPalabrasClave(textoBusqueda, PALABRAS_URGENCIA_Y_ENGANO);
  const analisisLetrasRaras = detectarLetrasRarasYAlfabetoMixto(nombreHost);

  return {
    urlCompleta,
    protocolo: normalizada.protocolo,
    nombreHost,
    dominioPrincipal,
    subdominios,
    cantidadSubdominios: subdominios.length,
    longitudTotal: urlCompleta.length,
    longitudDominio: nombreHost.length,
    longitudRuta: ruta.length,
    ruta,
    parametros,
    cantidadParametros: Object.keys(parametros).length,
    esIpDirecta: normalizada.esIp,
    versionIp: normalizada.versionIp,
    esPunycode: verificarEsPunycode(nombreHost),
    tieneArroba,
    porcentajeCodificacionUrl: calcularPorcentajeCodificacion(urlCompleta),
    cantidadGuiones: (nombreHost.match(/-/g) || []).length,
    cantidadPuntos: (nombreHost.match(/\./g) || []).length,
    caracteresEspecialesSospechosos: (nombreHost.match(/[@_~$!=+]/g) || []).length,
    extensionTld,
    tldEsSospechoso: TLDS_SOSPECHOSOS.has(extensionTld),
    contienePalabrasAutenticacion,
    contienePalabrasBancarias,
    contienePalabrasUrgencia,
    similitudMarcas: detectarSimilitudMarcas(nombreHost, dominioPrincipal),
    entropiaShannon: calcularEntropiaShannon(nombreHost),
    vectoresIngenieriaSocial: clasificarVectoresIngenieriaSocial(
      contienePalabrasAutenticacion,
      contienePalabrasBancarias,
      contienePalabrasUrgencia
    ),
    dominioDecodificadoUnicode: analisisLetrasRaras.hostUnicode,
    tieneLetrasRarasHomoglifos: analisisLetrasRaras.tieneLetrasRaras,
    tieneAlfabetoMixto: analisisLetrasRaras.tieneAlfabetoMixto,
    desgloseLetrasRaras: analisisLetrasRaras.desglose,
  };
}
