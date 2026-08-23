import { ResultadoSimilitudMarca, SustitucionCaracterVisual } from "../types/analisis";

/**
 * Marcas objetivo monitoreadas comúnmente suplantadas en ataques de phishing en Colombia y globales.
 */
export interface MarcaObjetivo {
  nombre: string;
  dominioLegitimo: string;
  nombresAlternativos: string[];
}

export const MARCAS_OBJETIVO: MarcaObjetivo[] = [
  // Banca y billeteras digitales Colombia
  { nombre: "bancolombia", dominioLegitimo: "bancolombia.com", nombresAlternativos: ["grupo-bancolombia"] },
  { nombre: "nequi", dominioLegitimo: "nequi.com.co", nombresAlternativos: ["nequicolombia"] },
  { nombre: "daviplata", dominioLegitimo: "daviplata.com", nombresAlternativos: ["daviplata-app"] },
  { nombre: "davivienda", dominioLegitimo: "davivienda.com", nombresAlternativos: ["banco-davivienda"] },
  { nombre: "bancodebogota", dominioLegitimo: "bancodebogota.com", nombresAlternativos: ["banco-bogota", "bogotabanco"] },
  { nombre: "bbva", dominioLegitimo: "bbva.com.co", nombresAlternativos: ["bbvacolombia", "banco-bbva"] },
  { nombre: "pse", dominioLegitimo: "pse.com.co", nombresAlternativos: ["pagos-pse", "pagossegurospse"] },
  { nombre: "transfiya", dominioLegitimo: "transfiya.com.co", nombresAlternativos: ["transfi-ya"] },
  { nombre: "scotiabank", dominioLegitimo: "scotiabankcolpatria.com", nombresAlternativos: ["colpatria", "scotiabankcolombia"] },
  { nombre: "bancopopular", dominioLegitimo: "bancopopular.com.co", nombresAlternativos: ["popularcolombia"] },
  { nombre: "bancoagrario", dominioLegitimo: "bancoagrario.gov.co", nombresAlternativos: ["banco-agrario"] },
  { nombre: "bancoavvillas", dominioLegitimo: "avvillas.com.co", nombresAlternativos: ["avvillas", "banco-avvillas"] },
  { nombre: "bancocaja-social", dominioLegitimo: "bancocajasocial.com", nombresAlternativos: ["cajasocial"] },
  { nombre: "lulo-bank", dominioLegitimo: "lulobank.com", nombresAlternativos: ["lulobank"] },
  { nombre: "nu-bank", dominioLegitimo: "nu.com.co", nombresAlternativos: ["nubank", "nubankcolombia"] },

  // Entidades gubernamentales y tributarias Colombia
  { nombre: "dian", dominioLegitimo: "dian.gov.co", nombresAlternativos: ["muisca-dian", "factura-dian"] },
  { nombre: "sisben", dominioLegitimo: "sisben.gov.co", nombresAlternativos: ["sisben-colombia"] },
  { nombre: "procuraduria", dominioLegitimo: "procuraduria.gov.co", nombresAlternativos: [] },
  { nombre: "fiscalia", dominioLegitimo: "fiscalia.gov.co", nombresAlternativos: [] },
  { nombre: "policia", dominioLegitimo: "policia.gov.co", nombresAlternativos: ["ponal"] },
  { nombre: "runt", dominioLegitimo: "runt.gov.co", nombresAlternativos: ["runtcolombia"] },
  { nombre: "simit", dominioLegitimo: "fcm.org.co", nombresAlternativos: ["simit-multas"] },

  // Envíos y paquetería Colombia
  { nombre: "4-72", dominioLegitimo: "4-72.com.co", nombresAlternativos: ["cuatrosietedos", "472colombia", "serviciospostales"] },
  { nombre: "servientrega", dominioLegitimo: "servientrega.com", nombresAlternativos: ["servientrega-rastreo"] },
  { nombre: "envia", dominioLegitimo: "envia.co", nombresAlternativos: ["enviacolombia"] },
  { nombre: "interrapidisimo", dominioLegitimo: "interrapidisimo.com", nombresAlternativos: ["inter-rapidisimo"] },
  { nombre: "coordinadora", dominioLegitimo: "coordinadora.com", nombresAlternativos: ["coordinadora-envios"] },

  // Servicios globales populares frecuentemente suplantados
  { nombre: "google", dominioLegitimo: "google.com", nombresAlternativos: ["gmail"] },
  { nombre: "microsoft", dominioLegitimo: "microsoft.com", nombresAlternativos: ["outlook", "office365", "live"] },
  { nombre: "netflix", dominioLegitimo: "netflix.com", nombresAlternativos: [] },
  { nombre: "paypal", dominioLegitimo: "paypal.com", nombresAlternativos: [] },
  { nombre: "amazon", dominioLegitimo: "amazon.com", nombresAlternativos: [] },
  { nombre: "facebook", dominioLegitimo: "facebook.com", nombresAlternativos: ["meta", "instagram", "whatsapp"] },
  { nombre: "apple", dominioLegitimo: "apple.com", nombresAlternativos: ["icloud"] },
];

/**
 * Tabla de sustituciones comunes de homoglifos / caracteres visualmente engañosos.
 */
export const MAPA_HOMOGLIFOS: Record<string, string> = {
  "0": "o",
  "1": "l",
  "|": "l",
  "!": "i",
  "3": "e",
  "4": "a",
  "@": "a",
  "5": "s",
  "$": "s",
  "7": "t",
  "8": "b",
  "9": "g",
  "vv": "w",
  "rn": "m",
  "cl": "d",
};

/**
 * Calcula la distancia de Levenshtein entre dos cadenas de texto.
 */
export function calcularDistanciaLevenshtein(cadenaA: string, cadenaB: string): number {
  const longitudA = cadenaA.length;
  const longitudB = cadenaB.length;

  if (longitudA === 0) return longitudB;
  if (longitudB === 0) return longitudA;

  const matriz: number[][] = [];

  for (let fila = 0; fila <= longitudA; fila++) {
    matriz[fila] = [fila];
  }

  for (let columna = 0; columna <= longitudB; columna++) {
    matriz[0][columna] = columna;
  }

  for (let fila = 1; fila <= longitudA; fila++) {
    for (let columna = 1; columna <= longitudB; columna++) {
      const costo = cadenaA[fila - 1] === cadenaB[columna - 1] ? 0 : 1;
      matriz[fila][columna] = Math.min(
        matriz[fila - 1][columna] + 1,
        matriz[fila][columna - 1] + 1,
        matriz[fila - 1][columna - 1] + costo
      );
    }
  }

  return matriz[longitudA][longitudB];
}

/**
 * Reemplaza homoglifos conocidos en un texto para revelar la palabra oculta
 * y genera el desglose de posiciones exactas para visualización en modo Rayos X.
 */
export function desofuscarHomoglifos(texto: string): {
  textoDesofuscado: string;
  sustitucionesDetectadas: string[];
  desgloseCaracteres: SustitucionCaracterVisual[];
} {
  let textoResultado = texto.toLowerCase();
  const sustitucionesDetectadas: string[] = [];
  const desgloseCaracteres: SustitucionCaracterVisual[] = [];

  // Mapear sustituciones multi-carácter
  if (textoResultado.includes("vv")) {
    const pos = textoResultado.indexOf("vv");
    textoResultado = textoResultado.replace(/vv/g, "w");
    sustitucionesDetectadas.push("vv -> w");
    desgloseCaracteres.push({
      posicion: pos,
      caracterSospechoso: "vv",
      caracterLegitimo: "w",
      tipo: "Doble carácter por consonante",
    });
  }
  if (textoResultado.includes("rn")) {
    const pos = textoResultado.indexOf("rn");
    textoResultado = textoResultado.replace(/rn/g, "m");
    sustitucionesDetectadas.push("rn -> m");
    desgloseCaracteres.push({
      posicion: pos,
      caracterSospechoso: "rn",
      caracterLegitimo: "m",
      tipo: "Doble carácter por consonante",
    });
  }
  if (textoResultado.includes("cl")) {
    const pos = textoResultado.indexOf("cl");
    textoResultado = textoResultado.replace(/cl/g, "d");
    sustitucionesDetectadas.push("cl -> d");
    desgloseCaracteres.push({
      posicion: pos,
      caracterSospechoso: "cl",
      caracterLegitimo: "d",
      tipo: "Doble carácter por consonante",
    });
  }

  // Mapear caracteres individuales
  for (let i = 0; i < texto.length; i++) {
    const char = texto[i].toLowerCase();
    if (MAPA_HOMOGLIFOS[char]) {
      const charLegitimo = MAPA_HOMOGLIFOS[char];
      desgloseCaracteres.push({
        posicion: i,
        caracterSospechoso: char,
        caracterLegitimo: charLegitimo,
        tipo: `Homoglifo Numérico/Visual (${char} por ${charLegitimo})`,
      });
    }
  }

  for (const [caracterVisual, caracterReal] of Object.entries(MAPA_HOMOGLIFOS)) {
    if (caracterVisual.length === 1 && textoResultado.includes(caracterVisual)) {
      const regex = new RegExp(caracterVisual.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&"), "g");
      textoResultado = textoResultado.replace(regex, caracterReal);
      sustitucionesDetectadas.push(`${caracterVisual} -> ${caracterReal}`);
    }
  }

  return { textoDesofuscado: textoResultado, sustitucionesDetectadas, desgloseCaracteres };
}

/**
 * Extrae las etiquetas o palabras constitutivas de un nombre de host.
 */
export function extraerTokensDeHost(nombreHost: string): string[] {
  return nombreHost
    .toLowerCase()
    .split(/[.\-_]/)
    .filter((token) => token.length >= 2);
}

/**
 * Analiza un nombre de host contra la lista de marcas conocidas para identificar suplantaciones.
 */
export function detectarSimilitudMarcas(
  nombreHost: string,
  dominioPrincipal: string
): ResultadoSimilitudMarca[] {
  const hostMinusculas = nombreHost.toLowerCase();
  const dominioMinusculas = dominioPrincipal.toLowerCase();
  const resultados: ResultadoSimilitudMarca[] = [];

  const { textoDesofuscado, sustitucionesDetectadas, desgloseCaracteres } = desofuscarHomoglifos(hostMinusculas);
  const tokensHost = extraerTokensDeHost(hostMinusculas);
  const tokensDesofuscados = extraerTokensDeHost(textoDesofuscado);

  for (const marca of MARCAS_OBJETIVO) {
    const dominioLegitimoMinusculas = marca.dominioLegitimo.toLowerCase();

    // Si el dominio analizado ES el dominio legítimo (o un subdominio legítimo del mismo), no es suplantación
    if (
      dominioMinusculas === dominioLegitimoMinusculas ||
      hostMinusculas.endsWith(`.${dominioLegitimoMinusculas}`)
    ) {
      continue;
    }

    const nombresAComparar = [marca.nombre, ...marca.nombresAlternativos];

    for (const nombreMarca of nombresAComparar) {
      // 1. Detección por presencia literal de la marca dentro de un dominio diferente (ej: bancolombia-login.xyz)
      if (hostMinusculas.includes(nombreMarca) && !dominioMinusculas.includes(dominioLegitimoMinusculas)) {
        resultados.push({
          marcaObjetivo: marca.nombre,
          dominioOriginal: marca.dominioLegitimo,
          nombreDetectado: nombreMarca,
          distancia: 0,
          similitudPorcentaje: 100,
          tipoSustitucion: ["Nombre de marca incrustado en dominio no oficial"],
          desgloseCaracteres,
        });
        break;
      }

      // 2. Detección por desofuscación de homoglifos (ej: banc0l0mbia -> bancolombia)
      if (
        textoDesofuscado.includes(nombreMarca) &&
        !hostMinusculas.includes(nombreMarca) &&
        !dominioMinusculas.includes(dominioLegitimoMinusculas)
      ) {
        resultados.push({
          marcaObjetivo: marca.nombre,
          dominioOriginal: marca.dominioLegitimo,
          nombreDetectado: textoDesofuscado,
          distancia: 1,
          similitudPorcentaje: 95,
          tipoSustitucion: [...sustitucionesDetectadas, "Uso de homoglifos para imitar marca"],
          desgloseCaracteres,
        });
        break;
      }

      // 3. Detección por similitud Levenshtein en cada token del dominio
      const todosLosTokens = Array.from(new Set([...tokensHost, ...tokensDesofuscados]));

      for (const token of todosLosTokens) {
        if (token.length < 3) continue;

        const distancia = calcularDistanciaLevenshtein(token, nombreMarca);
        const longitudMaxima = Math.max(token.length, nombreMarca.length);
        const similitud = Math.round(((longitudMaxima - distancia) / longitudMaxima) * 100);

        if (distancia > 0 && distancia <= 2 && similitud >= 75 && longitudMaxima >= 4) {
          resultados.push({
            marcaObjetivo: marca.nombre,
            dominioOriginal: marca.dominioLegitimo,
            nombreDetectado: token,
            distancia,
            similitudPorcentaje: similitud,
            tipoSustitucion: [`Typosquatting/Variación tipográfica (${distancia} cambios)`],
            desgloseCaracteres,
          });
          break;
        }
      }
    }
  }

  return resultados;
}
