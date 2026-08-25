import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "../types/analisis";
import { ReglaRiesgo, ResultadoEvaluacionRegla } from "../types/reglas";

export abstract class BaseReglaRiesgo implements ReglaRiesgo {
  abstract readonly id: string;
  abstract readonly nombre: string;
  abstract readonly descripcion: string;
  abstract readonly severidad: SeveridadRegla;
  abstract readonly puntuacionBase: number;
  readonly habilitada: boolean = true;
  abstract readonly tecnicaMitre?: TecnicaMitreAttack;

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
      tecnicaMitre: this.tecnicaMitre,
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
      tecnicaMitre: this.tecnicaMitre,
    };
  }
}
