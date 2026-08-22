import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  ShieldCheck,
  Settings,
  Flag,
  RotateCcw,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { ResultadoAnalisisUrl } from "../../types/analisis";
import { ConfiguracionExtension, EstadisticasSesion } from "../../types/configuracion";
import {
  CONFIGURACION_PREDETERMINADA,
  ESTADISTICAS_PREDETERMINADAS,
  ServicioAlmacenamiento,
} from "../../storage/servicio-almacenamiento";
import "./popup.css";

export const ComponentePopup: React.FC = () => {
  const [analisisActual, setAnalisisActual] = useState<ResultadoAnalisisUrl | null>(null);
  const [configuracion, setConfiguracion] = useState<ConfiguracionExtension>(
    CONFIGURACION_PREDETERMINADA
  );
  const [estadisticas, setEstadisticas] = useState<EstadisticasSesion>(
    ESTADISTICAS_PREDETERMINADAS
  );
  const [cargando, setCargando] = useState<boolean>(true);
  const [mostrarReglasDetalladas, setMostrarReglasDetalladas] = useState<boolean>(false);

  // Cargar estado inicial y analizar pestaña activa
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
          // Entorno de prueba / simulación fuera de extensión
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

  return (
    <div className="contenedor-popup">
      {/* Cabecera */}
      <header className="cabecera">
        <div className="logo-titulo">
          <ShieldCheck className="icono-escudo" size={22} />
          <div>
            <h1 className="titulo-app">PhishGuard Colombia</h1>
            <p className="subtitulo-app">Detección de Fraude & Phishing</p>
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

      {/* Interruptor de Protección */}
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
        <label className="interruptor" aria-label="Alternar protección en tiempo real">
          <input
            type="checkbox"
            checked={configuracion.proteccionHabilitada}
            onChange={alternarProteccion}
          />
          <span className="deslizador"></span>
        </label>
      </section>

      {/* Tarjeta de Sitio Actual */}
      <section className="tarjeta-analisis">
        {cargando ? (
          <p style={{ textAlign: "center", color: "#94a3b8", padding: "16px" }}>
            Analizando sitio web...
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
                    "Se encontraron patrones inusuales o posibles alertas."}
                  {analisisActual.clasificacion === "MALICIOSO" &&
                    "Evidencia clara de suplantación, fraude o amenaza."}
                </span>
              </div>
            </div>

            {analisisActual.reglasCoincidentes.length > 0 && (
              <div className="seccion-reglas">
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
                    <div key={regla.id} className="item-regla">
                      <div className="titulo-item-regla">
                        <span>{regla.nombre}</span>
                        <span style={{ color: "#ef4444" }}>+{regla.puntuacion} pts</span>
                      </div>
                      <p className="desc-item-regla">{regla.descripcion}</p>
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

      {/* Métricas de la Sesión */}
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

      {/* Acciones Rápidas */}
      <footer className="acciones-pie">
        <button
          className="boton-secundario"
          onClick={abrirFormularioReporte}
          title="Reportar sitio o falso positivo"
        >
          <Flag size={14} />
          Reportar Sitio
        </button>
        <button
          className="boton-secundario"
          onClick={reiniciarContadores}
          title="Reiniciar contadores de sesión"
        >
          <RotateCcw size={14} />
          Reiniciar Métricas
        </button>
      </footer>

      <div className="pie-privacidad">
        🔒 Análisis local privado • Sin almacenamiento de historial
      </div>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-popup");
if (elementoRaiz) {
  const raiz = createRoot(elementoRaiz);
  raiz.render(<ComponentePopup />);
}
