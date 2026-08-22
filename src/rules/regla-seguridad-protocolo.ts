import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

/**
 * Regla para evaluar la seguridad del protocolo de transporte (HTTP vs HTTPS).
 * No penaliza sitios locales de desarrollo (localhost/127.0.0.1) ni blogs informativos simples,
 * pero penaliza severamente el uso de HTTP cuando hay interacción con credenciales o pagos.
 */
export class ReglaSeguridadProtocolo extends BaseReglaRiesgo {
  readonly id = "REGLA_SEGURIDAD_PROTOCOLO";
  readonly nombre = "Protocolo No Cifrado (HTTP)";
  readonly descripcion =
    "Detecta la ausencia de cifrado HTTPS, especialmente en páginas que solicitan datos sensibles o pagos.";
  readonly severidad: SeveridadRegla = "MEDIA";
  readonly puntuacionBase = 15;

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    // Ignorar entornos de desarrollo local legítimos
    if (
      caracteristicas.nombreHost === "localhost" ||
      caracteristicas.nombreHost === "127.0.0.1" ||
      caracteristicas.nombreHost.endsWith(".local")
    ) {
      return this.crearResultadoNoActivado();
    }

    if (caracteristicas.protocolo === "https") {
      return this.crearResultadoNoActivado();
    }

    const evidencias: EvidenciaRiesgo[] = [
      {
        campo: "protocolo",
        valorEncontrado: caracteristicas.protocolo,
        descripcion: "El sitio no utiliza conexión segura cifrada (HTTPS).",
      },
    ];

    let puntuacion = this.puntuacionBase;

    // Si tiene palabras de autenticación o financieras sin HTTPS, el riesgo aumenta
    if (
      caracteristicas.contienePalabrasAutenticacion.length > 0 ||
      caracteristicas.contienePalabrasBancarias.length > 0
    ) {
      puntuacion += 20;
      evidencias.push({
        campo: "sensibilidadSinCifrado",
        valorEncontrado: "Autenticación/Banca sin HTTPS",
        descripcion: "El sitio maneja flujos de acceso o financieros a través de un canal no cifrado.",
      });
    }

    return this.crearResultadoActivado(puntuacion, evidencias);
  }
}
