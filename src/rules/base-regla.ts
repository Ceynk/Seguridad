import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla } from "../types/analisis";
import { ReglaRiesgo, ResultadoEvaluacionRegla } from "../types/reglas";

/**
 * Clase base abstracta para la creación de reglas de detección de riesgo.
 */
export abstract class BaseReglaRiesgo implements ReglaRiesgo {
  abstract readonly id: string;
  abstract readonly nombre: string;
  abstract readonly descripcion: string;
  abstract readonly severidad: SeveridadRegla;
  abstract readonly puntuacionBase: number;
  readonly habilitada: boolean = true;

  abstract evaluar(
    caracteristicas: CaracteristicasUrl
  ): Promise<ResultadoEvaluacionRegla> | ResultadoEvaluacionRegla;

  protected crearResultadoNoActivado(): ResultadoEvaluacionRegla {
    return {
      activada: false,
      reglaId: this.id,
      nombre: this.nombre,
      descripcion: this.descripcion,
      severidad: this.severidad,
      puntuacion: 0,
      evidencias: [],
    };
  }

  protected crearResultadoActivado(
    puntuacion: number,
    evidencias: EvidenciaRiesgo[]
  ): ResultadoEvaluacionRegla {
    return {
      activada: true,
      reglaId: this.id,
      nombre: this.nombre,
      descripcion: this.descripcion,
      severidad: this.severidad,
      puntuacion,
      evidencias,
    };
  }
}
