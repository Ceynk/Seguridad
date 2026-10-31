import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Activity, AlertTriangle, Check, ChevronDown, ChevronRight, Copy,
  Download, FileCode, Flag, Globe, RotateCcw, Search, Settings, ShieldAlert, ShieldCheck,
} from "lucide-react";
import { ClasificacionRiesgo, ResultadoAnalisisUrl } from "../../tipos/analisis";
import { ConfiguracionExtension, EstadisticasSesion } from "../../tipos/configuracion";
import {
  CONFIGURACION_PREDETERMINADA, ESTADISTICAS_PREDETERMINADAS, ServicioAlmacenamiento,
} from "../../almacenamiento/servicio-almacenamiento";
import { MotorRiesgo } from "../../analizador/motor-riesgo";
import { descargarInformeForense, exportarInformeMarkdown } from "../../utilidades/cripto-forense";
import { MAPA_HOMOGLIFOS } from "../../analizador/homoglifos";
import "./emergente.css";

const motorLocal = new MotorRiesgo();
type PestanaEmergente = "estado" | "analisis" | "csirt";

const CASOS_PRUEBA = [
  { nombre: "Banc0l0mbia", url: "http://banc0l0mbia-login-verificacion.xyz/portal/actualizar" },
  { nombre: "Nequ1", url: "http://nequ1-recuperacion-cuenta.top/seguridad" },
  { nombre: "Google", url: "https://www.google.com/search?q=ciberseguridad" },
];

const ESQUEMAS_INTERNOS = ["chrome://", "chrome-extension://", "about:", "edge://"];

const METADATOS_ESTADO: Record<ClasificacionRiesgo, { clase: string; subtitulo: string; Icono: typeof ShieldCheck }> = {
  SEGURO: { clase: "estado-seguro", subtitulo: "No se detectaron amenazas", Icono: ShieldCheck },
  SOSPECHOSO: { clase: "estado-sospechoso", subtitulo: "Precaución recomendada", Icono: AlertTriangle },
  MALICIOSO: { clase: "estado-peligroso", subtitulo: "Sitio fraudulento detectado", Icono: ShieldAlert },
};

async function obtenerUrlPestanaActiva(): Promise<{ url: string; interna: boolean }> {
  if (typeof chrome === "undefined" || !chrome.tabs?.query) return { url: CASOS_PRUEBA[0].url, interna: false };
  const [pestana] = await chrome.tabs.query({ active: true, currentWindow: true });
  const url = pestana?.url;
  if (!url) return { url: CASOS_PRUEBA[0].url, interna: false };
  if (ESQUEMAS_INTERNOS.some((prefijo) => url.startsWith(prefijo))) return { url: CASOS_PRUEBA[0].url, interna: true };
  return { url, interna: false };
}

export const ComponenteEmergente: React.FC = () => {
  const [pestanaActiva, establecerPestanaActiva] = useState<PestanaEmergente>("estado");
  const [analisisActual, establecerAnalisisActual] = useState<ResultadoAnalisisUrl | null>(null);
  const [configuracion, establecerConfiguracion] = useState<ConfiguracionExtension>(CONFIGURACION_PREDETERMINADA);
  const [estadisticas, establecerEstadisticas] = useState<EstadisticasSesion>(ESTADISTICAS_PREDETERMINADAS);
  const [cargando, establecerCargando] = useState(true);
  const [copiado, establecerCopiado] = useState(false);
  const [urlManual, establecerUrlManual] = useState("");
  const [esPaginaInterna, establecerEsPaginaInterna] = useState(false);
  const [mostrarReglas, establecerMostrarReglas] = useState(true);

  const analizarUrl = async (urlObjetivo: string): Promise<void> => {
    establecerCargando(true);
    try {
      establecerAnalisisActual(await motorLocal.analizarUrl(urlObjetivo));
      establecerPestanaActiva("estado");
    } finally {
      establecerCargando(false);
    }
  };

  useEffect(() => {
    (async () => {
      try {
        const [configGuardada, estadisticasGuardadas, destino] = await Promise.all([
          ServicioAlmacenamiento.obtenerConfiguracion(),
          ServicioAlmacenamiento.obtenerEstadisticas(),
          obtenerUrlPestanaActiva(),
        ]);
        establecerConfiguracion(configGuardada);
        establecerEstadisticas(estadisticasGuardadas);
        establecerEsPaginaInterna(destino.interna);
        motorLocal.actualizarConfiguracion(configGuardada);
        establecerAnalisisActual(await motorLocal.analizarUrl(destino.url));
      } catch {
        establecerAnalisisActual(await motorLocal.analizarUrl(CASOS_PRUEBA[0].url));
      } finally {
        establecerCargando(false);
      }
    })();
  }, []);

  const alternarProteccion = async () => {
    const actualizada = { ...configuracion, proteccionHabilitada: !configuracion.proteccionHabilitada };
    establecerConfiguracion(actualizada);
    motorLocal.actualizarConfiguracion(actualizada);
    await ServicioAlmacenamiento.guardarConfiguracion(actualizada);
    chrome?.runtime?.sendMessage?.({ tipo: "ACTUALIZAR_CONFIGURACION", configuracion: actualizada });
  };

  const abrirOpciones = () => {
    if (typeof chrome !== "undefined" && chrome.runtime?.openOptionsPage) chrome.runtime.openOptionsPage();
    else window.open("../opciones/index.html", "_blank");
  };

  const abrirFormularioReporte = () => {
    const urlCodificada = analisisActual ? encodeURIComponent(analisisActual.url) : "";
    if (typeof chrome !== "undefined" && chrome.tabs?.create) {
      chrome.tabs.create({ url: chrome.runtime.getURL(`src/paginas/opciones/index.html?reportar=${urlCodificada}`) });
    } else {
      window.open(`../opciones/index.html?reportar=${urlCodificada}`, "_blank");
    }
  };

  const copiarFichaMarkdown = () => {
    if (!analisisActual?.fichaForense) return;
    void navigator.clipboard.writeText(exportarInformeMarkdown(analisisActual.fichaForense));
    establecerCopiado(true);
    setTimeout(() => establecerCopiado(false), 2000);
  };

  const descargarFichaMarkdown = () => {
    if (analisisActual?.fichaForense) {
      descargarInformeForense(`csirt_${analisisActual.nombreHost}.md`, analisisActual.fichaForense);
    }
  };

  const reiniciarContadores = async () => {
    await ServicioAlmacenamiento.reiniciarEstadisticas();
    establecerEstadisticas(ESTADISTICAS_PREDETERMINADAS);
  };

  const manejarBusquedaManual = (evento: React.FormEvent) => {
    evento.preventDefault();
    if (urlManual.trim()) void analizarUrl(urlManual.trim());
  };

  const estadoVisual = analisisActual ? METADATOS_ESTADO[analisisActual.clasificacion] : null;
  const IconoEstado = estadoVisual?.Icono ?? ShieldCheck;
  const reglasConMitre = analisisActual?.reglasCoincidentes.filter((r) => r.tecnicaMitre) ?? [];

  return (
    <div className="contenedor-aplicacion">
      <header className="cabecera-aplicacion">
        <div className="marca">
          <ShieldCheck size={18} className="icono-marca" />
          <span className="nombre-marca">PhishGuard</span>
        </div>
        <div className="acciones-cabecera">
          <label className="interruptor" title="Protección en tiempo real">
            <input type="checkbox" checked={configuracion.proteccionHabilitada} onChange={alternarProteccion} />
            <span className="deslizador-interruptor" />
          </label>
          <button className="boton-icono" onClick={abrirOpciones} title="Configuración">
            <Settings size={16} />
          </button>
        </div>
      </header>

      <nav className="barra-pestanas">
        {([["estado", "Estado"], ["analisis", "Análisis URL"], ["csirt", "CSIRT"]] as [PestanaEmergente, string][]).map(
          ([id, etiqueta]) => (
            <button
              key={id}
              className={`boton-pestana ${pestanaActiva === id ? "activa" : ""}`}
              onClick={() => establecerPestanaActiva(id)}
            >
              {etiqueta}
            </button>
          )
        )}
      </nav>

      <main className="contenido-principal">
        {cargando ? (
          <div className="estado-carga">
            <Activity className="giratorio" size={24} />
            <span>Analizando enlace...</span>
          </div>
        ) : analisisActual && estadoVisual ? (
          <>
            {pestanaActiva === "estado" && (
              <div className="panel-pestana">
                <div className={`tarjeta-estado ${estadoVisual.clase}`}>
                  <div className="cabecera-estado">
                    <IconoEstado className="icono-estado" size={32} />
                    <div className="grupo-titulo-estado">
                      <h2 className="titulo-estado">{analisisActual.clasificacion}</h2>
                      <span className="subtitulo-estado">{estadoVisual.subtitulo}</span>
                    </div>
                  </div>
                  <div className="contenedor-barra-riesgo">
                    <div className="etiquetas-barra-riesgo">
                      <span>Riesgo</span>
                      <span>{analisisActual.puntuacionRiesgo}/100</span>
                    </div>
                    <div className="pista-barra-riesgo">
                      <div className="relleno-barra-riesgo" style={{ width: `${Math.min(100, analisisActual.puntuacionRiesgo)}%` }} />
                    </div>
                  </div>
                </div>

                <div className="tarjeta-dominio">
                  <span className="etiqueta-dominio">Dominio actual</span>
                  <div className="valor-dominio" title={analisisActual.url}>
                    <Globe size={14} className="texto-atenuado" />
                    <span className="truncado">
                      {analisisActual.nombreHost.split("").map((caracter, i) => (
                        <span key={i} className={MAPA_HOMOGLIFOS[caracter.toLowerCase()] ? "caracter-alerta" : ""}>
                          {caracter}
                        </span>
                      ))}
                    </span>
                  </div>
                </div>

                {analisisActual.reglasCoincidentes.length > 0 && (
                  <div className="seccion-indicadores">
                    <div className="cabecera-seccion" onClick={() => establecerMostrarReglas(!mostrarReglas)}>
                      <h3>Indicadores detectados ({analisisActual.reglasCoincidentes.length})</h3>
                      {mostrarReglas ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </div>
                    {mostrarReglas && (
                      <div className="lista-indicadores">
                        {analisisActual.reglasCoincidentes.map((regla) => (
                          <div key={regla.id} className="elemento-indicador">
                            <div className="principal-indicador">
                              <span>{regla.nombre}</span>
                              <span className="puntaje-indicador">+{regla.puntuacion}</span>
                            </div>
                            <p className="descripcion-indicador">{regla.descripcion}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {pestanaActiva === "analisis" && (
              <div className="panel-pestana">
                <p className="descripcion-seccion">
                  Analiza un enlace independientemente de la página actual para verificar su seguridad antes de acceder.
                </p>
                <form onSubmit={manejarBusquedaManual} className="formulario-busqueda">
                  <div className="envoltorio-entrada">
                    <Search size={14} className="icono-busqueda" />
                    <input
                      type="url"
                      className="entrada-busqueda"
                      placeholder="https://ejemplo.com/login"
                      value={urlManual}
                      onChange={(e) => establecerUrlManual(e.target.value)}
                      required
                    />
                  </div>
                  <button type="submit" className="boton boton-primario">Analizar</button>
                </form>

                {esPaginaInterna && (
                  <div className="seccion-ejemplos">
                    <span className="etiqueta-ejemplos">Enlaces de prueba:</span>
                    <div className="contenedor-ejemplos">
                      {CASOS_PRUEBA.map((caso) => (
                        <button key={caso.nombre} className="ficha-ejemplo" onClick={() => analizarUrl(caso.url)}>
                          {caso.nombre}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                <div className="cuadricula-estadisticas">
                  {([
                    [estadisticas.sitiosAnalizados, "Analizados", ""],
                    [estadisticas.sitiosSospechososDetectados, "Sospechosos", "texto-advertencia"],
                    [estadisticas.amenazasBloqueadas, "Bloqueados", "texto-peligro"],
                  ] as [number, string, string][]).map(([valor, etiqueta, clase]) => (
                    <div key={etiqueta} className="caja-estadistica">
                      <span className={`valor-estadistica ${clase}`}>{valor}</span>
                      <span className="etiqueta-estadistica">{etiqueta}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {pestanaActiva === "csirt" && (
              <div className="panel-pestana">
                <div className="cabecera-csirt">
                  <FileCode size={16} />
                  <h3>Registro Técnico</h3>
                </div>
                <div className="registro-csirt">
                  <div className="fila-registro">
                    <span className="etiqueta-registro">Estado</span>
                    <span className={`insignia-${analisisActual.clasificacion.toLowerCase()}`}>
                      {analisisActual.clasificacion}
                    </span>
                  </div>
                  <div className="fila-registro">
                    <span className="etiqueta-registro">Host</span>
                    <span className="valor-registro monoespaciado truncado" title={analisisActual.nombreHost}>
                      {analisisActual.nombreHost}
                    </span>
                  </div>
                  <div className="fila-registro">
                    <span className="etiqueta-registro">Riesgo</span>
                    <span className="valor-registro">
                      {analisisActual.puntuacionRiesgo} (Entropía: {analisisActual.caracteristicas.entropiaShannon.toFixed(2)})
                    </span>
                  </div>
                  <div className="fila-registro">
                    <span className="etiqueta-registro">SHA-256</span>
                    <span className="valor-registro monoespaciado truncado" title={analisisActual.huellaSha256}>
                      {analisisActual.huellaSha256 || "-"}
                    </span>
                  </div>
                  {reglasConMitre.length > 0 && (
                    <div className="bloque-registro">
                      <span className="etiqueta-registro">Tácticas MITRE ATT&CK</span>
                      <div className="etiquetas-mitre">
                        {reglasConMitre.map((regla) => (
                          <span key={regla.id} className="etiqueta-mitre">{regla.tecnicaMitre?.id}</span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
                <div className="botones-accion">
                  <button className="boton boton-borde expandir" onClick={copiarFichaMarkdown}>
                    {copiado ? <Check size={14} /> : <Copy size={14} />} Copiar
                  </button>
                  <button className="boton boton-borde expandir" onClick={descargarFichaMarkdown}>
                    <Download size={14} /> Descargar
                  </button>
                </div>
              </div>
            )}
          </>
        ) : null}
      </main>

      <footer className="pie-aplicacion">
        <div className="acciones-pie">
          <button className="enlace-pie" onClick={abrirFormularioReporte} title="Reportar falso positivo">
            <Flag size={12} /> Reportar
          </button>
          <button className="enlace-pie" onClick={reiniciarContadores} title="Reiniciar contadores">
            <RotateCcw size={12} /> Reiniciar
          </button>
        </div>
        <span className="version-pie">PhishGuard v1.0.0</span>
      </footer>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-emergente");
if (elementoRaiz) createRoot(elementoRaiz).render(<ComponenteEmergente />);
