import { describe, it, expect } from "vitest";
import { MotorRiesgo } from "../src/analyzer/motor-riesgo";

describe("Prevención de Falsos Positivos en Sitios Legítimos", () => {
  const motor = new MotorRiesgo();

  const sitiosLegitimos = [
    "https://www.google.com",
    "https://www.google.com.co/search?q=tramites+colombia&oq=tramites",
    "https://www.microsoft.com/es-es/microsoft-365",
    "https://github.com/facebook/react/issues/12345",
    "https://www.bancolombia.com/personas",
    "https://sucursalvirtual.bancolombia.com/gestion/login",
    "https://www.nequi.com.co/ayuda/como-enviar-plata",
    "https://www.daviplata.com/preguntas-frecuentes",
    "https://www.dian.gov.co/tramites-y-servicios/declaracion-renta",
    "https://www.pse.com.co/persona/como-funciona",
    "https://www.servientrega.com/wps/portal/rastreo-envios",
    "https://www.4-72.com.co/servicios-postales-nacionales",
    "https://aws.amazon.com/es/console/?nc2=h_m_c",
    "https://www.netflix.com/co/login",
  ];

  it.each(sitiosLegitimos)(
    "debe clasificar como SEGURO y NO bloquear %s",
    async (url) => {
      const resultado = await motor.analizarUrl(url);

      expect(resultado.clasificacion).toBe("SEGURO");
      expect(resultado.puntuacionRiesgo).toBeLessThan(30);
    }
  );

  it("NO debe bloquear URLs legítimas con muchos parámetros o rutas extensas", async () => {
    const urlCompleja =
      "https://github.com/search?q=ciberseguridad+colombia+extension&type=repositories&s=updated&o=desc&page=2";
    const resultado = await motor.analizarUrl(urlCompleja);

    expect(resultado.clasificacion).toBe("SEGURO");
    expect(resultado.puntuacionRiesgo).toBeLessThan(30);
  });
});
