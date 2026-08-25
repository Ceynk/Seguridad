import { ClasificacionRiesgo } from "./analisis";

export type ModoProteccion = "ESTRICTO" | "EQUILIBRADO" | "PERMISIVO";

export interface ConfiguracionExtension {
  proteccionHabilitada: boolean;
  modoProteccion: ModoProteccion;
  umbralSospechoso: number;
  umbralMalicioso: number;
  bloquearMaliciososAutomaticamente: boolean;
  advertirSospechosos: boolean;
  listaBlancaPersonalizada: string[];
  listaNegraPersonalizada: string[];
  permitirBypassTemporal: boolean;
}

export interface RegistroSitioReciente {
  url: string;
  nombreHost: string;
  puntuacionRiesgo: number;
  clasificacion: ClasificacionRiesgo;
  marcaTemporal: number;
}

export interface EstadisticasSesion {
  sitiosAnalizados: number;
  amenazasBloqueadas: number;
  sitiosSospechososDetectados: number;
  ultimoSitioAnalizado?: RegistroSitioReciente;
}
