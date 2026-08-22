/**
 * Utilidades para normalización, sanitización y análisis sintáctico seguro de URLs.
 */

export interface UrlNormalizada {
  urlValida: boolean;
  urlOriginal: string;
  urlSanitizada: string;
  protocolo: string;
  nombreHost: string;
  dominioPrincipal: string;
  subdominios: string[];
  ruta: string;
  parametros: Record<string, string>;
  puerto: string;
  esIp: boolean;
  versionIp?: "IPv4" | "IPv6";
}

// Expresión regular para validar direcciones IPv4
const REGEX_IPV4 = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;

// Expresión regular para validar direcciones IPv6
const REGEX_IPV6 = /^\[?([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}\]?$/;

// Sufijos conocidos de dos niveles comunes en Colombia y Latinoamérica
const SUFIJOS_DOBLE_NIVEL = new Set([
  "com.co",
  "gov.co",
  "org.co",
  "edu.co",
  "net.co",
  "mil.co",
  "nom.co",
  "co.uk",
  "com.br",
  "com.mx",
  "com.ar",
  "com.pe",
  "com.cl",
  "com.es",
  "org.uk",
  "gov.uk",
]);

/**
 * Determina si una cadena es una dirección IP válida (IPv4 o IPv6).
 */
export function verificarEsDireccionIp(host: string): { esIp: boolean; version?: "IPv4" | "IPv6" } {
  const hostLimpio = host.replace(/^\[|\]$/g, "");
  if (REGEX_IPV4.test(hostLimpio)) {
    return { esIp: true, version: "IPv4" };
  }
  if (REGEX_IPV6.test(hostLimpio)) {
    return { esIp: true, version: "IPv6" };
  }
  return { esIp: false };
}

/**
 * Extrae el dominio principal y los subdominios teniendo en cuenta TLDs simples y de doble nivel.
 */
export function extraerDominioYSubdominios(nombreHost: string): {
  dominioPrincipal: string;
  subdominios: string[];
} {
  const hostMinusculas = nombreHost.toLowerCase().trim();
  const partes = hostMinusculas.split(".");

  if (partes.length <= 1) {
    return { dominioPrincipal: hostMinusculas, subdominios: [] };
  }

  // Verificar si termina en sufijo de doble nivel (ej: bancolombia.com.co)
  if (partes.length >= 3) {
    const posibleSufijoDoble = `${partes[partes.length - 2]}.${partes[partes.length - 1]}`;
    if (SUFIJOS_DOBLE_NIVEL.has(posibleSufijoDoble)) {
      const nombreDominio = partes[partes.length - 3];
      const dominioPrincipal = `${nombreDominio}.${posibleSufijoDoble}`;
      const subdominios = partes.slice(0, partes.length - 3);
      return { dominioPrincipal, subdominios };
    }
  }

  // TLD de un solo nivel (ej: bancolombia.com)
  const dominioPrincipal = `${partes[partes.length - 2]}.${partes[partes.length - 1]}`;
  const subdominios = partes.slice(0, partes.length - 2);

  return { dominioPrincipal, subdominios };
}

/**
 * Normaliza y analiza sintácticamente una URL con manejo seguro de errores.
 */
export function normalizarYAnalizarUrl(urlEntrada: string): UrlNormalizada {
  try {
    let urlProcesada = urlEntrada.trim();

    // Si no incluye protocolo, agregamos http por defecto para parsear
    if (!/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(urlProcesada)) {
      urlProcesada = `http://${urlProcesada}`;
    }

    const objetoUrl = new URL(urlProcesada);
    const nombreHost = objetoUrl.hostname.toLowerCase();
    const infoIp = verificarEsDireccionIp(nombreHost);
    const { dominioPrincipal, subdominios } = extraerDominioYSubdominios(nombreHost);

    const parametros: Record<string, string> = {};
    objetoUrl.searchParams.forEach((valor, clave) => {
      parametros[clave] = valor;
    });

    return {
      urlValida: true,
      urlOriginal: urlEntrada,
      urlSanitizada: objetoUrl.href,
      protocolo: objetoUrl.protocol.replace(":", ""),
      nombreHost,
      dominioPrincipal: infoIp.esIp ? nombreHost : dominioPrincipal,
      subdominios: infoIp.esIp ? [] : subdominios,
      ruta: objetoUrl.pathname,
      parametros,
      puerto: objetoUrl.port,
      esIp: infoIp.esIp,
      versionIp: infoIp.version,
    };
  } catch {
    return {
      urlValida: false,
      urlOriginal: urlEntrada,
      urlSanitizada: urlEntrada,
      protocolo: "desconocido",
      nombreHost: "",
      dominioPrincipal: "",
      subdominios: [],
      ruta: "",
      parametros: {},
      puerto: "",
      esIp: false,
    };
  }
}
