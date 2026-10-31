import { ResultadoSimilitudMarca, SustitucionCaracterVisual } from "../tipos/analisis";

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

function construirMapaUnicode(
  grupos: { etiqueta: string; pares: [string, string][] }[],
  individuales: [string, string, string][]
): Record<string, { equivalente: string; origen: string }> {
  const resultado: Record<string, { equivalente: string; origen: string }> = {};
  const codigoHex = (c: string) => (c.codePointAt(0) ?? 0).toString(16).toUpperCase().padStart(4, "0");

  for (const grupo of grupos) {
    for (const [caracter, equivalente] of grupo.pares) {
      resultado[caracter] = { equivalente, origen: `${grupo.etiqueta} (U+${codigoHex(caracter)})` };
    }
  }
  for (const [caracter, equivalente, etiqueta] of individuales) {
    resultado[caracter] = { equivalente, origen: `${etiqueta} (U+${codigoHex(caracter)})` };
  }
  return resultado;
}

export const MAPA_HOMOGLIFOS_UNICODE = construirMapaUnicode(
  [
    {
      etiqueta: "Cirílico",
      pares: [
        ["\u0430", "a"], ["\u0435", "e"], ["\u043E", "o"], ["\u0440", "p"], ["\u0441", "c"], ["\u0443", "y"],
        ["\u0445", "x"], ["\u0456", "i"], ["\u0458", "j"], ["\u0455", "s"], ["\u0432", "b"], ["\u043C", "m"],
        ["\u043D", "h"], ["\u0442", "t"], ["\u0448", "w"], ["\u0449", "w"], ["\u043F", "n"], ["\u0434", "d"],
      ],
    },
    {
      etiqueta: "Cirílico Mayúscula",
      pares: [
        ["\u0410", "a"], ["\u0412", "b"], ["\u0415", "e"], ["\u041A", "k"], ["\u041C", "m"], ["\u041D", "h"],
        ["\u041E", "o"], ["\u0420", "p"], ["\u0421", "c"], ["\u0422", "t"], ["\u0423", "y"], ["\u0425", "x"],
        ["\u0406", "i"], ["\u0408", "j"],
      ],
    },
    {
      etiqueta: "Griego",
      pares: [
        ["\u03B1", "a"], ["\u03B5", "e"], ["\u03B9", "i"], ["\u03BA", "k"], ["\u03BF", "o"],
        ["\u03C1", "p"], ["\u03C5", "u"], ["\u03BD", "v"], ["\u03C9", "w"],
      ],
    },
    {
      etiqueta: "Griego Mayúscula",
      pares: [
        ["\u0391", "a"], ["\u0392", "b"], ["\u0395", "e"], ["\u039F", "o"],
        ["\u03A1", "p"], ["\u03A4", "t"], ["\u03A7", "x"],
      ],
    },
  ],
  [
    ["\u0105", "a", "Latino con colita"],
    ["\u0101", "a", "Latino con macrón"],
    ["\u0113", "e", "Latino con macrón"],
    ["\u0117", "e", "Latino con punto"],
    ["\u0119", "e", "Latino con colita"],
    ["\u012B", "i", "Latino con macrón"],
    ["\u0131", "i", "Latino sin punto"],
    ["\u014D", "o", "Latino con macrón"],
    ["\u0142", "l", "Latino con barra"],
    ["\u0251", "a", "Alfa Latina"],
    ["\u0261", "g", "G Latina de guión"],
  ]
);

export const MAPA_HOMOGLIFOS: Record<string, string> = {
  "0": "o", "1": "l", "|": "l", "!": "i", "3": "e", "4": "a", "@": "a",
  "5": "s", "$": "s", "7": "t", "8": "b", "9": "g", "vv": "w", "rn": "m", "cl": "d",
};

for (const [caracterUnicode, info] of Object.entries(MAPA_HOMOGLIFOS_UNICODE)) {
  MAPA_HOMOGLIFOS[caracterUnicode] = info.equivalente;
}

const PUNY_MIN = 1;
const PUNY_MAX = 26;
const PUNY_SESGO = 38;
const PUNY_AMORTIGUACION = 700;
const PUNY_SESGO_INICIAL = 72;
const PUNY_CODIGO_INICIAL = 128;
const PUNY_DELIMITADOR = "-";

function adaptarPunycode(delta: number, cantidadPuntos: number, primeraVez: boolean): number {
  let ajuste = primeraVez ? Math.floor(delta / PUNY_AMORTIGUACION) : delta >> 1;
  ajuste += Math.floor(ajuste / cantidadPuntos);
  let k = 0;
  while (ajuste > (((36 - PUNY_MIN) * PUNY_MAX) >> 1)) {
    ajuste = Math.floor(ajuste / (36 - PUNY_MIN));
    k += 36;
  }
  return Math.floor(k + ((36 - PUNY_MIN + 1) * ajuste) / (ajuste + PUNY_SESGO));
}

export function decodificarEtiquetaPunycode(etiqueta: string): string {
  const minusculas = etiqueta.toLowerCase();
  if (!minusculas.startsWith("xn--")) return etiqueta;

  const entrada = minusculas.slice(4);
  const indiceDelimitador = entrada.lastIndexOf(PUNY_DELIMITADOR);
  const salida: number[] = [];
  let n = PUNY_CODIGO_INICIAL;
  let i = 0;
  let sesgo = PUNY_SESGO_INICIAL;

  for (let j = 0; j < indiceDelimitador; j++) {
    salida.push(entrada.charCodeAt(j));
  }

  let indiceEntrada = indiceDelimitador > 0 ? indiceDelimitador + 1 : 0;
  while (indiceEntrada < entrada.length) {
    const iAnterior = i;
    let peso = 1;
    let k = 36;
    while (true) {
      if (indiceEntrada >= entrada.length) return etiqueta;
      const c = entrada.charCodeAt(indiceEntrada++);
      const digito = c >= 48 && c <= 57 ? c - 22 : c >= 97 && c <= 122 ? c - 97 : -1;
      if (digito < 0) return etiqueta;

      i += digito * peso;
      const umbral = k <= sesgo ? PUNY_MIN : k >= sesgo + PUNY_MAX ? PUNY_MAX : k - sesgo;
      if (digito < umbral) break;
      peso *= 36 - umbral;
      k += 36;
    }
    sesgo = adaptarPunycode(i - iAnterior, salida.length + 1, iAnterior === 0);
    n += Math.floor(i / (salida.length + 1));
    i %= salida.length + 1;
    salida.splice(i, 0, n);
    i++;
  }

  return String.fromCodePoint(...salida);
}

export function decodificarDominioPunycode(nombreHost: string): string {
  if (!nombreHost.toLowerCase().includes("xn--")) return nombreHost;
  return nombreHost.split(".").map(decodificarEtiquetaPunycode).join(".");
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

  for (const parte of hostUnicode.split(".")) {
    let tieneLatin = false;
    let tieneConfusable = false;

    for (let posicion = 0; posicion < parte.length; posicion++) {
      const caracter = parte[posicion];
      const codigo = caracter.codePointAt(0) ?? 0;

      if ((codigo >= 97 && codigo <= 122) || (codigo >= 65 && codigo <= 90)) {
        tieneLatin = true;
      }

      const info = MAPA_HOMOGLIFOS_UNICODE[caracter] || MAPA_HOMOGLIFOS_UNICODE[caracter.toLowerCase()];
      if (info) {
        tieneConfusable = true;
        desglose.push({
          posicion,
          caracterSospechoso: caracter,
          caracterLegitimo: info.equivalente,
          tipo: `Letra confusable ${info.origen} que aparenta ser '${info.equivalente}'`,
        });
      }
    }

    if (tieneLatin && tieneConfusable) {
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
  if (!cadenaA.length) return cadenaB.length;
  if (!cadenaB.length) return cadenaA.length;

  const costos = Array.from({ length: cadenaB.length + 1 }, (_, i) => i);
  for (let i = 1; i <= cadenaA.length; i++) {
    let diagonalAnterior = i - 1;
    costos[0] = i;
    for (let j = 1; j <= cadenaB.length; j++) {
      const temporal = costos[j];
      const costo = cadenaA[i - 1] === cadenaB[j - 1] ? 0 : 1;
      costos[j] = Math.min(costos[j] + 1, costos[j - 1] + 1, diagonalAnterior + costo);
      diagonalAnterior = temporal;
    }
  }
  return costos[cadenaB.length];
}

const DIGRAFOS_VISUALES: [string, string][] = [
  ["vv", "w"],
  ["rn", "m"],
  ["cl", "d"],
];

export function desofuscarHomoglifos(texto: string): {
  textoDesofuscado: string;
  sustitucionesDetectadas: string[];
  desgloseCaracteres: SustitucionCaracterVisual[];
} {
  const textoUnicode = decodificarDominioPunycode(texto);
  let textoResultado = textoUnicode.toLowerCase();
  const sustitucionesDetectadas: string[] = [];
  const desgloseCaracteres: SustitucionCaracterVisual[] = [];

  for (const [par, reemplazo] of DIGRAFOS_VISUALES) {
    if (textoResultado.includes(par)) {
      const posicion = textoResultado.indexOf(par);
      textoResultado = textoResultado.replaceAll(par, reemplazo);
      sustitucionesDetectadas.push(`${par} -> ${reemplazo}`);
      desgloseCaracteres.push({
        posicion,
        caracterSospechoso: par,
        caracterLegitimo: reemplazo,
        tipo: `Doble carácter por consonante (${par} por ${reemplazo})`,
      });
    }
  }

  for (let i = 0; i < textoUnicode.length; i++) {
    const caracter = textoUnicode[i];
    const minuscula = caracter.toLowerCase();
    const infoUnicode = MAPA_HOMOGLIFOS_UNICODE[caracter] || MAPA_HOMOGLIFOS_UNICODE[minuscula];

    if (infoUnicode) {
      desgloseCaracteres.push({
        posicion: i,
        caracterSospechoso: caracter,
        caracterLegitimo: infoUnicode.equivalente,
        tipo: `Letra confusable ${infoUnicode.origen} que simula '${infoUnicode.equivalente}'`,
      });
    } else if (MAPA_HOMOGLIFOS[minuscula]) {
      const caracterLegitimo = MAPA_HOMOGLIFOS[minuscula];
      desgloseCaracteres.push({
        posicion: i,
        caracterSospechoso: caracter,
        caracterLegitimo,
        tipo: `Homoglifo Numérico/Visual ('${caracter}' por '${caracterLegitimo}')`,
      });
    }
  }

  for (const [caracterVisual, info] of Object.entries(MAPA_HOMOGLIFOS_UNICODE)) {
    const minuscula = caracterVisual.toLowerCase();
    if (textoResultado.includes(minuscula)) {
      textoResultado = textoResultado.replaceAll(minuscula, info.equivalente);
      sustitucionesDetectadas.push(`${caracterVisual} (${info.origen}) -> ${info.equivalente}`);
    }
  }

  for (const [caracterVisual, caracterReal] of Object.entries(MAPA_HOMOGLIFOS)) {
    if (caracterVisual.length === 1 && textoResultado.includes(caracterVisual)) {
      textoResultado = textoResultado.replaceAll(caracterVisual, caracterReal);
      sustitucionesDetectadas.push(`${caracterVisual} -> ${caracterReal}`);
    }
  }

  return { textoDesofuscado: textoResultado, sustitucionesDetectadas, desgloseCaracteres };
}

export function extraerSegmentosHost(nombreHost: string): string[] {
  return nombreHost
    .toLowerCase()
    .split(/[.\-_]/)
    .filter((segmento) => segmento.length >= 2);
}

export function detectarSimilitudMarcas(
  nombreHost: string,
  dominioPrincipal: string
): ResultadoSimilitudMarca[] {
  const hostMinusculas = nombreHost.toLowerCase();
  const dominioMinusculas = dominioPrincipal.toLowerCase();
  const resultados: ResultadoSimilitudMarca[] = [];

  const { textoDesofuscado, sustitucionesDetectadas, desgloseCaracteres } = desofuscarHomoglifos(hostMinusculas);
  const segmentosHost = extraerSegmentosHost(hostMinusculas);
  const todosLosSegmentos = Array.from(new Set([...segmentosHost, ...extraerSegmentosHost(textoDesofuscado)]));

  for (const marca of MARCAS_OBJETIVO) {
    const dominioLegitimo = marca.dominioLegitimo.toLowerCase();
    if (dominioMinusculas === dominioLegitimo || hostMinusculas.endsWith(`.${dominioLegitimo}`)) {
      continue;
    }

    for (const nombreMarca of [marca.nombre, ...marca.nombresAlternativos]) {
      const marcaEsSegmento = segmentosHost.includes(nombreMarca);
      const alternativaCompuesta = nombreMarca.includes("-") && hostMinusculas.includes(nombreMarca);

      if ((marcaEsSegmento || alternativaCompuesta) && !dominioMinusculas.includes(dominioLegitimo)) {
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
        !dominioMinusculas.includes(dominioLegitimo)
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

      for (const segmento of todosLosSegmentos) {
        if (segmento.length < 3) continue;

        const distancia = calcularDistanciaLevenshtein(segmento, nombreMarca);
        const longitudMaxima = Math.max(segmento.length, nombreMarca.length);
        const similitud = Math.round(((longitudMaxima - distancia) / longitudMaxima) * 100);

        if (distancia > 0 && distancia <= 2 && similitud >= 75 && longitudMaxima >= 4) {
          resultados.push({
            marcaObjetivo: marca.nombre,
            dominioOriginal: marca.dominioLegitimo,
            nombreDetectado: segmento,
            distancia,
            similitudPorcentaje: similitud,
            tipoSustitucion: [`Variación tipográfica (${distancia} cambios)`],
            desgloseCaracteres,
          });
          break;
        }
      }
    }
  }

  return resultados;
}
