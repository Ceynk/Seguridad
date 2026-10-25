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
    return Array.isArray(datos) ? datos : [];
  } catch (error) {
    if (error.code === "ENOENT") {
      return [];
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
  const entrada = await ejecutarEscrituraExclusiva(async () => {
    const datos = await cargarDatos();
    const existente = datos.find(
      (elemento) => elemento.indicador === indicador.indicador && elemento.tipo === indicador.tipo
    );
    if (existente) {
      existente.reportes = Number.isInteger(existente.reportes) ? existente.reportes + 1 : 1;
      existente.esMalicioso = existente.reportes >= 5;
      existente.actualizadoEn = new Date().toISOString();
    } else {
      datos.push({
        ...indicador,
        reportes: 1,
        esMalicioso: false,
        actualizadoEn: new Date().toISOString()
      });
    }
    await guardarDatos(datos);
    return datos.find(
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
    const ruta = new URL(solicitud.url, "http://localhost").pathname;
    if (solicitud.method === "GET" && ruta === "/v1/lista-negra") {
      respuestaJson(respuesta, 200, await cargarDatos());
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
