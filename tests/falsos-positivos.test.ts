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
    "https://www.spotify.com/co/account/overview/",
    "https://www.twitch.tv/directory",
    "https://www.airbnb.com.co/s/homes?tab_id=home_tab&refinement_paths%5B%5D=%2Fhomes&query=bogot%C3%A1&place_id=ChIJB3UJ2yYA8pYR0V4K0l6g5Q4&checkin=2024-07-01&checkout=2024-07-05&adults=2&children=0&infants=0&search_type=filter_change",
    "https://www.booking.com/searchresults.es.html?aid=304142&label=gen173nr-1FCAEoggI46AdIM1gEaLsBiAEBmAExuAEHyAEM2AEB6AEB-AECiAIBqAIDuALY7J6MBsACAdICJDRjYjEwYzY",
    "https://www.instagram.com/"
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

  it("no debe confundir una marca contenida dentro de una palabra con suplantación", async () => {
    const resultado = await motor.analizarUrl("https://mygoogleanalytics.example.com/dashboard");

    expect(resultado.clasificacion).toBe("SEGURO");
    expect(resultado.reglasCoincidentes.some((regla) => regla.id === "REGLA_SUPLANTACION")).toBe(false);
  });
});
