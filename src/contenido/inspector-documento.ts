interface EvidenciaDocumento {
  tieneFormularioSensible: boolean;
  tieneCampoContrasena: boolean;
}

const ATRIBUTOS_VIGILADOS = ["action", "type", "name", "id"];
const CONJUNTO_ATRIBUTOS = new Set(ATRIBUTOS_VIGILADOS);

let evidenciaAnterior: EvidenciaDocumento | null = null;
let temporizador: number | undefined;
let observador: MutationObserver | null = null;

function extensionDisponible(): boolean {
  return typeof chrome !== "undefined" && Boolean(chrome.runtime?.id);
}

function obtenerEvidenciaDocumento(): EvidenciaDocumento {
  let tieneCampoContrasena = false;
  let tieneFormularioSensible = false;

  for (const formulario of Array.from(document.forms)) {
    const tieneContrasena = Boolean(formulario.querySelector('input[type="password"]'));
    const contexto = `${formulario.action} ${formulario.textContent ?? ""}`.toLocaleLowerCase();
    const contextoSensible = /login|ingreso|cuenta|pago|tarjeta|seguridad|verificar/.test(contexto);

    tieneCampoContrasena ||= tieneContrasena;
    tieneFormularioSensible ||= tieneContrasena || contextoSensible;
  }

  return { tieneFormularioSensible, tieneCampoContrasena };
}

function enviarEvidenciaDocumento(): void {
  if (!extensionDisponible()) return;

  const evidencia = obtenerEvidenciaDocumento();
  if (
    evidenciaAnterior &&
    evidenciaAnterior.tieneFormularioSensible === evidencia.tieneFormularioSensible &&
    evidenciaAnterior.tieneCampoContrasena === evidencia.tieneCampoContrasena
  ) {
    return;
  }

  evidenciaAnterior = evidencia;
  void chrome.runtime.sendMessage({ tipo: "ANALIZAR_DOCUMENTO", ...evidencia }).catch(() => undefined);
}

function programarActualizacion(): void {
  if (!extensionDisponible()) {
    observador?.disconnect();
    observador = null;
    return;
  }
  window.clearTimeout(temporizador);
  temporizador = window.setTimeout(enviarEvidenciaDocumento, 250);
}

function mutacionRelevante(mutacion: MutationRecord): boolean {
  if (mutacion.type === "attributes") {
    return Boolean(mutacion.attributeName && CONJUNTO_ATRIBUTOS.has(mutacion.attributeName));
  }
  return Array.from(mutacion.addedNodes).some(
    (nodo) =>
      (nodo.nodeType === Node.ELEMENT_NODE &&
        (nodo as Element).matches("form, input, textarea, select, button, label")) ||
      (nodo.nodeType === Node.TEXT_NODE && Boolean(nodo.textContent?.trim()))
  );
}

function iniciarObservacion(): void {
  if (!extensionDisponible() || !document.documentElement || observador) return;
  enviarEvidenciaDocumento();

  observador = new MutationObserver((mutaciones) => {
    if (mutaciones.some(mutacionRelevante)) {
      programarActualizacion();
    }
  });
  observador.observe(document.documentElement, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ATRIBUTOS_VIGILADOS,
  });
}

iniciarObservacion();
