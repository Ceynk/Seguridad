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

const PATRON_IPV4 = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
const PATRON_IPV6 = /^\[?([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}\]?$/;

const SUFIJOS_DOBLE_NIVEL = new Set([
  "com.co", "gov.co", "org.co", "edu.co", "net.co", "mil.co", "nom.co",
  "co.uk", "org.uk", "gov.uk", "com.br", "com.mx", "com.ar", "com.pe", "com.cl", "com.es",
]);

export function verificarEsDireccionIp(anfitrion: string): { esIp: boolean; version?: "IPv4" | "IPv6" } {
  const limpio = anfitrion.replace(/^\[|\]$/g, "");
  if (PATRON_IPV4.test(limpio)) return { esIp: true, version: "IPv4" };
  if (PATRON_IPV6.test(limpio)) return { esIp: true, version: "IPv6" };
  return { esIp: false };
}

export function extraerDominioYSubdominios(nombreHost: string): {
  dominioPrincipal: string;
  subdominios: string[];
} {
  const limpio = nombreHost.toLowerCase().trim();
  const partes = limpio.split(".");
  if (partes.length <= 1) return { dominioPrincipal: limpio, subdominios: [] };

  const sufijoDoble = `${partes.at(-2)}.${partes.at(-1)}`;
  const corte = partes.length >= 3 && SUFIJOS_DOBLE_NIVEL.has(sufijoDoble) ? -3 : -2;

  return {
    dominioPrincipal: partes.slice(corte).join("."),
    subdominios: partes.slice(0, corte),
  };
}

export function normalizarYAnalizarUrl(urlEntrada: string): UrlNormalizada {
  try {
    const conProtocolo = /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(urlEntrada.trim())
      ? urlEntrada.trim()
      : `http://${urlEntrada.trim()}`;

    const objetoUrl = new URL(conProtocolo);
    const nombreHost = objetoUrl.hostname.toLowerCase();
    const infoIp = verificarEsDireccionIp(nombreHost);
    const { dominioPrincipal, subdominios } = extraerDominioYSubdominios(nombreHost);

    return {
      urlValida: true,
      urlOriginal: urlEntrada,
      urlSanitizada: objetoUrl.href,
      protocolo: objetoUrl.protocol.replace(":", ""),
      nombreHost,
      dominioPrincipal: infoIp.esIp ? nombreHost : dominioPrincipal,
      subdominios: infoIp.esIp ? [] : subdominios,
      ruta: objetoUrl.pathname,
      parametros: Object.fromEntries(objetoUrl.searchParams.entries()),
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
