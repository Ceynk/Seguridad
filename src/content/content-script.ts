declare const chrome: any;

let temporizador: any = null;

function analizarEnlaces() {
  if (typeof chrome === "undefined" || !chrome.runtime || !chrome.runtime.id) return;
  
  const enlaces = document.getElementsByTagName("a");
  
  for (let i = 0; i < enlaces.length; i++) {
    const enlace = enlaces[i];
    const url = enlace.href;
    
    if (url && url.startsWith("http") && !enlace.hasAttribute("data-analizado")) {
      enlace.setAttribute("data-analizado", "true");
      
      chrome.runtime.sendMessage({ tipo: "ANALIZAR_ENLACE", url: url }, (respuesta: any) => {
        if (respuesta && respuesta.datos) {
          if (respuesta.datos.clasificacion === "MALICIOSO") {
            enlace.style.border = "2px solid red";
            enlace.style.backgroundColor = "#ffcccc";
            enlace.style.color = "red";
            enlace.title = "Enlace Malicioso";
          } else if (respuesta.datos.clasificacion === "SOSPECHOSO") {
            enlace.style.border = "2px solid orange";
            enlace.style.backgroundColor = "#ffebcc";
            enlace.title = "Enlace Sospechoso";
          }
        }
      });
    }
  }
}

analizarEnlaces();

const observador = new MutationObserver(() => {
  if (temporizador) clearTimeout(temporizador);
  temporizador = setTimeout(() => {
    analizarEnlaces();
  }, 500);
});

observador.observe(document.body, { childList: true, subtree: true });
