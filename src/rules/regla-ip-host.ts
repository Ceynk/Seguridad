import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

export class ReglaIpHost extends BaseReglaRiesgo {
  readonly id = "REGLA_IP_HOST";
  readonly nombre = "Uso de Dirección IP Directa como Host";
  readonly descripcion =
    "Detecta si la URL apunta directamente a una dirección IP numérica (IPv4 o IPv6), técnica frecuente en infraestructura de phishing efímera.";
  readonly severidad: SeveridadRegla = "ALTA";
  readonly puntuacionBase = 35;
  readonly tecnicaMitre: TecnicaMitreAttack = {
    id: "T1071.001",
    nombre: "Application Layer Protocol: Web Protocols via Direct IP",
    tactica: "Command and Control",
    enlace: "https://attack.mitre.org/techniques/T1071/001/",
  };

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
