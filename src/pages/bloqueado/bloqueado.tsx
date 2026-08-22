import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { ShieldAlert, ArrowLeft, Flag, CheckCircle, Info } from "lucide-react";
import "./bloqueado.css";

interface MotivoBloqueo {
  nombre: string;
  descripcion: string;
  puntuacion: number;
  severidad: string;
}

export const ComponenteBloqueado: React.FC = () => {
  // Parsear parámetros de la URL de forma segura
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
        nombre: "Actividad Sospechosa",
        descripcion: "El sitio web presenta indicadores de alto riesgo compatibles con fraude.",
        puntuacion: puntuacionRiesgo,
        severidad: "CRITICA",
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
    // Guardar reporte localmente / notificar
    setReporteEnviado(true);
    setMostrarReporte(false);
  };

  return (
    <div className="contenedor-bloqueo">
      {/* Franja superior de alerta */}
      <header className="franja-alerta">
        <ShieldAlert className="icono-alerta-grande" size={44} />
        <div className="titulos-alerta">
          <h1>Sitio Bloqueado por Seguridad</h1>
          <p>Detectamos señales compatibles con phishing o fraude.</p>
        </div>
      </header>

      {/* Cuerpo explicativo */}
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
        </div>

        <div className="seccion-dato">
          <span className="etiqueta-dato">Motivos Técnicos de la Detección</span>
          <div className="lista-motivos">
            {motivos.length > 0 ? (
              motivos.map((motivo, indice) => (
                <div key={indice} className="item-motivo">
                  <div className="titulo-motivo">
                    <span>{motivo.nombre}</span>
                    <span style={{ color: "#ef4444", fontSize: "11px" }}>
                      +{motivo.puntuacion} pts ({motivo.severidad})
                    </span>
                  </div>
                  <p className="descripcion-motivo">{motivo.descripcion}</p>
                </div>
              ))
            ) : (
              <div className="item-motivo">
                <div className="titulo-motivo">Coincidencia en Base de Amenazas</div>
                <p className="descripcion-motivo">
                  El dominio coincide con patrones conocidos de páginas fraudulentas dirigidas a usuarios en Colombia.
                </p>
              </div>
            )}
          </div>
        </div>

        <div className="aviso-educativo">
          <div style={{ display: "flex", gap: "8px", alignItems: "flex-start" }}>
            <Info size={18} style={{ flexShrink: 0, marginTop: "2px" }} />
            <div>
              <strong>¿Por qué se bloqueó esta página?</strong>
              <br />
              Los atacantes suelen crear páginas web con nombres muy parecidos a bancos, plataformas de pagos (como PSE o Nequi) o entidades del gobierno para intentar robar contraseñas, números de tarjeta o información personal.
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
            Gracias. Tu reporte de falso positivo ha sido registrado para revisión.
          </div>
        )}

        {mostrarReporte && !reporteEnviado && (
          <form onSubmit={enviarReporteFalsoPositivo} className="formulario-reporte">
            <span className="etiqueta-dato">Reportar Falso Positivo</span>
            <p style={{ fontSize: "12px", color: "#9ca3af" }}>
              Si estás seguro de que {nombreHost} es un sitio legítimo oficial, indícanos por qué:
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

        {/* Acciones principales */}
        <footer className="acciones-bloqueo">
          <button className="boton-principal" onClick={regresarASeguridad}>
            <ArrowLeft size={16} />
            Volver a un Sitio Seguro
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
