import { describe, it, expect } from "vitest";
import {
  calcularDistanciaLevenshtein,
  desofuscarHomoglifos,
  detectarSimilitudMarcas,
  detectarLetrasRarasYAlfabetoMixto,
} from "../src/analizador/homoglifos";

describe("Módulo de Homoglifos y Similitud de Marcas", () => {
  it("debe calcular correctamente la distancia de Levenshtein entre dos cadenas", () => {
    expect(calcularDistanciaLevenshtein("banco", "banco")).toBe(0);
    expect(calcularDistanciaLevenshtein("banco", "banc0")).toBe(1);
    expect(calcularDistanciaLevenshtein("bancolombia", "banc0l0mbia")).toBe(2);
    expect(calcularDistanciaLevenshtein("nequi", "nequi1")).toBe(1);
    expect(calcularDistanciaLevenshtein("", "hola")).toBe(4);
  });

  it("debe desofuscar sustituciones conocidas de homoglifos", () => {
    const caso1 = desofuscarHomoglifos("banc0l0mbia");
    expect(caso1.textoDesofuscado).toBe("bancolombia");
    expect(caso1.sustitucionesDetectadas.length).toBeGreaterThan(0);

    expect(desofuscarHomoglifos("nequ1").textoDesofuscado).toBe("nequl");
    expect(desofuscarHomoglifos("d4v1pl4t4").textoDesofuscado).toBe("davlplata");
  });

  it("debe detectar suplantación de Bancolombia en dominios no oficiales", () => {
    const similitudes = detectarSimilitudMarcas(
      "bancolombia-actualizacion-datos.xyz",
      "bancolombia-actualizacion-datos.xyz"
    );
    expect(similitudes.length).toBeGreaterThan(0);
    expect(similitudes[0].marcaObjetivo).toBe("bancolombia");
    expect(similitudes[0].dominioOriginal).toBe("bancolombia.com");
  });

  it("debe detectar suplantación mediante variaciones tipográficas u homoglifos para Nequi o DIAN", () => {
    const similitudesNequi = detectarSimilitudMarcas("nequ1-recuperacion.top", "nequ1-recuperacion.top");
    expect(similitudesNequi.length).toBeGreaterThan(0);
    expect(similitudesNequi[0].marcaObjetivo).toBe("nequi");

    const similitudesDian = detectarSimilitudMarcas(
      "dian-pago-multas-colombia.click",
      "dian-pago-multas-colombia.click"
    );
    expect(similitudesDian.length).toBeGreaterThan(0);
    expect(similitudesDian[0].marcaObjetivo).toBe("dian");
  });

  it("NO debe marcar como suplantación al dominio oficial legítimo", () => {
    expect(detectarSimilitudMarcas("sucursalvirtual.bancolombia.com", "bancolombia.com").length).toBe(0);
    expect(detectarSimilitudMarcas("www.nequi.com.co", "nequi.com.co").length).toBe(0);
    expect(detectarSimilitudMarcas("muisca.dian.gov.co", "dian.gov.co").length).toBe(0);
    expect(detectarSimilitudMarcas("www.instagram.com", "instagram.com").length).toBe(0);
  });

  it("debe detectar suplantación de Instagram en dominios no oficiales", () => {
    const similitudes = detectarSimilitudMarcas(
      "instagram-verificacion-cuenta.xyz",
      "instagram-verificacion-cuenta.xyz"
    );
    expect(similitudes.length).toBeGreaterThan(0);
    expect(similitudes[0].marcaObjetivo).toBe("instagram");
  });

  it("debe detectar letras raras cirílicas que parecen latinas y marcar alfabeto mixto", () => {
    const resultado = desofuscarHomoglifos("b\u0430ncolombi\u0430.com");
    expect(resultado.textoDesofuscado).toBe("bancolombia.com");
    expect(resultado.desgloseCaracteres.length).toBe(2);
    expect(resultado.desgloseCaracteres[0].caracterLegitimo).toBe("a");
    expect(resultado.desgloseCaracteres[0].tipo).toContain("Cirílico");
  });

  it("debe decodificar dominios Punycode a Unicode y detectar suplantación", () => {
    const similitudes = detectarSimilitudMarcas("xn--bncolombi-e1a.com", "xn--bncolombi-e1a.com");
    expect(similitudes.length).toBeGreaterThan(0);
    expect(similitudes[0].marcaObjetivo).toBe("bancolombia");
  });

  it("debe detectar alfabeto mixto cuando conviven letras latinas con cirílicas o griegas", () => {
    const analisis = detectarLetrasRarasYAlfabetoMixto("g\u043E\u043Egle.com");
    expect(analisis.tieneLetrasRaras).toBe(true);
    expect(analisis.tieneAlfabetoMixto).toBe(true);
    expect(analisis.desglose.length).toBe(2);
  });
});
