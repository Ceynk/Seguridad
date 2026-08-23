import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

export class ReglaPunycodeHomografo extends BaseReglaRiesgo {
  readonly id = "REGLA_PUNYCODE_HOMOGRAFO";
  readonly nombre = "Uso de Dominio Internacionalizado (Punycode / Homógrafo)";
  readonly descripcion =
    "Detecta nombres de dominio codificados en Punycode (prefijo 'xn--'), técnica habitual para suplantar dominios con caracteres visualmente idénticos.";
  readonly severidad: SeveridadRegla = "ALTA";
  readonly puntuacionBase = 35;
  readonly tecnicaMitre: TecnicaMitreAttack = {
    id: "T1036.007",
    nombre: "Masquerading: IDN Homograph Attacks",
    tactica: "Defense Evasion",
    enlace: "https://attack.mitre.org/techniques/T1036/007/",
  };

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    if (!caracteristicas.esPunycode) {
      return this.crearResultadoNoActivado();
    }

    const evidencias: EvidenciaRiesgo[] = [
      {
        campo: "nombreHost",
        valorEncontrado: caracteristicas.nombreHost,
        descripcion: `El dominio utiliza codificación Punycode (${caracteristicas.nombreHost}), indicativo de posibles caracteres homográficos no latinos.`,
      },
    ];

    let puntuacion = this.puntuacionBase;

    if (
      caracteristicas.contienePalabrasAutenticacion.length > 0 ||
      caracteristicas.contienePalabrasBancarias.length > 0 ||
      caracteristicas.similitudMarcas.length > 0
    ) {
      puntuacion += 25;
      evidencias.push({
        campo: "punycodeConMarca",
        valorEncontrado: "Punycode + Entidad Sensible",
        descripcion: "El dominio Punycode coincide con patrones bancarios o de autenticación.",
      });
    }

    return this.crearResultadoActivado(puntuacion, evidencias);
  }
}
