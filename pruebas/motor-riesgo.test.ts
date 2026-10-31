import { describe, it, expect } from "vitest";
import { MotorRiesgo } from "../src/analizador/motor-riesgo";

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
    const resultado = await motor.analizarUrl("http://bancolombia-actualizar-datos-seguridad.xyz/portal/login");

    expect(resultado.clasificacion).toBe("MALICIOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(70);
    expect(resultado.reglasCoincidentes.length).toBeGreaterThanOrEqual(2);
  });

  it("debe clasificar como SOSPECHOSO un dominio con señales aisladas o moderadas", async () => {
    const resultado = await motor.analizarUrl("http://tienda-articulos-nuevos.top/productos");

    expect(resultado.clasificacion).toBe("SOSPECHOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(30);
    expect(resultado.puntuacionRiesgo).toBeLessThan(70);
  });

  it("debe clasificar como SEGURO un dominio en lista blanca oficial", async () => {
    for (const url of [
      "https://www.google.com/search?q=ciberseguridad",
      "https://sucursalvirtual.bancolombia.com/login",
    ]) {
      const resultado = await motor.analizarUrl(url);
      expect(resultado.clasificacion).toBe("SEGURO");
      expect(resultado.puntuacionRiesgo).toBe(0);
      expect(resultado.esDominioConocidoSeguro).toBe(true);
    }
  });

  it("debe clasificar como MALICIOSO y activar bloqueo ante un dominio falso con letras raras", async () => {
    const resultado = await motor.analizarUrl("https://b\u0430ncolombi\u0430-pagos.com/portal");

    expect(resultado.clasificacion).toBe("MALICIOSO");
    expect(resultado.puntuacionRiesgo).toBeGreaterThanOrEqual(85);
    expect(resultado.reglasCoincidentes.some((r) => r.id === "REGLA_PUNYCODE_HOMOGRAFO")).toBe(true);
  });

  it("debe clasificar como MALICIOSO y 95 pts un dominio añadido a la lista negra personalizada", async () => {
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

    const exacta = await motorConListaNegra.analizarUrl("https://sitio-phishing-agregado.com/login");
    expect(exacta.clasificacion).toBe("MALICIOSO");
    expect(exacta.puntuacionRiesgo).toBe(95);
    expect(exacta.esDominioConocidoMalicioso).toBe(true);
    expect(exacta.reglasCoincidentes[0].id).toBe("REPUTACION_LISTA_NEGRA");

    for (const variante of [
      "https://www.sitio-phishing-agregado.com/otra-cosa",
      "http://sub.dominio-malo.xyz/alerta",
    ]) {
      const resultado = await motorConListaNegra.analizarUrl(variante);
      expect(resultado.clasificacion).toBe("MALICIOSO");
      expect(resultado.puntuacionRiesgo).toBe(95);
    }
  });

  it("no debe elevar una señal débil aislada a maliciosa", async () => {
    const resultado = await motor.analizarUrl("https://catalogo-ejemplo.top/productos");
    expect(resultado.clasificacion).toBe("SEGURO");
    expect(resultado.puntuacionRiesgo).toBeLessThan(70);
  });

  it("debe priorizar la combinación de suplantación y contexto sensible", async () => {
    const resultado = await motor.analizarUrl(
      "https://bancolombia-seguridad-falsa.xyz/actualizar-datos/verificar-clave"
    );

    expect(resultado.clasificacion).toBe("MALICIOSO");
    expect(resultado.reglasCoincidentes.some((regla) => regla.id === "REGLA_CONTEXTO_SOCIAL")).toBe(true);
    expect(resultado.reglasCoincidentes.some((regla) => regla.id === "REGLA_SUPLANTACION")).toBe(true);
  });

  it("solo debe recomendar bloqueo automático con evidencia fuerte", async () => {
    const debil = await motor.analizarUrl("https://catalogo-ejemplo.top/productos");
    const fuerte = await motor.analizarUrl("http://banc0l0mbia-login-verificacion.xyz/portal");

    expect(debil.esBloqueoAltaConfianza).toBe(false);
    expect(fuerte.esBloqueoAltaConfianza).toBe(true);
  });
});
