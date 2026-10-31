import {
  CaracteristicasUrl,
  ClasificacionRiesgo,
  NivelConfianza,
  ReglaCoincidente,
  ResultadoAnalisisUrl,
} from "../tipos/analisis";
import { ConfiguracionExtension } from "../tipos/configuracion";
import { ProveedorReputacion } from "../tipos/reputacion";
import { ProveedorReputacionLocal } from "../reputacion/reputacion-local";
import { extraerCaracteristicasUrl } from "./extractor-caracteristicas";
import { calcularHashSha256Simple, generarFichaForense } from "../utilidades/cripto-forense";

export interface OpcionesMotorRiesgo {
  proveedorReputacion?: ProveedorReputacion;
  umbralSospechoso?: number;
  umbralMalicioso?: number;
}

export class MotorRiesgo {
  private proveedorReputacion: ProveedorReputacion;
  private umbralSospechoso: number;
  private umbralMalicioso: number;

  constructor(opciones: OpcionesMotorRiesgo = {}) {
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

  public clasificarPuntuacion(puntuacion: number): ClasificacionRiesgo {
    if (puntuacion >= this.umbralMalicioso) return "MALICIOSO";
    if (puntuacion >= this.umbralSospechoso) return "SOSPECHOSO";
    return "SEGURO";
  }

  public async analizarUrl(urlEntrada: string): Promise<ResultadoAnalisisUrl> {
    const inicio = performance.now();
    const c = extraerCaracteristicasUrl(urlEntrada);
    const huellaSha256 = calcularHashSha256Simple(urlEntrada);
    const reputacion = await this.proveedorReputacion.consultarDominio(c.nombreHost);

    if (reputacion.encontrado && reputacion.tipo === "NEGRA") {
      return this.crearResultadoReputacion(
        urlEntrada, c, huellaSha256, 95, "MALICIOSO", true,
        "REPUTACION_LISTA_NEGRA", reputacion.descripcion || "Bloqueado", inicio
      );
    }
    if (reputacion.encontrado && reputacion.tipo === "BLANCA" && !c.tieneArroba) {
      return this.crearResultadoReputacion(
        urlEntrada, c, huellaSha256, 0, "SEGURO", false,
        "LISTA_BLANCA", "Sitio seguro verificado", inicio
      );
    }

    const tieneAutenticacion = c.contienePalabrasAutenticacion.length > 0;
    const tieneBanca = c.contienePalabrasBancarias.length > 0;
    const tieneUrgencia = c.contienePalabrasUrgencia.length > 0;
    const combinacionCredenciales = tieneAutenticacion && (tieneBanca || tieneUrgencia);
    const combinacionFraude = tieneBanca && tieneUrgencia;
    const hayContextoSocial = combinacionCredenciales || combinacionFraude;
    const palabrasSociales = [
      ...c.contienePalabrasAutenticacion,
      ...c.contienePalabrasBancarias,
      ...c.contienePalabrasUrgencia,
    ];

    const definicionesReglas: [boolean, string, string, string, number][] = [
      [c.esIpDirecta, "REGLA_IP", "Uso de IP", "El dominio es una dirección IP, táctica para evadir filtros.", 40],
      [c.tldEsSospechoso, "REGLA_TLD", "TLD de abuso frecuente", `La extensión .${c.extensionTld} tiene una alta tasa histórica de abuso.`, 20],
      [c.tieneArroba, "REGLA_ARROBA", "Uso de arroba", "La URL contiene '@', lo cual oculta el destino real.", 35],
      [c.cantidadSubdominios >= 3, "REGLA_SUBDOMINIOS", "Exceso subdominios", "Tiene una estructura de subdominios inusualmente profunda.", 10],
      [c.cantidadGuiones >= 2, "REGLA_GUIONES", "Exceso guiones", "El dominio tiene varias etiquetas compuestas.", 10],
      [c.longitudDominio > 45, "REGLA_LONGITUD_DOMINIO", "Dominio largo", "El nombre de dominio es inusualmente extenso.", 10],
      [c.porcentajeCodificacionUrl > 25, "REGLA_CODIFICACION", "Codificación alta", "La URL oculta una parte relevante de su contenido.", 10],
      [
        hayContextoSocial,
        "REGLA_CONTEXTO_SOCIAL",
        "Contexto de ingeniería social",
        `Combina términos de acción sensible y presión o contexto financiero: ${palabrasSociales.join(", ")}.`,
        combinacionCredenciales && combinacionFraude ? 30 : 25,
      ],
      [c.esPunycode || c.tieneLetrasRarasHomoglifos, "REGLA_PUNYCODE_HOMOGRAFO", "Caracteres engañosos", "El dominio utiliza caracteres extraños para imitar letras normales.", 60],
      [c.similitudMarcas.length > 0, "REGLA_SUPLANTACION", "Imitación", "El dominio se parece visualmente a una marca conocida.", 50],
      [c.protocolo === "http" && hayContextoSocial, "REGLA_HTTP", "Conexión insegura", "Usa HTTP durante una operación sensible.", 5],
    ];

    const reglasCoincidentes: ReglaCoincidente[] = definicionesReglas
      .filter(([activa]) => activa)
      .map(([, id, nombre, descripcion, puntuacion]) => ({
        id,
        nombre,
        descripcion,
        severidad: puntuacion >= 30 ? "ALTA" : "MEDIA",
        puntuacion,
        evidencias: [],
        tecnicaMitre: {
          id: id === "REGLA_PUNYCODE_HOMOGRAFO" ? "T1036.007" : "T1566.002",
          nombre: "Suplantación de identidad",
          tactica: "Acceso Inicial",
          enlace: "https://attack.mitre.org",
        },
      }));

    const puntuacionRiesgo = Math.min(100, reglasCoincidentes.reduce((suma, r) => suma + r.puntuacion, 0));
    const clasificacion = this.clasificarPuntuacion(puntuacionRiesgo);
    const hayImitacionVisual = c.esPunycode || c.tieneLetrasRarasHomoglifos || c.similitudMarcas.length > 0;
    const esBloqueoAltaConfianza = hayImitacionVisual || ((c.tieneArroba || c.esIpDirecta) && hayContextoSocial);
    const nivelConfianza: NivelConfianza =
      esBloqueoAltaConfianza || puntuacionRiesgo > 70 ? "ALTO" : puntuacionRiesgo >= 30 ? "MEDIO" : "BAJO";

    const resultado: ResultadoAnalisisUrl = {
      url: urlEntrada,
      nombreHost: c.nombreHost,
      marcaTemporal: Date.now(),
      puntuacionRiesgo,
      clasificacion,
      nivelConfianza,
      reglasCoincidentes,
      caracteristicas: c,
      esDominioConocidoSeguro: false,
      esDominioConocidoMalicioso: false,
      tiempoProcesamientoMs: Math.round(performance.now() - inicio),
      esBloqueoAltaConfianza,
      requiereConfirmacionDocumento: clasificacion === "MALICIOSO" && !esBloqueoAltaConfianza,
      huellaSha256,
    };

    const detallePerfil = reglasCoincidentes.length > 0
      ? reglasCoincidentes.map((r) => `- ${r.descripcion} (+${r.puntuacion} pts)`).join("\n")
      : "No se encontraron características sospechosas.";
    resultado.fichaForense = generarFichaForense(resultado);
    resultado.fichaForense.resumenTecnico = `Perfil de riesgo:\n${detallePerfil}\n`;
    return resultado;
  }

  private crearResultadoReputacion(
    url: string,
    caracteristicas: CaracteristicasUrl,
    huellaSha256: string,
    puntuacionRiesgo: number,
    clasificacion: ClasificacionRiesgo,
    esMalicioso: boolean,
    id: string,
    descripcion: string,
    inicio: number
  ): ResultadoAnalisisUrl {
    const resultado: ResultadoAnalisisUrl = {
      url,
      nombreHost: caracteristicas.nombreHost,
      marcaTemporal: Date.now(),
      puntuacionRiesgo,
      clasificacion,
      nivelConfianza: "ALTO",
      reglasCoincidentes: [{ id, nombre: descripcion, descripcion, severidad: "CRITICA", puntuacion: puntuacionRiesgo, evidencias: [] }],
      caracteristicas,
      esDominioConocidoSeguro: !esMalicioso,
      esDominioConocidoMalicioso: esMalicioso,
      tiempoProcesamientoMs: Math.round(performance.now() - inicio),
      esBloqueoAltaConfianza: esMalicioso,
      requiereConfirmacionDocumento: false,
      huellaSha256,
    };
    resultado.fichaForense = generarFichaForense(resultado);
    resultado.fichaForense.resumenTecnico = descripcion;
    return resultado;
  }
}
