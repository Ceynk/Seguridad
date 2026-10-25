import { describe, it, expect } from "vitest";
import { extraerCaracteristicasUrl } from "../src/analyzer/extractor-caracteristicas";

describe("Extractor de Características de URLs", () => {
  it("debe extraer correctamente protocolo, host, ruta y parámetros de una URL estándar", () => {
    const url = "https://www.ejemplo.com.co/cuenta/login?usuario=juan&origen=web";
    const caracteristicas = extraerCaracteristicasUrl(url);

    expect(caracteristicas.protocolo).toBe("https");
    expect(caracteristicas.nombreHost).toBe("www.ejemplo.com.co");
    expect(caracteristicas.dominioPrincipal).toBe("ejemplo.com.co");
    expect(caracteristicas.subdominios).toEqual(["www"]);
    expect(caracteristicas.cantidadSubdominios).toBe(1);
    expect(caracteristicas.cantidadParametros).toBe(2);
    expect(caracteristicas.esIpDirecta).toBe(false);
  });

  it("debe detectar direcciones IP directas en formato IPv4 e IPv6", () => {
    const urlIpv4 = "http://192.168.1.100/login/bancolombia";
    const carIpv4 = extraerCaracteristicasUrl(urlIpv4);
    expect(carIpv4.esIpDirecta).toBe(true);
    expect(carIpv4.versionIp).toBe("IPv4");

    const urlIpv6 = "http://[2001:db8::1]/verificar";
    const carIpv6 = extraerCaracteristicasUrl(urlIpv6);
    expect(carIpv6.esIpDirecta).toBe(true);
    expect(carIpv6.versionIp).toBe("IPv6");
  });

  it("debe detectar uso de punycode (IDN)", () => {
    const urlPunycode = "https://xn--bancolomba-1cb.com/seguridad";
    const carPunycode = extraerCaracteristicasUrl(urlPunycode);
    expect(carPunycode.esPunycode).toBe(true);
  });

  it("debe detectar el símbolo @ en la URL", () => {
    const urlArroba = "https://bancolombia.com@phishing-servidor.xyz/ingreso";
    const carArroba = extraerCaracteristicasUrl(urlArroba);
    expect(carArroba.tieneArroba).toBe(true);
  });

  it("no debe marcar el identificador @ de un canal de YouTube", () => {
    const carYouTube = extraerCaracteristicasUrl("https://www.youtube.com/@canal-ejemplo");
    expect(carYouTube.tieneArroba).toBe(false);
  });

  it("debe identificar palabras clave de autenticación, banca y urgencia", () => {
    const urlSensible = "http://tramites-urgentes.xyz/dian/fotomulta/pagos/verificar-clave";
    const car = extraerCaracteristicasUrl(urlSensible);

    expect(car.contienePalabrasAutenticacion).toContain("verificar");
    expect(car.contienePalabrasAutenticacion).toContain("clave");
    expect(car.contienePalabrasBancarias).toContain("pagos");
    expect(car.contienePalabrasUrgencia).toContain("fotomulta");
    expect(car.contienePalabrasUrgencia).toContain("urgente");
  });

  it("debe reconocer TLDs clasificados como de alto riesgo de abuso", () => {
    const urlTld = "https://portal-servicios.xyz/acceso";
    const car = extraerCaracteristicasUrl(urlTld);
    expect(car.extensionTld).toBe("xyz");
    expect(car.tldEsSospechoso).toBe(true);
  });
});
