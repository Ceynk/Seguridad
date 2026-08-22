import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

/**
 * Regla para evaluar la presencia y combinación de palabras clave altamente sospechosas
 * (autenticación forzada, urgencia/multas, banca no oficial).
 */
export class ReglaPalabrasSospechosas extends BaseReglaRiesgo {
  readonly id = "REGLA_PALABRAS_SOSPECHOSAS";
  readonly nombre = "Patrones Léxicos de Ingeniería Social";
  readonly descripcion =
    "Detecta la acumulación de términos de urgencia, pagos, multas, premios y autenticación en la estructura de la URL.";
  readonly severidad: SeveridadRegla = "MEDIA";
  readonly puntuacionBase = 20;

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    const palabrasDetectadas: string[] = [
      ...caracteristicas.contienePalabrasAutenticacion,
      ...caracteristicas.contienePalabrasBancarias,
      ...caracteristicas.contienePalabrasUrgencia,
    ];

    if (palabrasDetectadas.length === 0) {
      return this.crearResultadoNoActivado();
    }

    const evidencias: EvidenciaRiesgo[] = [];
    let puntuacion = 0;

    // Si combina autenticación con urgencia/multas o banca (patrón típico phishing)
    const tieneAutenticacion = caracteristicas.contienePalabrasAutenticacion.length > 0;
    const tieneBanca = caracteristicas.contienePalabrasBancarias.length > 0;
    const tieneUrgencia = caracteristicas.contienePalabrasUrgencia.length > 0;

    if (tieneAutenticacion && (tieneBanca || tieneUrgencia)) {
      puntuacion += 30;
      evidencias.push({
        campo: "combinacionTerminos",
        valorEncontrado: palabrasDetectadas.join(", "),
        descripcion: "Combinación de términos de autenticación/acceso con entidades financieras o alertas de urgencia.",
      });
    } else if (palabrasDetectadas.length >= 3) {
      puntuacion += 25;
      evidencias.push({
        campo: "densidadTerminos",
        valorEncontrado: palabrasDetectadas.join(", "),
        descripcion: `Múltiples palabras clave sensibles en la URL (${palabrasDetectadas.length} términos).`,
      });
    } else if (palabrasDetectadas.length >= 1) {
      puntuacion += 10;
      evidencias.push({
        campo: "terminosSensibles",
        valorEncontrado: palabrasDetectadas.join(", "),
        descripcion: "Presencia de términos sensibles en la URL.",
      });
    }

    if (puntuacion === 0) {
      return this.crearResultadoNoActivado();
    }

    return this.crearResultadoActivado(puntuacion, evidencias);
  }
}
