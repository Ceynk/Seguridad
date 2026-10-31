import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  CheckCircle, FlaskConical, ListPlus, Lock, Plus,
  Search, Shield, ShieldAlert, Sliders, Trash2, Zap,
} from "lucide-react";
import { ConfiguracionExtension, ModoProteccion } from "../../tipos/configuracion";
import { CONFIGURACION_PREDETERMINADA, ServicioAlmacenamiento } from "../../almacenamiento/servicio-almacenamiento";
import { MotorRiesgo } from "../../analizador/motor-riesgo";
import { ResultadoAnalisisUrl } from "../../tipos/analisis";
import { normalizarDominioOUrl } from "../../reputacion/reputacion-local";
import "./opciones.css";

const motorSimulacion = new MotorRiesgo();

const MODOS_PROTECCION: {
  modo: ModoProteccion;
  titulo: string;
  descripcion: string;
  umbralSospechoso: number;
  umbralMalicioso: number;
}[] = [
  { modo: "ESTRICTO", titulo: "Estricto", descripcion: "Máxima alerta. Umbrales bajos (Sospechoso >= 20, Malicioso >= 55).", umbralSospechoso: 20, umbralMalicioso: 55 },
  { modo: "EQUILIBRADO", titulo: "Equilibrado", descripcion: "Balance recomendado con prevención de falsos positivos (Sospechoso >= 30, Malicioso >= 70).", umbralSospechoso: 30, umbralMalicioso: 70 },
  { modo: "PERMISIVO", titulo: "Permisivo", descripcion: "Solo bloquea con evidencia contundente acumulativa (Sospechoso >= 45, Malicioso >= 80).", umbralSospechoso: 45, umbralMalicioso: 80 },
];

const ANFITRIONES_HOMOGLIFO: Record<string, string> = {
  bancolombia: "banc0l0mbia-portal-verificacion",
  nequi: "nequ1-recuperacion-cuenta",
  daviplata: "dav1plata-desbloqueo-app",
  dian: "d1an-multas-facturacion",
  pse: "pse-pag0s-segur0s",
};

const RUTAS_VECTOR: Record<string, string> = {
  miedo_multa: "/dian/fotomultas/comparendo-pendiente-inmediato",
  premio: "/sorteo/ganador/reembolso-saldo-inmediato",
  urgencia: "/seguridad/actualizar-datos-urgente",
};

interface ParametrosLaboratorio {
  marca: string;
  tecnica: string;
  tld: string;
  vector: string;
}

const SELECTORES_LABORATORIO: { clave: keyof ParametrosLaboratorio; etiqueta: string; opciones: [string, string][] }[] = [
  {
    clave: "marca",
    etiqueta: "1. Entidad Objetivo",
    opciones: [["bancolombia", "Bancolombia"], ["nequi", "Nequi"], ["daviplata", "Daviplata"], ["dian", "DIAN"], ["pse", "PSE"], ["servientrega", "Servientrega"]],
  },
  {
    clave: "tecnica",
    etiqueta: "2. Técnica de Evasión",
    opciones: [
      ["homoglifo", "Homoglifos Visuales (0 por o, 1 por l)"],
      ["typosquatting", "Variación Tipográfica Levenshtein"],
      ["subdominios_anidados", "Subdominios Anidados"],
      ["userinfo_arroba", "Uso de @ en URL"],
    ],
  },
  {
    clave: "tld",
    etiqueta: "3. Extensión TLD",
    opciones: [["xyz", ".xyz"], ["top", ".top"], ["click", ".click"], ["tk", ".tk"]],
  },
  {
    clave: "vector",
    etiqueta: "4. Vector Léxico",
    opciones: [
      ["urgencia", "Urgencia Temporal (Cuenta Bloqueada)"],
      ["miedo_multa", "Sanción Legal (Fotomulta / Embargo)"],
      ["premio", "Recompensa (Devolución de Saldo)"],
    ],
  },
];

function construirUrlAtaque({ marca, tecnica, tld, vector }: ParametrosLaboratorio): string {
  if (tecnica === "userinfo_arroba") {
    return `http://${marca}.com.co@servidor-phishing-externo.${tld}/login-actualizar-datos`;
  }
  const anfitrion =
    tecnica === "homoglifo" ? `${ANFITRIONES_HOMOGLIFO[marca] ?? "serv1entrega-env1os"}.${tld}` :
    tecnica === "typosquatting" ? `${marca}a-login-oficial.${tld}` :
    tecnica === "subdominios_anidados" ? `seguridad.login.portal-verificacion.${marca}.${tld}` :
    `${marca}-actualizar-datos.${tld}`;
  return `http://${anfitrion}${RUTAS_VECTOR[vector] ?? RUTAS_VECTOR.urgencia}`;
}

interface PropiedadesListaExcepcion {
  titulo: string;
  claseTitulo: string;
  marcador: string;
  textoBoton: string;
  esPeligro?: boolean;
  dominios: string[];
  mensajeVacio: string;
  tituloEliminar: string;
  valorEntrada: string;
  alCambiarEntrada: (valor: string) => void;
  alAgregar: (evento: React.FormEvent) => void;
  alEliminar: (dominio: string) => void;
}

const SeccionListaExcepcion: React.FC<PropiedadesListaExcepcion> = (p) => (
  <div className="bloque-lista">
    <span className={p.claseTitulo}>{p.titulo}</span>
    <form onSubmit={p.alAgregar} className="grupo-formulario-lista">
      <input
        type="text"
        className="entrada-texto"
        placeholder={p.marcador}
        value={p.valorEntrada}
        onChange={(e) => p.alCambiarEntrada(e.target.value)}
      />
      <button type="submit" className={`boton-agregar ${p.esPeligro ? "peligro" : ""}`}>
        <Plus size={14} /> {p.textoBoton}
      </button>
    </form>
    <div className="contenedor-fichas">
      {p.dominios.length === 0 ? (
        <span className="mensaje-vacio">{p.mensajeVacio}</span>
      ) : (
        p.dominios.map((dominio) => (
          <span key={dominio} className="ficha-dominio">
            {dominio}
            <button type="button" className="boton-eliminar-ficha" onClick={() => p.alEliminar(dominio)} title={p.tituloEliminar}>
              <Trash2 size={12} />
            </button>
          </span>
        ))
      )}
    </div>
  </div>
);

export const ComponenteOpciones: React.FC = () => {
  const [configuracion, establecerConfiguracion] = useState<ConfiguracionExtension>(CONFIGURACION_PREDETERMINADA);
  const [entradaBlanca, establecerEntradaBlanca] = useState("");
  const [entradaNegra, establecerEntradaNegra] = useState("");
  const [guardadoExitoso, establecerGuardadoExitoso] = useState(false);
  const [urlPrueba, establecerUrlPrueba] = useState("");
  const [resultadoPrueba, establecerResultadoPrueba] = useState<ResultadoAnalisisUrl | null>(null);
  const [analizandoPrueba, establecerAnalizandoPrueba] = useState(false);
  const [parametrosLab, establecerParametrosLab] = useState<ParametrosLaboratorio>({
    marca: "bancolombia", tecnica: "homoglifo", tld: "xyz", vector: "urgencia",
  });
  const [resultadoLab, establecerResultadoLab] = useState<ResultadoAnalisisUrl | null>(null);

  const urlGeneradaLab = construirUrlAtaque(parametrosLab);

  useEffect(() => {
    (async () => {
      const guardada = await ServicioAlmacenamiento.obtenerConfiguracion();
      establecerConfiguracion(guardada);
      motorSimulacion.actualizarConfiguracion(guardada);
      const reportada = new URLSearchParams(window.location.search).get("reportar");
      if (reportada) establecerUrlPrueba(decodeURIComponent(reportada));
    })();
  }, []);

  const persistirConfiguracion = async (nueva: ConfiguracionExtension): Promise<void> => {
    establecerConfiguracion(nueva);
    motorSimulacion.actualizarConfiguracion(nueva);
    await ServicioAlmacenamiento.guardarConfiguracion(nueva);
    chrome?.runtime?.sendMessage?.({ tipo: "ACTUALIZAR_CONFIGURACION", configuracion: nueva });
    establecerGuardadoExitoso(true);
    setTimeout(() => establecerGuardadoExitoso(false), 2500);
  };

  const cambiarModo = (modoSeleccionado: ModoProteccion): void => {
    const definicion = MODOS_PROTECCION.find((m) => m.modo === modoSeleccionado);
    if (!definicion) return;
    void persistirConfiguracion({
      ...configuracion,
      modoProteccion: definicion.modo,
      umbralSospechoso: definicion.umbralSospechoso,
      umbralMalicioso: definicion.umbralMalicioso,
    });
  };

  const agregarDominioLista = (tipo: "blanca" | "negra", entrada: string): void => {
    const limpio = normalizarDominioOUrl(entrada);
    if (!limpio) return;
    if (tipo === "blanca") {
      if (configuracion.listaBlancaPersonalizada.includes(limpio)) return;
      void persistirConfiguracion({
        ...configuracion,
        listaBlancaPersonalizada: [...configuracion.listaBlancaPersonalizada, limpio],
        listaNegraPersonalizada: configuracion.listaNegraPersonalizada.filter((d) => d !== limpio),
      });
      establecerEntradaBlanca("");
    } else {
      if (configuracion.listaNegraPersonalizada.includes(limpio)) return;
      void persistirConfiguracion({
        ...configuracion,
        listaNegraPersonalizada: [...configuracion.listaNegraPersonalizada, limpio],
        listaBlancaPersonalizada: configuracion.listaBlancaPersonalizada.filter((d) => d !== limpio),
      });
      establecerEntradaNegra("");
    }
  };

  const eliminarDominioLista = (tipo: "blanca" | "negra", dominio: string): void => {
    void persistirConfiguracion(
      tipo === "blanca"
        ? { ...configuracion, listaBlancaPersonalizada: configuracion.listaBlancaPersonalizada.filter((d) => d !== dominio) }
        : { ...configuracion, listaNegraPersonalizada: configuracion.listaNegraPersonalizada.filter((d) => d !== dominio) }
    );
  };

  const probarPantallaBloqueo = (): void => {
    if (typeof chrome !== "undefined" && chrome.runtime?.sendMessage) {
      chrome.runtime.sendMessage({ tipo: "PROBAR_BLOQUEO_EN_VIVO", url: urlGeneradaLab });
    } else {
      window.open(`../bloqueado/index.html?url=${encodeURIComponent(urlGeneradaLab)}&puntuacion=95&host=banc0l0mbia-portal-verificacion.xyz`, "_blank");
    }
  };

  const ejecutarPruebaUrl = async (evento: React.FormEvent): Promise<void> => {
    evento.preventDefault();
    if (!urlPrueba.trim()) return;
    establecerAnalizandoPrueba(true);
    try {
      establecerResultadoPrueba(await motorSimulacion.analizarUrl(urlPrueba.trim()));
    } finally {
      establecerAnalizandoPrueba(false);
    }
  };

  return (
    <div className="contenedor-opciones">
      <header className="cabecera-opciones">
        <Shield className="icono-escudo-principal" size={36} />
        <div className="titulos-cabecera">
          <h1>Panel de Control y Simulación</h1>
          <p>PhishGuard Colombia - Heurística y Reglas de Detección</p>
        </div>
      </header>

      {guardadoExitoso && (
        <div className="mensaje-guardado">
          <CheckCircle size={18} /> Configuración actualizada correctamente.
        </div>
      )}

      <section className="contenedor-laboratorio">
        <div className="cabecera-laboratorio">
          <FlaskConical size={22} color="#c084fc" />
          <h2 className="titulo-laboratorio">Simulador de URLs y Vectores de Ataque</h2>
        </div>
        <p className="texto-descriptivo">
          Permite generar y evaluar variantes sintácticas para comprobar la respuesta del motor de detección ante distintas técnicas de evasión.
        </p>

        <div className="cuadricula-selectores-lab">
          {SELECTORES_LABORATORIO.map(({ clave, etiqueta, opciones }) => (
            <div key={clave} className="campo-lab">
              <label className="etiqueta-lab">{etiqueta}</label>
              <select
                className="selector-lab"
                value={parametrosLab[clave]}
                onChange={(e) => establecerParametrosLab({ ...parametrosLab, [clave]: e.target.value })}
              >
                {opciones.map(([valor, nombre]) => <option key={valor} value={valor}>{nombre}</option>)}
              </select>
            </div>
          ))}
        </div>

        <div>
          <span className="subtitulo-url-generada">URL Sintetizada:</span>
          <div className="caja-url-generada"><span>{urlGeneradaLab}</span></div>
        </div>

        <div className="acciones-laboratorio">
          <button
            className="boton-lanzar-simulacion expandir"
            onClick={async () => establecerResultadoLab(await motorSimulacion.analizarUrl(urlGeneradaLab))}
          >
            <Zap size={16} /> Evaluar URL Sintetizada
          </button>
          <button className="boton-lanzar-simulacion peligro" onClick={probarPantallaBloqueo}>
            <ShieldAlert size={16} /> Probar Pantalla de Bloqueo
          </button>
        </div>

        {resultadoLab && (
          <div className="resultado-simulacion borde-alerta">
            <div className="fila-resumen-simulacion">
              <span className="texto-resumen-fuerte">
                Resultado: {resultadoLab.clasificacion} ({resultadoLab.puntuacionRiesgo}/100)
              </span>
              <span className="tiempo-ejecucion">{resultadoLab.tiempoProcesamientoMs} ms</span>
            </div>
            <div className="cuadricula-detalles-lab">
              <div className="celda-detalle-lab">
                <strong>Entropía Shannon:</strong> {resultadoLab.caracteristicas.entropiaShannon} bits/símbolo
              </div>
              <div className="celda-detalle-lab">
                <strong>MITRE ATT&CK:</strong>{" "}
                {resultadoLab.reglasCoincidentes.map((r) => r.tecnicaMitre?.id).filter(Boolean).join(", ") || "N/A"}
              </div>
            </div>
            <div className="lista-reglas-lab">
              <strong className="titulo-reglas-alerta">Reglas Activadas:</strong>
              {resultadoLab.reglasCoincidentes.map((regla) => (
                <div key={regla.id} className="item-regla-lab">
                  - <strong>{regla.nombre}</strong> (+{regla.puntuacion} pts): {regla.descripcion}
                </div>
              ))}
            </div>
          </div>
        )}
      </section>

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion"><Sliders size={18} /> Nivel de Sensibilidad</h2>
        <div className="cuadricula-modos">
          {MODOS_PROTECCION.map(({ modo, titulo, descripcion }) => (
            <div
              key={modo}
              className={`tarjeta-modo ${configuracion.modoProteccion === modo ? "modo-seleccionado" : ""}`}
              onClick={() => cambiarModo(modo)}
            >
              <span className="titulo-modo">{titulo}</span>
              <p className="descripcion-modo">{descripcion}</p>
            </div>
          ))}
        </div>
        <div className="fila-opcion">
          <div className="info-opcion">
            <span className="nombre-opcion">Bloqueo automático de sitios maliciosos</span>
            <span className="descripcion-opcion">
              Impide la carga de páginas clasificadas como MALICIOSAS redirigiendo a la pantalla de bloqueo.
            </span>
          </div>
          <input
            type="checkbox"
            checked={configuracion.bloquearMaliciososAutomaticamente}
            onChange={(e) => void persistirConfiguracion({ ...configuracion, bloquearMaliciososAutomaticamente: e.target.checked })}
          />
        </div>
      </section>

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion"><ListPlus size={18} /> Listas de Excepción</h2>
        <SeccionListaExcepcion
          titulo="Lista Blanca (Siempre permitidos)"
          claseTitulo="titulo-lista-blanca"
          marcador="ejemplo: portal-corporativo-interno.com"
          textoBoton="Agregar"
          dominios={configuracion.listaBlancaPersonalizada}
          mensajeVacio="No hay dominios en la lista blanca personal."
          tituloEliminar="Eliminar de lista blanca"
          valorEntrada={entradaBlanca}
          alCambiarEntrada={establecerEntradaBlanca}
          alAgregar={(e) => { e.preventDefault(); agregarDominioLista("blanca", entradaBlanca); }}
          alEliminar={(d) => eliminarDominioLista("blanca", d)}
        />
        <SeccionListaExcepcion
          titulo="Lista Negra (Siempre bloqueados)"
          claseTitulo="titulo-lista-negra"
          marcador="ejemplo: dominio-sospechoso-frecuente.xyz"
          textoBoton="Bloquear"
          esPeligro
          dominios={configuracion.listaNegraPersonalizada}
          mensajeVacio="No hay dominios en la lista negra personal."
          tituloEliminar="Eliminar de lista negra"
          valorEntrada={entradaNegra}
          alCambiarEntrada={establecerEntradaNegra}
          alAgregar={(e) => { e.preventDefault(); agregarDominioLista("negra", entradaNegra); }}
          alEliminar={(d) => eliminarDominioLista("negra", d)}
        />
      </section>

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion"><Search size={18} /> Probador de URLs</h2>
        <form onSubmit={ejecutarPruebaUrl} className="grupo-formulario-lista">
          <input
            type="text"
            className="entrada-texto"
            placeholder="Ej: http://banc0l0mbia-login-verificacion.xyz/portal/actualizar-datos"
            value={urlPrueba}
            onChange={(e) => establecerUrlPrueba(e.target.value)}
            required
          />
          <button type="submit" className="boton-agregar" disabled={analizandoPrueba}>
            {analizandoPrueba ? "Analizando..." : "Analizar"}
          </button>
        </form>

        {resultadoPrueba && (
          <div className="resultado-simulacion">
            <div className="fila-resumen-simulacion">
              <strong>Host: {resultadoPrueba.nombreHost}</strong>
              <span className={`insignia-clasificacion ${resultadoPrueba.clasificacion.toLowerCase()}`}>
                {resultadoPrueba.clasificacion} ({resultadoPrueba.puntuacionRiesgo} / 100)
              </span>
            </div>
            <div className="metadatos-prueba">
              Tiempo: {resultadoPrueba.tiempoProcesamientoMs} ms - Entropía Shannon: {resultadoPrueba.caracteristicas.entropiaShannon} bits/símbolo
            </div>
            {resultadoPrueba.reglasCoincidentes.length > 0 && (
              <div className="lista-reglas-prueba">
                <strong className="encabezado-reglas-prueba">Reglas activadas:</strong>
                {resultadoPrueba.reglasCoincidentes.map((regla) => (
                  <div key={regla.id} className="tarjeta-regla-prueba">
                    <div className="cabecera-regla-prueba">
                      <span className="nombre-regla-prueba">{regla.nombre}</span>
                      {regla.tecnicaMitre && <span className="codigo-mitre-prueba">[{regla.tecnicaMitre.id}]</span>}
                    </div>
                    <div className="metadatos-prueba">{regla.descripcion}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      <section className="seccion-configuracion">
        <h2 className="titulo-seccion"><Lock size={18} /> Privacidad</h2>
        <p className="texto-descriptivo">
          - El análisis se ejecuta de manera local en el navegador.<br />
          - No se almacena historial de navegación persistente ni se inspeccionan contraseñas o campos de formulario.<br />
          - Los reportes forenses se generan bajo demanda para auditoría técnica.
        </p>
      </section>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-opciones");
if (elementoRaiz) createRoot(elementoRaiz).render(<ComponenteOpciones />);
