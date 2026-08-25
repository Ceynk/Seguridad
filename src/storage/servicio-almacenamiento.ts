import {
  ConfiguracionExtension,
  EstadisticasSesion,
  RegistroSitioReciente,
} from "../types/configuracion";

export const CONFIGURACION_PREDETERMINADA: ConfiguracionExtension = {
  proteccionHabilitada: true,
  modoProteccion: "EQUILIBRADO",
  umbralSospechoso: 30,
  umbralMalicioso: 70,
  bloquearMaliciososAutomaticamente: true,
  advertirSospechosos: true,
  listaBlancaPersonalizada: [],
  listaNegraPersonalizada: [],
  permitirBypassTemporal: true,
};

export const ESTADISTICAS_PREDETERMINADAS: EstadisticasSesion = {
  sitiosAnalizados: 0,
  amenazasBloqueadas: 0,
  sitiosSospechososDetectados: 0,
};

const CLAVE_ALMACENAMIENTO_CONFIGURACION = "phishguard_configuracion";
const CLAVE_ALMACENAMIENTO_ESTADISTICAS = "phishguard_estadisticas_sesion";

export class ServicioAlmacenamiento {
  private static memoriaLocal: Record<string, unknown> = {};

  private static esEntornoExtension(): boolean {
    return (
      typeof chrome !== "undefined" &&
      typeof chrome.storage !== "undefined" &&
      typeof chrome.storage.local !== "undefined"
    );
  }

  public static async obtenerConfiguracion(): Promise<ConfiguracionExtension> {
    if (this.esEntornoExtension()) {
      return new Promise((resolver) => {
        chrome.storage.local.get(
          [CLAVE_ALMACENAMIENTO_CONFIGURACION],
          (resultado) => {
            if (resultado && resultado[CLAVE_ALMACENAMIENTO_CONFIGURACION]) {
              resolver({
                ...CONFIGURACION_PREDETERMINADA,
                ...resultado[CLAVE_ALMACENAMIENTO_CONFIGURACION],
              });
            } else {
              resolver(CONFIGURACION_PREDETERMINADA);
            }
          }
        );
      });
    }

    const enMemoria = this.memoriaLocal[CLAVE_ALMACENAMIENTO_CONFIGURACION] as
      | ConfiguracionExtension
      | undefined;
    return enMemoria ? { ...CONFIGURACION_PREDETERMINADA, ...enMemoria } : CONFIGURACION_PREDETERMINADA;
  }

  public static async guardarConfiguracion(
    configuracion: ConfiguracionExtension
  ): Promise<void> {
    if (this.esEntornoExtension()) {
      return new Promise((resolver) => {
        chrome.storage.local.set(
          { [CLAVE_ALMACENAMIENTO_CONFIGURACION]: configuracion },
          () => resolver()
        );
      });
    }

    this.memoriaLocal[CLAVE_ALMACENAMIENTO_CONFIGURACION] = configuracion;
  }

  public static async obtenerEstadisticas(): Promise<EstadisticasSesion> {
    if (this.esEntornoExtension()) {
      return new Promise((resolver) => {
        chrome.storage.local.get(
          [CLAVE_ALMACENAMIENTO_ESTADISTICAS],
          (resultado) => {
            if (resultado && resultado[CLAVE_ALMACENAMIENTO_ESTADISTICAS]) {
              resolver({
                ...ESTADISTICAS_PREDETERMINADAS,
                ...resultado[CLAVE_ALMACENAMIENTO_ESTADISTICAS],
              });
            } else {
              resolver(ESTADISTICAS_PREDETERMINADAS);
            }
          }
        );
      });
    }

    const enMemoria = this.memoriaLocal[CLAVE_ALMACENAMIENTO_ESTADISTICAS] as
      | EstadisticasSesion
      | undefined;
    return enMemoria ? { ...ESTADISTICAS_PREDETERMINADAS, ...enMemoria } : ESTADISTICAS_PREDETERMINADAS;
  }

  public static async registrarAnalisis(
    sitio: RegistroSitioReciente
  ): Promise<EstadisticasSesion> {
    const estadisticasActuales = await this.obtenerEstadisticas();

    estadisticasActuales.sitiosAnalizados += 1;
    if (sitio.clasificacion === "MALICIOSO") {
      estadisticasActuales.amenazasBloqueadas += 1;
    } else if (sitio.clasificacion === "SOSPECHOSO") {
      estadisticasActuales.sitiosSospechososDetectados += 1;
    }

    estadisticasActuales.ultimoSitioAnalizado = sitio;

    if (this.esEntornoExtension()) {
      await new Promise<void>((resolver) => {
        chrome.storage.local.set(
          { [CLAVE_ALMACENAMIENTO_ESTADISTICAS]: estadisticasActuales },
          () => resolver()
        );
      });
    } else {
      this.memoriaLocal[CLAVE_ALMACENAMIENTO_ESTADISTICAS] = estadisticasActuales;
    }

    return estadisticasActuales;
  }

  public static async reiniciarEstadisticas(): Promise<void> {
    if (this.esEntornoExtension()) {
      await new Promise<void>((resolver) => {
        chrome.storage.local.set(
          { [CLAVE_ALMACENAMIENTO_ESTADISTICAS]: ESTADISTICAS_PREDETERMINADAS },
          () => resolver()
        );
      });
    } else {
      this.memoriaLocal[CLAVE_ALMACENAMIENTO_ESTADISTICAS] = ESTADISTICAS_PREDETERMINADAS;
    }
  }
}
