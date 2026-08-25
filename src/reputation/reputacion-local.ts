import {
  EntradaReputacion,
  ProveedorReputacion,
  ResultadoReputacion,
} from "../types/reputacion";

export const DOMINIOS_CONFIABLES_LOCALES: EntradaReputacion[] = [
  { dominio: "google.com", tipo: "BLANCA", categoria: "Tecnología/Búsqueda", fechaRegistro: "2026-01-01", descripcion: "Dominio oficial de Google" },
  { dominio: "google.com.co", tipo: "BLANCA", categoria: "Tecnología/Búsqueda", fechaRegistro: "2026-01-01", descripcion: "Dominio oficial de Google Colombia" },
  { dominio: "microsoft.com", tipo: "BLANCA", categoria: "Tecnología", fechaRegistro: "2026-01-01", descripcion: "Dominio oficial de Microsoft" },
  { dominio: "github.com", tipo: "BLANCA", categoria: "Desarrollo", fechaRegistro: "2026-01-01", descripcion: "Dominio oficial de GitHub" },
  { dominio: "apple.com", tipo: "BLANCA", categoria: "Tecnología", fechaRegistro: "2026-01-01", descripcion: "Dominio oficial de Apple" },
  { dominio: "amazon.com", tipo: "BLANCA", categoria: "Comercio", fechaRegistro: "2026-01-01", descripcion: "Dominio oficial de Amazon" },
  { dominio: "netflix.com", tipo: "BLANCA", categoria: "Entretenimiento", fechaRegistro: "2026-01-01", descripcion: "Dominio oficial de Netflix" },

  { dominio: "bancolombia.com", tipo: "BLANCA", categoria: "Banca Colombia", fechaRegistro: "2026-01-01", descripcion: "Portal oficial Bancolombia" },
  { dominio: "nequi.com.co", tipo: "BLANCA", categoria: "Banca Digital", fechaRegistro: "2026-01-01", descripcion: "Portal oficial Nequi Colombia" },
  { dominio: "daviplata.com", tipo: "BLANCA", categoria: "Banca Digital", fechaRegistro: "2026-01-01", descripcion: "Portal oficial Daviplata" },
  { dominio: "davivienda.com", tipo: "BLANCA", categoria: "Banca Colombia", fechaRegistro: "2026-01-01", descripcion: "Portal oficial Banco Davivienda" },
  { dominio: "bancodebogota.com", tipo: "BLANCA", categoria: "Banca Colombia", fechaRegistro: "2026-01-01", descripcion: "Portal oficial Banco de Bogotá" },
  { dominio: "bbva.com.co", tipo: "BLANCA", categoria: "Banca Colombia", fechaRegistro: "2026-01-01", descripcion: "Portal oficial BBVA Colombia" },
  { dominio: "pse.com.co", tipo: "BLANCA", categoria: "Pasarela Pagos", fechaRegistro: "2026-01-01", descripcion: "Portal oficial ACH Colombia PSE" },
  { dominio: "transfiya.com.co", tipo: "BLANCA", categoria: "Pasarela Pagos", fechaRegistro: "2026-01-01", descripcion: "Portal oficial Transfiya" },

  { dominio: "dian.gov.co", tipo: "BLANCA", categoria: "Gobierno Colombia", fechaRegistro: "2026-01-01", descripcion: "Dirección de Impuestos y Aduanas Nacionales" },
  { dominio: "gov.co", tipo: "BLANCA", categoria: "Gobierno Colombia", fechaRegistro: "2026-01-01", descripcion: "Portal del Estado Colombiano" },
  { dominio: "runt.gov.co", tipo: "BLANCA", categoria: "Gobierno Colombia", fechaRegistro: "2026-01-01", descripcion: "Registro Único Nacional de Tránsito" },
  { dominio: "fcm.org.co", tipo: "BLANCA", categoria: "Gobierno/Tránsito", fechaRegistro: "2026-01-01", descripcion: "Federación Colombiana de Municipios (SIMIT)" },

  { dominio: "4-72.com.co", tipo: "BLANCA", categoria: "Logística", fechaRegistro: "2026-01-01", descripcion: "Servicios Postales Nacionales 4-72" },
  { dominio: "servientrega.com", tipo: "BLANCA", categoria: "Logística", fechaRegistro: "2026-01-01", descripcion: "Servientrega Colombia" },
];

export const DOMINIOS_MALICIOSOS_PRUEBA: EntradaReputacion[] = [
  {
    dominio: "phishing-bancolombia-ficticio.xyz",
    tipo: "NEGRA",
    categoria: "Phishing Bancario Ficticio",
    fechaRegistro: "2026-01-01",
    descripcion: "Dominio ficticio de prueba para validación de bloqueo Bancolombia",
  },
  {
    dominio: "nequi-login-falso-prueba.top",
    tipo: "NEGRA",
    categoria: "Phishing Billetera Digital Ficticio",
    fechaRegistro: "2026-01-01",
    descripcion: "Dominio ficticio de prueba para validación de bloqueo Nequi",
  },
  {
    dominio: "dian-multas-facturacion-prueba.click",
    tipo: "NEGRA",
    categoria: "Fraude Gubernamental Ficticio",
    fechaRegistro: "2026-01-01",
    descripcion: "Dominio ficticio de prueba para validación de bloqueo DIAN",
  },
  {
    dominio: "pse-pagos-verificacion-falsa.tk",
    tipo: "NEGRA",
    categoria: "Suplantación Pasarela Ficticia",
    fechaRegistro: "2026-01-01",
    descripcion: "Dominio ficticio de prueba para validación de bloqueo PSE",
  },
  {
    dominio: "rastreo-4-72-paquetes-ficticio.fit",
    tipo: "NEGRA",
    categoria: "Phishing Paquetería Ficticio",
    fechaRegistro: "2026-01-01",
    descripcion: "Dominio ficticio de prueba para validación de bloqueo paquetería",
  },
];

export class ProveedorReputacionLocal implements ProveedorReputacion {
  private mapaReputacion: Map<string, EntradaReputacion> = new Map();
  private listaBlancaUsuario: Set<string> = new Set();
  private listaNegraUsuario: Set<string> = new Set();

  constructor() {
    this.cargarListasPorDefecto();
  }

  private cargarListasPorDefecto(): void {
    for (const entrada of DOMINIOS_CONFIABLES_LOCALES) {
      this.mapaReputacion.set(entrada.dominio.toLowerCase(), entrada);
    }
    for (const entrada of DOMINIOS_MALICIOSOS_PRUEBA) {
      this.mapaReputacion.set(entrada.dominio.toLowerCase(), entrada);
    }
  }

  public actualizarListasUsuario(listaBlanca: string[], listaNegra: string[]): void {
    this.listaBlancaUsuario = new Set(listaBlanca.map((d) => d.toLowerCase().trim()));
    this.listaNegraUsuario = new Set(listaNegra.map((d) => d.toLowerCase().trim()));
  }

  public async consultarDominio(dominio: string): Promise<ResultadoReputacion> {
    const dominioMinusculas = dominio.toLowerCase().trim();

    if (this.listaNegraUsuario.has(dominioMinusculas)) {
      return {
        encontrado: true,
        tipo: "NEGRA",
        categoria: "Lista Negra Personalizada",
        descripcion: "Dominio bloqueado manualmente por el usuario.",
        confianza: 100,
      };
    }

    if (this.listaBlancaUsuario.has(dominioMinusculas)) {
      return {
        encontrado: true,
        tipo: "BLANCA",
        categoria: "Lista Blanca Personalizada",
        descripcion: "Dominio permitido manualmente por el usuario.",
        confianza: 100,
      };
    }

    const entradaDirecta = this.mapaReputacion.get(dominioMinusculas);
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
      if (entrada.tipo === "BLANCA" && dominioMinusculas.endsWith(`.${dominioRegistrado}`)) {
        return {
          encontrado: true,
          tipo: "BLANCA",
          categoria: entrada.categoria,
          descripcion: `Subdominio de entidad legítima conocida: ${dominioRegistrado}`,
          confianza: 90,
        };
      }
    }

    return {
      encontrado: false,
      confianza: 0,
    };
  }

  public async consultarUrl(urlTexto: string): Promise<ResultadoReputacion> {
    try {
      const parsed = new URL(urlTexto);
      return this.consultarDominio(parsed.hostname);
    } catch {
      return { encontrado: false, confianza: 0 };
    }
  }
}
