/**
 * Tipos y modelos de datos para el análisis de URLs y evaluación de riesgos.
 * Todas las estructuras y variables en español.
 */

export type ClasificacionRiesgo = "SEGURO" | "SOSPECHOSO" | "MALICIOSO";

export type NivelConfianza = "BAJO" | "MEDIO" | "ALTO";

export type SeveridadRegla = "BAJA" | "MEDIA" | "ALTA" | "CRITICA";

export interface EvidenciaRiesgo {
  campo: string;
  valorEncontrado: string | number | boolean;
  descripcion: string;
}

export interface ReglaCoincidente {
  id: string;
  nombre: string;
  descripcion: string;
  severidad: SeveridadRegla;
  puntuacion: number;
  evidencias: EvidenciaRiesgo[];
}

export interface ResultadoSimilitudMarca {
  marcaObjetivo: string;
  dominioOriginal: string;
  nombreDetectado: string;
  distancia: number;
  similitudPorcentaje: number;
  tipoSustitucion: string[];
}

export interface CaracteristicasUrl {
  urlCompleta: string;
  protocolo: string;
  nombreHost: string;
  dominioPrincipal: string;
  subdominios: string[];
  cantidadSubdominios: number;
  longitudTotal: number;
  longitudDominio: number;
  longitudRuta: number;
  ruta: string;
  parametros: Record<string, string>;
  cantidadParametros: number;
  esIpDirecta: boolean;
  versionIp?: "IPv4" | "IPv6";
  esPunycode: boolean;
  tieneArroba: boolean;
  porcentajeCodificacionUrl: number;
  cantidadGuiones: number;
  cantidadPuntos: number;
  caracteresEspecialesSospechosos: number;
  extensionTld: string;
  tldEsSospechoso: boolean;
  contienePalabrasAutenticacion: string[];
  contienePalabrasBancarias: string[];
  contienePalabrasUrgencia: string[];
  similitudMarcas: ResultadoSimilitudMarca[];
}

export interface ResultadoAnalisisUrl {
  url: string;
  nombreHost: string;
  marcaTemporal: number;
  puntuacionRiesgo: number;
  clasificacion: ClasificacionRiesgo;
  nivelConfianza: NivelConfianza;
  reglasCoincidentes: ReglaCoincidente[];
  caracteristicas: CaracteristicasUrl;
  esDominioConocidoSeguro: boolean;
  esDominioConocidoMalicioso: boolean;
  tiempoProcesamientoMs: number;
}
