import {
  EntradaReputacion,
  ProveedorReputacion,
  ResultadoReputacion,
  TipoListaReputacion,
} from "../tipos/reputacion";

const FECHA_BASE = "2026-01-01";

function crearEntradas(
  tipo: TipoListaReputacion,
  registros: [string, string, string][]
): EntradaReputacion[] {
  return registros.map(([dominio, categoria, descripcion]) => ({
    dominio,
    tipo,
    categoria,
    fechaRegistro: FECHA_BASE,
    descripcion,
  }));
}

export const DOMINIOS_CONFIABLES_LOCALES: EntradaReputacion[] = crearEntradas("BLANCA", [
  ["google.com", "Tecnología/Búsqueda", "Dominio oficial de Google"],
  ["google.com.co", "Tecnología/Búsqueda", "Dominio oficial de Google Colombia"],
  ["microsoft.com", "Tecnología", "Dominio oficial de Microsoft"],
  ["github.com", "Desarrollo", "Dominio oficial de GitHub"],
  ["apple.com", "Tecnología", "Dominio oficial de Apple"],
  ["amazon.com", "Comercio", "Dominio oficial de Amazon"],
  ["netflix.com", "Entretenimiento", "Dominio oficial de Netflix"],
  ["facebook.com", "Redes Sociales", "Dominio oficial de Facebook"],
  ["instagram.com", "Redes Sociales", "Dominio oficial de Instagram"],
  ["cdninstagram.com", "Redes Sociales", "CDN oficial de Instagram"],
  ["whatsapp.com", "Mensajería", "Dominio oficial de WhatsApp"],
  ["spotify.com", "Entretenimiento", "Dominio oficial de Spotify"],
  ["twitch.tv", "Entretenimiento", "Dominio oficial de Twitch"],
  ["airbnb.com.co", "Comercio", "Dominio oficial de Airbnb Colombia"],
  ["booking.com", "Turismo", "Dominio oficial de Booking"],
  ["aws.amazon.com", "Tecnología", "Dominio oficial de Amazon Web Services"],
  ["bancolombia.com", "Banca Colombia", "Portal oficial Bancolombia"],
  ["nequi.com.co", "Banca Digital", "Portal oficial Nequi Colombia"],
  ["daviplata.com", "Banca Digital", "Portal oficial Daviplata"],
  ["davivienda.com", "Banca Colombia", "Portal oficial Banco Davivienda"],
  ["bancodebogota.com", "Banca Colombia", "Portal oficial Banco de Bogotá"],
  ["bbva.com.co", "Banca Colombia", "Portal oficial BBVA Colombia"],
  ["pse.com.co", "Pasarela Pagos", "Portal oficial ACH Colombia PSE"],
  ["transfiya.com.co", "Pasarela Pagos", "Portal oficial Transfiya"],
  ["dian.gov.co", "Gobierno Colombia", "Dirección de Impuestos y Aduanas Nacionales"],
  ["gov.co", "Gobierno Colombia", "Portal del Estado Colombiano"],
  ["runt.gov.co", "Gobierno Colombia", "Registro Único Nacional de Tránsito"],
  ["fcm.org.co", "Gobierno/Tránsito", "Federación Colombiana de Municipios (SIMIT)"],
  ["4-72.com.co", "Logística", "Servicios Postales Nacionales 4-72"],
  ["servientrega.com", "Logística", "Servientrega Colombia"],
]);

export const DOMINIOS_MALICIOSOS_PRUEBA: EntradaReputacion[] = crearEntradas("NEGRA", [
  ["phishing-bancolombia-ficticio.xyz", "Phishing Bancario Ficticio", "Dominio ficticio de prueba para validación de bloqueo Bancolombia"],
  ["nequi-login-falso-prueba.top", "Phishing Billetera Digital Ficticio", "Dominio ficticio de prueba para validación de bloqueo Nequi"],
  ["dian-multas-facturacion-prueba.click", "Fraude Gubernamental Ficticio", "Dominio ficticio de prueba para validación de bloqueo DIAN"],
  ["pse-pagos-verificacion-falsa.tk", "Suplantación Pasarela Ficticia", "Dominio ficticio de prueba para validación de bloqueo PSE"],
  ["rastreo-4-72-paquetes-ficticio.fit", "Phishing Paquetería Ficticio", "Dominio ficticio de prueba para validación de bloqueo paquetería"],
]);

export function normalizarDominioOUrl(entrada: string): string {
  return entrada
    .trim()
    .toLowerCase()
    .replace(/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//, "")
    .replace(/^\/\//, "")
    .replace(/[/?#].*$/, "")
    .replace(/:\d+$/, "");
}

export function coincideDominio(dominio: string, base: string): boolean {
  return (
    dominio === base ||
    dominio.endsWith(`.${base}`) ||
    (base.startsWith("www.") && dominio === base.slice(4)) ||
    (dominio.startsWith("www.") && dominio.slice(4) === base)
  );
}

export class ProveedorReputacionLocal implements ProveedorReputacion {
  private mapaReputacion = new Map<string, EntradaReputacion>();
  private listaBlancaUsuario = new Set<string>();
  private listaNegraUsuario = new Set<string>();

  constructor() {
    for (const entrada of [...DOMINIOS_CONFIABLES_LOCALES, ...DOMINIOS_MALICIOSOS_PRUEBA]) {
      this.mapaReputacion.set(entrada.dominio.toLowerCase(), entrada);
    }
  }

  public actualizarListasUsuario(listaBlanca: string[], listaNegra: string[]): void {
    this.listaBlancaUsuario = new Set(listaBlanca.map(normalizarDominioOUrl).filter(Boolean));
    this.listaNegraUsuario = new Set(listaNegra.map(normalizarDominioOUrl).filter(Boolean));
  }

  public async consultarDominio(dominio: string): Promise<ResultadoReputacion> {
    const limpio = normalizarDominioOUrl(dominio);

    for (const bloqueado of this.listaNegraUsuario) {
      if (coincideDominio(limpio, bloqueado)) {
        return {
          encontrado: true,
          tipo: "NEGRA",
          categoria: "Lista Negra Personalizada",
          descripcion: "Dominio bloqueado manualmente por el usuario.",
          confianza: 100,
        };
      }
    }

    for (const permitido of this.listaBlancaUsuario) {
      if (coincideDominio(limpio, permitido)) {
        return {
          encontrado: true,
          tipo: "BLANCA",
          categoria: "Lista Blanca Personalizada",
          descripcion: "Dominio permitido manualmente por el usuario.",
          confianza: 100,
        };
      }
    }

    const entradaDirecta = this.mapaReputacion.get(limpio);
    if (entradaDirecta) {
      return {
        encontrado: true,
        tipo: entradaDirecta.tipo,
        categoria: entradaDirecta.categoria,
        descripcion: entradaDirecta.descripcion,
        confianza: 95,
      };
    }

    for (const [dominioRegistrado, entrada] of this.mapaReputacion.entries()) {
      if (entrada.tipo === "BLANCA" && coincideDominio(limpio, dominioRegistrado)) {
        return {
          encontrado: true,
          tipo: "BLANCA",
          categoria: entrada.categoria,
          descripcion: `Subdominio de entidad legítima conocida: ${dominioRegistrado}`,
          confianza: 90,
        };
      }
    }

    return { encontrado: false, confianza: 0 };
  }

  public async consultarUrl(urlTexto: string): Promise<ResultadoReputacion> {
    try {
      const objetoUrl = new URL(urlTexto);
      return this.consultarDominio(objetoUrl.hostname);
    } catch {
      return { encontrado: false, confianza: 0 };
    }
  }
}
