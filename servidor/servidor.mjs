import { createServer } from "node:http";
import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const directorioActual = dirname(fileURLToPath(import.meta.url));
const archivoDatos = resolve(
  process.env.SMISHGUARD_DATA_FILE || `${directorioActual}/datos/lista-negra.json`
);
const puerto = Number(process.env.PORT || 8787);
const limiteVentana = 60_000;
const limiteSolicitudes = 60;
const ventanas = new Map();
let escrituraEnCurso = Promise.resolve();

const ejecutarEscrituraExclusiva = (operacion) => {
  const anterior = escrituraEnCurso;
  let liberar;
  escrituraEnCurso = new Promise((resolver) => {
    liberar = resolver;
  });
  return anterior.then(operacion).finally(liberar);
};

const respuestaJson = (respuesta, estado, cuerpo) => {
  respuesta.writeHead(estado, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "Access-Control-Allow-Origin": process.env.ALLOW_ORIGIN || "*",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type"
  });
  respuesta.end(JSON.stringify(cuerpo));
};

const obtenerDireccion = (solicitud) => {
  const reenviado = solicitud.headers["x-forwarded-for"];
  return String(reenviado || solicitud.socket.remoteAddress || "desconocido")
    .split(",")[0]
    .trim();
};

const permitirSolicitud = (solicitud) => {
  const ahora = Date.now();
  const direccion = obtenerDireccion(solicitud);
  const ventana = ventanas.get(direccion);
  if (!ventana || ahora - ventana.inicio >= limiteVentana) {
    ventanas.set(direccion, { inicio: ahora, cantidad: 1 });
    return true;
  }
  if (ventana.cantidad >= limiteSolicitudes) {
    return false;
  }
  ventana.cantidad += 1;
  return true;
};

const cargarDatos = async () => {
  try {
    const contenido = await readFile(archivoDatos, "utf8");
    const datos = JSON.parse(contenido);
    if (Array.isArray(datos)) {
      return {
        version: 1,
        inteligencia: [],
        reportes: datos,
        cambios: [],
        ejecucionesFuentes: []
      };
    }
    return {
      version: 1,
      inteligencia: Array.isArray(datos.inteligencia) ? datos.inteligencia : [],
      reportes: Array.isArray(datos.reportes) ? datos.reportes : [],
      cambios: Array.isArray(datos.cambios) ? datos.cambios : [],
      ejecucionesFuentes: Array.isArray(datos.ejecucionesFuentes) ? datos.ejecucionesFuentes : []
    };
  } catch (error) {
    if (error.code === "ENOENT") {
      return { version: 1, inteligencia: [], reportes: [], cambios: [], ejecucionesFuentes: [] };
    }
    throw error;
  }
};

const guardarDatos = async (datos) => {
  await mkdir(dirname(archivoDatos), { recursive: true });
  const temporal = `${archivoDatos}.temporal`;
  await writeFile(temporal, JSON.stringify(datos, null, 2), "utf8");
  await rename(temporal, archivoDatos);
};

const normalizarIndicador = (valor, tipo) => {
  const texto = String(valor || "").trim();
  const tipoNormalizado = String(tipo || "").trim().toLowerCase();
  if (tipoNormalizado === "dominio") {
    const dominio = texto
      .replace(/^https?:\/\//i, "")
      .replace(/^www\./i, "")
      .split("/")[0]
      .toLowerCase();
    if (!/^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$/.test(dominio)) {
      return null;
    }
    return { indicador: dominio, tipo: tipoNormalizado };
  }
  if (tipoNormalizado === "numero") {
    const numero = texto.replace(/[().\s-]/g, "");
    if (!/^\+?[0-9]{8,15}$/.test(numero)) {
      return null;
    }
    return { indicador: numero, tipo: tipoNormalizado };
  }
  return null;
};

const CATEGORIAS = new Set([
  "fraude",
  "suplantacion",
  "robocall",
  "spam",
  "publicidad_no_deseada",
  "sospechoso",
  "legitimo",
  "desconocido"
]);

const normalizarNumeroColombiano = (valor) => {
  const normalizado = normalizarIndicador(valor, "numero");
  if (!normalizado || !normalizado.indicador.startsWith("+57")) {
    return null;
  }
  const numeroE164 = `+${normalizado.indicador.replace(/^\+/, "")}`;
  if (!/^\+57[0-9]{10}$/.test(numeroE164)) {
    return null;
  }
  return numeroE164;
};

const calcularConfianza = (entrada) => {
  const reportes = Math.min(50, entrada.reportesIndependientes || 0);
  const fuentes = Math.min(3, (entrada.fuentes || []).length);
  const antiguedad = entrada.primerReporte
    ? Math.max(0, Math.min(15, Math.floor((Date.now() - Date.parse(entrada.primerReporte)) / 86_400_000 / 30)))
    : 0;
  return Math.min(100, reportes * 8 + fuentes * 10 + antiguedad);
};

const registrarCambio = (datos, numeroE164, tipo, antes, despues) => {
  datos.cambios.push({
    numeroE164,
    tipo,
    antes,
    despues,
    fecha: new Date().toISOString()
  });
};

const actualizarInteligencia = (datos, numeroE164, categoria, fuente) => {
  const ahora = new Date().toISOString();
  let entrada = datos.inteligencia.find((item) => item.numeroE164 === numeroE164);
  const antes = entrada ? { ...entrada, fuentes: [...entrada.fuentes] } : null;
  if (!entrada) {
    entrada = {
      numeroE164,
      indicativoInternacional: "+57",
      pais: "CO",
      categoria,
      reportesIndependientes: 0,
      primerReporte: ahora,
      ultimoReporte: ahora,
      fuentes: [],
      nivelConfianza: 0,
      estadoVerificacion: "sospecha",
      activo: true,
      actualizadoEn: ahora
    };
    datos.inteligencia.push(entrada);
  }
  entrada.reportesIndependientes += 1;
  entrada.ultimoReporte = ahora;
  if (fuente && !entrada.fuentes.includes(fuente)) {
    entrada.fuentes.push(fuente);
  }
  entrada.categoria = categoria;
  entrada.nivelConfianza = calcularConfianza(entrada);
  entrada.estadoVerificacion =
    entrada.reportesIndependientes >= 5 && entrada.nivelConfianza >= 50
      ? "confirmado"
      : "sospecha";
  entrada.activo = true;
  entrada.actualizadoEn = ahora;
  registrarCambio(datos, numeroE164, "reporte", antes, entrada);
  return entrada;
};

const leerCuerpo = async (solicitud) => {
  let contenido = "";
  for await (const parte of solicitud) {
    contenido += parte;
    if (contenido.length > 4096) {
      throw new Error("El cuerpo excede el tamaño permitido");
    }
  }
  return JSON.parse(contenido || "{}");
};

const crearReporte = async (solicitud, respuesta) => {
  let cuerpo;
  try {
    cuerpo = await leerCuerpo(solicitud);
  } catch (error) {
    respuestaJson(respuesta, 400, { error: error.message });
    return;
  }
  const indicador = normalizarIndicador(cuerpo.indicador, cuerpo.tipo);
  if (!indicador) {
    respuestaJson(respuesta, 400, { error: "Indicador o tipo no válido" });
    return;
  }
  if (indicador.tipo === "numero") {
    const numeroE164 = normalizarNumeroColombiano(indicador.indicador);
    if (!numeroE164) {
      respuestaJson(respuesta, 400, { error: "Solo se aceptan números colombianos válidos en formato E.164" });
      return;
    }
    const categoria = String(cuerpo.categoria || "sospechoso").trim().toLowerCase();
    if (!CATEGORIAS.has(categoria)) {
      respuestaJson(respuesta, 400, { error: "Categoría no válida" });
      return;
    }
    const entrada = await ejecutarEscrituraExclusiva(async () => {
      const datos = await cargarDatos();
      const fuente = String(cuerpo.fuente || "comunidad").slice(0, 80);
      const categoria = String(cuerpo.categoria || "sospechoso").trim().toLowerCase();
      const huella = String(
        cuerpo.idempotencyKey || `${numeroE164}|${categoria}|${fuente}|${new Date().toISOString().slice(0, 10)}`
      ).slice(0, 160);
      const reporteExistente = datos.reportes.find((reporte) => reporte.huella === huella);
      if (reporteExistente) {
        return datos.inteligencia.find((item) => item.numeroE164 === numeroE164);
      }
      datos.reportes.push({
        tipo: "numero",
        indicador: numeroE164,
        categoria,
        fuente,
        huella,
        recibidoEn: new Date().toISOString()
      });
      const resultado = actualizarInteligencia(
        datos,
        numeroE164,
        categoria,
        fuente
      );
      await guardarDatos(datos);
      return resultado;
    });
    respuestaJson(respuesta, 201, entrada);
    return;
  }
  const entrada = await ejecutarEscrituraExclusiva(async () => {
    const datos = await cargarDatos();
    const existente = datos.reportes.find(
      (elemento) => elemento.indicador === indicador.indicador && elemento.tipo === indicador.tipo
    );
    if (existente) {
      existente.reportes = Number.isInteger(existente.reportes) ? existente.reportes + 1 : 1;
      existente.esMalicioso = existente.reportes >= 5;
      existente.actualizadoEn = new Date().toISOString();
    } else {
      datos.reportes.push({
        ...indicador,
        reportes: 1,
        esMalicioso: false,
        actualizadoEn: new Date().toISOString()
      });
    }
    await guardarDatos(datos);
    return datos.reportes.find(
      (elemento) => elemento.indicador === indicador.indicador && elemento.tipo === indicador.tipo
    );
  });
  respuestaJson(respuesta, 201, entrada);
};

const servidor = createServer(async (solicitud, respuesta) => {
  if (solicitud.method === "OPTIONS") {
    respuestaJson(respuesta, 204, {});
    return;
  }
  if (!permitirSolicitud(solicitud)) {
    respuestaJson(respuesta, 429, { error: "Límite de solicitudes alcanzado" });
    return;
  }
  try {
    const url = new URL(solicitud.url, "http://localhost");
    const ruta = url.pathname;
    if (solicitud.method === "GET" && ruta === "/v1/lista-negra") {
      const desde = Number(url.searchParams.get("desde") || 0);
      const estado = await cargarDatos();
      const datos = estado.reportes;
      const filtrados = Number.isFinite(desde) && desde > 0
        ? datos.filter((elemento) => Date.parse(elemento.actualizadoEn) > desde)
        : datos;
      respuestaJson(respuesta, 200, filtrados);
      return;
    }
    if (solicitud.method === "GET" && ruta === "/v1/inteligencia") {
      const desde = Number(url.searchParams.get("desde") || 0);
      const estado = await cargarDatos();
      const datos = Number.isFinite(desde) && desde > 0
        ? estado.inteligencia.filter((elemento) => Date.parse(elemento.actualizadoEn) > desde)
        : estado.inteligencia;
      respuestaJson(respuesta, 200, datos);
      return;
    }
    if (solicitud.method === "GET" && ruta === "/v1/estado") {
      const estado = await cargarDatos();
      respuestaJson(respuesta, 200, {
        version: estado.version,
        numeros: estado.inteligencia.length,
        paises: [...new Set(estado.inteligencia.map((item) => item.pais))],
        ultimaActualizacion: estado.inteligencia
          .map((item) => item.actualizadoEn)
          .sort()
          .at(-1) || null,
        fuentes: [...new Set(estado.inteligencia.flatMap((item) => item.fuentes))]
      });
      return;
    }
    if (solicitud.method === "POST" && ruta === "/v1/reportes") {
      await crearReporte(solicitud, respuesta);
      return;
    }
    respuestaJson(respuesta, 404, { error: "Ruta no encontrada" });
  } catch (error) {
    console.error(error);
    respuestaJson(respuesta, 500, { error: "Error interno del servidor" });
  }
});

servidor.listen(puerto, "0.0.0.0", () => {
  console.log(`Servidor comunitario escuchando en el puerto ${puerto}`);
});
