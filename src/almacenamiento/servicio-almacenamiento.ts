import {
  ConfiguracionExtension,
  EstadisticasSesion,
  RegistroSitioReciente,
} from "../tipos/configuracion";

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

const CLAVE_CONFIGURACION = "phishguard_configuracion";
const CLAVE_ESTADISTICAS = "phishguard_estadisticas_sesion";

export class ServicioAlmacenamiento {
  private static memoriaLocal: Record<string, unknown> = {};

  private static tieneAlmacenamientoExtension(): boolean {
    return typeof chrome !== "undefined" && Boolean(chrome.storage?.local);
  }

  private static async leer<T extends object>(clave: string, predeterminado: T): Promise<T> {
    if (this.tieneAlmacenamientoExtension()) {
      return new Promise((resolver) => {
        chrome.storage.local.get([clave], (resultado) => {
          const guardado = resultado?.[clave] as Partial<T> | undefined;
          resolver(guardado ? { ...predeterminado, ...guardado } : { ...predeterminado });
        });
      });
    }
    const enMemoria = this.memoriaLocal[clave] as Partial<T> | undefined;
    return enMemoria ? { ...predeterminado, ...enMemoria } : { ...predeterminado };
  }

  private static async escribir<T>(clave: string, valor: T): Promise<void> {
    if (this.tieneAlmacenamientoExtension()) {
      return new Promise((resolver) => {
        chrome.storage.local.set({ [clave]: valor }, () => resolver());
      });
    }
    this.memoriaLocal[clave] = valor;
  }

  public static obtenerConfiguracion(): Promise<ConfiguracionExtension> {
    return this.leer(CLAVE_CONFIGURACION, CONFIGURACION_PREDETERMINADA);
  }

  public static guardarConfiguracion(configuracion: ConfiguracionExtension): Promise<void> {
    return this.escribir(CLAVE_CONFIGURACION, configuracion);
  }

  public static obtenerEstadisticas(): Promise<EstadisticasSesion> {
    return this.leer(CLAVE_ESTADISTICAS, ESTADISTICAS_PREDETERMINADAS);
  }

  public static async registrarAnalisis(sitio: RegistroSitioReciente): Promise<EstadisticasSesion> {
    const estadisticas = await this.obtenerEstadisticas();
    estadisticas.sitiosAnalizados += 1;
    if (sitio.clasificacion === "MALICIOSO") {
      estadisticas.amenazasBloqueadas += 1;
    } else if (sitio.clasificacion === "SOSPECHOSO") {
      estadisticas.sitiosSospechososDetectados += 1;
    }
    estadisticas.ultimoSitioAnalizado = sitio;
    await this.escribir(CLAVE_ESTADISTICAS, estadisticas);
    return estadisticas;
  }

  public static reiniciarEstadisticas(): Promise<void> {
    return this.escribir(CLAVE_ESTADISTICAS, { ...ESTADISTICAS_PREDETERMINADAS });
  }
}
