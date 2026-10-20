import {
  CaracteristicasUrl,
  ClasificacionRiesgo,
  NivelConfianza,
  ReglaCoincidente,
  ResultadoAnalisisUrl,
} from "../types/analisis";
import { ConfiguracionExtension } from "../types/configuracion";
import { ProveedorReputacion } from "../types/reputacion";
import { ProveedorReputacionLocal } from "../reputation/reputacion-local";
import { extraerCaracteristicasUrl } from "./extractor-caracteristicas";
import { calcularHashSha256Simple, generarFichaForense } from "../utils/cripto-forense";

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
    const tiempoInicio = performance.now();
    const caracteristicas = extraerCaracteristicasUrl(urlEntrada);
    const nombreHost = caracteristicas.nombreHost;
    const huellaSha256 = calcularHashSha256Simple(urlEntrada);

    const resultadoReputacion = await this.proveedorReputacion.consultarDominio(nombreHost);

    if (resultadoReputacion.encontrado && resultadoReputacion.tipo === "NEGRA") {
      return this.crearResultadoRapido(urlEntrada, caracteristicas, 95, "MALICIOSO", true, "REPUTACION_LISTA_NEGRA", resultadoReputacion.descripcion || "Bloqueado", tiempoInicio);
    }
    if (resultadoReputacion.encontrado && resultadoReputacion.tipo === "BLANCA" && !caracteristicas.tieneArroba) {
      return this.crearResultadoRapido(urlEntrada, caracteristicas, 0, "SEGURO", false, "LISTA_BLANCA", "Sitio seguro verificado", tiempoInicio);
    }

    let puntuacion = 0;
    let explicacionPerfil = "Perfil de riesgo:\n";
    const reglasCoincidentes: ReglaCoincidente[] = [];

    function agregarRegla(id: string, nombre: string, desc: string, puntos: number) {
      puntuacion += puntos;
      explicacionPerfil += `- ${desc} (+${puntos} pts)\n`;
      reglasCoincidentes.push({
        id, nombre, descripcion: desc, severidad: puntos >= 30 ? "ALTA" : "MEDIA", puntuacion: puntos, evidencias: [],
        tecnicaMitre: {
          id: id === "REGLA_PUNYCODE_HOMOGRAFO" ? "T1036.007" : "T1566.002",
          nombre: "Phishing",
          tactica: "Initial Access",
          enlace: "https://attack.mitre.org"
        }
      });
    }

    if (caracteristicas.esIpDirecta) {
      agregarRegla("REGLA_IP", "Uso de IP", "El dominio es una dirección IP, táctica para evadir filtros.", 40);
    }

    if (caracteristicas.tldEsSospechoso) {
      agregarRegla("REGLA_TLD", "TLD de abuso frecuente", `La extensión .${caracteristicas.extensionTld} tiene una alta tasa histórica de abuso.`, 20);
    }

    if (caracteristicas.tieneArroba) {
      agregarRegla("REGLA_ARROBA", "Uso de arroba", "La URL contiene '@', lo cual oculta el destino real.", 35);
    }
    if (caracteristicas.cantidadSubdominios >= 3) {
      agregarRegla("REGLA_SUBDOMINIOS", "Exceso subdominios", "Tiene una estructura de subdominios inusualmente profunda.", 10);
    }
    if (caracteristicas.cantidadGuiones >= 2) {
      agregarRegla("REGLA_GUIONES", "Exceso guiones", "El dominio tiene varias etiquetas compuestas.", 10);
    }
    
    if (caracteristicas.longitudDominio > 45) {
      agregarRegla("REGLA_LONGITUD_DOMINIO", "Dominio largo", "El nombre de dominio es inusualmente extenso.", 10);
    }
    
    if (caracteristicas.porcentajeCodificacionUrl > 25) {
      agregarRegla("REGLA_CODIFICACION", "Codificación alta", "La URL oculta una parte relevante de su contenido.", 10);
    }

    const contieneAutenticacion = caracteristicas.contienePalabrasAutenticacion.length > 0;
    const contieneBanca = caracteristicas.contienePalabrasBancarias.length > 0;
    const contieneUrgencia = caracteristicas.contienePalabrasUrgencia.length > 0;
    const combinacionCredenciales = contieneAutenticacion && (contieneBanca || contieneUrgencia);
    const combinacionFraude = contieneBanca && contieneUrgencia;

    if (combinacionCredenciales || combinacionFraude) {
      const palabrasTotales = [
        ...caracteristicas.contienePalabrasAutenticacion,
        ...caracteristicas.contienePalabrasBancarias,
        ...caracteristicas.contienePalabrasUrgencia
      ];
      agregarRegla(
        "REGLA_CONTEXTO_SOCIAL",
        "Contexto de ingeniería social",
        `Combina términos de acción sensible y presión o contexto financiero: ${palabrasTotales.join(", ")}.`,
        combinacionCredenciales && combinacionFraude ? 30 : 25
      );
    }

    if (caracteristicas.esPunycode || caracteristicas.tieneLetrasRarasHomoglifos) {
      agregarRegla("REGLA_PUNYCODE_HOMOGRAFO", "Caracteres engañosos", "El dominio utiliza caracteres extraños para imitar letras normales.", 60);
    }
    if (caracteristicas.similitudMarcas.length > 0) {
      agregarRegla("REGLA_SUPLANTACION", "Imitación", `El dominio se parece visualmente a una marca conocida.`, 50);
    }

    if (caracteristicas.protocolo === "http" && (combinacionCredenciales || combinacionFraude)) {
      agregarRegla("REGLA_HTTP", "Conexión insegura", "Usa HTTP durante una operación sensible.", 5);
    }

    if (puntuacion === 0) {
      explicacionPerfil += "No se encontraron características sospechosas.\n";
    }

    if (puntuacion > 100) puntuacion = 100;

    const clasificacion = this.clasificarPuntuacion(puntuacion);
    const tieneEvidenciaFuerte =
      caracteristicas.tieneArroba ||
      caracteristicas.esIpDirecta ||
      caracteristicas.esPunycode ||
      caracteristicas.tieneLetrasRarasHomoglifos ||
      caracteristicas.similitudMarcas.length > 0;
    const esBloqueoAltaConfianza =
      tieneEvidenciaFuerte &&
      (caracteristicas.esPunycode ||
        caracteristicas.tieneLetrasRarasHomoglifos ||
        caracteristicas.similitudMarcas.length > 0 ||
        (caracteristicas.tieneArroba && (combinacionCredenciales || combinacionFraude)) ||
        (caracteristicas.esIpDirecta && (combinacionCredenciales || combinacionFraude)));
    const nivelConfianza: NivelConfianza = esBloqueoAltaConfianza || puntuacion > 70 ? "ALTO" : puntuacion >= 30 ? "MEDIO" : "BAJO";
    const tiempoFin = performance.now();

    const resultadoFinal: ResultadoAnalisisUrl = {
      url: urlEntrada,
      nombreHost,
      marcaTemporal: Date.now(),
      puntuacionRiesgo: puntuacion,
      clasificacion,
      nivelConfianza,
      reglasCoincidentes,
      caracteristicas,
      esDominioConocidoSeguro: false,
      esDominioConocidoMalicioso: false,
      tiempoProcesamientoMs: Math.round(tiempoFin - tiempoInicio),
      esBloqueoAltaConfianza,
      requiereConfirmacionDocumento: clasificacion === "MALICIOSO" && !esBloqueoAltaConfianza,
      huellaSha256,
    };

    resultadoFinal.fichaForense = generarFichaForense(resultadoFinal);
    resultadoFinal.fichaForense.resumenTecnico = explicacionPerfil;

    return resultadoFinal;
  }

  private crearResultadoRapido(url: string, chars: CaracteristicasUrl, pts: number, clasificacion: ClasificacionRiesgo, esMalo: boolean, reglaId: string, desc: string, inicio: number): ResultadoAnalisisUrl {
    const res: ResultadoAnalisisUrl = {
      url, nombreHost: chars.nombreHost, marcaTemporal: Date.now(), puntuacionRiesgo: pts, clasificacion, nivelConfianza: "ALTO",
      reglasCoincidentes: [{id: reglaId, nombre: desc, descripcion: desc, severidad: "CRITICA", puntuacion: pts, evidencias: []}],
      caracteristicas: chars, esDominioConocidoSeguro: !esMalo, esDominioConocidoMalicioso: esMalo, tiempoProcesamientoMs: Math.round(performance.now() - inicio),
      esBloqueoAltaConfianza: esMalo,
      requiereConfirmacionDocumento: false,
      huellaSha256: calcularHashSha256Simple(url)
    };
    res.fichaForense = generarFichaForense(res);
    res.fichaForense.resumenTecnico = desc;
    return res;
  }
}
