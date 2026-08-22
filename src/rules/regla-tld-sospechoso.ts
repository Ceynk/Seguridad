import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

/**
 * Regla para evaluar dominios con TLDs de bajo costo frecuentemente abusados (.xyz, .top, .tk, etc.)
 * combinados con marcas o palabras sensibles.
 */
export class ReglaTldSospechoso extends BaseReglaRiesgo {
  readonly id = "REGLA_TLD_SOSPECHOSO";
  readonly nombre = "Extensión de Dominio (TLD) de Alto Riesgo";
  readonly descripcion =
    "Detecta extensiones de dominio gratuitas o de bajo costo comúnmente abusadas para despliegues masivos de sitios fraudulentos.";
  readonly severidad: SeveridadRegla = "MEDIA";
  readonly puntuacionBase = 20;

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    if (!caracteristicas.tldEsSospechoso) {
      return this.crearResultadoNoActivado();
    }

    const evidencias: EvidenciaRiesgo[] = [
      {
        campo: "extensionTld",
        valorEncontrado: `.${caracteristicas.extensionTld}`,
        descripcion: `El dominio utiliza la extensión '.${caracteristicas.extensionTld}', clasificada con alta tasa de abuso en campañas de phishing.`,
      },
    ];

    let puntuacion = this.puntuacionBase;

    // Si el TLD sospechoso está asociado a una marca imitada o palabras financieras, el riesgo se eleva
    if (
      caracteristicas.similitudMarcas.length > 0 ||
      caracteristicas.contienePalabrasBancarias.length > 0 ||
      caracteristicas.contienePalabrasAutenticacion.length > 0
    ) {
      puntuacion += 25;
      evidencias.push({
        campo: "tldConMarca",
        valorEncontrado: `.${caracteristicas.extensionTld} + Términos Sensibles`,
        descripcion: "Combinación de TLD de alto riesgo con identidad institucional o bancaria.",
      });
    }

    return this.crearResultadoActivado(puntuacion, evidencias);
  }
}
