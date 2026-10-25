interface EvidenciaDocumento {
  tieneFormularioSensible: boolean;
  tieneCampoContrasena: boolean;
}

let evidenciaAnterior: EvidenciaDocumento | null = null;
let temporizadorEvidencia: number | undefined;
let observador: MutationObserver | null = null;

function extensionDisponible(): boolean {
  return typeof chrome !== "undefined" && Boolean(chrome.runtime && chrome.runtime.id);
}

function obtenerEvidenciaDocumento(): EvidenciaDocumento {
  let tieneCampoContrasena = false;
  let tieneFormularioSensible = false;

  for (const formulario of Array.from(document.forms)) {
    const tieneContrasena = Boolean(formulario.querySelector('input[type="password"]'));
    const contexto = `${formulario.action} ${formulario.textContent ?? ""}`.toLocaleLowerCase();
    const tieneContextoSensible =
      /login|ingreso|cuenta|pago|tarjeta|seguridad|verificar/.test(contexto);

    tieneCampoContrasena ||= tieneContrasena;
    tieneFormularioSensible ||= tieneContrasena || tieneContextoSensible;
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
  void chrome.runtime
    .sendMessage({ tipo: "ANALIZAR_DOCUMENTO", ...evidencia })
    .catch(() => undefined);
}

function programarActualizacion(): void {
  if (!extensionDisponible()) {
    observador?.disconnect();
    observador = null;
    return;
  }
  window.clearTimeout(temporizadorEvidencia);
  temporizadorEvidencia = window.setTimeout(enviarEvidenciaDocumento, 250);
}

function mutacionPuedeCambiarEvidencia(mutation: MutationRecord): boolean {
  if (mutation.type === "attributes") {
    return mutation.attributeName === "action" ||
      mutation.attributeName === "type" ||
      mutation.attributeName === "name" ||
      mutation.attributeName === "id";
  }

  for (const nodo of Array.from(mutation.addedNodes)) {
    if (
      (nodo.nodeType === Node.ELEMENT_NODE &&
        (nodo as Element).matches("form, input, textarea, select, button, label")) ||
      (nodo.nodeType === Node.TEXT_NODE && Boolean(nodo.textContent?.trim()))
    ) {
      return true;
    }
  }
  return false;
}

function observarCambios(): void {
  if (!extensionDisponible() || !document.documentElement || observador) return;
  enviarEvidenciaDocumento();

  observador = new MutationObserver((mutaciones) => {
    if (mutaciones.some(mutacionPuedeCambiarEvidencia)) {
      programarActualizacion();
    }
  });
  observador.observe(document.documentElement, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ["action", "type", "name", "id"],
  });
}

observarCambios();
