import { createHash } from "node:crypto";
import { readFile, rename, writeFile, mkdir } from "node:fs/promises";
import { dirname } from "node:path";

const CATEGORIAS = new Set([
  "fraude",
  "suplantacion",
  "robocall",
  "spam",
  "publicidad_no_deseada",
  "sospechoso"
]);

const normalizarNumero = (valor) => {
  const texto = String(valor || "").trim().replace(/[().\s-]/g, "");
  const numero = texto.startsWith("00") ? `+${texto.slice(2)}` : texto;
  if (!/^\+?[0-9]{8,15}$/.test(numero)) return null;
  const e164 = numero.startsWith("+") ? numero : `+${numero}`;
  return /^\+57[0-9]{10}$/.test(e164) ? e164 : null;
};

const huella = (fuente, registro, numero, categoria) =>
  registro.id ||
  createHash("sha256")
    .update(`${fuente.id}|${numero}|${categoria}|${registro.fecha || ""}`)
    .digest("hex");

const validarFuente = (fuente) => {
  if (!fuente || !fuente.id || !fuente.url) {
    throw new Error("Cada fuente requiere id y url");
  }
  if (!fuente.licencia || fuente.permiteRedistribucion !== true) {
    throw new Error(`La fuente ${fuente.id} no autoriza redistribución`);
  }
  if (fuente.pais !== "CO") {
    throw new Error(`La fuente ${fuente.id} no está declarada para Colombia`);
  }
  if (!["json", "csv"].includes(fuente.formato || "json")) {
    throw new Error(`Formato no compatible para ${fuente.id}`);
  }
};

const separarCsv = (linea) => {
  const columnas = [];
  let actual = "";
  let entreComillas = false;
  for (let indice = 0; indice < linea.length; indice += 1) {
    const caracter = linea[indice];
    if (caracter === '"' && linea[indice + 1] === '"') {
      actual += '"';
      indice += 1;
    } else if (caracter === '"') {
      entreComillas = !entreComillas;
    } else if (caracter === "," && !entreComillas) {
      columnas.push(actual.trim());
      actual = "";
    } else {
      actual += caracter;
    }
  }
  columnas.push(actual.trim());
  return columnas;
};

const analizarCsv = (texto) => {
  const lineas = texto.split(/\r?\n/).filter((linea) => linea.trim());
  if (!lineas.length) return [];
  const encabezados = separarCsv(lineas[0]).map((valor) => valor.toLowerCase());
  return lineas.slice(1).map((linea) => {
    const valores = separarCsv(linea);
    return Object.fromEntries(encabezados.map((encabezado, indice) => [encabezado, valores[indice] || ""]));
  });
};

const obtenerRegistros = (texto, formato) => {
  if (formato === "csv") return analizarCsv(texto);
  const contenido = JSON.parse(texto);
  if (!Array.isArray(contenido)) throw new Error("El JSON de la fuente debe ser un arreglo");
  return contenido;
};

const normalizarRegistro = (fuente, registro, consultadoEn) => {
  const numero = normalizarNumero(registro.numeroE164 || registro.numero || registro.phone);
  const categoria = String(
    registro.categoria || fuente.categoriaPredeterminada || "sospechoso"
  ).trim().toLowerCase();
  if (!numero || !CATEGORIAS.has(categoria)) return null;
  const confianza = Number(registro.confianza ?? fuente.confianzaPredeterminada ?? 30);
  if (!Number.isFinite(confianza) || confianza < 0 || confianza > 100) return null;
  return {
    numeroE164: numero,
    categoria,
    confianza: Math.round(confianza),
    reportes: Math.max(1, Number.parseInt(registro.reportes || "1", 10) || 1),
    fecha: registro.fecha || registro.updatedAt || consultadoEn,
    fuente: fuente.id,
    licencia: fuente.licencia,
    consulta: consultadoEn,
    huella: huella(fuente, registro, numero, categoria)
  };
};

const fusionarRegistro = (datos, registro) => {
  const yaImportado = datos.reportes.some((item) => item.huella === registro.huella);
  if (yaImportado) return false;
  datos.reportes.push({
    tipo: "numero",
    indicador: registro.numeroE164,
    categoria: registro.categoria,
    fuente: registro.fuente,
    huella: registro.huella,
    recibidoEn: registro.consulta,
    licencia: registro.licencia
  });

  let entrada = datos.inteligencia.find((item) => item.numeroE164 === registro.numeroE164);
  if (!entrada) {
    entrada = {
      numeroE164: registro.numeroE164,
      indicativoInternacional: "+57",
      pais: "CO",
      categoria: registro.categoria,
      reportesIndependientes: 0,
      primerReporte: registro.consulta,
      ultimoReporte: registro.consulta,
      fuentes: [],
      detalleFuentes: [],
      nivelConfianza: 0,
      estadoVerificacion: "sospecha",
      activo: true,
      actualizadoEn: registro.consulta
    };
    datos.inteligencia.push(entrada);
  }
  entrada.reportesIndependientes += registro.reportes;
  entrada.ultimoReporte = registro.fecha;
  entrada.actualizadoEn = registro.consulta;
  if (!entrada.fuentes.includes(registro.fuente)) entrada.fuentes.push(registro.fuente);
  const detalle = entrada.detalleFuentes || (entrada.detalleFuentes = []);
  const existente = detalle.find((item) => item.fuente === registro.fuente);
  const observacion = {
    fuente: registro.fuente,
    licencia: registro.licencia,
    fechaConsulta: registro.consulta,
    fechaDato: registro.fecha,
    categoria: registro.categoria,
    confianza: registro.confianza,
    reportes: registro.reportes
  };
  if (existente) Object.assign(existente, observacion);
  else detalle.push(observacion);
  if (entrada.nivelConfianza < registro.confianza) {
    entrada.nivelConfianza = registro.confianza;
    entrada.categoria = registro.categoria;
  }
  entrada.estadoVerificacion =
    entrada.reportesIndependientes >= 5 && entrada.nivelConfianza >= 50
      ? "confirmado"
      : "sospecha";
  return true;
};

export const importarFuentes = async ({
  archivoDatos,
  fuentes,
  fetchImpl = fetch,
  ahora = new Date().toISOString()
}) => {
  const contenido = JSON.parse(await readFile(archivoDatos, "utf8"));
  const datos = Array.isArray(contenido)
    ? { version: 1, inteligencia: [], reportes: contenido, cambios: [], ejecucionesFuentes: [] }
    : {
        version: contenido.version || 1,
        inteligencia: Array.isArray(contenido.inteligencia) ? contenido.inteligencia : [],
        reportes: Array.isArray(contenido.reportes) ? contenido.reportes : [],
        cambios: Array.isArray(contenido.cambios) ? contenido.cambios : [],
        ejecucionesFuentes: Array.isArray(contenido.ejecucionesFuentes)
          ? contenido.ejecucionesFuentes
          : []
      };
  const ejecuciones = [];
  let importados = 0;
  for (const fuente of fuentes) {
    const ejecucion = { fuente: fuente.id, consultadoEn: ahora, estado: "fallido", importados: 0 };
    try {
      validarFuente(fuente);
      const respuesta = await fetchImpl(fuente.url);
      if (!respuesta.ok) throw new Error(`HTTP ${respuesta.status}`);
      const registros = obtenerRegistros(
        await respuesta.text(),
        fuente.formato || "json"
      );
      for (const registro of registros) {
        const normalizado = normalizarRegistro(fuente, registro, ahora);
        if (normalizado && fusionarRegistro(datos, normalizado)) {
          importados += 1;
          ejecucion.importados += 1;
        }
      }
      ejecucion.estado = "exitoso";
    } catch (error) {
      ejecucion.error = error.message;
    }
    ejecuciones.push(ejecucion);
  }
  datos.ejecucionesFuentes = [...(datos.ejecucionesFuentes || []), ...ejecuciones].slice(-100);
  const temporal = `${archivoDatos}.temporal`;
  await mkdir(dirname(archivoDatos), { recursive: true });
  await writeFile(temporal, JSON.stringify(datos, null, 2), "utf8");
  await rename(temporal, archivoDatos);
  return { importados, ejecuciones };
};

export { analizarCsv, normalizarNumero, normalizarRegistro };
