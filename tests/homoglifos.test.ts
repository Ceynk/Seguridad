import { describe, it, expect } from "vitest";
import {
  calcularDistanciaLevenshtein,
  desofuscarHomoglifos,
  detectarSimilitudMarcas,
} from "../src/analyzer/homoglifos";

describe("Módulo de Homoglifos y Similitud de Marcas", () => {
  it("debe calcular correctamente la distancia de Levenshtein entre dos cadenas", () => {
    expect(calcularDistanciaLevenshtein("banco", "banco")).toBe(0);
    expect(calcularDistanciaLevenshtein("banco", "banc0")).toBe(1);
    expect(calcularDistanciaLevenshtein("bancolombia", "banc0l0mbia")).toBe(2);
    expect(calcularDistanciaLevenshtein("nequi", "nequi1")).toBe(1);
    expect(calcularDistanciaLevenshtein("", "test")).toBe(4);
  });

  it("debe desofuscar sustituciones conocidas de homoglifos", () => {
    const caso1 = desofuscarHomoglifos("banc0l0mbia");
    expect(caso1.textoDesofuscado).toBe("bancolombia");
    expect(caso1.sustitucionesDetectadas.length).toBeGreaterThan(0);

    const caso2 = desofuscarHomoglifos("nequ1");
    expect(caso2.textoDesofuscado).toBe("nequl"); // 1 -> l

    const caso3 = desofuscarHomoglifos("d4v1pl4t4");
    expect(caso3.textoDesofuscado).toBe("davlplata");
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

  it("debe detectar suplantación mediante typosquatting u homoglifos para Nequi o Daviplata", () => {
    const similitudesNequi = detectarSimilitudMarcas(
      "nequ1-recuperacion.top",
      "nequ1-recuperacion.top"
    );
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
    const oficialesBancolombia = detectarSimilitudMarcas(
      "sucursalvirtual.bancolombia.com",
      "bancolombia.com"
    );
    expect(oficialesBancolombia.length).toBe(0);

    const oficialesNequi = detectarSimilitudMarcas("www.nequi.com.co", "nequi.com.co");
    expect(oficialesNequi.length).toBe(0);

    const oficialesDian = detectarSimilitudMarcas("muisca.dian.gov.co", "dian.gov.co");
    expect(oficialesDian.length).toBe(0);
  });
});
