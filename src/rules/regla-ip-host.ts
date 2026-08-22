import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

/**
 * Regla para detectar el uso directo de una dirección IP numérica en el hostname
 * en lugar de un nombre de dominio calificado legítimo.
 */
export class ReglaIpHost extends BaseReglaRiesgo {
  readonly id = "REGLA_IP_HOST";
  readonly nombre = "Uso de Dirección IP Directa como Host";
  readonly descripcion =
    "Detecta si la URL apunta directamente a una dirección IP numérica (IPv4 o IPv6), técnica frecuente en infraestructura de phishing efímera.";
  readonly severidad: SeveridadRegla = "ALTA";
  readonly puntuacionBase = 35;

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    if (!caracteristicas.esIpDirecta) {
      return this.crearResultadoNoActivado();
    }

    const evidencias: EvidenciaRiesgo[] = [
      {
        campo: "nombreHost",
        valorEncontrado: caracteristicas.nombreHost,
        descripcion: `El sitio utiliza una dirección IP (${caracteristicas.versionIp || "IPv4"}) directa en lugar de un dominio registrado.`,
      },
    ];

    let puntuacion = this.puntuacionBase;

    // Si además tiene palabras bancarias o de autenticación, el riesgo sube considerablemente
    if (
      caracteristicas.contienePalabrasAutenticacion.length > 0 ||
      caracteristicas.contienePalabrasBancarias.length > 0
    ) {
      puntuacion += 20;
      evidencias.push({
        campo: "contextoIp",
        valorEncontrado: [
          ...caracteristicas.contienePalabrasAutenticacion,
          ...caracteristicas.contienePalabrasBancarias,
        ].join(", "),
        descripcion: "La IP contiene rutas o parámetros asociados a credenciales o banca.",
      });
    }

    return this.crearResultadoActivado(puntuacion, evidencias);
  }
}
