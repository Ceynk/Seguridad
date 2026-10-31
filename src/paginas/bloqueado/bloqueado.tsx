import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { AlertTriangle, ArrowLeft, CheckCircle, Download, Flag, Info, Lock, ShieldAlert } from "lucide-react";
import { calcularEntropiaShannon, calcularHashSha256Simple, descargarInformeForense } from "../../utilidades/cripto-forense";
import { FichaForenseIncidente, TecnicaMitre } from "../../tipos/analisis";
import "./bloqueado.css";

interface MotivoBloqueo {
  nombre: string;
  descripcion: string;
  puntuacion: number;
  severidad: string;
  tecnicaMitre?: TecnicaMitre;
}

const TECNICA_PREDETERMINADA: TecnicaMitre = {
  id: "T1566.002",
  nombre: "Enlace de suplantación dirigida",
  tactica: "Acceso Inicial",
  enlace: "https://attack.mitre.org/techniques/T1566/002/",
};

function obtenerMotivos(parametros: URLSearchParams, puntuacion: number): MotivoBloqueo[] {
  try {
    const crudos = parametros.get("motivos");
    if (crudos) return JSON.parse(crudos);
  } catch {}
  return [{
    nombre: "Suplantación de Identidad",
    descripcion: "El sitio web presenta indicadores de alto riesgo compatibles con fraude cibernético.",
    puntuacion,
    severidad: "CRITICA",
    tecnicaMitre: TECNICA_PREDETERMINADA,
  }];
}

export const ComponenteBloqueado: React.FC = () => {
  const parametrosUrl = new URLSearchParams(window.location.search);
  const urlBloqueada = parametrosUrl.get("url") || "Desconocida";
  const nombreHost = parametrosUrl.get("host") || "Sitio no identificado";
  const puntuacionRiesgo = Number.parseInt(parametrosUrl.get("puntuacion") || "95", 10);
  const motivos = obtenerMotivos(parametrosUrl, puntuacionRiesgo);

  const [mostrarReporte, establecerMostrarReporte] = useState(false);
  const [comentarioReporte, establecerComentarioReporte] = useState("");
  const [reporteEnviado, establecerReporteEnviado] = useState(false);
  const [mostrarModalConsentimiento, establecerMostrarModalConsentimiento] = useState(false);
  const [consentimientoAceptado, establecerConsentimientoAceptado] = useState(false);
  const [redireccionando, establecerRedireccionando] = useState(false);

  const regresarASeguridad = (): void => {
    if (window.history.length > 1) window.history.back();
    else window.close();
  };

  const enviarReporteFalsoPositivo = (evento: React.FormEvent): void => {
    evento.preventDefault();
    establecerReporteEnviado(true);
    establecerMostrarReporte(false);
  };

  const avanzarBajoConsentimiento = async (): Promise<void> => {
    if (!consentimientoAceptado || redireccionando) return;
    establecerRedireccionando(true);
    try {
      if (typeof chrome !== "undefined" && chrome.runtime?.sendMessage) {
        await new Promise<void>((resolver) => {
          chrome.runtime.sendMessage({ tipo: "PERMITIR_DOMINIO_TEMPORALMENTE", dominio: nombreHost }, () => resolver());
        });
      }
    } catch {}

    if (urlBloqueada && urlBloqueada !== "Desconocida") window.location.href = urlBloqueada;
    else regresarASeguridad();
  };

  const descargarReporteForense = (): void => {
    const huellaSha256 = calcularHashSha256Simple(urlBloqueada);
    const ficha: FichaForenseIncidente = {
      idIncidente: `INC-${Date.now().toString(36).toUpperCase()}-${huellaSha256.slice(0, 6).toUpperCase()}`,
      marcaTemporalIso: new Date().toISOString(),
      urlObjetivo: urlBloqueada,
      nombreHost,
      huellaSha256,
      clasificacion: "MALICIOSO",
      puntuacionRiesgo,
      entropiaShannon: calcularEntropiaShannon(nombreHost),
      tecnicasMitreDetectadas: [TECNICA_PREDETERMINADA],
      vectoresIngenieriaSocial: [],
      reglasDetonadas: motivos.map((m) => ({
        nombre: m.nombre,
        severidad: "CRITICA",
        puntuacion: m.puntuacion,
        evidencias: [{ campo: "motivoBloqueo", valorEncontrado: m.nombre, descripcion: m.descripcion }],
      })),
      resumenTecnico: `El artefacto '${nombreHost}' fue bloqueado preventivamente por coincidencia con firmas de fraude.`,
      recomendacionRespuesta: "1. Aislar sesión.\n2. Notificar a CSIRT.",
    };
    descargarInformeForense(`incidente_bloqueo_${nombreHost}.md`, ficha);
  };

  return (
    <div className="contenedor-bloqueo">
      <div className="tarjeta-bloqueo">
        <header className="cabecera-bloqueo">
          <div className="envoltorio-icono-bloqueo"><ShieldAlert size={48} /></div>
          <h1 className="titulo-bloqueo">Página bloqueada por seguridad</h1>
          <p className="subtitulo-bloqueo">
            PhishGuard ha impedido el acceso a este sitio debido a indicadores de riesgo críticos.
          </p>
        </header>

        <main className="contenido-bloqueo">
          <div className="seccion-informacion">
            <span className="etiqueta-informacion">Dominio / URL</span>
            <div className="caja-url">
              <Lock size={14} />
              <span className="truncado">{urlBloqueada}</span>
            </div>
          </div>

          <div className="cuadricula-metricas">
            <div className="caja-metrica peligro">
              <span className="etiqueta-metrica">Nivel de Peligro</span>
              <span className="valor-metrica">MALICIOSO</span>
            </div>
            <div className="caja-metrica advertencia">
              <span className="etiqueta-metrica">Riesgo Calculado</span>
              <span className="valor-metrica">{puntuacionRiesgo} / 100</span>
            </div>
          </div>

          <div className="seccion-informacion">
            <span className="etiqueta-informacion">Motivos principales del bloqueo</span>
            <div className="lista-motivos">
              {motivos.map((motivo, indice) => (
                <div key={indice} className="elemento-motivo">
                  <div className="cabecera-motivo">
                    <span>{motivo.nombre}</span>
                    <span className="puntaje-motivo">+{motivo.puntuacion}</span>
                  </div>
                  <p className="descripcion-motivo">{motivo.descripcion}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="aviso-educativo">
            <Info size={20} className="icono-educativo" />
            <div className="texto-educativo">
              <strong>Prevención de fraude</strong>
              <p>Este sitio utiliza una estructura o nombre engañoso para intentar capturar contraseñas o datos financieros. Te recomendamos no continuar.</p>
            </div>
          </div>

          {reporteEnviado && (
            <div className="aviso-exito">
              <CheckCircle size={18} />
              <span>Reporte de falso positivo enviado correctamente.</span>
            </div>
          )}

          {mostrarReporte && !reporteEnviado && (
            <form onSubmit={enviarReporteFalsoPositivo} className="formulario-reporte">
              <span className="etiqueta-formulario">Reportar como sitio seguro</span>
              <textarea
                className="area-texto"
                rows={3}
                placeholder="¿Por qué consideras que este sitio es legítimo?"
                value={comentarioReporte}
                onChange={(e) => establecerComentarioReporte(e.target.value)}
                required
              />
              <div className="acciones-formulario">
                <button type="button" className="boton boton-texto" onClick={() => establecerMostrarReporte(false)}>Cancelar</button>
                <button type="submit" className="boton boton-secundario">Enviar Reporte</button>
              </div>
            </form>
          )}
        </main>

        <footer className="pie-bloqueo">
          <div className="acciones-primarias">
            <button className="boton boton-primario boton-grande" onClick={regresarASeguridad}>
              <ArrowLeft size={18} /> Regresar a un sitio seguro
            </button>
          </div>
          <div className="acciones-secundarias">
            <button className="boton boton-borde" onClick={descargarReporteForense}>
              <Download size={14} /> Ficha Forense
            </button>
            {!mostrarReporte && !reporteEnviado && (
              <button className="boton boton-texto" onClick={() => establecerMostrarReporte(true)}>
                <Flag size={14} /> Reportar falso positivo
              </button>
            )}
          </div>
          <div className="zona-riesgo">
            <button className="enlace-peligro" onClick={() => establecerMostrarModalConsentimiento(true)}>
              Deseo continuar bajo mi propia responsabilidad
            </button>
          </div>
        </footer>
      </div>

      {mostrarModalConsentimiento && (
        <div className="superposicion-modal">
          <div className="contenido-modal">
            <div className="cabecera-modal">
              <AlertTriangle size={24} className="texto-advertencia" />
              <h2>Confirmación de acceso</h2>
            </div>
            <div className="cuerpo-modal">
              <p>Estás a punto de acceder a <strong>{nombreHost}</strong>, un sitio clasificado como peligroso.</p>
              <div className="caja-advertencia">
                <strong>Advertencia:</strong> Si continúas, podrías exponer tus datos personales o financieros. No ingreses contraseñas ni números de tarjeta.
              </div>
              <label className="casilla-consentimiento">
                <input
                  type="checkbox"
                  checked={consentimientoAceptado}
                  onChange={(e) => establecerConsentimientoAceptado(e.target.checked)}
                />
                <span>Comprendo los riesgos y deseo acceder bajo mi propia responsabilidad.</span>
              </label>
            </div>
            <div className="pie-modal">
              <button
                className="boton boton-primario"
                onClick={() => {
                  establecerMostrarModalConsentimiento(false);
                  establecerConsentimientoAceptado(false);
                }}
              >
                Mantenerme seguro
              </button>
              <button
                className="boton boton-peligro"
                disabled={!consentimientoAceptado || redireccionando}
                onClick={avanzarBajoConsentimiento}
              >
                {redireccionando ? "Accediendo..." : "Continuar de todos modos"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-bloqueado");
if (elementoRaiz) createRoot(elementoRaiz).render(<ComponenteBloqueado />);
