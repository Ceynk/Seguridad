import {
  CaracteristicasUrl,
  ClasificacionRiesgo,
  NivelConfianza,
  ReglaCoincidente,
  ResultadoAnalisisUrl,
} from "../types/analisis";
import { ConfiguracionExtension } from "../types/configuracion";
import { ProveedorReputacion } from "../types/reputacion";
import { RegistroReglas } from "../rules/registro-reglas";
import { ProveedorReputacionLocal } from "../reputation/reputacion-local";
import { extraerCaracteristicasUrl } from "./extractor-caracteristicas";

/**
 * Opciones para instanciar o configurar el Motor de Riesgo.
 */
export interface OpcionesMotorRiesgo {
  registroReglas?: RegistroReglas;
  proveedorReputacion?: ProveedorReputacion;
  umbralSospechoso?: number;
  umbralMalicioso?: number;
}

/**
 * Motor central de análisis de riesgo, evaluación de reglas heurísticas y reputación.
 */
export class MotorRiesgo {
  private registroReglas: RegistroReglas;
  private proveedorReputacion: ProveedorReputacion;
  private umbralSospechoso: number;
  private umbralMalicioso: number;

  constructor(opciones: OpcionesMotorRiesgo = {}) {
    this.registroReglas = opciones.registroReglas || new RegistroReglas();
    this.proveedorReputacion = opciones.proveedorReputacion || new ProveedorReputacionLocal();
    this.umbralSospechoso = opciones.umbralSospechoso ?? 30;
    this.umbralMalicioso = opciones.umbralMalicioso ?? 70;
  }

  public actualizarConfiguracion(configuracion: ConfiguracionExtension): void {
    this.umbralSospechoso = configuracion.umbralSospechoso;
    this.umbralMalicioso = configuracion.umbralMalicioso;
    if (this.proveedorReputacion instanceof ProveedorReputacionLocal) {
      this.proveedorReputacion.actualizarListasUsuario(
        configuracion.listaBlancaPersonalizada,
        configuracion.listaNegraPersonalizada
      );
    }
  }

  /**
   * Clasifica una puntuación numérica (0-100) en el estado de riesgo correspondiente.
   */
  public clasificarPuntuacion(puntuacion: number): ClasificacionRiesgo {
    if (puntuacion >= this.umbralMalicioso) {
      return "MALICIOSO";
    }
    if (puntuacion >= this.umbralSospechoso) {
      return "SOSPECHOSO";
    }
    return "SEGURO";
  }

  /**
   * Determina el nivel de confianza de la clasificación en función del volumen de evidencias.
   */
  private determinarNivelConfianza(
    reglasCoincidentes: ReglaCoincidente[],
    esDominioConocido: boolean
  ): NivelConfianza {
    if (esDominioConocido) {
      return "ALTO";
    }
    if (reglasCoincidentes.length >= 2) {
      return "ALTO";
    }
    if (reglasCoincidentes.length === 1) {
      const regla = reglasCoincidentes[0];
      return regla.severidad === "CRITICA" || regla.severidad === "ALTA" ? "MEDIO" : "BAJO";
    }
    return "ALTO"; // Alta confianza en que un sitio sin ninguna anomalía es seguro
  }

  /**
   * Realiza el análisis completo de una URL de manera sincrónica/asincrónica y devuelve el veredicto.
   */
  public async analizarUrl(urlEntrada: string): Promise<ResultadoAnalisisUrl> {
    const tiempoInicio = performance.now();
    const caracteristicas: CaracteristicasUrl = extraerCaracteristicasUrl(urlEntrada);
    const nombreHost = caracteristicas.nombreHost;

    // 1. Consultar reputación del dominio
    const resultadoReputacion = await this.proveedorReputacion.consultarDominio(nombreHost);

    // Caso A: Dominio en lista negra conocida
    if (resultadoReputacion.encontrado && resultadoReputacion.tipo === "NEGRA") {
      const tiempoFin = performance.now();
      const reglaReputacion: ReglaCoincidente = {
        id: "REPUTACION_LISTA_NEGRA",
        nombre: "Dominio en Lista de Amenazas",
        descripcion: resultadoReputacion.descripcion || "Dominio identificado previamente como malicioso.",
        severidad: "CRITICA",
        puntuacion: 95,
        evidencias: [
          {
            campo: "reputacion",
            valorEncontrado: resultadoReputacion.categoria || "Malicioso",
            descripcion: resultadoReputacion.descripcion || "Coincidencia directa con amenaza conocida.",
          },
        ],
      };

      return {
        url: urlEntrada,
        nombreHost,
        marcaTemporal: Date.now(),
        puntuacionRiesgo: 95,
        clasificacion: "MALICIOSO",
        nivelConfianza: "ALTO",
        reglasCoincidentes: [reglaReputacion],
        caracteristicas,
        esDominioConocidoSeguro: false,
        esDominioConocidoMalicioso: true,
        tiempoProcesamientoMs: Math.round(tiempoFin - tiempoInicio),
      };
    }

    // Caso B: Dominio en lista blanca confiable y sin carácter '@' engañoso
    if (
      resultadoReputacion.encontrado &&
      resultadoReputacion.tipo === "BLANCA" &&
      !caracteristicas.tieneArroba
    ) {
      const tiempoFin = performance.now();
      return {
        url: urlEntrada,
        nombreHost,
        marcaTemporal: Date.now(),
        puntuacionRiesgo: 0,
        clasificacion: "SEGURO",
        nivelConfianza: "ALTO",
        reglasCoincidentes: [],
        caracteristicas,
        esDominioConocidoSeguro: true,
        esDominioConocidoMalicioso: false,
        tiempoProcesamientoMs: Math.round(tiempoFin - tiempoInicio),
      };
    }

    // Caso C: Evaluación heurística mediante el registro de reglas
    const reglasActivas = this.registroReglas.obtenerReglasActivas();
    const reglasCoincidentes: ReglaCoincidente[] = [];
    let sumaPuntuacion = 0;

    for (const regla of reglasActivas) {
      const resultado = await regla.evaluar(caracteristicas);
      if (resultado.activada) {
        reglasCoincidentes.push({
          id: resultado.reglaId,
          nombre: resultado.nombre,
          descripcion: resultado.descripcion,
          severidad: resultado.severidad,
          puntuacion: resultado.puntuacion,
          evidencias: resultado.evidencias,
        });
        sumaPuntuacion += resultado.puntuacion;
      }
    }

    // Factor de amplificación si se activan múltiples reglas simultáneas (sinergia de señales de ataque)
    if (reglasCoincidentes.length >= 3) {
      sumaPuntuacion = Math.round(sumaPuntuacion * 1.25);
    } else if (reglasCoincidentes.length === 2) {
      sumaPuntuacion = Math.round(sumaPuntuacion * 1.1);
    }

    // Limitar la puntuación entre 0 y 100
    const puntuacionRiesgo = Math.min(100, Math.max(0, sumaPuntuacion));
    const clasificacion = this.clasificarPuntuacion(puntuacionRiesgo);
    const nivelConfianza = this.determinarNivelConfianza(
      reglasCoincidentes,
      resultadoReputacion.encontrado
    );
    const tiempoFin = performance.now();

    return {
      url: urlEntrada,
      nombreHost,
      marcaTemporal: Date.now(),
      puntuacionRiesgo,
      clasificacion,
      nivelConfianza,
      reglasCoincidentes,
      caracteristicas,
      esDominioConocidoSeguro: false,
      esDominioConocidoMalicioso: false,
      tiempoProcesamientoMs: Math.round(tiempoFin - tiempoInicio),
    };
  }
}
