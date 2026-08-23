import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "./analisis";

/**
 * Interface para el resultado de evaluación de una regla individual.
 */
export interface ResultadoEvaluacionRegla {
  activada: boolean;
  reglaId: string;
  nombre: string;
  descripcion: string;
  severidad: SeveridadRegla;
  puntuacion: number;
  evidencias: EvidenciaRiesgo[];
  tecnicaMitre?: TecnicaMitreAttack;
}

/**
 * Interface que debe implementar toda regla de detección de riesgo.
 */
export interface ReglaRiesgo {
  readonly id: string;
  readonly nombre: string;
  readonly descripcion: string;
  readonly severidad: SeveridadRegla;
  readonly puntuacionBase: number;
  readonly habilitada: boolean;
  readonly tecnicaMitre?: TecnicaMitreAttack;

  /**
   * Evalúa las características extraídas de la URL y determina si la regla se activa.
   */
  evaluar(caracteristicas: CaracteristicasUrl): Promise<ResultadoEvaluacionRegla> | ResultadoEvaluacionRegla;
}
