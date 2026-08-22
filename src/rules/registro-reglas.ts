import { ReglaRiesgo } from "../types/reglas";
import { ReglaSuplantacionMarca } from "./regla-suplantacion-marca";
import { ReglaIpHost } from "./regla-ip-host";
import { ReglaSeguridadProtocolo } from "./regla-seguridad-protocolo";
import { ReglaPunycodeHomografo } from "./regla-punycode-homografo";
import { ReglaPalabrasSospechosas } from "./regla-palabras-sospechosas";
import { ReglaTldSospechoso } from "./regla-tld-sospechoso";
import { ReglaEstructuraUrl } from "./regla-estructura-url";

/**
 * Registro y fábrica de reglas de análisis de riesgo de la extensión.
 */
export class RegistroReglas {
  private reglas: Map<string, ReglaRiesgo> = new Map();

  constructor() {
    this.registrarReglasPorDefecto();
  }

  private registrarReglasPorDefecto(): void {
    this.registrarRegla(new ReglaSuplantacionMarca());
    this.registrarRegla(new ReglaIpHost());
    this.registrarRegla(new ReglaSeguridadProtocolo());
    this.registrarRegla(new ReglaPunycodeHomografo());
    this.registrarRegla(new ReglaPalabrasSospechosas());
    this.registrarRegla(new ReglaTldSospechoso());
    this.registrarRegla(new ReglaEstructuraUrl());
  }

  public registrarRegla(regla: ReglaRiesgo): void {
    this.reglas.set(regla.id, regla);
  }

  public desregistrarRegla(idRegla: string): boolean {
    return this.reglas.delete(idRegla);
  }

  public obtenerReglasActivas(): ReglaRiesgo[] {
    return Array.from(this.reglas.values()).filter((regla) => regla.habilitada);
  }

  public obtenerReglaPorId(idRegla: string): ReglaRiesgo | undefined {
    return this.reglas.get(idRegla);
  }

  public obtenerTotalReglas(): number {
    return this.reglas.size;
  }
}
