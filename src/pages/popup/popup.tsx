import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  ShieldCheck,
  Settings,
  Flag,
  RotateCcw,
  Search,
  Activity,
  FileCode,
  ShieldAlert,
  Copy,
  Download,
  Check,
  Globe,
  AlertTriangle,
  ChevronRight,
  ChevronDown
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

type TabPopup = "estado" | "analisis" | "csirt";

const CASOS_DEMO = [
  { nombre: "Banc0l0mbia", url: "http://banc0l0mbia-login-verificacion.xyz/portal/actualizar" },
  { nombre: "Nequ1", url: "http://nequ1-recuperacion-cuenta.top/seguridad" },
  { nombre: "Google", url: "https://www.google.com/search?q=ciberseguridad" },
];

export const ComponentePopup: React.FC = () => {
  const [tabActiva, setTabActiva] = useState<TabPopup>("estado");
  const [analisisActual, setAnalisisActual] = useState<ResultadoAnalisisUrl | null>(null);
  const [configuracion, setConfiguracion] = useState<ConfiguracionExtension>(CONFIGURACION_PREDETERMINADA);
  const [estadisticas, setEstadisticas] = useState<EstadisticasSesion>(ESTADISTICAS_PREDETERMINADAS);
  const [cargando, setCargando] = useState<boolean>(true);
  const [copiado, setCopiado] = useState<boolean>(false);
  const [urlBusquedaManual, setUrlBusquedaManual] = useState<string>("");
  const [esPaginaInterna, setEsPaginaInterna] = useState<boolean>(false);
  const [mostrarReglas, setMostrarReglas] = useState<boolean>(true);

  const analizarUrlEnPopup = async (urlAAnalizar: string): Promise<void> => {
    setCargando(true);
    try {
      const resultado = await motorLocal.analizarUrl(urlAAnalizar);
      setAnalisisActual(resultado);
      setTabActiva("estado");
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    async function inicializarDatos() {
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
            if (url.startsWith("chrome://") || url.startsWith("chrome-extension://") || url.startsWith("about:") || url.startsWith("edge://")) {
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

  const alternarProteccion = async () => {
    const nuevaConfig = { ...configuracion, proteccionHabilitada: !configuracion.proteccionHabilitada };
    setConfiguracion(nuevaConfig);
    motorLocal.actualizarConfiguracion(nuevaConfig);
    await ServicioAlmacenamiento.guardarConfiguracion(nuevaConfig);
    if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.sendMessage) {
      chrome.runtime.sendMessage({ tipo: "ACTUALIZAR_CONFIGURACION", configuracion: nuevaConfig });
    }
  };

  const abrirPaginaOpciones = () => {
    if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.openOptionsPage) {
      chrome.runtime.openOptionsPage();
    } else {
      window.open("../opciones/index.html", "_blank");
    }
  };

  const abrirFormularioReporte = () => {
    const urlReportar = analisisActual ? encodeURIComponent(analisisActual.url) : "";
    if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.create) {
      chrome.tabs.create({ url: chrome.runtime.getURL(`src/pages/opciones/index.html?reportar=${urlReportar}`) });
    } else {
      window.open(`../opciones/index.html?reportar=${urlReportar}`, "_blank");
    }
  };

  const copiarFichaMarkdown = () => {
    if (!analisisActual?.fichaForense) return;
    navigator.clipboard.writeText(exportarInformeMarkdown(analisisActual.fichaForense));
    setCopiado(true);
    setTimeout(() => setCopiado(false), 2000);
  };

  const reiniciarContadores = async (): Promise<void> => {
    await ServicioAlmacenamiento.reiniciarEstadisticas();
    setEstadisticas(ESTADISTICAS_PREDETERMINADAS);
  };

  const descargarFichaMarkdown = () => {
    if (!analisisActual?.fichaForense) return;
    const blob = new Blob([exportarInformeMarkdown(analisisActual.fichaForense)], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = `csirt_${analisisActual.nombreHost}.md`;
    enlace.click();
    URL.revokeObjectURL(url);
  };

  const manejarBusquedaManual = (e: React.FormEvent) => {
    e.preventDefault();
    if (urlBusquedaManual.trim()) analizarUrlEnPopup(urlBusquedaManual.trim());
  };

  const obtenerColorEstado = (clasificacion: string) => {
    if (clasificacion === "SEGURO") return "estado-seguro";
    if (clasificacion === "SOSPECHOSO") return "estado-sospechoso";
    return "estado-peligroso";
  };

  const obtenerIconoEstado = (clasificacion: string) => {
    if (clasificacion === "SEGURO") return <ShieldCheck className="icono-estado" size={32} />;
    if (clasificacion === "SOSPECHOSO") return <AlertTriangle className="icono-estado" size={32} />;
    return <ShieldAlert className="icono-estado" size={32} />;
  };

  const renderizarHostLimpio = (host: string) => {
    return host.split("").map((char, indice) => {
      const esHomoglifo = Boolean(MAPA_HOMOGLIFOS[char.toLowerCase()]);
      return (
        <span key={indice} className={esHomoglifo ? "char-alerta" : ""}>
          {char}
        </span>
      );
    });
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="brand">
          <ShieldCheck size={18} className="brand-icon" />
          <span className="brand-name">PhishGuard</span>
        </div>
        <div className="header-actions">
          <label className="toggle-switch" title="Protección en tiempo real">
            <input type="checkbox" checked={configuracion.proteccionHabilitada} onChange={alternarProteccion} />
            <span className="toggle-slider"></span>
          </label>
          <button className="icon-btn" onClick={abrirPaginaOpciones} title="Configuración">
            <Settings size={16} />
          </button>
        </div>
      </header>

      <nav className="nav-tabs">
        <button className={`tab-btn ${tabActiva === "estado" ? "active" : ""}`} onClick={() => setTabActiva("estado")}>Estado</button>
        <button className={`tab-btn ${tabActiva === "analisis" ? "active" : ""}`} onClick={() => setTabActiva("analisis")}>Análisis URL</button>
        <button className={`tab-btn ${tabActiva === "csirt" ? "active" : ""}`} onClick={() => setTabActiva("csirt")}>CSIRT</button>
      </nav>

      <main className="app-content">
        {cargando ? (
          <div className="loading-state">
            <Activity className="spinner" size={24} />
            <span>Analizando enlace...</span>
          </div>
        ) : analisisActual ? (
          <>
            {tabActiva === "estado" && (
              <div className="tab-panel">
                <div className={`status-card ${obtenerColorEstado(analisisActual.clasificacion)}`}>
                  <div className="status-header">
                    {obtenerIconoEstado(analisisActual.clasificacion)}
                    <div className="status-title-group">
                      <h2 className="status-title">{analisisActual.clasificacion}</h2>
                      <span className="status-subtitle">
                        {analisisActual.clasificacion === "SEGURO" && "No se detectaron amenazas"}
                        {analisisActual.clasificacion === "SOSPECHOSO" && "Precaución recomendada"}
                        {analisisActual.clasificacion === "MALICIOSO" && "Sitio fraudulento detectado"}
                      </span>
                    </div>
                  </div>
                  
                  <div className="risk-bar-container">
                    <div className="risk-bar-labels">
                      <span>Riesgo</span>
                      <span>{analisisActual.puntuacionRiesgo}/100</span>
                    </div>
                    <div className="risk-bar-track">
                      <div 
                        className="risk-bar-fill" 
                        style={{ width: `${Math.min(100, analisisActual.puntuacionRiesgo)}%` }}
                      ></div>
                    </div>
                  </div>
                </div>

                <div className="domain-card">
                  <span className="domain-label">Dominio actual</span>
                  <div className="domain-value" title={analisisActual.url}>
                    <Globe size={14} className="text-muted" />
                    <span className="truncate">{renderizarHostLimpio(analisisActual.nombreHost)}</span>
                  </div>
                </div>

                {analisisActual.reglasCoincidentes.length > 0 && (
                  <div className="indicators-section">
                    <div className="section-header" onClick={() => setMostrarReglas(!mostrarReglas)}>
                      <h3>Indicadores detectados ({analisisActual.reglasCoincidentes.length})</h3>
                      {mostrarReglas ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </div>
                    
                    {mostrarReglas && (
                      <div className="indicators-list">
                        {analisisActual.reglasCoincidentes.map(regla => (
                          <div key={regla.id} className="indicator-item">
                            <div className="indicator-main">
                              <span className="indicator-name">{regla.nombre}</span>
                              <span className="indicator-score">+{regla.puntuacion}</span>
                            </div>
                            <p className="indicator-desc">{regla.descripcion}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {tabActiva === "analisis" && (
              <div className="tab-panel">
                <p className="section-desc">Analiza un enlace independientemente de la página actual para verificar su seguridad antes de acceder.</p>
                
                <form onSubmit={manejarBusquedaManual} className="manual-search-form">
                  <div className="search-input-wrapper">
                    <Search size={14} className="search-icon" />
                    <input
                      type="url"
                      className="search-input"
                      placeholder="https://ejemplo.com/login"
                      value={urlBusquedaManual}
                      onChange={(e) => setUrlBusquedaManual(e.target.value)}
                      required
                    />
                  </div>
                  <button type="submit" className="btn btn-primary">Analizar</button>
                </form>

                {esPaginaInterna && (
                  <div className="demo-section">
                    <span className="demo-label">Enlaces de prueba:</span>
                    <div className="demo-chips">
                      {CASOS_DEMO.map((caso, i) => (
                        <button key={i} className="demo-chip" onClick={() => analizarUrlEnPopup(caso.url)}>
                          {caso.nombre}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                <div className="stats-grid">
                  <div className="stat-box">
                    <span className="stat-value">{estadisticas.sitiosAnalizados}</span>
                    <span className="stat-label">Analizados</span>
                  </div>
                  <div className="stat-box">
                    <span className="stat-value text-warning">{estadisticas.sitiosSospechososDetectados}</span>
                    <span className="stat-label">Sospechosos</span>
                  </div>
                  <div className="stat-box">
                    <span className="stat-value text-danger">{estadisticas.amenazasBloqueadas}</span>
                    <span className="stat-label">Bloqueados</span>
                  </div>
                </div>
              </div>
            )}

            {tabActiva === "csirt" && (
              <div className="tab-panel">
                <div className="csirt-header">
                  <FileCode size={16} />
                  <h3>Registro Técnico</h3>
                </div>

                <div className="csirt-record">
                  <div className="record-row">
                    <span className="record-label">Estado</span>
                    <span className={`record-badge badge-${analisisActual.clasificacion.toLowerCase()}`}>
                      {analisisActual.clasificacion}
                    </span>
                  </div>
                  <div className="record-row">
                    <span className="record-label">Host</span>
                    <span className="record-value mono truncate" title={analisisActual.nombreHost}>{analisisActual.nombreHost}</span>
                  </div>
                  <div className="record-row">
                    <span className="record-label">Riesgo</span>
                    <span className="record-value">{analisisActual.puntuacionRiesgo} (Entropía: {analisisActual.caracteristicas.entropiaShannon.toFixed(2)})</span>
                  </div>
                  <div className="record-row">
                    <span className="record-label">SHA-256</span>
                    <span className="record-value mono truncate" title={analisisActual.huellaSha256}>{analisisActual.huellaSha256 || "-"}</span>
                  </div>
                  
                  {analisisActual.reglasCoincidentes.some(r => r.tecnicaMitre) && (
                    <div className="record-block">
                      <span className="record-label block">Tácticas MITRE ATT&CK</span>
                      <div className="mitre-tags">
                        {analisisActual.reglasCoincidentes.filter(r => r.tecnicaMitre).map((r, idx) => (
                          <span key={idx} className="mitre-tag">{r.tecnicaMitre?.id}</span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                <div className="action-buttons mt-auto">
                  <button className="btn btn-outline flex-1" onClick={copiarFichaMarkdown}>
                    {copiado ? <Check size={14} /> : <Copy size={14} />}
                    Copiar
                  </button>
                  <button className="btn btn-outline flex-1" onClick={descargarFichaMarkdown}>
                    <Download size={14} />
                    Descargar
                  </button>
                </div>
              </div>
            )}
          </>
        ) : null}
      </main>

      <footer className="app-footer">
        <div className="footer-actions">
          <button className="footer-link" onClick={abrirFormularioReporte} title="Reportar falso positivo">
            <Flag size={12} /> Reportar
          </button>
          <button className="footer-link" onClick={reiniciarContadores} title="Reiniciar contadores">
            <RotateCcw size={12} /> Reiniciar
          </button>
        </div>
        <span className="footer-version">PhishGuard v1.0.0</span>
      </footer>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-popup");
if (elementoRaiz) {
  const raiz = createRoot(elementoRaiz);
  raiz.render(<ComponentePopup />);
}
