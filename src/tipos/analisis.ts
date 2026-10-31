export type ClasificacionRiesgo = "SEGURO" | "SOSPECHOSO" | "MALICIOSO";
export type NivelConfianza = "BAJO" | "MEDIO" | "ALTO";
export type SeveridadRegla = "BAJA" | "MEDIA" | "ALTA" | "CRITICA";

export interface TecnicaMitre {
  id: string;
  nombre: string;
  tactica: string;
  enlace: string;
}

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
  tecnicaMitre?: TecnicaMitre;
}

export interface SustitucionCaracterVisual {
  posicion: number;
  caracterSospechoso: string;
  caracterLegitimo: string;
  tipo: string;
}

export interface ResultadoSimilitudMarca {
  marcaObjetivo: string;
  dominioOriginal: string;
  nombreDetectado: string;
  distancia: number;
  similitudPorcentaje: number;
  tipoSustitucion: string[];
  desgloseCaracteres?: SustitucionCaracterVisual[];
}

export type PrincipioCoercion =
  | "URGENCIA_TEMPORAL"
  | "AUTORIDAD_INSTITUCIONAL"
  | "MIEDO_SANCION"
  | "RECOMPENSA_FINANCIERA"
  | "ACCESO_CRITICO";

export interface VectorIngenieriaSocial {
  principioCoercion: PrincipioCoercion;
  titulo: string;
  descripcion: string;
  terminosIdentificados: string[];
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
  entropiaShannon: number;
  vectoresIngenieriaSocial: VectorIngenieriaSocial[];
  dominioDecodificadoUnicode?: string;
  tieneLetrasRarasHomoglifos: boolean;
  tieneAlfabetoMixto: boolean;
  desgloseLetrasRaras: SustitucionCaracterVisual[];
}

export interface FichaForenseIncidente {
  idIncidente: string;
  marcaTemporalIso: string;
  urlObjetivo: string;
  nombreHost: string;
  huellaSha256: string;
  clasificacion: ClasificacionRiesgo;
  puntuacionRiesgo: number;
  entropiaShannon: number;
  tecnicasMitreDetectadas: TecnicaMitre[];
  vectoresIngenieriaSocial: VectorIngenieriaSocial[];
  reglasDetonadas: {
    nombre: string;
    severidad: SeveridadRegla;
    puntuacion: number;
    evidencias: EvidenciaRiesgo[];
  }[];
  resumenTecnico: string;
  recomendacionRespuesta: string;
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
  esBloqueoAltaConfianza: boolean;
  requiereConfirmacionDocumento: boolean;
  huellaSha256?: string;
  fichaForense?: FichaForenseIncidente;
}
