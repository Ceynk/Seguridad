import { ResultadoSimilitudMarca, SustitucionCaracterVisual } from "../types/analisis";

export interface MarcaObjetivo {
  nombre: string;
  dominioLegitimo: string;
  nombresAlternativos: string[];
}

export const MARCAS_OBJETIVO: MarcaObjetivo[] = [
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

  { nombre: "dian", dominioLegitimo: "dian.gov.co", nombresAlternativos: ["muisca-dian", "factura-dian"] },
  { nombre: "sisben", dominioLegitimo: "sisben.gov.co", nombresAlternativos: ["sisben-colombia"] },
  { nombre: "procuraduria", dominioLegitimo: "procuraduria.gov.co", nombresAlternativos: [] },
  { nombre: "fiscalia", dominioLegitimo: "fiscalia.gov.co", nombresAlternativos: [] },
  { nombre: "policia", dominioLegitimo: "policia.gov.co", nombresAlternativos: ["ponal"] },
  { nombre: "runt", dominioLegitimo: "runt.gov.co", nombresAlternativos: ["runtcolombia"] },
  { nombre: "simit", dominioLegitimo: "fcm.org.co", nombresAlternativos: ["simit-multas"] },

  { nombre: "4-72", dominioLegitimo: "4-72.com.co", nombresAlternativos: ["cuatrosietedos", "472colombia", "serviciospostales"] },
  { nombre: "servientrega", dominioLegitimo: "servientrega.com", nombresAlternativos: ["servientrega-rastreo"] },
  { nombre: "envia", dominioLegitimo: "envia.co", nombresAlternativos: ["enviacolombia"] },
  { nombre: "interrapidisimo", dominioLegitimo: "interrapidisimo.com", nombresAlternativos: ["inter-rapidisimo"] },
  { nombre: "coordinadora", dominioLegitimo: "coordinadora.com", nombresAlternativos: ["coordinadora-envios"] },

  { nombre: "google", dominioLegitimo: "google.com", nombresAlternativos: ["gmail"] },
  { nombre: "microsoft", dominioLegitimo: "microsoft.com", nombresAlternativos: ["outlook", "office365", "live"] },
  { nombre: "netflix", dominioLegitimo: "netflix.com", nombresAlternativos: [] },
  { nombre: "paypal", dominioLegitimo: "paypal.com", nombresAlternativos: [] },
  { nombre: "amazon", dominioLegitimo: "amazon.com", nombresAlternativos: [] },
  { nombre: "facebook", dominioLegitimo: "facebook.com", nombresAlternativos: ["meta"] },
  { nombre: "instagram", dominioLegitimo: "instagram.com", nombresAlternativos: ["meta"] },
  { nombre: "whatsapp", dominioLegitimo: "whatsapp.com", nombresAlternativos: ["meta"] },
  { nombre: "apple", dominioLegitimo: "apple.com", nombresAlternativos: ["icloud"] },
];

export const MAPA_HOMOGLIFOS_UNICODE: Record<string, { equivalente: string; origen: string }> = {
  // Cirílico minúsculas
  "\u0430": { equivalente: "a", origen: "Cirílico (U+0430)" },
  "\u0435": { equivalente: "e", origen: "Cirílico (U+0435)" },
  "\u043E": { equivalente: "o", origen: "Cirílico (U+043E)" },
  "\u0440": { equivalente: "p", origen: "Cirílico (U+0440)" },
  "\u0441": { equivalente: "c", origen: "Cirílico (U+0441)" },
  "\u0443": { equivalente: "y", origen: "Cirílico (U+0443)" },
  "\u0445": { equivalente: "x", origen: "Cirílico (U+0445)" },
  "\u0456": { equivalente: "i", origen: "Cirílico (U+0456)" },
  "\u0458": { equivalente: "j", origen: "Cirílico (U+0458)" },
  "\u0455": { equivalente: "s", origen: "Cirílico (U+0455)" },
  "\u0432": { equivalente: "b", origen: "Cirílico (U+0432)" },
  "\u043C": { equivalente: "m", origen: "Cirílico (U+043C)" },
  "\u043D": { equivalente: "h", origen: "Cirílico (U+043D)" },
  "\u0442": { equivalente: "t", origen: "Cirílico (U+0442)" },
  "\u0448": { equivalente: "w", origen: "Cirílico (U+0448)" },
  "\u0449": { equivalente: "w", origen: "Cirílico (U+0449)" },
  "\u043F": { equivalente: "n", origen: "Cirílico (U+043F)" },
  "\u0434": { equivalente: "d", origen: "Cirílico (U+0434)" },

  // Cirílico mayúsculas
  "\u0410": { equivalente: "a", origen: "Cirílico Mayúscula (U+0410)" },
  "\u0412": { equivalente: "b", origen: "Cirílico Mayúscula (U+0412)" },
  "\u0415": { equivalente: "e", origen: "Cirílico Mayúscula (U+0415)" },
  "\u041A": { equivalente: "k", origen: "Cirílico Mayúscula (U+041A)" },
  "\u041C": { equivalente: "m", origen: "Cirílico Mayúscula (U+041C)" },
  "\u041D": { equivalente: "h", origen: "Cirílico Mayúscula (U+041D)" },
  "\u041E": { equivalente: "o", origen: "Cirílico Mayúscula (U+041E)" },
  "\u0420": { equivalente: "p", origen: "Cirílico Mayúscula (U+0420)" },
  "\u0421": { equivalente: "c", origen: "Cirílico Mayúscula (U+0421)" },
  "\u0422": { equivalente: "t", origen: "Cirílico Mayúscula (U+0422)" },
  "\u0423": { equivalente: "y", origen: "Cirílico Mayúscula (U+0423)" },
  "\u0425": { equivalente: "x", origen: "Cirílico Mayúscula (U+0425)" },
  "\u0406": { equivalente: "i", origen: "Cirílico Mayúscula (U+0406)" },
  "\u0408": { equivalente: "j", origen: "Cirílico Mayúscula (U+0408)" },

  // Griego minúsculas y mayúsculas
  "\u03B1": { equivalente: "a", origen: "Griego (U+03B1)" },
  "\u03B5": { equivalente: "e", origen: "Griego (U+03B5)" },
  "\u03B9": { equivalente: "i", origen: "Griego (U+03B9)" },
  "\u03BA": { equivalente: "k", origen: "Griego (U+03BA)" },
  "\u03BF": { equivalente: "o", origen: "Griego (U+03BF)" },
  "\u03C1": { equivalente: "p", origen: "Griego (U+03C1)" },
  "\u03C5": { equivalente: "u", origen: "Griego (U+03C5)" },
  "\u03BD": { equivalente: "v", origen: "Griego (U+03BD)" },
  "\u03C9": { equivalente: "w", origen: "Griego (U+03C9)" },
  "\u0391": { equivalente: "a", origen: "Griego Mayúscula (U+0391)" },
  "\u0392": { equivalente: "b", origen: "Griego Mayúscula (U+0392)" },
  "\u0395": { equivalente: "e", origen: "Griego Mayúscula (U+0395)" },
  "\u039F": { equivalente: "o", origen: "Griego Mayúscula (U+039F)" },
  "\u03A1": { equivalente: "p", origen: "Griego Mayúscula (U+03A1)" },
  "\u03A4": { equivalente: "t", origen: "Griego Mayúscula (U+03A4)" },
  "\u03A7": { equivalente: "x", origen: "Griego Mayúscula (U+03A7)" },

  // Latinos confusables con diacríticos o formas variantes
  "\u0105": { equivalente: "a", origen: "Latino con colita (U+0105)" },
  "\u0101": { equivalente: "a", origen: "Latino con macrón (U+0101)" },
  "\u0113": { equivalente: "e", origen: "Latino con macrón (U+0113)" },
  "\u0117": { equivalente: "e", origen: "Latino con punto (U+0117)" },
  "\u0119": { equivalente: "e", origen: "Latino con colita (U+0119)" },
  "\u012B": { equivalente: "i", origen: "Latino con macrón (U+012B)" },
  "\u0131": { equivalente: "i", origen: "Latino sin punto (U+0131)" },
  "\u014D": { equivalente: "o", origen: "Latino con macrón (U+014D)" },
  "\u0142": { equivalente: "l", origen: "Latino con barra (U+0142)" },
  "\u0251": { equivalente: "a", origen: "Alfa Latina (U+0251)" },
  "\u0261": { equivalente: "g", origen: "G Latina de guión (U+0261)" },
};

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

// Inclusión de caracteres unicode en MAPA_HOMOGLIFOS para compatibilidad directa
for (const [charUnicode, info] of Object.entries(MAPA_HOMOGLIFOS_UNICODE)) {
  MAPA_HOMOGLIFOS[charUnicode] = info.equivalente;
}

// Decodificador Punycode RFC 3492
const PUNY_TMIN = 1;
const PUNY_TMAX = 26;
const PUNY_SKEW = 38;
const PUNY_DAMP = 700;
const PUNY_INITIAL_BIAS = 72;
const PUNY_INITIAL_N = 128;
const PUNY_DELIMITER = "-";

function adaptarPunycode(delta: number, numpuntos: number, primeraVez: boolean): number {
  delta = primeraVez ? Math.floor(delta / PUNY_DAMP) : delta >> 1;
  delta += Math.floor(delta / numpuntos);
  let k = 0;
  while (delta > (((36 - PUNY_TMIN) * PUNY_TMAX) >> 1)) {
    delta = Math.floor(delta / (36 - PUNY_TMIN));
    k += 36;
  }
  return Math.floor(k + ((36 - PUNY_TMIN + 1) * delta) / (delta + PUNY_SKEW));
}

export function decodificarEtiquetaPunycode(etiqueta: string): string {
  const etiquetaMinusculas = etiqueta.toLowerCase();
  if (!etiquetaMinusculas.startsWith("xn--")) {
    return etiqueta;
  }

  const entrada = etiquetaMinusculas.slice(4);
  const indiceDelimitador = entrada.lastIndexOf(PUNY_DELIMITER);
  const salida: number[] = [];
  let n = PUNY_INITIAL_N;
  let i = 0;
  let bias = PUNY_INITIAL_BIAS;

  if (indiceDelimitador > 0) {
    for (let j = 0; j < indiceDelimitador; j++) {
      salida.push(entrada.charCodeAt(j));
    }
  }

  let inIdx = indiceDelimitador > 0 ? indiceDelimitador + 1 : 0;
  while (inIdx < entrada.length) {
    const iAnterior = i;
    let w = 1;
    let k = 36;
    while (true) {
      if (inIdx >= entrada.length) return etiqueta;
      const c = entrada.charCodeAt(inIdx++);
      let digito: number;
      if (c >= 48 && c <= 57) digito = c - 22;
      else if (c >= 97 && c <= 122) digito = c - 97;
      else return etiqueta;

      i += digito * w;
      const t = k <= bias ? PUNY_TMIN : k >= bias + PUNY_TMAX ? PUNY_TMAX : k - bias;
      if (digito < t) break;
      w *= 36 - t;
      k += 36;
    }
    bias = adaptarPunycode(i - iAnterior, salida.length + 1, iAnterior === 0);
    n += Math.floor(i / (salida.length + 1));
    i %= salida.length + 1;
    salida.splice(i, 0, n);
    i++;
  }

  return String.fromCodePoint(...salida);
}

export function decodificarDominioPunycode(nombreHost: string): string {
  if (!nombreHost.toLowerCase().includes("xn--")) {
    return nombreHost;
  }
  return nombreHost
    .split(".")
    .map((parte) => decodificarEtiquetaPunycode(parte))
    .join(".");
}

export function detectarLetrasRarasYAlfabetoMixto(nombreHost: string): {
  tieneLetrasRaras: boolean;
  tieneAlfabetoMixto: boolean;
  desglose: SustitucionCaracterVisual[];
  hostUnicode: string;
} {
  const hostUnicode = decodificarDominioPunycode(nombreHost);
  const desglose: SustitucionCaracterVisual[] = [];
  let tieneAlfabetoMixto = false;

  const partes = hostUnicode.split(".");

  for (const parte of partes) {
    let tieneLatin = false;
    let tieneNoLatinConfusable = false;

    for (let pos = 0; pos < parte.length; pos++) {
      const char = parte[pos];
      const charMinuscula = char.toLowerCase();
      const code = char.codePointAt(0) || 0;

      // Letra latina básica
      if ((code >= 97 && code <= 122) || (code >= 65 && code <= 90)) {
        tieneLatin = true;
      }

      // Revisar si es confusable Unicode
      if (MAPA_HOMOGLIFOS_UNICODE[char] || MAPA_HOMOGLIFOS_UNICODE[charMinuscula]) {
        const info = MAPA_HOMOGLIFOS_UNICODE[char] || MAPA_HOMOGLIFOS_UNICODE[charMinuscula];
        tieneNoLatinConfusable = true;
        desglose.push({
          posicion: pos,
          caracterSospechoso: char,
          caracterLegitimo: info.equivalente,
          tipo: `Letra confusable ${info.origen} que aparenta ser '${info.equivalente}'`,
        });
      }
    }

    if (tieneLatin && tieneNoLatinConfusable) {
      tieneAlfabetoMixto = true;
    }
  }

  return {
    tieneLetrasRaras: desglose.length > 0,
    tieneAlfabetoMixto,
    desglose,
    hostUnicode,
  };
}

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

export function desofuscarHomoglifos(texto: string): {
  textoDesofuscado: string;
  sustitucionesDetectadas: string[];
  desgloseCaracteres: SustitucionCaracterVisual[];
} {
  // Primero decodificar punycode si aplica
  const textoUnicode = decodificarDominioPunycode(texto);
  let textoResultado = textoUnicode.toLowerCase();
  const sustitucionesDetectadas: string[] = [];
  const desgloseCaracteres: SustitucionCaracterVisual[] = [];

  if (textoResultado.includes("vv")) {
    const pos = textoResultado.indexOf("vv");
    textoResultado = textoResultado.replace(/vv/g, "w");
    sustitucionesDetectadas.push("vv -> w");
    desgloseCaracteres.push({
      posicion: pos,
      caracterSospechoso: "vv",
      caracterLegitimo: "w",
      tipo: "Doble carácter por consonante (vv por w)",
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
      tipo: "Doble carácter por consonante (rn por m)",
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
      tipo: "Doble carácter por consonante (cl por d)",
    });
  }

  for (let i = 0; i < textoUnicode.length; i++) {
    const char = textoUnicode[i];
    const charMin = char.toLowerCase();

    if (MAPA_HOMOGLIFOS_UNICODE[char] || MAPA_HOMOGLIFOS_UNICODE[charMin]) {
      const info = MAPA_HOMOGLIFOS_UNICODE[char] || MAPA_HOMOGLIFOS_UNICODE[charMin];
      desgloseCaracteres.push({
        posicion: i,
        caracterSospechoso: char,
        caracterLegitimo: info.equivalente,
        tipo: `Letra confusable ${info.origen} que simula '${info.equivalente}'`,
      });
    } else if (MAPA_HOMOGLIFOS[charMin]) {
      const charLegitimo = MAPA_HOMOGLIFOS[charMin];
      desgloseCaracteres.push({
        posicion: i,
        caracterSospechoso: char,
        caracterLegitimo: charLegitimo,
        tipo: `Homoglifo Numérico/Visual ('${char}' por '${charLegitimo}')`,
      });
    }
  }

  for (const [caracterVisual, info] of Object.entries(MAPA_HOMOGLIFOS_UNICODE)) {
    if (textoResultado.includes(caracterVisual.toLowerCase())) {
      const regex = new RegExp(caracterVisual.toLowerCase(), "g");
      textoResultado = textoResultado.replace(regex, info.equivalente);
      sustitucionesDetectadas.push(`${caracterVisual} (${info.origen}) -> ${info.equivalente}`);
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

export function extraerTokensDeHost(nombreHost: string): string[] {
  return nombreHost
    .toLowerCase()
    .split(/[.\-_]/)
    .filter((token) => token.length >= 2);
}

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

    if (
      dominioMinusculas === dominioLegitimoMinusculas ||
      hostMinusculas.endsWith(`.${dominioLegitimoMinusculas}`)
    ) {
      continue;
    }

    const nombresAComparar = [marca.nombre, ...marca.nombresAlternativos];

    for (const nombreMarca of nombresAComparar) {
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
