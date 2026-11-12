import test from "node:test";
import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { normalizarIndicador, normalizarNumeroColombiano } from "../servidor.mjs";

const rutaServidor = fileURLToPath(new URL("../servidor.mjs", import.meta.url));

test("acepta dominios normalizados y rechaza inválidos", () => {
  assert.deepEqual(normalizarIndicador("https://www.Ejemplo.com/login", "dominio"), {
    indicador: "ejemplo.com",
    tipo: "dominio"
  });
  assert.equal(normalizarIndicador("dominio inválido", "dominio"), null);
  assert.equal(normalizarIndicador("sms completo con datos", "dominio"), null);
});

test("normaliza números colombianos con o sin prefijo +57", () => {
  assert.equal(normalizarNumeroColombiano("+57 310 205 4740"), "+573102054740");
  assert.equal(normalizarNumeroColombiano("573102054740"), "+573102054740");
  assert.equal(normalizarNumeroColombiano("3102054740"), "+573102054740");
  assert.equal(normalizarNumeroColombiano("300123"), null);
});

test("registra reportes de dominios y números y consolida lista negra", async () => {
  const carpeta = await mkdtemp(join(tmpdir(), "smishguard-srv-"));
  const archivoDatos = join(carpeta, "lista-negra.json");
  const puerto = 18790;
  const proceso = spawn("node", [rutaServidor], {
    env: { ...process.env, PORT: String(puerto), SMISHGUARD_DATA_FILE: archivoDatos }
  });

  try {
    await new Promise((resolver) => proceso.stdout.once("data", resolver));

    const respDominio = await fetch(`http://localhost:${puerto}/v1/reportes`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ indicador: "https://www.PhishingBanco.com/acceso", tipo: "dominio" })
    });
    const datosDominio = await respDominio.json();
    assert.equal(respDominio.status, 201);
    assert.equal(datosDominio.indicador, "phishingbanco.com");
    assert.equal(datosDominio.reportes, 1);
    assert.equal(datosDominio.esMalicioso, true);

    const respNumero = await fetch(`http://localhost:${puerto}/v1/reportes`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ indicador: "3102054740", tipo: "numero", categoria: "fraude" })
    });
    const ultimoNumero = await respNumero.json();

    assert.equal(ultimoNumero.indicador, "+573102054740");
    assert.equal(ultimoNumero.tipo, "numero");
    assert.equal(ultimoNumero.reportes, 1);
    assert.equal(ultimoNumero.esMalicioso, true);
    assert.equal(ultimoNumero.estadoVerificacion, "confirmado");

    const respLista = await fetch(`http://localhost:${puerto}/v1/lista-negra`);
    const listaNegra = await respLista.json();
    assert.equal(listaNegra.length, 2);

    const entradaNumero = listaNegra.find((item) => item.indicador === "+573102054740");
    assert.equal(entradaNumero.reportes, 1);
    assert.equal(entradaNumero.esMalicioso, true);
  } finally {
    proceso.kill();
    await rm(carpeta, { recursive: true, force: true });
  }
});
