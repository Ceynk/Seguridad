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
import { normalizarDominioOUrl } from "../../reputation/reputacion-local";
import "./opciones.css";

const motorRiesgoSandbox = new MotorRiesgo();

export const ComponenteOpciones: React.FC = () => {
  const [configuracion, setConfiguracion] = useState<ConfiguracionExtension>(
    CONFIGURACION_PREDETERMINADA
  );
  const [nuevoDominioBlanco, setNuevoDominioBlanco] = useState<string>("");
  const [nuevoDominioNegro, setNuevoDominioNegro] = useState<string>("");
  const [guardadoExitoso, setGuardadoExitoso] = useState<boolean>(false);

  const [urlPrueba, setUrlPrueba] = useState<string>("");
  const [resultadoPrueba, setResultadoPrueba] = useState<ResultadoAnalisisUrl | null>(null);
  const [analizandoPrueba, setAnalizandoPrueba] = useState<boolean>(false);

  const [marcaVictima, setMarcaVictima] = useState<string>("bancolombia");
  const [tecnicaEvasion, setTecnicaEvasion] = useState<string>("homoglifo");
  const [tldMalicioso, setTldMalicioso] = useState<string>("xyz");
  const [vectorCoercion, setVectorCoercion] = useState<string>("urgencia");
  const [urlGeneradaLab, setUrlGeneradaLab] = useState<string>("");
  const [resultadoLab, setResultadoLab] = useState<ResultadoAnalisisUrl | null>(null);

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
    const dominioLimpio = normalizarDominioOUrl(nuevoDominioBlanco);
    if (!dominioLimpio || configuracion.listaBlancaPersonalizada.includes(dominioLimpio)) return;

    const nuevaListaBlanca = [...configuracion.listaBlancaPersonalizada, dominioLimpio];
    const nuevaListaNegra = configuracion.listaNegraPersonalizada.filter((d) => d !== dominioLimpio);
    const nuevaConfig = {
      ...configuracion,
      listaBlancaPersonalizada: nuevaListaBlanca,
      listaNegraPersonalizada: nuevaListaNegra,
    };
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
    const dominioLimpio = normalizarDominioOUrl(nuevoDominioNegro);
    if (!dominioLimpio || configuracion.listaNegraPersonalizada.includes(dominioLimpio)) return;

    const nuevaListaNegra = [...configuracion.listaNegraPersonalizada, dominioLimpio];
    const nuevaListaBlanca = configuracion.listaBlancaPersonalizada.filter((d) => d !== dominioLimpio);
    const nuevaConfig = {
      ...configuracion,
      listaNegraPersonalizada: nuevaListaNegra,
      listaBlancaPersonalizada: nuevaListaBlanca,
    };
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
      <header className="cabecera-opciones">
        <Shield className="icono-escudo-principal" size={36} />
        <div className="titulos-cabecera">
          <h1>Panel de Control & Simulación</h1>
          <p>PhishGuard Colombia • Heurística y Reglas de Detección</p>
        </div>
      </header>

      {guardadoExitoso && (
        <div className="mensaje-guardado">
          <CheckCircle size={18} />
          Configuración actualizada correctamente.
        </div>
      )}

      <section className="contenedor-laboratorio">
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <FlaskConical size={22} color="#c084fc" />
          <h2 style={{ fontSize: "18px", fontWeight: 700, color: "#6b21a8" }}>
            Simulador de URLs y Vectores de Ataque
          </h2>
        </div>
        <p style={{ fontSize: "12px", color: "#475569", lineHeight: "1.5" }}>
          Permite generar y evaluar variantes sintácticas para comprobar la respuesta del motor de detección ante distintas técnicas de evasión.
        </p>

        <div className="grid-selectores-lab">
          <div className="campo-lab">
            <label className="etiqueta-lab">1. Entidad Objetivo</label>
            <select
              className="select-lab"
              value={marcaVictima}
              onChange={(e) =>
                actualizarParametrosLab(e.target.value, tecnicaEvasion, tldMalicioso, vectorCoercion)
              }
            >
              <option value="bancolombia">Bancolombia</option>
              <option value="nequi">Nequi</option>
              <option value="daviplata">Daviplata</option>
              <option value="dian">DIAN</option>
              <option value="pse">PSE</option>
              <option value="servientrega">Servientrega</option>
            </select>
          </div>

          <div className="campo-lab">
            <label className="etiqueta-lab">2. Técnica de Evasión</label>
            <select
              className="select-lab"
              value={tecnicaEvasion}
              onChange={(e) =>
                actualizarParametrosLab(marcaVictima, e.target.value, tldMalicioso, vectorCoercion)
              }
            >
              <option value="homoglifo">Homoglifos Visuales (0 por o, 1 por l)</option>
              <option value="typosquatting">Typosquatting / Variación Levenshtein</option>
              <option value="subdominios_anidados">Subdominios Anidados</option>
              <option value="userinfo_arroba">Uso de @ en URL</option>
            </select>
          </div>

          <div className="campo-lab">
            <label className="etiqueta-lab">3. Extensión TLD</label>
            <select
              className="select-lab"
              value={tldMalicioso}
              onChange={(e) =>
                actualizarParametrosLab(marcaVictima, tecnicaEvasion, e.target.value, vectorCoercion)
              }
            >
              <option value="xyz">.xyz</option>
              <option value="top">.top</option>
              <option value="click">.click</option>
              <option value="tk">.tk</option>
            </select>
          </div>

          <div className="campo-lab">
            <label className="etiqueta-lab">4. Vector Léxico</label>
            <select
              className="select-lab"
              value={vectorCoercion}
              onChange={(e) =>
                actualizarParametrosLab(marcaVictima, tecnicaEvasion, tldMalicioso, e.target.value)
              }
            >
              <option value="urgencia">Urgencia Temporal (Cuenta Bloqueada)</option>
              <option value="miedo_multa">Sanción Legal (Fotomulta / Embargo)</option>
              <option value="premio">Recompensa (Devolución de Saldo)</option>
            </select>
          </div>
        </div>

        <div>
          <span style={{ fontSize: "11px", fontWeight: 700, color: "#64748b" }}>
            URL Sintetizada:
          </span>
          <div className="caja-url-generada">
            <span>{urlGeneradaLab}</span>
          </div>
        </div>

        <div style={{ display: "flex", gap: "8px" }}>
          <button className="boton-lanzar-simulacion" style={{ flex: 1 }} onClick={ejecutarSimulacionLaboratorio}>
            <Zap size={16} /> Evaluar URL Sintetizada
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
            <ShieldAlert size={16} /> Probar Pantalla de Bloqueo
          </button>
        </div>

        {resultadoLab && (
          <div className="resultado-sandbox" style={{ borderLeft: "4px solid #ef4444" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontWeight: 700, fontSize: "14px" }}>
                Resultado: {resultadoLab.clasificacion} ({resultadoLab.puntuacionRiesgo}/100)
              </span>
              <span style={{ fontSize: "11px", color: "#0284c7", fontFamily: "ui-monospace" }}>
                {resultadoLab.tiempoProcesamientoMs} ms
              </span>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px", marginTop: "4px" }}>
              <div style={{ background: "#f1f5f9", padding: "6px", borderRadius: "4px", fontSize: "11px" }}>
                <strong>Entropía Shannon:</strong> {resultadoLab.caracteristicas.entropiaShannon} bits/símbolo
              </div>
              <div style={{ background: "#f1f5f9", padding: "6px", borderRadius: "4px", fontSize: "11px" }}>
                <strong>MITRE ATT&CK:</strong> {resultadoLab.reglasCoincidentes.map((r) => r.tecnicaMitre?.id).filter(Boolean).join(", ") || "N/A"}
              </div>
            </div>

            <div style={{ marginTop: "6px" }}>
              <strong style={{ fontSize: "11px", color: "#dc2626" }}>Reglas Activadas:</strong>
              {resultadoLab.reglasCoincidentes.map((regla) => (
                <div key={regla.id} style={{ fontSize: "11px", color: "#334155", marginTop: "2px" }}>
                  • <strong>{regla.nombre}</strong> (+{regla.puntuacion} pts): {regla.descripcion}
                </div>
              ))}
            </div>
          </div>
        )}
      </section>

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <Sliders size={18} /> Nivel de Sensibilidad
        </h2>

        <div className="cuadricula-modos">
          <div
            className={`tarjeta-modo ${
              configuracion.modoProteccion === "ESTRICTO" ? "modo-seleccionado" : ""
            }`}
            onClick={() => cambiarModo("ESTRICTO")}
          >
            <span className="titulo-modo">Estricto</span>
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
            <span className="titulo-modo">Equilibrado</span>
            <p className="desc-modo">
              Balance recomendado con prevención de falsos positivos (Sospechoso ≥ 30, Malicioso ≥ 70).
            </p>
          </div>

          <div
            className={`tarjeta-modo ${
              configuracion.modoProteccion === "PERMISIVO" ? "modo-seleccionado" : ""
            }`}
            onClick={() => cambiarModo("PERMISIVO")}
          >
            <span className="titulo-modo">Permisivo</span>
            <p className="desc-modo">
              Solo bloquea con evidencia contundente acumulativa (Sospechoso ≥ 45, Malicioso ≥ 80).
            </p>
          </div>
        </div>

        <div className="fila-opcion">
          <div className="info-opcion">
            <span className="nombre-opcion">Bloqueo automático de sitios maliciosos</span>
            <span className="desc-opcion">
              Impide la carga de páginas clasificadas como MALICIOSAS redirigiendo a la pantalla de bloqueo.
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

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <ListPlus size={18} /> Listas de Excepción
        </h2>

        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          <span style={{ fontSize: "13px", fontWeight: 600, color: "#10b981" }}>
            Lista Blanca (Siempre permitidos)
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
                No hay dominios en la lista blanca personal.
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

        <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "12px" }}>
          <span style={{ fontSize: "13px", fontWeight: 600, color: "#ef4444" }}>
            Lista Negra (Siempre bloqueados)
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
                No hay dominios en la lista negra personal.
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

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <Search size={18} /> Probador de URLs
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

            <div style={{ fontSize: "11px", color: "#64748b" }}>
              Tiempo: {resultadoPrueba.tiempoProcesamientoMs} ms • Entropía Shannon: {resultadoPrueba.caracteristicas.entropiaShannon} bits/símbolo
            </div>

            {resultadoPrueba.reglasCoincidentes.length > 0 && (
              <div style={{ display: "flex", flexDirection: "column", gap: "6px", marginTop: "8px" }}>
                <strong style={{ fontSize: "12px", color: "#334155" }}>Reglas activadas:</strong>
                {resultadoPrueba.reglasCoincidentes.map((regla) => (
                  <div
                    key={regla.id}
                    style={{
                      backgroundColor: "#f1f5f9",
                      padding: "8px",
                      borderRadius: "6px",
                      fontSize: "11px",
                    }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between" }}>
                      <span style={{ fontWeight: 600, color: "#dc2626" }}>{regla.nombre}</span>
                      {regla.tecnicaMitre && (
                        <span style={{ color: "#2563eb", fontFamily: "ui-monospace" }}>
                          [{regla.tecnicaMitre.id}]
                        </span>
                      )}
                    </div>
                    <div style={{ color: "#64748b" }}>{regla.descripcion}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion">
          <Lock size={18} /> Privacidad
        </h2>
        <p style={{ fontSize: "12px", color: "#64748b", lineHeight: "1.6" }}>
          • El análisis se ejecuta de manera local en el navegador.
          <br />
          • No se almacena historial de navegación persistente ni se inspeccionan contraseñas o campos de formulario.
          <br />
          • Los reportes forenses se generan bajo demanda para auditoría técnica.
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
