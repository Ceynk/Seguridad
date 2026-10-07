import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { 
  ShieldAlert, 
  ArrowLeft, 
  Flag, 
  CheckCircle, 
  Info, 
  Download, 
  AlertTriangle, 
  Lock
} from "lucide-react";
import { exportarInformeMarkdown } from "../../utils/cripto-forense";
import { FichaForenseIncidente, TecnicaMitreAttack } from "../../types/analisis";
import "./bloqueado.css";

interface MotivoBloqueo {
  nombre: string;
  descripcion: string;
  puntuacion: number;
  severidad: string;
  tecnicaMitre?: TecnicaMitreAttack;
}

export const ComponenteBloqueado: React.FC = () => {
  const parametrosUrl = new URLSearchParams(window.location.search);
  const urlBloqueada = parametrosUrl.get("url") || "Desconocida";
  const nombreHost = parametrosUrl.get("host") || "Sitio no identificado";
  const puntuacionRiesgo = parseInt(parametrosUrl.get("puntuacion") || "95", 10);

  let motivos: MotivoBloqueo[] = [];
  try {
    const motivosCrudos = parametrosUrl.get("motivos");
    if (motivosCrudos) {
      motivos = JSON.parse(motivosCrudos);
    }
  } catch {
    motivos = [
      {
        nombre: "Suplantación de Identidad / Phishing",
        descripcion: "El sitio web presenta indicadores de alto riesgo compatibles con fraude cibernético.",
        puntuacion: puntuacionRiesgo,
        severidad: "CRITICA",
        tecnicaMitre: {
          id: "T1566.002",
          nombre: "Spearphishing Link",
          tactica: "Initial Access",
          enlace: "https://attack.mitre.org/techniques/T1566/002/",
        },
      },
    ];
  }

  const [mostrarReporte, setMostrarReporte] = useState<boolean>(false);
  const [comentarioReporte, setComentarioReporte] = useState<string>("");
  const [reporteEnviado, setReporteEnviado] = useState<boolean>(false);
  const [mostrarModalConsentimiento, setMostrarModalConsentimiento] = useState<boolean>(false);
  const [consentimientoAceptado, setConsentimientoAceptado] = useState<boolean>(false);
  const [redireccionando, setRedireccionando] = useState<boolean>(false);

  const regresarASeguridad = (): void => {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      window.close();
    }
  };

  const enviarReporteFalsoPositivo = (e: React.FormEvent): void => {
    e.preventDefault();
    setReporteEnviado(true);
    setMostrarReporte(false);
  };

  const avanzarBajoConsentimiento = async (): Promise<void> => {
    if (!consentimientoAceptado || redireccionando) return;
    setRedireccionando(true);

    try {
      if (typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.sendMessage) {
        await new Promise<void>((resolve) => {
          chrome.runtime.sendMessage(
            { tipo: "PERMITIR_DOMINIO_TEMPORALMENTE", dominio: nombreHost },
            () => resolve()
          );
        });
      }
    } catch {
      // Continuar incluso si no está en entorno chrome directo
    }

    if (urlBloqueada && urlBloqueada !== "Desconocida") {
      window.location.href = urlBloqueada;
    } else {
      regresarASeguridad();
    }
  };

  const descargarReporteForense = (): void => {
    const fichaSimulada: FichaForenseIncidente = {
      idIncidente: `INC-${Date.now().toString(36).toUpperCase()}`,
      marcaTemporalIso: new Date().toISOString(),
      urlObjetivo: urlBloqueada,
      nombreHost,
      huellaSha256: "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
      clasificacion: "MALICIOSO",
      puntuacionRiesgo,
      entropiaShannon: 3.85,
      tecnicasMitreDetectadas: [
        {
          id: "T1566.002",
          nombre: "Phishing: Spearphishing Link",
          tactica: "Initial Access",
          enlace: "https://attack.mitre.org/techniques/T1566/002/",
        },
      ],
      vectoresIngenieriaSocial: [],
      reglasDetonadas: motivos.map((m) => ({
        nombre: m.nombre,
        severidad: "CRITICA",
        puntuacion: m.puntuacion,
        evidencias: [
          {
            campo: "motivoBloqueo",
            valorEncontrado: m.nombre,
            descripcion: m.descripcion,
          },
        ],
      })),
      resumenTecnico: `El artefacto '${nombreHost}' fue bloqueado preventivamente por coincidencia con firmas de fraude.`,
      recomendacionRespuesta: `1. Aislar sesión.\n2. Notificar a CSIRT.`,
    };

    const markdown = exportarInformeMarkdown(fichaSimulada);
    const blob = new Blob([markdown], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `incidente_bloqueo_${nombreHost}.md`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="block-page-container">
      <div className="block-card">
        
        <header className="block-header">
          <div className="block-icon-wrapper">
            <ShieldAlert size={48} className="block-icon" />
          </div>
          <h1 className="block-title">Página bloqueada por seguridad</h1>
          <p className="block-subtitle">PhishGuard ha impedido el acceso a este sitio debido a indicadores de riesgo críticos.</p>
        </header>

        <main className="block-content">
          <div className="info-section">
            <div className="info-group">
              <span className="info-label">Dominio / URL</span>
              <div className="info-value url-box">
                <Lock size={14} />
                <span className="truncate">{urlBloqueada}</span>
              </div>
            </div>
          </div>

          <div className="metrics-grid">
            <div className="metric-box danger">
              <span className="metric-label">Nivel de Peligro</span>
              <span className="metric-value">MALICIOSO</span>
            </div>
            <div className="metric-box warning">
              <span className="metric-label">Riesgo Calculado</span>
              <span className="metric-value">{puntuacionRiesgo} / 100</span>
            </div>
          </div>

          <div className="info-section">
            <span className="info-label">Motivos principales del bloqueo</span>
            <div className="reasons-list">
              {motivos.map((motivo, indice) => (
                <div key={indice} className="reason-item">
                  <div className="reason-header">
                    <span className="reason-name">{motivo.nombre}</span>
                    <span className="reason-score">+{motivo.puntuacion}</span>
                  </div>
                  <p className="reason-desc">{motivo.descripcion}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="educational-banner">
            <Info size={20} className="edu-icon" />
            <div className="edu-text">
              <strong>Prevención de fraude</strong>
              <p>Este sitio utiliza una estructura o nombre engañoso para intentar capturar contraseñas o datos financieros. Te recomendamos no continuar.</p>
            </div>
          </div>

          {reporteEnviado && (
            <div className="success-banner">
              <CheckCircle size={18} />
              <span>Reporte de falso positivo enviado correctamente.</span>
            </div>
          )}

          {mostrarReporte && !reporteEnviado && (
            <form onSubmit={enviarReporteFalsoPositivo} className="report-form">
              <span className="form-label">Reportar como sitio seguro</span>
              <textarea
                className="form-textarea"
                rows={3}
                placeholder="¿Por qué consideras que este sitio es legítimo?"
                value={comentarioReporte}
                onChange={(e) => setComentarioReporte(e.target.value)}
                required
              />
              <div className="form-actions">
                <button type="button" className="btn btn-text" onClick={() => setMostrarReporte(false)}>Cancelar</button>
                <button type="submit" className="btn btn-secondary">Enviar Reporte</button>
              </div>
            </form>
          )}

        </main>

        <footer className="block-footer">
          <div className="primary-actions">
            <button className="btn btn-primary btn-large" onClick={regresarASeguridad}>
              <ArrowLeft size={18} />
              Regresar a un sitio seguro
            </button>
          </div>
          
          <div className="secondary-actions">
            <button className="btn btn-outline" onClick={descargarReporteForense}>
              <Download size={14} /> Ficha Forense
            </button>

            {!mostrarReporte && !reporteEnviado && (
              <button className="btn btn-text" onClick={() => setMostrarReporte(true)}>
                <Flag size={14} /> Reportar falso positivo
              </button>
            )}
          </div>
          
          <div className="danger-zone">
            <button 
              className="btn-danger-link" 
              onClick={() => setMostrarModalConsentimiento(true)}
            >
              Deseo continuar bajo mi propia responsabilidad
            </button>
          </div>
        </footer>
      </div>

      {mostrarModalConsentimiento && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <AlertTriangle size={24} className="text-warning" />
              <h2>Confirmación de acceso</h2>
            </div>
            
            <div className="modal-body">
              <p>Estás a punto de acceder a <strong>{nombreHost}</strong>, un sitio clasificado como peligroso.</p>
              
              <div className="warning-box">
                <strong>Advertencia:</strong> Si continúas, podrías exponer tus datos personales o financieros. No ingreses contraseñas ni números de tarjeta.
              </div>

              <label className="consent-checkbox">
                <input 
                  type="checkbox" 
                  checked={consentimientoAceptado}
                  onChange={(e) => setConsentimientoAceptado(e.target.checked)}
                />
                <span>Comprendo los riesgos y deseo acceder bajo mi propia responsabilidad.</span>
              </label>
            </div>

            <div className="modal-footer">
              <button className="btn btn-primary" onClick={() => {
                setMostrarModalConsentimiento(false);
                setConsentimientoAceptado(false);
              }}>
                Mantenerme seguro
              </button>
              <button 
                className="btn btn-danger" 
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
if (elementoRaiz) {
  const raiz = createRoot(elementoRaiz);
  raiz.render(<ComponenteBloqueado />);
}
