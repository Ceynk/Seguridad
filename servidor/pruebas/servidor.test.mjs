import test from "node:test";
import assert from "node:assert/strict";

const validarDominio = (valor) =>
  /^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$/.test(valor);

test("acepta dominios normalizados", () => {
  assert.equal(validarDominio("ejemplo.com"), true);
  assert.equal(validarDominio("dominio inválido"), false);
});

test("rechaza contenido que no es un indicador", () => {
  assert.equal(validarDominio("sms completo con datos"), false);
});
