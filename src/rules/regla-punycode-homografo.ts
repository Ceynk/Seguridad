import { CaracteristicasUrl, EvidenciaRiesgo, SeveridadRegla, TecnicaMitreAttack } from "../types/analisis";
import { ResultadoEvaluacionRegla } from "../types/reglas";
import { BaseReglaRiesgo } from "./base-regla";

export class ReglaPunycodeHomografo extends BaseReglaRiesgo {
  readonly id = "REGLA_PUNYCODE_HOMOGRAFO";
  readonly nombre = "Ataque Homógrafo y Falsificación por Letras Raras";
  readonly descripcion =
    "Detecta dominios engañosos con letras raras (caracteres cirílicos, griegos o confusables) y codificación Punycode que simulan ser sitios web legítimos.";
  readonly severidad: SeveridadRegla = "CRITICA";
  readonly puntuacionBase = 85;
  readonly tecnicaMitre: TecnicaMitreAttack = {
    id: "T1036.007",
    nombre: "Masquerading: IDN Homograph Attacks",
    tactica: "Defense Evasion",
    enlace: "https://attack.mitre.org/techniques/T1036/007/",
  };

  evaluar(caracteristicas: CaracteristicasUrl): ResultadoEvaluacionRegla {
    const tieneHomoglifos = caracteristicas.tieneLetrasRarasHomoglifos;
    const esPunycode = caracteristicas.esPunycode;
    const tieneAlfabetoMixto = caracteristicas.tieneAlfabetoMixto;

    // Si no es Punycode ni tiene letras raras o alfabeto mixto, no se activa
    if (!esPunycode && !tieneHomoglifos && !tieneAlfabetoMixto) {
      return this.crearResultadoNoActivado();
    }

    const evidencias: EvidenciaRiesgo[] = [];
    let puntuacion = this.puntuacionBase;

    if (tieneAlfabetoMixto) {
      evidencias.push({
        campo: "alfabetoMixto",
        valorEncontrado: "Mezcla de alfabetos (Script Spoofing)",
        descripcion:
          "El dominio combina letras del alfabeto latino con caracteres no latinos (cirílicos o griegos) para aparentar que el texto es legítimo a simple vista.",
      });
    }

    if (caracteristicas.desgloseLetrasRaras && caracteristicas.desgloseLetrasRaras.length > 0) {
      const resumenLetras = caracteristicas.desgloseLetrasRaras
        .slice(0, 5)
        .map((d) => `'${d.caracterSospechoso}' (${d.tipo})`)
        .join(", ");

      evidencias.push({
        campo: "letrasRarasDetectadas",
        valorEncontrado: `${caracteristicas.desgloseLetrasRaras.length} caracteres confusables`,
        descripcion: `Letras anómalas detectadas: ${resumenLetras}. Están colocadas para engañar visualmente al usuario.`,
      });
    }

    if (esPunycode) {
      const dominioDecodificado = caracteristicas.dominioDecodificadoUnicode || caracteristicas.nombreHost;
      evidencias.push({
        campo: "nombreHostPunycode",
        valorEncontrado: caracteristicas.nombreHost,
        descripcion: `El dominio utiliza codificación internacionalizada Punycode (${caracteristicas.nombreHost}), la cual traduce a: '${dominioDecodificado}'.`,
      });
    }

    if (
      caracteristicas.similitudMarcas.length > 0 ||
      caracteristicas.contienePalabrasBancarias.length > 0 ||
      caracteristicas.contienePalabrasAutenticacion.length > 0
    ) {
      puntuacion = 95;
      evidencias.push({
        campo: "homografoConMarcaOEntidad",
        valorEncontrado: "Suplantación de Marca / Sector Financiero",
        descripcion:
          "El dominio utiliza letras sustitutas para fingir la identidad de una entidad bancaria, marca o servicio sensible.",
      });
    }

    return this.crearResultadoActivado(puntuacion, evidencias);
  }
}

