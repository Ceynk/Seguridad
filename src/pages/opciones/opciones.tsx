import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Shield,
  Sliders,
  ListPlus,
  Search,
  Lock,
  Plus,
  Trash2,
  CheckCircle,
} from "lucide-react";
import { ConfiguracionExtension, ModoProteccion } from "../../types/configuracion";
import {
  CONFIGURACION_PREDETERMINADA,
  ServicioAlmacenamiento,
} from "../../storage/servicio-almacenamiento";
import { MotorRiesgo } from "../../analyzer/motor-riesgo";
import { ResultadoAnalisisUrl } from "../../types/analisis";
import "./opciones.css";

const motorRiesgoSandbox = new MotorRiesgo();

export const ComponenteOpciones: React.FC = () => {
  const [configuracion, setConfiguracion] = useState<ConfiguracionExtension>(
    CONFIGURACION_PREDETERMINADA
  );
  const [nuevoDominioBlanco, setNuevoDominioBlanco] = useState<string>("");
  const [nuevoDominioNegro, setNuevoDominioNegro] = useState<string>("");
  const [guardadoExitoso, setGuardadoExitoso] = useState<boolean>(false);

  // Sandbox para pruebas de URLs
  const [urlPrueba, setUrlPrueba] = useState<string>("");
  const [resultadoPrueba, setResultadoPrueba] = useState<ResultadoAnalisisUrl | null>(null);
  const [analizandoPrueba, setAnalizandoPrueba] = useState<boolean>(false);

  useEffect(() => {
    async function cargarConfiguracion(): Promise<void> {
      const config = await ServicioAlmacenamiento.obtenerConfiguracion();
      setConfiguracion(config);
      motorRiesgoSandbox.actualizarConfiguracion(config);

      // Verificar si viene una URL para analizar desde los parámetros GET
      const parametros = new URLSearchParams(window.location.search);
      const urlReportada = parametros.get("reportar");
      if (urlReportada) {
        setUrlPrueba(decodeURIComponent(urlReportada));
      }
    }

    cargarConfiguracion();
  }, []);

  const persistirConfiguracion = async (nuevaConfig: ConfiguracionExtension): Promise<void> => {
    setConfiguracion(nuevaConfig);
    motorRiesgoSandbox.actualizarConfiguracion(nuevaConfig);
    await ServicioAlmacenamiento.guardarConfiguracion(nuevaConfig);

    if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.sendMessage) {
      chrome.runtime.sendMessage({
        tipo: "ACTUALIZAR_CONFIGURACION",
        configuracion: nuevaConfig,
      });
    }

    setGuardadoExitoso(true);
    setTimeout(() => setGuardadoExitoso(false), 2500);
  };

  const cambiarModo = (modo: ModoProteccion): void => {
    let umbralSospechoso = 30;
    let umbralMalicioso = 70;

    if (modo === "ESTRICTO") {
      umbralSospechoso = 20;
      umbralMalicioso = 55;
    } else if (modo === "PERMISIVO") {
      umbralSospechoso = 45;
      umbralMalicioso = 80;
    }

    const nuevaConfig: ConfiguracionExtension = {
      ...configuracion,
      modoProteccion: modo,
      umbralSospechoso,
      umbralMalicioso,
    };
    persistirConfiguracion(nuevaConfig);
  };

  const agregarListaBlanca = (e: React.FormEvent): void => {
    e.preventDefault();
    const dominioLimpio = nuevoDominioBlanco.trim().toLowerCase().replace(/^https?:\/\//, "");
    if (!dominioLimpio || configuracion.listaBlancaPersonalizada.includes(dominioLimpio)) return;

    const nuevaLista = [...configuracion.listaBlancaPersonalizada, dominioLimpio];
    const nuevaConfig = { ...configuracion, listaBlancaPersonalizada: nuevaLista };
    persistirConfiguracion(nuevaConfig);
    setNuevoDominioBlanco("");
  };

  const eliminarListaBlanca = (dominio: string): void => {
    const nuevaLista = configuracion.listaBlancaPersonalizada.filter((d) => d !== dominio);
    const nuevaConfig = { ...configuracion, listaBlancaPersonalizada: nuevaLista };
    persistirConfiguracion(nuevaConfig);
  };

  const agregarListaNegra = (e: React.FormEvent): void => {
    e.preventDefault();
    const dominioLimpio = nuevoDominioNegro.trim().toLowerCase().replace(/^https?:\/\//, "");
    if (!dominioLimpio || configuracion.listaNegraPersonalizada.includes(dominioLimpio)) return;

    const nuevaLista = [...configuracion.listaNegraPersonalizada, dominioLimpio];
    const nuevaConfig = { ...configuracion, listaNegraPersonalizada: nuevaLista };
    persistirConfiguracion(nuevaConfig);
    setNuevoDominioNegro("");
  };

  const eliminarListaNegra = (dominio: string): void => {
    const nuevaLista = configuracion.listaNegraPersonalizada.filter((d) => d !== dominio);
    const nuevaConfig = { ...configuracion, listaNegraPersonalizada: nuevaLista };
    persistirConfiguracion(nuevaConfig);
  };

  const ejecutarPruebaUrl = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    if (!urlPrueba.trim()) return;

    setAnalizandoPrueba(true);
    try {
      const resultado = await motorRiesgoSandbox.analizarUrl(urlPrueba.trim());
      setResultadoPrueba(resultado);
    } finally {
      setAnalizandoPrueba(false);
    }
  };

  return (
    <div className="contenedor-opciones">
      {/* Cabecera */}
      <header className="cabecera-opciones">
        <Shield className="icono-escudo-principal" size={36} />
        <div className="titulos-cabecera">
          <h1>Panel de Configuración y Seguridad</h1>
          <p>Personaliza las reglas de detección, sensibilidad y listas de confianza de PhishGuard Colombia</p>
        </div>
      </header>

      {guardadoExitoso && (
        <div className="mensaje-guardado">
          <CheckCircle size={18} />
          Configuración actualizada y aplicada correctamente.
        </div>
      )}

      {/* Sensibilidad y Modos de Protección */}
      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <Sliders size={18} /> Nivel de Sensibilidad y Protección
        </h2>

        <div className="cuadricula-modos">
          <div
            className={`tarjeta-modo ${
              configuracion.modoProteccion === "ESTRICTO" ? "modo-seleccionado" : ""
            }`}
            onClick={() => cambiarModo("ESTRICTO")}
          >
            <span className="titulo-modo">🛡️ Estricto</span>
            <p className="desc-modo">
              Máxima alerta. Umbrales bajos (Sospechoso ≥ 20, Malicioso ≥ 55). Recomendado para usuarios vulnerables o alta exposición.
            </p>
          </div>

          <div
            className={`tarjeta-modo ${
              configuracion.modoProteccion === "EQUILIBRADO" ? "modo-seleccionado" : ""
            }`}
            onClick={() => cambiarModo("EQUILIBRADO")}
          >
            <span className="titulo-modo">⚖️ Equilibrado (Recomendado)</span>
            <p className="desc-modo">
              Balance óptimo entre detección y prevención de falsos positivos (Sospechoso ≥ 30, Malicioso ≥ 70).
            </p>
          </div>

          <div
            className={`tarjeta-modo ${
              configuracion.modoProteccion === "PERMISIVO" ? "modo-seleccionado" : ""
            }`}
            onClick={() => cambiarModo("PERMISIVO")}
          >
            <span className="titulo-modo">🔓 Permisivo</span>
            <p className="desc-modo">
              Solo bloquea amenazas con evidencia contundente y acumulativa (Sospechoso ≥ 45, Malicioso ≥ 80).
            </p>
          </div>
        </div>

        <div className="fila-opcion">
          <div className="info-opcion">
            <span className="nombre-opcion">Bloqueo automático de sitios maliciosos</span>
            <span className="desc-opcion">
              Impide la carga de páginas clasificadas como MALICIOSAS redirigiendo a la pantalla de advertencia segura.
            </span>
          </div>
          <input
            type="checkbox"
            checked={configuracion.bloquearMaliciososAutomaticamente}
            onChange={(e) =>
              persistirConfiguracion({
                ...configuracion,
                bloquearMaliciososAutomaticamente: e.target.checked,
              })
            }
          />
        </div>
      </section>

      {/* Listas Personalizadas */}
      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <ListPlus size={18} /> Listas de Excepción Personalizadas
        </h2>

        {/* Lista Blanca */}
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          <span style={{ fontSize: "13px", fontWeight: 600, color: "#10b981" }}>
            Lista Blanca de Confianza (Siempre permitidos)
          </span>
          <form onSubmit={agregarListaBlanca} className="grupo-formulario-lista">
            <input
              type="text"
              className="input-texto"
              placeholder="ejemplo: portal-corporativo-interno.com"
              value={nuevoDominioBlanco}
              onChange={(e) => setNuevoDominioBlanco(e.target.value)}
            />
            <button type="submit" className="boton-agregar">
              <Plus size={14} /> Agregar
            </button>
          </form>
          <div className="contenedor-chips">
            {configuracion.listaBlancaPersonalizada.length === 0 ? (
              <span style={{ fontSize: "12px", color: "#64748b" }}>
                No has agregado dominios a tu lista blanca personal.
              </span>
            ) : (
              configuracion.listaBlancaPersonalizada.map((dom) => (
                <span key={dom} className="chip-dominio">
                  {dom}
                  <button
                    className="boton-eliminar-chip"
                    onClick={() => eliminarListaBlanca(dom)}
                    title="Eliminar de lista blanca"
                  >
                    <Trash2 size={12} />
                  </button>
                </span>
              ))
            )}
          </div>
        </div>

        {/* Lista Negra */}
        <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "12px" }}>
          <span style={{ fontSize: "13px", fontWeight: 600, color: "#ef4444" }}>
            Lista Negra de Bloqueo (Siempre bloqueados)
          </span>
          <form onSubmit={agregarListaNegra} className="grupo-formulario-lista">
            <input
              type="text"
              className="input-texto"
              placeholder="ejemplo: dominio-sospechoso-frecuente.xyz"
              value={nuevoDominioNegro}
              onChange={(e) => setNuevoDominioNegro(e.target.value)}
            />
            <button type="submit" className="boton-agregar" style={{ backgroundColor: "#ef4444" }}>
              <Plus size={14} /> Bloquear
            </button>
          </form>
          <div className="contenedor-chips">
            {configuracion.listaNegraPersonalizada.length === 0 ? (
              <span style={{ fontSize: "12px", color: "#64748b" }}>
                No has agregado dominios a tu lista negra personal.
              </span>
            ) : (
              configuracion.listaNegraPersonalizada.map((dom) => (
                <span key={dom} className="chip-dominio">
                  {dom}
                  <button
                    className="boton-eliminar-chip"
                    onClick={() => eliminarListaNegra(dom)}
                    title="Eliminar de lista negra"
                  >
                    <Trash2 size={12} />
                  </button>
                </span>
              ))
            )}
          </div>
        </div>
      </section>

      {/* Probador / Sandbox de Análisis de URLs */}
      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <Search size={18} /> Probador Heurístico de URLs (Sandbox de Análisis)
        </h2>
        <p style={{ fontSize: "12px", color: "#94a3b8" }}>
          Inserta cualquier URL para inspeccionar en tiempo real cómo el motor extrae características, evalúa homoglifos y asigna la puntuación de riesgo.
        </p>

        <form onSubmit={ejecutarPruebaUrl} className="caja-sandbox">
          <div className="grupo-formulario-lista">
            <input
              type="text"
              className="input-texto"
              placeholder="Ej: http://banc0l0mbia-login-verificacion.xyz/portal/actualizar-datos"
              value={urlPrueba}
              onChange={(e) => setUrlPrueba(e.target.value)}
              required
            />
            <button type="submit" className="boton-agregar" disabled={analizandoPrueba}>
              {analizandoPrueba ? "Analizando..." : "Analizar URL"}
            </button>
          </div>

          {resultadoPrueba && (
            <div className="resultado-sandbox">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontWeight: 600 }}>Host: {resultadoPrueba.nombreHost}</span>
                <span
                  style={{
                    padding: "3px 10px",
                    borderRadius: "12px",
                    fontSize: "11px",
                    fontWeight: 700,
                    backgroundColor:
                      resultadoPrueba.clasificacion === "MALICIOSO"
                        ? "rgba(239,68,68,0.2)"
                        : resultadoPrueba.clasificacion === "SOSPECHOSO"
                        ? "rgba(245,158,11,0.2)"
                        : "rgba(16,185,129,0.2)",
                    color:
                      resultadoPrueba.clasificacion === "MALICIOSO"
                        ? "#ef4444"
                        : resultadoPrueba.clasificacion === "SOSPECHOSO"
                        ? "#f59e0b"
                        : "#10b981",
                  }}
                >
                  {resultadoPrueba.clasificacion} ({resultadoPrueba.puntuacionRiesgo} / 100)
                </span>
              </div>

              <div style={{ fontSize: "11px", color: "#94a3b8" }}>
                Tiempo de procesamiento local: {resultadoPrueba.tiempoProcesamientoMs} ms • Confianza: {resultadoPrueba.nivelConfianza}
              </div>

              {resultadoPrueba.reglasCoincidentes.length > 0 ? (
                <div style={{ display: "flex", flexDirection: "column", gap: "6px", marginTop: "8px" }}>
                  <strong style={{ fontSize: "12px", color: "#e2e8f0" }}>Reglas Disparadas:</strong>
                  {resultadoPrueba.reglasCoincidentes.map((regla) => (
                    <div
                      key={regla.id}
                      style={{
                        backgroundColor: "rgba(15, 23, 42, 0.6)",
                        padding: "8px",
                        borderRadius: "6px",
                        fontSize: "11px",
                      }}
                    >
                      <div style={{ fontWeight: 600, color: "#f87171" }}>
                        {regla.nombre} (+{regla.puntuacion} pts)
                      </div>
                      <div style={{ color: "#94a3b8" }}>{regla.descripcion}</div>
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{ color: "#34d399", fontSize: "12px", marginTop: "4px" }}>
                  ✓ No se identificaron anomalías sospechosas ni indicios de phishing.
                </div>
              )}
            </div>
          )}
        </form>
      </section>

      {/* Privacidad y Seguridad */}
      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <Lock size={18} /> Declaración de Privacidad y Arquitectura Local
        </h2>
        <p style={{ fontSize: "12px", color: "#94a3b8", lineHeight: "1.6" }}>
          PhishGuard Colombia está diseñado bajo la filosofía de <strong>Privacidad por Diseño (Privacy by Design)</strong>.
          <br />
          • El análisis léxico, de homoglifos y reputación se ejecuta <strong>100% de manera local</strong> en tu dispositivo mediante el Service Worker de la extensión.
          <br />
          • <strong>NO</strong> se recopila ni almacena tu historial de navegación.
          <br />
          • <strong>NO</strong> se leen formularios ni contraseñas introducidas en páginas web.
          <br />
          • Los datos de sesión (conteo de sitios analizados y último host inspeccionado) residen únicamente en el almacenamiento volátil y seguro <code>chrome.storage.local</code>.
        </p>
      </section>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-opciones");
if (elementoRaiz) {
  const raiz = createRoot(elementoRaiz);
  raiz.render(<ComponenteOpciones />);
}
