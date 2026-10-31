import { describe, it, expect } from "vitest";
import { extraerCaracteristicasUrl } from "../src/analizador/extractor-caracteristicas";

describe("Extractor de Características de URLs", () => {
  it("debe extraer correctamente protocolo, host, ruta y parámetros de una URL estándar", () => {
    const caracteristicas = extraerCaracteristicasUrl(
      "https://www.ejemplo.com.co/cuenta/login?usuario=juan&origen=web"
    );

    expect(caracteristicas.protocolo).toBe("https");
    expect(caracteristicas.nombreHost).toBe("www.ejemplo.com.co");
    expect(caracteristicas.dominioPrincipal).toBe("ejemplo.com.co");
    expect(caracteristicas.subdominios).toEqual(["www"]);
    expect(caracteristicas.cantidadSubdominios).toBe(1);
    expect(caracteristicas.cantidadParametros).toBe(2);
    expect(caracteristicas.esIpDirecta).toBe(false);
  });

  it("debe detectar direcciones IP directas en formato IPv4 e IPv6", () => {
    const ipv4 = extraerCaracteristicasUrl("http://192.168.1.100/login/bancolombia");
    expect(ipv4.esIpDirecta).toBe(true);
    expect(ipv4.versionIp).toBe("IPv4");

    const ipv6 = extraerCaracteristicasUrl("http://[2001:db8::1]/verificar");
    expect(ipv6.esIpDirecta).toBe(true);
    expect(ipv6.versionIp).toBe("IPv6");
  });

  it("debe detectar uso de punycode (IDN)", () => {
    expect(extraerCaracteristicasUrl("https://xn--bancolomba-1cb.com/seguridad").esPunycode).toBe(true);
  });

  it("debe detectar el símbolo @ en la URL", () => {
    expect(extraerCaracteristicasUrl("https://bancolombia.com@phishing-servidor.xyz/ingreso").tieneArroba).toBe(true);
  });

  it("no debe marcar el identificador @ de un canal de YouTube", () => {
    expect(extraerCaracteristicasUrl("https://www.youtube.com/@canal-ejemplo").tieneArroba).toBe(false);
  });

  it("debe identificar palabras clave de autenticación, banca y urgencia", () => {
    const caracteristicas = extraerCaracteristicasUrl(
      "http://tramites-urgentes.xyz/dian/fotomulta/pagos/verificar-clave"
    );

    expect(caracteristicas.contienePalabrasAutenticacion).toContain("verificar");
    expect(caracteristicas.contienePalabrasAutenticacion).toContain("clave");
    expect(caracteristicas.contienePalabrasBancarias).toContain("pagos");
    expect(caracteristicas.contienePalabrasUrgencia).toContain("fotomulta");
    expect(caracteristicas.contienePalabrasUrgencia).toContain("urgente");
  });

  it("debe reconocer TLDs clasificados como de alto riesgo de abuso", () => {
    const caracteristicas = extraerCaracteristicasUrl("https://portal-servicios.xyz/acceso");
    expect(caracteristicas.extensionTld).toBe("xyz");
    expect(caracteristicas.tldEsSospechoso).toBe(true);
  });
});
