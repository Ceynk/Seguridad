/**
 * Script de contenido (Content Script) auxiliar para PhishGuard Colombia.
 * Principio de privacidad estricto:
 * - NO lee contraseñas ni campos de formularios (input type=password, credit card, etc.).
 * - NO captura datos personales de la sesión del usuario.
 * - Solo actúa como puente de notificación no invasivo si se requiere advertencia in-situ.
 */

(function inicializarScriptContenido() {
  // Verificación de contexto de extensión activo
  if (typeof chrome === "undefined" || !chrome.runtime || !chrome.runtime.id) {
    return;
  }

  // Notificar al service worker para mantener la consistencia de estado si la pestaña fue recargada
  try {
    const urlActual = window.location.href;
    if (urlActual && !urlActual.startsWith("chrome-extension://")) {
      chrome.runtime.sendMessage({
        tipo: "NOTIFICAR_PAGINA_CARGADA",
        url: urlActual,
      });
    }
  } catch {
    // Silencioso para evitar interferir con páginas web de terceros
  }
})();
