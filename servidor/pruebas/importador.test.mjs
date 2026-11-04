import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, readFile, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { importarFuentes, normalizarNumero } from "../fuentes/importador.mjs";

const fuente = {
  id: "feed-autorizado-colombia",
  url: "https://ejemplo.test/numeros.json",
  formato: "json",
  pais: "CO",
  licencia: "CC-BY-4.0",
  permiteRedistribucion: true
};

test("normaliza solo números móviles colombianos E.164", () => {
  assert.equal(normalizarNumero("300 123"), null);
  assert.equal(normalizarNumero("+57 310 205 4740"), "+573102054740");
  assert.equal(normalizarNumero("+573102054740"), "+573102054740");
});

test("importa una vez y conserva procedencia", async () => {
  const carpeta = await mkdtemp(join(tmpdir(), "smishguard-"));
  const archivo = join(carpeta, "datos.json");
  await writeFile(archivo, JSON.stringify({
    version: 1, inteligencia: [], reportes: [], cambios: [], ejecucionesFuentes: []
  }));
  const fetchImpl = async () => ({
    ok: true,
    text: async () => JSON.stringify([{
      id: "registro-1",
      numeroE164: "+573102054740",
      categoria: "fraude",
      confianza: 80,
      reportes: 3
    }])
  });
  const primera = await importarFuentes({ archivoDatos: archivo, fuentes: [fuente], fetchImpl });
  const segunda = await importarFuentes({ archivoDatos: archivo, fuentes: [fuente], fetchImpl });
  const datos = JSON.parse(await readFile(archivo, "utf8"));
  assert.equal(primera.importados, 1);
  assert.equal(segunda.importados, 0);
  assert.equal(datos.inteligencia[0].detalleFuentes[0].fuente, fuente.id);
  assert.equal(datos.inteligencia[0].nivelConfianza, 80);
});

test("registra fallos sin crear números", async () => {
  const carpeta = await mkdtemp(join(tmpdir(), "smishguard-"));
  const archivo = join(carpeta, "datos.json");
  await writeFile(archivo, JSON.stringify({
    version: 1, inteligencia: [], reportes: [], cambios: [], ejecucionesFuentes: []
  }));
  const resultado = await importarFuentes({
    archivoDatos: archivo,
    fuentes: [fuente],
    fetchImpl: async () => ({ ok: false, status: 503 })
  });
  const datos = JSON.parse(await readFile(archivo, "utf8"));
  assert.equal(resultado.importados, 0);
  assert.equal(datos.inteligencia.length, 0);
  assert.equal(datos.ejecucionesFuentes[0].estado, "fallido");
});
