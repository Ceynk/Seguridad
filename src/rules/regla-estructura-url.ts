import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

/**
 * Regla para analizar anomalías estructurales en la URL:
 * - Subdominios anidados en exceso (>= 3).
 * - Carácter '@' en la URL (engaño de credenciales RFC).
 * - Longitud excesiva de dominio o URL.
 * - Exceso de guiones o caracteres especiales para evadir filtros.
 * - Alto porcentaje de codificación URL (%XX).
 */
export class ReglaEstructuraUrl extends BaseReglaRiesgo {
  readonly id = "REGLA_ESTRUCTURA_URL";
  readonly nombre = "Anomalías Estructurales en la URL";
  readonly descripcion =
    "Detecta técnicas de evasión estructural como exceso de subdominios anidados, uso de @, longitud anómala o exceso de símbolos.";
  readonly severidad: SeveridadRegla = "ALTA";
  readonly puntuacionBase = 20;

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    const evidencias: EvidenciaRiesgo[] = [];
    let puntuacion = 0;

    // 1. Carácter '@' en la URL (intento de suplantación de host vía userinfo RFC)
    if (caracteristicas.tieneArroba) {
      puntuacion += 35;
      evidencias.push({
        campo: "tieneArroba",
        valorEncontrado: true,
        descripcion: "Presencia del carácter '@' en la URL, utilizado para ofuscar el servidor de destino real.",
      });
    }

    // 2. Subdominios anidados en exceso (>= 3 subdominios)
    if (caracteristicas.cantidadSubdominios >= 3) {
      puntuacion += 20;
      evidencias.push({
        campo: "cantidadSubdominios",
        valorEncontrado: caracteristicas.cantidadSubdominios,
        descripcion: `Estructura profunda de subdominios anidados (${caracteristicas.subdominios.join(".")}).`,
      });
    }

    // 3. Exceso de guiones en el nombre de host (frecuente en phishing para simular rutas oficiales)
    if (caracteristicas.cantidadGuiones >= 3) {
      puntuacion += 15;
      evidencias.push({
        campo: "cantidadGuiones",
        valorEncontrado: caracteristicas.cantidadGuiones,
        descripcion: `Múltiples guiones en el nombre de host (${caracteristicas.cantidadGuiones} guiones), patrón típico de evasión.`,
      });
    }

    // 4. Longitud anómala del dominio (> 45 caracteres)
    if (caracteristicas.longitudDominio > 45) {
      puntuacion += 15;
      evidencias.push({
        campo: "longitudDominio",
        valorEncontrado: caracteristicas.longitudDominio,
        descripcion: `Nombre de dominio inusualmente largo (${caracteristicas.longitudDominio} caracteres).`,
      });
    }

    // 5. Alto porcentaje de codificación URL (> 25%)
    if (caracteristicas.porcentajeCodificacionUrl > 25) {
      puntuacion += 15;
      evidencias.push({
        campo: "porcentajeCodificacionUrl",
        valorEncontrado: `${caracteristicas.porcentajeCodificacionUrl}%`,
        descripcion: "Alto porcentaje de caracteres codificados en hexadecimal para ofuscar el contenido.",
      });
    }

    // 6. Caracteres especiales anómalos en el host
    if (caracteristicas.caracteresEspecialesSospechosos > 0) {
      puntuacion += 20;
      evidencias.push({
        campo: "caracteresEspecialesSospechosos",
        valorEncontrado: caracteristicas.caracteresEspecialesSospechosos,
        descripcion: "Presencia de caracteres especiales no estándar en el nombre de host.",
      });
    }

    if (evidencias.length === 0 || puntuacion === 0) {
      return this.crearResultadoNoActivado();
    }

    // Limitar puntuación acumulativa de estructura
    const puntuacionFinal = Math.min(puntuacion, 45);

    return this.crearResultadoActivado(puntuacionFinal, evidencias);
  }
}
