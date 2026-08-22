import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

/**
 * Regla para detectar intentos de suplantación de marcas comerciales, financieras o gubernamentales
 * mediante typosquatting, inserción en subdominios no oficiales u homoglifos.
 */
export class ReglaSuplantacionMarca extends BaseReglaRiesgo {
  readonly id = "REGLA_SUPLANTACION_MARCA";
  readonly nombre = "Suplantación de Marca o Entidad";
  readonly descripcion =
    "Detecta dominios que imitan la identidad de bancos, billeteras digitales, entidades públicas o servicios de paquetería.";
  readonly severidad: SeveridadRegla = "CRITICA";
  readonly puntuacionBase = 45;

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    if (caracteristicas.similitudMarcas.length === 0) {
      return this.crearResultadoNoActivado();
    }

    const evidencias: EvidenciaRiesgo[] = [];
    let puntuacionTotal = 0;

    for (const similitud of caracteristicas.similitudMarcas) {
      evidencias.push({
        campo: "marcaImitada",
        valorEncontrado: similitud.marcaObjetivo,
        descripcion: `El dominio '${caracteristicas.nombreHost}' imita a la entidad '${similitud.marcaObjetivo}' (Dominio legítimo: ${similitud.dominioOriginal})`,
      });

      evidencias.push({
        campo: "tecnicaSuplantacion",
        valorEncontrado: similitud.tipoSustitucion.join(", "),
        descripcion: `Técnica detectada: ${similitud.tipoSustitucion.join(", ")} (Similitud: ${similitud.similitudPorcentaje}%)`,
      });

      // Si la similitud es exacta o 100% (marca en dominio no oficial), puntuación muy alta
      if (similitud.similitudPorcentaje >= 95) {
        puntuacionTotal += 45;
      } else if (similitud.similitudPorcentaje >= 80) {
        puntuacionTotal += 35;
      } else {
        puntuacionTotal += 25;
      }
    }

    // Limitar la puntuación aportada por esta regla individualmente
    const puntuacionFinal = Math.min(puntuacionTotal, 55);

    return this.crearResultadoActivado(puntuacionFinal, evidencias);
  }
}
