import { ClasificacionRiesgo } from "./analisis";

/**
 * Modos de sensibilidad de protección disponibles.
 */
export type ModoProteccion = "ESTRICTO" | "EQUILIBRADO" | "PERMISIVO";

/**
 * Configuración general del usuario y la extensión.
 */
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

/**
 * Registro de un sitio analizado recientemente (efímero en sesión, sin persistir historial completo).
 */
export interface RegistroSitioReciente {
  url: string;
  nombreHost: string;
  puntuacionRiesgo: number;
  clasificacion: ClasificacionRiesgo;
  marcaTemporal: number;
}

/**
 * Estadísticas de la sesión actual de navegación.
 */
export interface EstadisticasSesion {
  sitiosAnalizados: number;
  amenazasBloqueadas: number;
  sitiosSospechososDetectados: number;
  ultimoSitioAnalizado?: RegistroSitioReciente;
}
