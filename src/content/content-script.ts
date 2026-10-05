declare const chrome: {
  runtime?: {
    id?: string;
    sendMessage: (message: { tipo: string; url: string }) => void;
  };
};

(function inicializarScriptContenido() {
  if (typeof chrome === "undefined" || !chrome.runtime || !chrome.runtime.id) {
    return;
  }

  try {
    const urlActual = window.location.href;
    if (urlActual && !urlActual.startsWith("chrome-extension://")) {
      chrome.runtime.sendMessage({
        tipo: "NOTIFICAR_PAGINA_CARGADA",
        url: urlActual,
      });
    }
  } catch {
  }
})();
