/**
 * Tipos de datos para el sistema de reputación de dominios y listas locales/externas.
 */

export type TipoListaReputacion = "BLANCA" | "NEGRA" | "SOSPECHOSA";

export interface EntradaReputacion {
  dominio: string;
  tipo: TipoListaReputacion;
  categoria?: string;
  fechaRegistro: string;
  descripcion: string;
}

export interface ResultadoReputacion {
  encontrado: boolean;
  tipo?: TipoListaReputacion;
  categoria?: string;
  descripcion?: string;
  confianza: number;
}

export interface IndicadorAmenaza {
  id: string;
  tipoIndicador: "DOMINIO" | "PATRON_URL" | "HOST_IP";
  valor: string;
  clasificacion: "MALICIOSO" | "SOSPECHOSO";
  descripcion: string;
  fuente: string;
  marcaTemporal: number;
}

/**
 * Contrato extensible para proveedores de reputación (local o futuro Threat Intelligence remoto).
 */
export interface ProveedorReputacion {
  consultarDominio(dominio: string): Promise<ResultadoReputacion>;
  consultarUrl?(url: string): Promise<ResultadoReputacion>;
}
