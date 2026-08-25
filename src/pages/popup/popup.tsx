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
  Search,
  Zap,
} from "lucide-react";
import { ResultadoAnalisisUrl } from "../../types/analisis";
import { ConfiguracionExtension, EstadisticasSesion } from "../../types/configuracion";
import {
  CONFIGURACION_PREDETERMINADA,
  ESTADISTICAS_PREDETERMINADAS,
  ServicioAlmacenamiento,
} from "../../storage/servicio-almacenamiento";
import { MotorRiesgo } from "../../analyzer/motor-riesgo";
import { exportarInformeMarkdown } from "../../utils/cripto-forense";
import { MAPA_HOMOGLIFOS } from "../../analyzer/homoglifos";
import "./popup.css";

const motorLocal = new MotorRiesgo();

type TabPopup = "general" | "forense" | "reporte";

const CASOS_DEMO = [
  {
    nombre: "Banc0l0mbia Phishing",
    url: "http://banc0l0mbia-login-verificacion.xyz/portal/actualizar-datos",
  },
  {
    nombre: "Nequ1 Alerta",
    url: "http://nequ1-recuperacion-cuenta.top/seguridad",
  },
  {
    nombre: "DIAN Fotomulta",
    url: "http://d1an-multas-facturacion.click/fotomulta/inmediato",
  },
  {
    nombre: "Google Legítimo",
    url: "https://www.google.com/search?q=ciberseguridad",
  },
];

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
  const [urlBusquedaManual, setUrlBusquedaManual] = useState<string>("");
  const [esPaginaInterna, setEsPaginaInterna] = useState<boolean>(false);

  const analizarUrlEnPopup = async (urlAAnalizar: string): Promise<void> => {
    setCargando(true);
    try {
      const resultado = await motorLocal.analizarUrl(urlAAnalizar);
      setAnalisisActual(resultado);
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    async function inicializarDatos(): Promise<void> {
      try {
        const configGuardada = await ServicioAlmacenamiento.obtenerConfiguracion();
        const estadisticasGuardadas = await ServicioAlmacenamiento.obtenerEstadisticas();
        setConfiguracion(configGuardada);
        setEstadisticas(estadisticasGuardadas);
        motorLocal.actualizarConfiguracion(configGuardada);

        let urlEncontrada = "";

        if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.query) {
          const [pestanaActiva] = await chrome.tabs.query({ active: true, currentWindow: true });

          if (pestanaActiva && pestanaActiva.url) {
            const url = pestanaActiva.url;
            if (
              url.startsWith("chrome://") ||
              url.startsWith("chrome-extension://") ||
              url.startsWith("about:") ||
              url.startsWith("edge://")
            ) {
              setEsPaginaInterna(true);
              urlEncontrada = CASOS_DEMO[0].url;
            } else {
              urlEncontrada = url;
            }
          } else {
            urlEncontrada = CASOS_DEMO[0].url;
          }
        } else {
          urlEncontrada = CASOS_DEMO[0].url;
        }

        const resultado = await motorLocal.analizarUrl(urlEncontrada);
        setAnalisisActual(resultado);
        setCargando(false);
      } catch {
        const resFallback = await motorLocal.analizarUrl(CASOS_DEMO[0].url);
        setAnalisisActual(resFallback);
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
    motorLocal.actualizarConfiguracion(nuevaConfig);
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
    enlace.download = `reporte_${analisisActual.nombreHost || "incidente"}.md`;
    enlace.click();
    URL.revokeObjectURL(url);
  };

  const manejarBusquedaManual = (e: React.FormEvent) => {
    e.preventDefault();
    if (urlBusquedaManual.trim()) {
      analizarUrlEnPopup(urlBusquedaManual.trim());
    }
  };

  const renderizarHostConRayosX = (host: string) => {
    const caracteresSospechosos: { char: string; legitimo: string }[] = [];

    const elementosChar = host.split("").map((char, indice) => {
      const esHomoglifo = Boolean(MAPA_HOMOGLIFOS[char.toLowerCase()]);
      if (esHomoglifo) {
        caracteresSospechosos.push({
          char,
          legitimo: MAPA_HOMOGLIFOS[char.toLowerCase()],
        });
      }
      return (
        <span
          key={indice}
          className={esHomoglifo ? "caracter-alterado" : "caracter-normal"}
        >
          {char}
        </span>
      );
    });

    return (
      <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
        <div className="visualizador-cadena-host">{elementosChar}</div>
        {caracteresSospechosos.length > 0 && (
          <div className="leyenda-homoglifo">
            <strong>Sustituciones detectadas:</strong>{" "}
            {caracteresSospechosos
              .map((s) => `'${s.char}' imita a '${s.legitimo}'`)
              .join(", ")}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="contenedor-popup">
      <header className="cabecera">
        <div className="logo-titulo">
          <ShieldCheck className="icono-escudo" size={22} />
          <div>
            <h1 className="titulo-app">PhishGuard Colombia</h1>
            <p className="subtitulo-app">Detección de Phishing</p>
          </div>
        </div>
        <button
          className="boton-icono"
          title="Configuración"
          onClick={abrirPaginaOpciones}
          aria-label="Abrir configuración"
        >
          <Settings size={18} />
        </button>
      </header>

      <nav className="barra-pestanas">
        <button
          className={`boton-pestana ${tabActiva === "general" ? "pestana-activa" : ""}`}
          onClick={() => setTabActiva("general")}
        >
          <Activity size={13} /> General
        </button>
        <button
          className={`boton-pestana ${tabActiva === "forense" ? "pestana-activa" : ""}`}
          onClick={() => setTabActiva("forense")}
        >
          <Binary size={13} /> Rayos X
        </button>
        <button
          className={`boton-pestana ${tabActiva === "reporte" ? "pestana-activa" : ""}`}
          onClick={() => setTabActiva("reporte")}
        >
          <ShieldAlert size={13} /> Ficha CSIRT
        </button>
      </nav>

      {esPaginaInterna && (
        <div className="banner-pruebas-rapidas">
          <span style={{ fontSize: "10px", color: "#93c5fd", fontWeight: 600 }}>
            Pestaña interna detectada. Ejemplos de prueba:
          </span>
          <div className="fila-botones-demos">
            {CASOS_DEMO.map((caso, i) => (
              <button
                key={i}
                className="boton-demo-chip"
                onClick={() => analizarUrlEnPopup(caso.url)}
              >
                <Zap size={11} color="#f59e0b" />
                <span>{caso.nombre}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      <form onSubmit={manejarBusquedaManual} style={{ display: "flex", gap: "6px" }}>
        <input
          type="text"
          className="input-rapido-url"
          placeholder="Inspeccionar URL..."
          value={urlBusquedaManual}
          onChange={(e) => setUrlBusquedaManual(e.target.value)}
        />
        <button
          type="submit"
          className="boton-icono"
          style={{ backgroundColor: "#1e293b", padding: "6px 10px" }}
          title="Analizar URL"
        >
          <Search size={14} />
        </button>
      </form>

      {tabActiva === "general" && (
        <>
          <section className="tarjeta-estado-proteccion">
            <div className="info-proteccion">
              <span className="texto-proteccion">
                Protección {configuracion.proteccionHabilitada ? "Activa" : "Desactivada"}
              </span>
              <span className="subtexto-proteccion">
                {configuracion.proteccionHabilitada
                  ? "Inspección local en tiempo real"
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
                Analizando...
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
            ) : null}
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

      {tabActiva === "forense" && (
        <section className="contenedor-forense">
          {analisisActual && (
            <>
              <div className="bloque-rayos-x">
                <span className="titulo-bloque-forense">
                  <Cpu size={14} /> Rayos X Léxicos y Homoglifos
                </span>
                {renderizarHostConRayosX(analisisActual.nombreHost)}
                <span style={{ fontSize: "11px", color: "#94a3b8" }}>
                  {analisisActual.caracteristicas.similitudMarcas.length > 0
                    ? `Marca identificada: ${analisisActual.caracteristicas.similitudMarcas[0].marcaObjetivo.toUpperCase()} (Similitud: ${analisisActual.caracteristicas.similitudMarcas[0].similitudPorcentaje}%)`
                    : "No se identificaron patrones de suplantación de marca conocida."}
                </span>
              </div>

              <div className="tarjeta-entropia">
                <div className="fila-entropia-valores">
                  <div>
                    <span style={{ fontSize: "11px", fontWeight: 700, color: "#c084fc" }}>
                      ENTROPÍA DE SHANNON (H)
                    </span>
                    <p style={{ fontSize: "10px", color: "#94a3b8" }}>
                      Aleatoriedad del dominio
                    </p>
                  </div>
                  <span className="valor-entropia">
                    {analisisActual.caracteristicas.entropiaShannon} bits
                  </span>
                </div>
                <div className="barra-entropia-fondo">
                  <div
                    className="barra-entropia-progreso"
                    style={{
                      width: `${Math.min(100, (analisisActual.caracteristicas.entropiaShannon / 5) * 100)}%`,
                      backgroundColor:
                        analisisActual.caracteristicas.entropiaShannon > 3.8 ? "#ef4444" : "#10b981",
                    }}
                  ></div>
                </div>
              </div>

              <div className="bloque-rayos-x">
                <span className="titulo-bloque-forense">
                  <ShieldAlert size={14} /> Marco MITRE ATT&CK
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
                      Sin técnicas MITRE asociadas a este artefacto.
                    </span>
                  )}
                </div>
              </div>

              {analisisActual.caracteristicas.vectoresIngenieriaSocial.length > 0 && (
                <div className="bloque-rayos-x">
                  <span className="titulo-bloque-forense">
                    <BrainCircuit size={14} /> Patrones de Ingeniería Social
                  </span>
                  {analisisActual.caracteristicas.vectoresIngenieriaSocial.map((vector, i) => (
                    <div key={i} style={{ fontSize: "11px", color: "#e2e8f0" }}>
                      • <strong style={{ color: "#fca5a5" }}>{vector.titulo}:</strong> {vector.descripcion}
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </section>
      )}

      {tabActiva === "reporte" && (
        <section className="contenedor-exportador">
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <Binary size={16} color="#38bdf8" />
            <strong style={{ fontSize: "13px" }}>Telemetría CSIRT / SOC</strong>
          </div>

          <div>
            <span style={{ fontSize: "11px", color: "#94a3b8" }}>Huella SHA-256:</span>
            <div className="caja-hash">{analisisActual?.huellaSha256 || "Generando..."}</div>
          </div>

          {analisisActual?.fichaForense && (
            <div className="caja-resumen-csirt">
              {exportarInformeMarkdown(analisisActual.fichaForense)}
            </div>
          )}

          <div style={{ display: "flex", gap: "8px", marginTop: "4px" }}>
            <button className="boton-accion-forense" style={{ flex: 1 }} onClick={copiarFichaMarkdown}>
              {copiado ? <Check size={14} /> : <Copy size={14} />}
              {copiado ? "Copiado" : "Copiar Ficha (.md)"}
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
        </section>
      )}

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
        Análisis local • Manifest V3
      </div>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-popup");
if (elementoRaiz) {
  const raiz = createRoot(elementoRaiz);
  raiz.render(<ComponentePopup />);
}
