import {
  ConfiguracionExtension,
  EstadisticasSesion,
  RegistroSitioReciente,
} from "../types/configuracion";

/**
 * Configuración predeterminada segura del sistema.
 */
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

/**
 * Estadísticas iniciales de sesión.
 */
export const ESTADISTICAS_PREDETERMINADAS: EstadisticasSesion = {
  sitiosAnalizados: 0,
  amenazasBloqueadas: 0,
  sitiosSospechososDetectados: 0,
};

const CLAVE_ALMACENAMIENTO_CONFIGURACION = "phishguard_configuracion";
const CLAVE_ALMACENAMIENTO_ESTADISTICAS = "phishguard_estadisticas_sesion";

/**
 * Servicio de almacenamiento local tipado y seguro para Chrome Storage API.
 * Respeta la privacidad: NO almacena historial de navegación persistente ni contenido privado.
 */
export class ServicioAlmacenamiento {
  private static memoriaLocal: Record<string, unknown> = {};

  /**
   * Determina si la API de Chrome Extensions está disponible.
   */
  private static esEntornoExtension(): boolean {
    return (
      typeof chrome !== "undefined" &&
      typeof chrome.storage !== "undefined" &&
      typeof chrome.storage.local !== "undefined"
    );
  }

  /**
   * Obtiene la configuración actual del usuario.
   */
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

  /**
   * Guarda las preferencias de configuración del usuario.
   */
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

  /**
   * Obtiene las estadísticas acumuladas en la sesión.
   */
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

  /**
   * Actualiza el registro del último sitio analizado y sus contadores.
   */
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

  /**
   * Restablece las métricas de la sesión a ceros.
   */
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
