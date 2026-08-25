import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { ShieldAlert, ArrowLeft, Flag, CheckCircle, Info, Download } from "lucide-react";
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
        {
          id: "T1036.007",
          nombre: "Masquerading: Homoglyph Attack",
          tactica: "Defense Evasion",
          enlace: "https://attack.mitre.org/techniques/T1036/007/",
        },
      ],
      vectoresIngenieriaSocial: [
        {
          principioCoercion: "AUTORIDAD_INSTITUCIONAL",
          titulo: "Suplantación de Entidad",
          descripcion: "Intento de engaño aprovechando la confianza en marcas establecidas.",
          terminosIdentificados: ["banco", "portal", "seguridad"],
        },
      ],
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
      recomendacionRespuesta: `1. Aislar sesión.\n2. Notificar a CSIRT.\n3. Aplicar regla perimetral de bloqueo en DNS.`,
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
    <div className="contenedor-bloqueo">
      <header className="franja-alerta">
        <ShieldAlert className="icono-alerta-grande" size={44} />
        <div className="titulos-alerta">
          <h1>Sitio Bloqueado por Seguridad</h1>
          <p>Se detectaron señales compatibles con suplantación de identidad o fraude.</p>
        </div>
      </header>

      <main className="cuerpo-bloqueo">
        <div className="seccion-dato">
          <span className="etiqueta-dato">Dirección Web Analizada</span>
          <div className="caja-url">{urlBloqueada}</div>
        </div>

        <div className="fila-metricas-riesgo">
          <div className="tarjeta-metrica">
            <span className="etiqueta-dato">Nivel de Riesgo</span>
            <div className="valor-metrica valor-rojo">MALICIOSO</div>
          </div>
          <div className="tarjeta-metrica">
            <span className="etiqueta-dato">Índice Calculado</span>
            <div className="valor-metrica valor-rojo">{puntuacionRiesgo} / 100</div>
          </div>
          <div className="tarjeta-metrica">
            <span className="etiqueta-dato">MITRE ATT&CK</span>
            <div className="valor-metrica" style={{ fontSize: "14px", color: "#60a5fa", marginTop: "4px" }}>
              T1566.002
            </div>
          </div>
        </div>

        <div className="seccion-dato">
          <span className="etiqueta-dato">Motivos Técnicos de la Detección</span>
          <div className="lista-motivos">
            {motivos.map((motivo, indice) => (
              <div key={indice} className="item-motivo">
                <div className="titulo-motivo">
                  <span>{motivo.nombre}</span>
                  <span style={{ color: "#ef4444", fontSize: "11px" }}>
                    +{motivo.puntuacion} pts ({motivo.severidad})
                  </span>
                </div>
                <p className="descripcion-motivo">{motivo.descripcion}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="aviso-educativo">
          <div style={{ display: "flex", gap: "8px", alignItems: "flex-start" }}>
            <Info size={18} style={{ flexShrink: 0, marginTop: "2px" }} />
            <div>
              <strong>Motivo de bloqueo preventivo:</strong>
              <br />
              El sitio utiliza un nombre o estructura engañosa similar a entidades conocidas (bancos, pasarelas de pago o portales públicos) para intentar capturar contraseñas o datos financieros.
            </div>
          </div>
        </div>

        {reporteEnviado && (
          <div
            style={{
              backgroundColor: "rgba(16, 185, 129, 0.15)",
              border: "1px solid #10b981",
              borderRadius: "8px",
              padding: "12px",
              display: "flex",
              alignItems: "center",
              gap: "8px",
              color: "#34d399",
              fontSize: "12px",
            }}
          >
            <CheckCircle size={16} />
            El reporte ha sido registrado para revisión.
          </div>
        )}

        {mostrarReporte && !reporteEnviado && (
          <form onSubmit={enviarReporteFalsoPositivo} className="formulario-reporte">
            <span className="etiqueta-dato">Reportar Falso Positivo</span>
            <p style={{ fontSize: "12px", color: "#9ca3af" }}>
              Si confirmas que {nombreHost} es un sitio legítimo, indícanos el motivo:
            </p>
            <textarea
              className="textarea-reporte"
              rows={3}
              placeholder="Explica brevemente por qué consideras que este sitio es seguro..."
              value={comentarioReporte}
              onChange={(e) => setComentarioReporte(e.target.value)}
              required
            />
            <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end" }}>
              <button
                type="button"
                className="boton-enlace"
                onClick={() => setMostrarReporte(false)}
              >
                Cancelar
              </button>
              <button type="submit" className="boton-principal">
                Enviar Reporte
              </button>
            </div>
          </form>
        )}

        <footer className="acciones-bloqueo">
          <button className="boton-principal" onClick={regresarASeguridad}>
            <ArrowLeft size={16} />
            Volver a un Sitio Seguro
          </button>

          <div style={{ display: "flex", gap: "8px" }}>
            <button
              className="boton-principal"
              style={{ backgroundColor: "#1e293b", border: "1px solid #374151" }}
              onClick={descargarReporteForense}
              title="Descargar Ficha Técnica de Incidente"
            >
              <Download size={14} /> Ficha Forense (.md)
            </button>

            {!mostrarReporte && !reporteEnviado && (
              <button
                className="boton-enlace"
                onClick={() => setMostrarReporte(true)}
              >
                <Flag size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: "4px" }} />
                Reportar Falso Positivo
              </button>
            )}
          </div>
        </footer>
      </main>
    </div>
  );
};

const elementoRaiz = document.getElementById("raiz-bloqueado");
if (elementoRaiz) {
  const raiz = createRoot(elementoRaiz);
  raiz.render(<ComponenteBloqueado />);
}
