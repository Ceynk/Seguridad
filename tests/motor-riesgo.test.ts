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

  it("debe clasificar como MALICIOSO una URL con múltiples indicadores combinados", async () => {
    const urlAtaque = "http://bancolombia-actualizar-datos-seguridad.xyz/portal/login";
    const resultado = await motor.analizarUrl(urlAtaque);

    expect(resultado.clasificacion).toBe("MALICIOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(70);
    expect(resultado.reglasCoincidentes.length).toBeGreaterThanOrEqual(2);
  });

  it("debe clasificar como SOSPECHOSO un dominio con señales aisladas o moderadas", async () => {
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

  it("debe clasificar como MALICIOSO y activar bloqueo ante un dominio falso con letras raras", async () => {
    // Dominio falso con 'а' cirílica (\u0430) en vez de 'a' latina
    const urlHomografo = "https://b\u0430ncolombi\u0430-pagos.com/portal";
    const resultado = await motor.analizarUrl(urlHomografo);

    expect(resultado.clasificacion).toBe("MALICIOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(85);
    expect(resultado.reglasCoincidentes.some((r) => r.id === "REGLA_PUNYCODE_HOMOGRAFO")).toBe(true);
  });

  it("debe clasificar como MALICIOSO y 95 pts un dominio añadido a la lista negra personalizada (Opción 3)", async () => {
    const motorConListaNegra = new MotorRiesgo();
    motorConListaNegra.actualizarConfiguracion({
      proteccionHabilitada: true,
      modoProteccion: "EQUILIBRADO",
      umbralSospechoso: 30,
      umbralMalicioso: 70,
      bloquearMaliciososAutomaticamente: true,
      advertirSospechosos: true,
      listaBlancaPersonalizada: [],
      listaNegraPersonalizada: ["https://sitio-phishing-agregado.com/login", "dominio-malo.xyz"],
      permitirBypassTemporal: true,
    });

    // 1. Probar URL exacta
    const res1 = await motorConListaNegra.analizarUrl("https://sitio-phishing-agregado.com/login");
    expect(res1.clasificacion).toBe("MALICIOSO");
    expect(res1.puntuacionRiesgo).toBe(95);
    expect(res1.esDominioConocidoMalicioso).toBe(true);
    expect(res1.reglasCoincidentes[0].id).toBe("REPUTACION_LISTA_NEGRA");

    // 2. Probar con www
    const res2 = await motorConListaNegra.analizarUrl("https://www.sitio-phishing-agregado.com/otra-cosa");
    expect(res2.clasificacion).toBe("MALICIOSO");
    expect(res2.puntuacionRiesgo).toBe(95);

    // 3. Probar subdominio
    const res3 = await motorConListaNegra.analizarUrl("http://sub.dominio-malo.xyz/alerta");
    expect(res3.clasificacion).toBe("MALICIOSO");
    expect(res3.puntuacionRiesgo).toBe(95);
  });
});
