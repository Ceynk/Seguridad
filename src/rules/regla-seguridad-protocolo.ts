import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

export class ReglaSeguridadProtocolo extends BaseReglaRiesgo {
  readonly id = "REGLA_SEGURIDAD_PROTOCOLO";
  readonly nombre = "Protocolo No Cifrado (HTTP)";
  readonly descripcion =
    "Detecta el uso de protocolo HTTP no cifrado en páginas web.";
  readonly severidad: SeveridadRegla = "MEDIA";
  readonly puntuacionBase = 15;
  readonly tecnicaMitre: TecnicaMitreAttack = {
    id: "T1040",
    nombre: "Network Sniffing",
    tactica: "Credential Access",
    enlace: "https://attack.mitre.org/techniques/T1040/",
  };

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
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
