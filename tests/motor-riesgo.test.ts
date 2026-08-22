import { describe, it, expect } from "vitest";
import { MotorRiesgo } from "../src/analyzer/motor-riesgo";

describe("Motor de Riesgo y Clasificación Heurística", () => {
  const motor = new MotorRiesgo();

  it("debe clasificar como MALICIOSO un dominio de la lista negra de prueba", async () => {
    const resultado = await motor.analizarUrl("http://phishing-bancolombia-ficticio.xyz/login");

    expect(resultado.clasificacion).toBe("MALICIOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(70);
    expect(resultado.esDominioConocidoMalicioso).toBe(true);
    expect(resultado.nivelConfianza).toBe("ALTO");
  });

  it("debe clasificar como MALICIOSO una URL con múltiples indicadores combinados (marca suplantada + TLD sospechoso + HTTP + palabras bancarias)", async () => {
    const urlAtaque = "http://bancolombia-actualizar-datos-seguridad.xyz/portal/login";
    const resultado = await motor.analizarUrl(urlAtaque);

    expect(resultado.clasificacion).toBe("MALICIOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(70);
    expect(resultado.reglasCoincidentes.length).toBeGreaterThanOrEqual(2);
  });

  it("debe clasificar como SOSPECHOSO un dominio con señales aisladas o moderadas", async () => {
    // TLD sospechoso + protocolo HTTP sin marca directa
    const urlSospechosa = "http://tienda-articulos-nuevos.top/productos";
    const resultado = await motor.analizarUrl(urlSospechosa);

    expect(resultado.clasificacion).toBe("SOSPECHOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(30);
    expect(resultado.puntuacionRiesgo).toBeLessThan(70);
  });

  it("debe clasificar como SEGURO un dominio en lista blanca oficial", async () => {
    const resGoogle = await motor.analizarUrl("https://www.google.com/search?q=ciberseguridad");
    expect(resGoogle.clasificacion).toBe("SEGURO");
    expect(resGoogle.puntuacionRiesgo).toBe(0);
    expect(resGoogle.esDominioConocidoSeguro).toBe(true);

    const resBancolombia = await motor.analizarUrl("https://sucursalvirtual.bancolombia.com/login");
    expect(resBancolombia.clasificacion).toBe("SEGURO");
    expect(resBancolombia.puntuacionRiesgo).toBe(0);
    expect(resBancolombia.esDominioConocidoSeguro).toBe(true);
  });
});
