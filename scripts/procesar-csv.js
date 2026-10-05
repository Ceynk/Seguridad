import fs from "fs";
import path from "path";

// 1. Ruta del CSV de entrada y del JSON de salida
const RUTA_CSV = "D:\\Seguridad\\domain_classification_dataset.csv"; // Cambia al nombre de tu archivo
const RUTA_SALIDA = "D:\\Seguridad\\src\\reputation\\malicioso.json";

function normalizarDominio(url) {
  let limpio = url.trim().toLowerCase();
  limpio = limpio.replace(/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//, "").replace(/^\/\//, "");
  limpio = limpio.replace(/[/?#].*$/, "");
  limpio = limpio.replace(/:\d+$/, "");
  return limpio;
}

function procesarDataset() {
  if (!fs.existsSync(RUTA_CSV)) {
    console.error(`No se encontró el archivo: ${RUTA_CSV}`);
    return;
  }

  const contenido = fs.readFileSync(RUTA_CSV, "utf-8");
  const lineas = contenido.split(/\r?\n/);
  const dominiosUnicos = new Set();

  for (let i = 0; i < lineas.length; i++) {
    const linea = lineas[i].trim();
    if (!linea) continue;

    // Si tiene comas (columnas csv), tomar la primera columna o la columna de URL
    const columnas = linea.split(",");
    const posibleUrl = columnas[0].replace(/['"]/g, "").trim();

    // Ignorar la fila de encabezados (ej: "url", "domain", "link")
    if (posibleUrl.toLowerCase() === "url" || posibleUrl.toLowerCase() === "domain") {
      continue;
    }

    const dominioLimpio = normalizarDominio(posibleUrl);
    // Filtrar cadenas válidas con al menos un punto (ej: algo.xyz)
    if (dominioLimpio && dominioLimpio.includes(".")) {
      dominiosUnicos.add(dominioLimpio);
    }
  }

  const listaFinal = Array.from(dominiosUnicos);

  // Asegurar que exista la carpeta src/data
  const dirSalida = path.dirname(RUTA_SALIDA);
  if (!fs.existsSync(dirSalida)) {
    fs.mkdirSync(dirSalida, { recursive: true });
  }

  fs.writeFileSync(RUTA_SALIDA, JSON.stringify(listaFinal, null, 2), "utf-8");
  console.log(`¡Procesamiento exitoso!`);
  console.log(`Total de dominios maliciosos únicos extraídos: ${listaFinal.length}`);
  console.log(`Guardado en: ${RUTA_SALIDA}`);
}

procesarDataset();