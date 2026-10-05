import { describe, it, expect } from "vitest";
import { extraerCaracteristicasUrl } from "../src/analyzer/extractor-caracteristicas";
import { ReglaSuplantacionMarca } from "../src/rules/regla-suplantacion-marca";
import { ReglaIpHost } from "../src/rules/regla-ip-host";
import { ReglaSeguridadProtocolo } from "../src/rules/regla-seguridad-protocolo";
import { ReglaPunycodeHomografo } from "../src/rules/regla-punycode-homografo";
import { ReglaPalabrasSospechosas } from "../src/rules/regla-palabras-sospechosas";
import { ReglaTldSospechoso } from "../src/rules/regla-tld-sospechoso";
import { ReglaEstructuraUrl } from "../src/rules/regla-estructura-url";

describe("Reglas Individuales de Detección de Riesgo", () => {
  it("ReglaSuplantacionMarca: debe activarse con alta puntuación ante suplantación", () => {
    const regla = new ReglaSuplantacionMarca();
    const car = extraerCaracteristicasUrl("http://bancolombia-actualizar-datos.xyz/login");
    const resultado = regla.evaluar(car);

    expect(resultado.activada).toBe(true);
    expect(resultado.severidad).toBe("CRITICA");
    expect(resultado.puntuacion).toBeGreaterThanOrEqual(40);
    expect(resultado.evidencias.length).toBeGreaterThan(0);
  });

  it("ReglaIpHost: debe activarse cuando el host es una dirección IP", () => {
    const regla = new ReglaIpHost();
    const carIp = extraerCaracteristicasUrl("http://45.33.32.156/banco/login");
    const resIp = regla.evaluar(carIp);

    expect(resIp.activada).toBe(true);
    expect(resIp.puntuacion).toBeGreaterThanOrEqual(35);

    const carNormal = extraerCaracteristicasUrl("https://mi-dominio-legitimo.com/login");
    const resNormal = regla.evaluar(carNormal);
    expect(resNormal.activada).toBe(false);
  });

  it("ReglaSeguridadProtocolo: debe activarse ante HTTP no seguro con credenciales", () => {
    const regla = new ReglaSeguridadProtocolo();
    const carHttp = extraerCaracteristicasUrl("http://sitio-desconocido.com/login/usuario");
    const resHttp = regla.evaluar(carHttp);

    expect(resHttp.activada).toBe(true);
    expect(resHttp.puntuacion).toBeGreaterThanOrEqual(15);

    const carHttps = extraerCaracteristicasUrl("https://sitio-desconocido.com/login/usuario");
    const resHttps = regla.evaluar(carHttps);
    expect(resHttps.activada).toBe(false);
  });

  it("ReglaPunycodeHomografo: debe activarse si el dominio contiene prefijo xn--", () => {
    const regla = new ReglaPunycodeHomografo();
    const carPunycode = extraerCaracteristicasUrl("https://xn--bancolombia-wvb.top/verificar");
    const res = regla.evaluar(carPunycode);

    expect(res.activada).toBe(true);
    expect(res.puntuacion).toBeGreaterThanOrEqual(35);
  });

  it("ReglaPalabrasSospechosas: debe activarse ante acumulación de términos de urgencia o autenticación", () => {
    const regla = new ReglaPalabrasSospechosas();
    const car = extraerCaracteristicasUrl("https://portal-avisos.com/dian/fotomulta/iniciar-sesion/urgente");
    const res = regla.evaluar(car);

    expect(res.activada).toBe(true);
    expect(res.puntuacion).toBeGreaterThanOrEqual(20);
  });

  it("ReglaTldSospechoso: debe activarse en TLDs de alta tasa de abuso como .xyz o .top", () => {
    const regla = new ReglaTldSospechoso();
    const car = extraerCaracteristicasUrl("https://servicios-tramites.top/confirmar");
    const res = regla.evaluar(car);

    expect(res.activada).toBe(true);
    expect(res.puntuacion).toBeGreaterThanOrEqual(20);
  });

  it("ReglaEstructuraUrl: debe activarse ante subdominios profundos, @ o exceso de guiones", () => {
    const regla = new ReglaEstructuraUrl();
    const carArroba = extraerCaracteristicasUrl("https://seguridad.bancolombia.com@phishing-servidor.xyz/token");
    const resArroba = regla.evaluar(carArroba);
    expect(resArroba.activada).toBe(true);

    const carSubdominios = extraerCaracteristicasUrl("https://a.b.c.d.e.servidor-tramites.com/");
    const resSub = regla.evaluar(carSubdominios);
    expect(resSub.activada).toBe(true);
  });

  it("ReglaPunycodeHomografo: debe emitir severidad CRÍTICA y puntaje de bloqueo (>=85) ante letras raras", () => {
    const regla = new ReglaPunycodeHomografo();
    const carLetrasRaras = extraerCaracteristicasUrl("https://b\u0430ncolombi\u0430.com/login");
    const res = regla.evaluar(carLetrasRaras);

    expect(res.activada).toBe(true);
    expect(res.severidad).toBe("CRITICA");
    expect(res.puntuacion).toBeGreaterThanOrEqual(85);
    expect(res.evidencias.some((e) => e.campo === "alfabetoMixto")).toBe(true);
  });
});

