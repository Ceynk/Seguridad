import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  ShieldCheck,
  Settings,
  Flag,
  RotateCcw,
  ChevronDown,
  ChevronUp,
  Activity,
  Binary,
  ShieldAlert,
  Copy,
  Download,
  Check,
  Cpu,
  BrainCircuit,
} from "lucide-react";
import { ResultadoAnalisisUrl } from "../../types/analisis";
import { ConfiguracionExtension, EstadisticasSesion } from "../../types/configuracion";
import {
  CONFIGURACION_PREDETERMINADA,
  ESTADISTICAS_PREDETERMINADAS,
  ServicioAlmacenamiento,
} from "../../storage/servicio-almacenamiento";
import { exportarInformeMarkdown } from "../../utils/cripto-forense";
import { MAPA_HOMOGLIFOS } from "../../analyzer/homoglifos";
import "./popup.css";

type TabPopup = "general" | "forense" | "reporte";

export const ComponentePopup: React.FC = () => {
  const [tabActiva, setTabActiva] = useState<TabPopup>("general");
  const [analisisActual, setAnalisisActual] = useState<ResultadoAnalisisUrl | null>(null);
  const [configuracion, setConfiguracion] = useState<ConfiguracionExtension>(
    CONFIGURACION_PREDETERMINADA
  );
  const [estadisticas, setEstadisticas] = useState<EstadisticasSesion>(
    ESTADISTICAS_PREDETERMINADAS
  );
  const [cargando, setCargando] = useState<boolean>(true);
  const [mostrarReglasDetalladas, setMostrarReglasDetalladas] = useState<boolean>(false);
  const [copiado, setCopiado] = useState<boolean>(false);

  useEffect(() => {
    async function inicializarDatos(): Promise<void> {
      try {
        const configGuardada = await ServicioAlmacenamiento.obtenerConfiguracion();
        const estadisticasGuardadas = await ServicioAlmacenamiento.obtenerEstadisticas();
        setConfiguracion(configGuardada);
        setEstadisticas(estadisticasGuardadas);

        if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.query) {
          const [pestanaActiva] = await chrome.tabs.query({ active: true, currentWindow: true });

          if (pestanaActiva && pestanaActiva.url && pestanaActiva.id) {
            chrome.runtime.sendMessage(
              {
                tipo: "OBTENER_ANALISIS_PESTANA_ACTUAL",
                idPestana: pestanaActiva.id,
                url: pestanaActiva.url,
              },
              (respuesta) => {
                if (respuesta && respuesta.exito && respuesta.datos) {
                  setAnalisisActual(respuesta.datos);
                }
                setCargando(false);
              }
            );
          } else {
            setCargando(false);
          }
        } else {
          setCargando(false);
        }
      } catch {
        setCargando(false);
      }
    }

    inicializarDatos();
  }, []);

  const alternarProteccion = async (): Promise<void> => {
    const nuevoEstado = !configuracion.proteccionHabilitada;
    const nuevaConfig: ConfiguracionExtension = {
      ...configuracion,
      proteccionHabilitada: nuevoEstado,
    };
    setConfiguracion(nuevaConfig);
    await ServicioAlmacenamiento.guardarConfiguracion(nuevaConfig);

    if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.sendMessage) {
      chrome.runtime.sendMessage({
        tipo: "ACTUALIZAR_CONFIGURACION",
        configuracion: nuevaConfig,
      });
    }
  };

  const abrirPaginaOpciones = (): void => {
    if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.openOptionsPage) {
      chrome.runtime.openOptionsPage();
    } else {
      window.open("../opciones/index.html", "_blank");
    }
  };

  const abrirFormularioReporte = (): void => {
    const urlReportar = analisisActual ? encodeURIComponent(analisisActual.url) : "";
    const paginaOpciones = `../opciones/index.html?reportar=${urlReportar}`;
    if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.create) {
      chrome.tabs.create({ url: chrome.runtime.getURL(`src/pages/opciones/index.html?reportar=${urlReportar}`) });
    } else {
      window.open(paginaOpciones, "_blank");
    }
  };

  const reiniciarContadores = async (): Promise<void> => {
    await ServicioAlmacenamiento.reiniciarEstadisticas();
    setEstadisticas(ESTADISTICAS_PREDETERMINADAS);
  };

  const copiarFichaMarkdown = (): void => {
    if (!analisisActual?.fichaForense) return;
    const markdown = exportarInformeMarkdown(analisisActual.fichaForense);
    navigator.clipboard.writeText(markdown);
    setCopiado(true);
    setTimeout(() => setCopiado(false), 2000);
  };

  const descargarFichaMarkdown = (): void => {
    if (!analisisActual?.fichaForense) return;
    const markdown = exportarInformeMarkdown(analisisActual.fichaForense);
    const blob = new Blob([markdown], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = `reporte_forense_${analisisActual.nombreHost || "incidente"}.md`;
    enlace.click();
    URL.revokeObjectURL(url);
  };

  // Renderizador de caracteres con resaltado de homoglifos / alteraciones visuales
  const renderizarHostConRayosX = (host: string) => {
    return (
      <div className="visualizador-cadena-host">
        {host.split("").map((char, indice) => {
          const esHomoglifo = Boolean(MAPA_HOMOGLIFOS[char.toLowerCase()]);
          return (
            <span
              key={indice}
              className={esHomoglifo ? "caracter-alterado" : "caracter-normal"}
              title={
                esHomoglifo
                  ? `Carácter sospechoso: '${char}' simula ser '${MAPA_HOMOGLIFOS[char.toLowerCase()]}'`
                  : undefined
              }
            >
              {char}
            </span>
          );
        })}
      </div>
    );
  };

  return (
    <div className="contenedor-popup">
      {/* Cabecera */}
      <header className="cabecera">
        <div className="logo-titulo">
          <ShieldCheck className="icono-escudo" size={22} />
          <div>
            <h1 className="titulo-app">PhishGuard Colombia</h1>
            <p className="subtitulo-app">Threat Intelligence & Cyber Forensics</p>
          </div>
        </div>
        <button
          className="boton-icono"
          title="Configuración y Laboratorio"
          onClick={abrirPaginaOpciones}
          aria-label="Abrir configuración"
        >
          <Settings size={18} />
        </button>
      </header>

      {/* Barra de Pestañas */}
      <nav className="barra-pestanas">
        <button
          className={`boton-pestana ${tabActiva === "general" ? "pestana-activa" : ""}`}
          onClick={() => setTabActiva("general")}
        >
          <Activity size={12} /> General
        </button>
        <button
          className={`boton-pestana ${tabActiva === "forense" ? "pestana-activa" : ""}`}
          onClick={() => setTabActiva("forense")}
        >
          <Binary size={12} /> Rayos X
        </button>
        <button
          className={`boton-pestana ${tabActiva === "reporte" ? "pestana-activa" : ""}`}
          onClick={() => setTabActiva("reporte")}
        >
          <ShieldAlert size={12} /> Ficha CSIRT
        </button>
      </nav>

      {/* TAB 1: VISTA GENERAL */}
      {tabActiva === "general" && (
        <>
          <section className="tarjeta-estado-proteccion">
            <div className="info-proteccion">
              <span className="texto-proteccion">
                Protección {configuracion.proteccionHabilitada ? "Activa" : "Desactivada"}
              </span>
              <span className="subtexto-proteccion">
                {configuracion.proteccionHabilitada
                  ? "Inspección local y heurística en tiempo real"
                  : "La extensión no bloqueará amenazas"}
              </span>
            </div>
            <label className="interruptor" aria-label="Alternar protección">
              <input
                type="checkbox"
                checked={configuracion.proteccionHabilitada}
                onChange={alternarProteccion}
              />
              <span className="deslizador"></span>
            </label>
          </section>

          <section className="tarjeta-analisis">
            {cargando ? (
              <p style={{ textAlign: "center", color: "#94a3b8", padding: "16px" }}>
                Analizando artefacto web...
              </p>
            ) : analisisActual ? (
              <>
                <div className="fila-host">
                  <span className="host-nombre" title={analisisActual.url}>
                    {analisisActual.nombreHost || "Sitio Web"}
                  </span>
                  <span
                    className={`insignia-estado insignia-${analisisActual.clasificacion.toLowerCase()}`}
                  >
                    {analisisActual.clasificacion}
                  </span>
                </div>

                <div className="fila-puntuacion">
                  <div
                    className={`medidor-puntuacion medidor-${analisisActual.clasificacion.toLowerCase()}`}
                  >
                    {analisisActual.puntuacionRiesgo}
                  </div>
                  <div className="detalles-riesgo">
                    <span className="etiqueta-riesgo">Índice de Riesgo (0-100)</span>
                    <span className="descripcion-riesgo">
                      {analisisActual.clasificacion === "SEGURO" && "Sin indicadores de riesgo detectados."}
                      {analisisActual.clasificacion === "SOSPECHOSO" &&
                        "Se encontraron anomalías moderadas o patrones atípicos."}
                      {analisisActual.clasificacion === "MALICIOSO" &&
                        "Evidencia contundente de fraude o suplantación."}
                    </span>
                  </div>
                </div>

                {analisisActual.reglasCoincidentes.length > 0 && (
                  <div style={{ marginTop: "4px", borderTop: "1px dashed #334155", paddingTop: "8px" }}>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        cursor: "pointer",
                      }}
                      onClick={() => setMostrarReglasDetalladas(!mostrarReglasDetalladas)}
                    >
                      <span style={{ fontSize: "11px", fontWeight: 600, color: "#94a3b8" }}>
                        {analisisActual.reglasCoincidentes.length} regla(s) activada(s)
                      </span>
                      {mostrarReglasDetalladas ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                    </div>

                    {mostrarReglasDetalladas &&
                      analisisActual.reglasCoincidentes.map((regla) => (
                        <div
                          key={regla.id}
                          style={{
                            backgroundColor: "rgba(15, 23, 42, 0.5)",
                            borderRadius: "6px",
                            padding: "6px 8px",
                            fontSize: "11px",
                            marginTop: "6px",
                          }}
                        >
                          <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 600 }}>
                            <span>{regla.nombre}</span>
                            <span style={{ color: "#ef4444" }}>+{regla.puntuacion} pts</span>
                          </div>
                          <p style={{ color: "#94a3b8", fontSize: "10px", marginTop: "2px" }}>
                            {regla.descripcion}
                          </p>
                        </div>
                      ))}
                  </div>
                )}
              </>
            ) : (
              <div style={{ textAlign: "center", padding: "12px", color: "#94a3b8" }}>
                <p>Página interna del navegador o sin contenido web.</p>
              </div>
            )}
          </section>

          <section className="seccion-estadisticas">
            <div className="caja-estadistica">
              <span className="numero-estadistica">{estadisticas.sitiosAnalizados}</span>
              <span className="etiqueta-estadistica">Analizados</span>
            </div>
            <div className="caja-estadistica">
              <span className="numero-estadistica" style={{ color: "#f59e0b" }}>
                {estadisticas.sitiosSospechososDetectados}
              </span>
              <span className="etiqueta-estadistica">Sospechosos</span>
            </div>
            <div className="caja-estadistica">
              <span className="numero-estadistica" style={{ color: "#ef4444" }}>
                {estadisticas.amenazasBloqueadas}
              </span>
              <span className="etiqueta-estadistica">Bloqueados</span>
            </div>
          </section>
        </>
      )}

      {/* TAB 2: MODO FORENSE / RAYOS X */}
      {tabActiva === "forense" && (
        <section className="contenedor-forense">
          {analisisActual ? (
            <>
              {/* Rayos X Léxicos */}
              <div className="bloque-rayos-x">
                <span className="titulo-bloque-forense">
                  <Cpu size={13} /> Rayos X Léxicos & Homoglifos
                </span>
                {renderizarHostConRayosX(analisisActual.nombreHost)}
                <span style={{ fontSize: "10px", color: "#94a3b8" }}>
                  {analisisActual.caracteristicas.similitudMarcas.length > 0
                    ? `🎯 Marca objetivo detectada: ${analisisActual.caracteristicas.similitudMarcas[0].marcaObjetivo} (Similitud: ${analisisActual.caracteristicas.similitudMarcas[0].similitudPorcentaje}%)`
                    : "No se identificaron patrones de sustitución tipográfica directa."}
                </span>
              </div>

              {/* Entropía de Shannon */}
              <div className="tarjeta-entropia">
                <div>
                  <div style={{ fontSize: "11px", fontWeight: 600 }}>Entropía de Shannon (H)</div>
                  <div style={{ fontSize: "10px", color: "#94a3b8" }}>
                    Mide aleatoriedad para detectar DGAs/Botnets
                  </div>
                </div>
                <span className="valor-entropia">
                  {analisisActual.caracteristicas.entropiaShannon} bits/símbolo
                </span>
              </div>

              {/* Mapeo MITRE ATT&CK */}
              <div className="bloque-rayos-x">
                <span className="titulo-bloque-forense">
                  <ShieldAlert size={13} /> Mapeo Marco MITRE ATT&CK®
                </span>
                <div className="contenedor-mitre">
                  {analisisActual.reglasCoincidentes.filter((r) => r.tecnicaMitre).length > 0 ? (
                    analisisActual.reglasCoincidentes
                      .filter((r) => r.tecnicaMitre)
                      .map((regla) => (
                        <div key={regla.id} className="chip-mitre">
                          <div className="cabecera-chip-mitre">
                            <span className="codigo-mitre">{regla.tecnicaMitre?.id}</span>
                            <span className="tactica-mitre">{regla.tecnicaMitre?.tactica}</span>
                          </div>
                          <span style={{ fontWeight: 600, color: "#f8fafc" }}>
                            {regla.tecnicaMitre?.nombre}
                          </span>
                        </div>
                      ))
                  ) : (
                    <span style={{ fontSize: "11px", color: "#64748b" }}>
                      Sin técnicas MITRE detonadas para este artefacto.
                    </span>
                  )}
                </div>
              </div>

              {/* Ingeniería Social */}
              {analisisActual.caracteristicas.vectoresIngenieriaSocial.length > 0 && (
                <div className="bloque-rayos-x">
                  <span className="titulo-bloque-forense">
                    <BrainCircuit size={13} /> Coerción Psicológica (Cialdini)
                  </span>
                  {analisisActual.caracteristicas.vectoresIngenieriaSocial.map((vector, i) => (
                    <div key={i} style={{ fontSize: "11px", color: "#e2e8f0" }}>
                      <strong>• {vector.titulo}:</strong> {vector.descripcion}
                    </div>
                  ))}
                </div>
              )}
            </>
          ) : (
            <p style={{ color: "#94a3b8", textAlign: "center", padding: "20px" }}>
              Sin artefacto web activo para inspección forense.
            </p>
          )}
        </section>
      )}

      {/* TAB 3: FICHA FORENSE CSIRT */}
      {tabActiva === "reporte" && (
        <section className="contenedor-exportador">
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <Binary size={16} color="#38bdf8" />
            <strong style={{ fontSize: "13px" }}>Telemetría Forense del Incidente</strong>
          </div>

          <div>
            <span style={{ fontSize: "11px", color: "#94a3b8" }}>Huella Criptográfica SHA-256:</span>
            <div className="caja-hash">{analisisActual?.huellaSha256 || "N/A"}</div>
          </div>

          <div style={{ display: "flex", gap: "8px", marginTop: "4px" }}>
            <button className="boton-accion-forense" style={{ flex: 1 }} onClick={copiarFichaMarkdown}>
              {copiado ? <Check size={14} /> : <Copy size={14} />}
              {copiado ? "Copiado al Portapapeles" : "Copiar Ficha (.md)"}
            </button>
            <button
              className="boton-accion-forense"
              style={{ backgroundColor: "#1e293b", border: "1px solid #334155" }}
              onClick={descargarFichaMarkdown}
              title="Descargar Ficha Forense"
            >
              <Download size={14} />
            </button>
          </div>

          <p style={{ fontSize: "11px", color: "#64748b", lineHeight: "1.4" }}>
            Formato estándar compatible con reportes para equipos de respuesta a incidentes (CSIRT/SOC) e informes académicos de ciberseguridad.
          </p>
        </section>
      )}

      {/* Acciones Rápidas Pie */}
      <footer className="acciones-pie">
        <button
          className="boton-secundario"
          onClick={abrirFormularioReporte}
          title="Reportar sitio o falso positivo"
        >
          <Flag size={14} />
          Reportar
        </button>
        <button
          className="boton-secundario"
          onClick={reiniciarContadores}
          title="Reiniciar contadores de sesión"
        >
          <RotateCcw size={14} />
          Reiniciar
        </button>
      </footer>

      <div className="pie-privacidad">
        🔒 Análisis Forense Local • Privacidad por Diseño (Manifest V3)
      </div>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-popup");
if (elementoRaiz) {
  const raiz = createRoot(elementoRaiz);
  raiz.render(<ComponentePopup />);
}
