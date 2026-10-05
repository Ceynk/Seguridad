import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

export class ReglaSuplantacionMarca extends BaseReglaRiesgo {
  readonly id = "REGLA_SUPLANTACION_MARCA";
  readonly nombre = "Suplantación de Marca o Entidad";
  readonly descripcion =
    "Detecta dominios que imitan la identidad de marcas o entidades oficiales.";
  readonly severidad: SeveridadRegla = "CRITICA";
  readonly puntuacionBase = 45;
  readonly tecnicaMitre: TecnicaMitreAttack = {
    id: "T1036.007",
    nombre: "Masquerading: Homoglyph",
    tactica: "Defense Evasion",
    enlace: "https://attack.mitre.org/techniques/T1036/007/",
  };

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
        descripcion: `El dominio '${caracteristicas.nombreHost}' imita a '${similitud.marcaObjetivo}' (Dominio legítimo: ${similitud.dominioOriginal})`,
      });

      evidencias.push({
        campo: "tecnicaSuplantacion",
        valorEncontrado: similitud.tipoSustitucion.join(", "),
        descripcion: `Técnica detectada: ${similitud.tipoSustitucion.join(", ")} (Similitud: ${similitud.similitudPorcentaje}%)`,
      });

      const usaHomoglifos = similitud.tipoSustitucion.some((t) =>
        t.toLowerCase().includes("homoglifo") || t.toLowerCase().includes("sustitución")
      );

      if (usaHomoglifos || similitud.similitudPorcentaje >= 95) {
        puntuacionTotal += 85;
      } else if (similitud.similitudPorcentaje >= 80) {
        puntuacionTotal += 55;
      } else {
        puntuacionTotal += 35;
      }
    }

    const puntuacionFinal = Math.min(puntuacionTotal, 95);
    return this.crearResultadoActivado(puntuacionFinal, evidencias);
  }
}
