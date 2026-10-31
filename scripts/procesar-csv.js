import fs from "node:fs";
import path from "node:path";

const RUTA_CSV = path.resolve("domain_classification_dataset.csv");
const RUTA_SALIDA = path.resolve("src/reputacion/malicioso.json");

function normalizarDominio(textoUrl) {
  return textoUrl
    .trim()
    .toLowerCase()
    .replace(/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//, "")
    .replace(/^\/\//, "")
    .replace(/[/?#].*$/, "")
    .replace(/:\d+$/, "");
}

function procesarConjuntoDatos() {
  if (!fs.existsSync(RUTA_CSV)) {
    console.error(`No se encontró el archivo: ${RUTA_CSV}`);
    return;
  }

  const lineas = fs.readFileSync(RUTA_CSV, "utf-8").split(/\r?\n/);
  const dominiosUnicos = new Set();

  for (const linea of lineas) {
    const primeraColumna = linea.trim().split(",")[0]?.replace(/['"]/g, "").trim();
    if (!primeraColumna || ["url", "domain"].includes(primeraColumna.toLowerCase())) {
      continue;
    }

    const dominioLimpio = normalizarDominio(primeraColumna);
    if (dominioLimpio.includes(".")) {
      dominiosUnicos.add(dominioLimpio);
    }
  }

  const listaFinal = Array.from(dominiosUnicos);
  fs.mkdirSync(path.dirname(RUTA_SALIDA), { recursive: true });
  fs.writeFileSync(RUTA_SALIDA, JSON.stringify(listaFinal, null, 2), "utf-8");
  console.log(`Procesamiento exitoso. Total de dominios únicos extraídos: ${listaFinal.length}`);
  console.log(`Guardado en: ${RUTA_SALIDA}`);
}

procesarConjuntoDatos();
