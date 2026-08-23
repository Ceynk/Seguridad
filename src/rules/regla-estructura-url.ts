import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

export class ReglaEstructuraUrl extends BaseReglaRiesgo {
  readonly id = "REGLA_ESTRUCTURA_URL";
  readonly nombre = "Anomalías Estructurales en la URL";
  readonly descripcion =
    "Detecta técnicas de evasión estructural como exceso de subdominios anidados, uso de @, longitud anómala o exceso de símbolos.";
  readonly severidad: SeveridadRegla = "ALTA";
  readonly puntuacionBase = 20;
  readonly tecnicaMitre: TecnicaMitreAttack = {
    id: "T1566.002",
    nombre: "Phishing: URL Obfuscation & Subdomain Nesting",
    tactica: "Initial Access",
    enlace: "https://attack.mitre.org/techniques/T1566/002/",
  };

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    const evidencias: EvidenciaRiesgo[] = [];
    let puntuacion = 0;

    if (caracteristicas.tieneArroba) {
      puntuacion += 35;
      evidencias.push({
        campo: "tieneArroba",
        valorEncontrado: true,
        descripcion: "Presencia del carácter '@' en la URL, utilizado para ofuscar el servidor de destino real.",
      });
    }

    if (caracteristicas.cantidadSubdominios >= 3) {
      puntuacion += 20;
      evidencias.push({
        campo: "cantidadSubdominios",
        valorEncontrado: caracteristicas.cantidadSubdominios,
        descripcion: `Estructura profunda de subdominios anidados (${caracteristicas.subdominios.join(".")}).`,
      });
    }

    if (caracteristicas.cantidadGuiones >= 3) {
      puntuacion += 15;
      evidencias.push({
        campo: "cantidadGuiones",
        valorEncontrado: caracteristicas.cantidadGuiones,
        descripcion: `Múltiples guiones en el nombre de host (${caracteristicas.cantidadGuiones} guiones), patrón típico de evasión.`,
      });
    }

    if (caracteristicas.longitudDominio > 45) {
      puntuacion += 15;
      evidencias.push({
        campo: "longitudDominio",
        valorEncontrado: caracteristicas.longitudDominio,
        descripcion: `Nombre de dominio inusualmente largo (${caracteristicas.longitudDominio} caracteres).`,
      });
    }

    if (caracteristicas.porcentajeCodificacionUrl > 25) {
      puntuacion += 15;
      evidencias.push({
        campo: "porcentajeCodificacionUrl",
        valorEncontrado: `${caracteristicas.porcentajeCodificacionUrl}%`,
        descripcion: "Alto porcentaje de caracteres codificados en hexadecimal para ofuscar el contenido.",
      });
    }

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

    const puntuacionFinal = Math.min(puntuacion, 45);
    return this.crearResultadoActivado(puntuacionFinal, evidencias);
  }
}
