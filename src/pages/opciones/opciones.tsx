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
  FlaskConical,
  Zap,
  ShieldAlert,
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

  // Sandbox para pruebas de URLs manuales
  const [urlPrueba, setUrlPrueba] = useState<string>("");
  const [resultadoPrueba, setResultadoPrueba] = useState<ResultadoAnalisisUrl | null>(null);
  const [analizandoPrueba, setAnalizandoPrueba] = useState<boolean>(false);

  // Estados del Laboratorio de Ataques Éticos (Cyber Attack Lab)
  const [marcaVictima, setMarcaVictima] = useState<string>("bancolombia");
  const [tecnicaEvasion, setTecnicaEvasion] = useState<string>("homoglifo");
  const [tldMalicioso, setTldMalicioso] = useState<string>("xyz");
  const [vectorCoercion, setVectorCoercion] = useState<string>("urgencia");
  const [urlGeneradaLab, setUrlGeneradaLab] = useState<string>("");
  const [resultadoLab, setResultadoLab] = useState<ResultadoAnalisisUrl | null>(null);

  // Generador dinámico del vector de ataque para demostración en clase
  const construirUrlAtaque = (
    marca: string,
    tecnica: string,
    tld: string,
    vector: string
  ): string => {
    let host = "";

    switch (tecnica) {
      case "homoglifo":
        if (marca === "bancolombia") host = `banc0l0mbia-portal-verificacion.${tld}`;
        else if (marca === "nequi") host = `nequ1-recuperacion-cuenta.${tld}`;
        else if (marca === "daviplata") host = `dav1plata-desbloqueo-app.${tld}`;
        else if (marca === "dian") host = `d1an-multas-facturacion.${tld}`;
        else if (marca === "pse") host = `pse-pag0s-segur0s.${tld}`;
        else host = `serv1entrega-env1os.${tld}`;
        break;

      case "typosquatting":
        host = `${marca}a-login-oficial.${tld}`;
        break;

      case "subdominios_anidados":
        host = `seguridad.login.portal-verificacion.${marca}.${tld}`;
        break;

      case "userinfo_arroba":
        return `http://${marca}.com.co@servidor-phishing-externo.${tld}/login-actualizar-datos`;

      default:
        host = `${marca}-actualizar-datos.${tld}`;
    }

    let ruta = "/portal/ingreso";
    if (vector === "miedo_multa") {
      ruta = "/dian/fotomultas/comparendo-pendiente-inmediato";
    } else if (vector === "premio") {
      ruta = "/sorteo/ganador/reembolso-saldo-inmediato";
    } else {
      ruta = "/seguridad/actualizar-datos-urgente";
    }

    return `http://${host}${ruta}`;
  };

  useEffect(() => {
    async function cargarConfiguracion(): Promise<void> {
      const config = await ServicioAlmacenamiento.obtenerConfiguracion();
      setConfiguracion(config);
      motorRiesgoSandbox.actualizarConfiguracion(config);

      const parametros = new URLSearchParams(window.location.search);
      const urlReportada = parametros.get("reportar");
      if (urlReportada) {
        setUrlPrueba(decodeURIComponent(urlReportada));
      }

      // Inicializar URL de laboratorio
      const urlInicial = construirUrlAtaque(marcaVictima, tecnicaEvasion, tldMalicioso, vectorCoercion);
      setUrlGeneradaLab(urlInicial);
    }

    cargarConfiguracion();
  }, []);

  const actualizarParametrosLab = (
    nuevaMarca: string,
    nuevaTecnica: string,
    nuevoTld: string,
    nuevoVector: string
  ) => {
    setMarcaVictima(nuevaMarca);
    setTecnicaEvasion(nuevaTecnica);
    setTldMalicioso(nuevoTld);
    setVectorCoercion(nuevoVector);
    const nuevaUrl = construirUrlAtaque(nuevaMarca, nuevaTecnica, nuevoTld, nuevoVector);
    setUrlGeneradaLab(nuevaUrl);
  };

  const ejecutarSimulacionLaboratorio = async (): Promise<void> => {
    const res = await motorRiesgoSandbox.analizarUrl(urlGeneradaLab);
    setResultadoLab(res);
  };

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
          <h1>Panel de Control & Laboratorio de Ciberseguridad</h1>
          <p>PhishGuard Colombia • Heurística Forense, Simulación de Ataques y Marco MITRE ATT&CK®</p>
        </div>
      </header>

      {guardadoExitoso && (
        <div className="mensaje-guardado">
          <CheckCircle size={18} />
          Configuración actualizada y aplicada correctamente.
        </div>
      )}

      {/* LABORATORIO DE SIMULACIÓN DE ATAQUES ÉTICOS */}
      <section className="contenedor-laboratorio">
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <FlaskConical size={22} color="#c084fc" />
          <h2 style={{ fontSize: "18px", fontWeight: 700, color: "#f3e8ff" }}>
            Laboratorio Interactivo de Simulación de Phishing (Para Demostración en Clase)
          </h2>
        </div>
        <p style={{ fontSize: "12px", color: "#cbd5e1", lineHeight: "1.5" }}>
          Construye vectores de ataque de phishing combinando entidades colombianas reales, técnicas de evasión de defensas y manipulación psicológica para ver cómo el motor de PhishGuard los detecta y desmantela capa por capa.
        </p>

        <div className="grid-selectores-lab">
          <div className="campo-lab">
            <label className="etiqueta-lab">1. Entidad Objetivo (Víctima)</label>
            <select
              className="select-lab"
              value={marcaVictima}
              onChange={(e) =>
                actualizarParametrosLab(e.target.value, tecnicaEvasion, tldMalicioso, vectorCoercion)
              }
            >
              <option value="bancolombia">Bancolombia (Banca Tradicional)</option>
              <option value="nequi">Nequi (Billetera Digital)</option>
              <option value="daviplata">Daviplata (Banca Móvil)</option>
              <option value="dian">DIAN (Gobierno / Tributario)</option>
              <option value="pse">PSE (Pasarela de Pagos ACH)</option>
              <option value="servientrega">Servientrega (Paquetería)</option>
            </select>
          </div>

          <div className="campo-lab">
            <label className="etiqueta-lab">2. Técnica de Evasión (MITRE)</label>
            <select
              className="select-lab"
              value={tecnicaEvasion}
              onChange={(e) =>
                actualizarParametrosLab(marcaVictima, e.target.value, tldMalicioso, vectorCoercion)
              }
            >
              <option value="homoglifo">Homoglifos Visuales (0 por o, 1 por l) [T1036.007]</option>
              <option value="typosquatting">Typosquatting / Levenshtein [T1036.007]</option>
              <option value="subdominios_anidados">Subdominios Profundos Anidados [T1566.002]</option>
              <option value="userinfo_arroba">Ofuscación RFC mediante '@' [T1566.002]</option>
            </select>
          </div>

          <div className="campo-lab">
            <label className="etiqueta-lab">3. Extensión TLD Maliciosa</label>
            <select
              className="select-lab"
              value={tldMalicioso}
              onChange={(e) =>
                actualizarParametrosLab(marcaVictima, tecnicaEvasion, e.target.value, vectorCoercion)
              }
            >
              <option value="xyz">.xyz (Abuso Masivo)</option>
              <option value="top">.top (Alta Frecuencia Phishing)</option>
              <option value="click">.click (Spam / Fake Portals)</option>
              <option value="tk">.tk (Dominio Gratuito)</option>
            </select>
          </div>

          <div className="campo-lab">
            <label className="etiqueta-lab">4. Vector de Coerción Psicológica</label>
            <select
              className="select-lab"
              value={vectorCoercion}
              onChange={(e) =>
                actualizarParametrosLab(marcaVictima, tecnicaEvasion, tldMalicioso, e.target.value)
              }
            >
              <option value="urgencia">Urgencia Temporal (Cuenta Bloqueada)</option>
              <option value="miedo_multa">Miedo / Coerción Legal (Embargo / Fotomulta)</option>
              <option value="premio">Cebo de Recompensa (Devolución de Dinero)</option>
            </select>
          </div>
        </div>

        <div>
          <span style={{ fontSize: "11px", fontWeight: 700, color: "#94a3b8" }}>
            Vector de Ataque Sintetizado:
          </span>
          <div className="caja-url-generada">
            <span>{urlGeneradaLab}</span>
          </div>
        </div>

        <div style={{ display: "flex", gap: "8px" }}>
          <button className="boton-lanzar-simulacion" style={{ flex: 1 }} onClick={ejecutarSimulacionLaboratorio}>
            <Zap size={16} /> Analizar Vector en Sandbox
          </button>
          <button
            className="boton-lanzar-simulacion"
            style={{ background: "linear-gradient(135deg, #ef4444 0%, #b91c1c 100%)" }}
            onClick={() => {
              if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.sendMessage) {
                chrome.runtime.sendMessage({ tipo: "PROBAR_BLOQUEO_EN_VIVO", url: urlGeneradaLab });
              } else {
                window.open(`../bloqueado/index.html?url=${encodeURIComponent(urlGeneradaLab)}&puntuacion=95&host=banc0l0mbia-portal-verificacion.xyz`, "_blank");
              }
            }}
          >
            <ShieldAlert size={16} /> Detonar Pantalla de Bloqueo en Pestaña
          </button>
        </div>

        {resultadoLab && (
          <div className="resultado-sandbox" style={{ borderLeft: "4px solid #ef4444" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontWeight: 700, fontSize: "14px" }}>
                🛡️ Veredicto Forense: {resultadoLab.clasificacion} ({resultadoLab.puntuacionRiesgo}/100)
              </span>
              <span style={{ fontSize: "11px", color: "#38bdf8", fontFamily: "ui-monospace" }}>
                {resultadoLab.tiempoProcesamientoMs} ms
              </span>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px", marginTop: "4px" }}>
              <div style={{ background: "rgba(15, 23, 42, 0.6)", padding: "6px", borderRadius: "4px", fontSize: "11px" }}>
                <strong>Entropía Shannon:</strong> {resultadoLab.caracteristicas.entropiaShannon} bits/símbolo
              </div>
              <div style={{ background: "rgba(15, 23, 42, 0.6)", padding: "6px", borderRadius: "4px", fontSize: "11px" }}>
                <strong>Mapeo MITRE:</strong> {resultadoLab.reglasCoincidentes.map((r) => r.tecnicaMitre?.id).filter(Boolean).join(", ") || "N/A"}
              </div>
            </div>

            <div style={{ marginTop: "6px" }}>
              <strong style={{ fontSize: "11px", color: "#fca5a5" }}>Capas Defensivas Detonadas:</strong>
              {resultadoLab.reglasCoincidentes.map((regla) => (
                <div key={regla.id} style={{ fontSize: "11px", color: "#e2e8f0", marginTop: "2px" }}>
                  • <strong>{regla.nombre}</strong> (+{regla.puntuacion} pts): {regla.descripcion}
                </div>
              ))}
            </div>
          </div>
        )}
      </section>

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
              Máxima alerta. Umbrales bajos (Sospechoso ≥ 20, Malicioso ≥ 55).
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
              Balance óptimo con prevención de falsos positivos (Sospechoso ≥ 30, Malicioso ≥ 70).
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
              Solo bloquea con evidencia contundente acumulativa (Sospechoso ≥ 45, Malicioso ≥ 80).
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
          <Search size={18} /> Probador Heurístico de URLs Arbitrarias
        </h2>

        <form onSubmit={ejecutarPruebaUrl} className="grupo-formulario-lista">
          <input
            type="text"
            className="input-texto"
            placeholder="Ej: http://banc0l0mbia-login-verificacion.xyz/portal/actualizar-datos"
            value={urlPrueba}
            onChange={(e) => setUrlPrueba(e.target.value)}
            required
          />
          <button type="submit" className="boton-agregar" disabled={analizandoPrueba}>
            {analizandoPrueba ? "Analizando..." : "Analizar"}
          </button>
        </form>

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
              Tiempo: {resultadoPrueba.tiempoProcesamientoMs} ms • Entropía Shannon: {resultadoPrueba.caracteristicas.entropiaShannon} bits/símbolo
            </div>

            {resultadoPrueba.reglasCoincidentes.length > 0 && (
              <div style={{ display: "flex", flexDirection: "column", gap: "6px", marginTop: "8px" }}>
                <strong style={{ fontSize: "12px", color: "#e2e8f0" }}>Reglas & Técnicas MITRE:</strong>
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
                    <div style={{ display: "flex", justifyContent: "space-between" }}>
                      <span style={{ fontWeight: 600, color: "#f87171" }}>{regla.nombre}</span>
                      {regla.tecnicaMitre && (
                        <span style={{ color: "#60a5fa", fontFamily: "ui-monospace" }}>
                          [{regla.tecnicaMitre.id}]
                        </span>
                      )}
                    </div>
                    <div style={{ color: "#94a3b8" }}>{regla.descripcion}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      {/* Privacidad */}
      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <Lock size={18} /> Privacidad y Arquitectura Local
        </h2>
        <p style={{ fontSize: "12px", color: "#94a3b8", lineHeight: "1.6" }}>
          • El análisis forense se ejecuta <strong>100% de manera local</strong> en el Service Worker.
          <br />
          • <strong>NO</strong> se registra historial de navegación ni se capturan formularios/contraseñas.
          <br />
          • Los datos forenses se generan bajo demanda para fines de respuesta a incidentes y auditoría técnica.
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
