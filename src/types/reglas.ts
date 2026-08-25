import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "./analisis";

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

export interface ReglaRiesgo {
  readonly id: string;
  readonly nombre: string;
  readonly descripcion: string;
  readonly severidad: SeveridadRegla;
  readonly puntuacionBase: number;
  readonly habilitada: boolean;
  readonly tecnicaMitre?: TecnicaMitreAttack;

  evaluar(caracteristicas: CaracteristicasUrl): Promise<ResultadoEvaluacionRegla> | ResultadoEvaluacionRegla;
}
