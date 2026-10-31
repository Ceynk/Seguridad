import { describe, it, expect } from "vitest";
import {
  calcularEntropiaShannon,
  calcularHashSha256Simple,
  exportarInformeMarkdown,
  generarFichaForense,
} from "../src/utilidades/cripto-forense";
import { MotorRiesgo } from "../src/analizador/motor-riesgo";

describe("Módulo de Criptografía Forense y Telemetría", () => {
  it("debe calcular la entropía de Shannon correctamente", () => {
    expect(calcularEntropiaShannon("aaaaa")).toBe(0);

    const entropiaNormal = calcularEntropiaShannon("bancolombia.com");
    expect(entropiaNormal).toBeGreaterThan(2.0);
    expect(entropiaNormal).toBeLessThan(4.5);

    expect(calcularEntropiaShannon("x8k2-m9zq-1p7v-99a.xyz")).toBeGreaterThan(3.5);
  });

  it("debe generar un hash SHA-256 determinista para una URL", () => {
    const url = "http://banc0l0mbia-login.xyz/portal";
    const huellaA = calcularHashSha256Simple(url);
    const huellaB = calcularHashSha256Simple(url);

    expect(huellaA).toBe(huellaB);
    expect(huellaA.length).toBe(64);
  });

  it("debe generar una Ficha Forense estructurada con técnicas MITRE ATT&CK", async () => {
    const motor = new MotorRiesgo();
    const resultado = await motor.analizarUrl("http://banc0l0mbia-login-verificacion.xyz/dian/fotomulta");
    const ficha = generarFichaForense(resultado);

    expect(ficha.idIncidente).toBeDefined();
    expect(ficha.huellaSha256).toBeDefined();
    expect(ficha.clasificacion).toBe("MALICIOSO");
    expect(ficha.tecnicasMitreDetectadas.length).toBeGreaterThan(0);
    expect(ficha.tecnicasMitreDetectadas.some((t) => t.id === "T1036.007" || t.id === "T1566.002")).toBe(true);
    expect(ficha.vectoresIngenieriaSocial.length).toBeGreaterThan(0);
  });

  it("debe exportar un informe en Markdown", async () => {
    const motor = new MotorRiesgo();
    const resultado = await motor.analizarUrl("http://banc0l0mbia-login-verificacion.xyz/dian/fotomulta");
    const ficha = generarFichaForense(resultado);
    const informe = exportarInformeMarkdown(ficha);

    expect(informe).toContain("FICHA TECNICA DE INCIDENTE FORENSE");
    expect(informe).toContain("MITRE ATT&CK");
    expect(informe).toContain(ficha.huellaSha256);
    expect(informe).toContain("Recomendaciones de Respuesta al Incidente");
  });
});
