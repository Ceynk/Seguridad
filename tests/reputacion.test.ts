import { describe, it, expect } from "vitest";
import { ProveedorReputacionLocal } from "../src/reputation/reputacion-local";

describe("Proveedor de Reputación Local", () => {
  const proveedor = new ProveedorReputacionLocal();

  it("debe reconocer dominios legítimos oficiales incluidos en la lista blanca por defecto", async () => {
    const resGoogle = await proveedor.consultarDominio("google.com");
    expect(resGoogle.encontrado).toBe(true);
    expect(resGoogle.tipo).toBe("BLANCA");

    const resBancolombia = await proveedor.consultarDominio("bancolombia.com");
    expect(resBancolombia.encontrado).toBe(true);
    expect(resBancolombia.tipo).toBe("BLANCA");

    const resSubdominio = await proveedor.consultarDominio("sucursalvirtual.bancolombia.com");
    expect(resSubdominio.encontrado).toBe(true);
    expect(resSubdominio.tipo).toBe("BLANCA");

    const resDian = await proveedor.consultarDominio("muisca.dian.gov.co");
    expect(resDian.encontrado).toBe(true);
    expect(resDian.tipo).toBe("BLANCA");

    const resInstagram = await proveedor.consultarDominio("instagram.com");
    expect(resInstagram.encontrado).toBe(true);
    expect(resInstagram.tipo).toBe("BLANCA");

    const resWwwInstagram = await proveedor.consultarDominio("www.instagram.com");
    expect(resWwwInstagram.encontrado).toBe(true);
    expect(resWwwInstagram.tipo).toBe("BLANCA");
  });

  it("debe reconocer dominios ficticios de prueba catalogados en la lista negra", async () => {
    const resPhishing = await proveedor.consultarDominio("phishing-bancolombia-ficticio.xyz");
    expect(resPhishing.encontrado).toBe(true);
    expect(resPhishing.tipo).toBe("NEGRA");

    const resNequiFalso = await proveedor.consultarDominio("nequi-login-falso-prueba.top");
    expect(resNequiFalso.encontrado).toBe(true);
    expect(resNequiFalso.tipo).toBe("NEGRA");
  });

  it("debe retornar no encontrado ante dominios desconocidos no registrados", async () => {
    const resDesconocido = await proveedor.consultarDominio("mi-sitio-nuevo-personal-123.com");
    expect(resDesconocido.encontrado).toBe(false);
  });

  it("debe permitir añadir listas blancas y negras personalizadas del usuario con soporte para URLs completas y subdominios", async () => {
    const proveedorConListas = new ProveedorReputacionLocal();
    proveedorConListas.actualizarListasUsuario(
      ["https://www.instagram.com/", "miempresa-interna.com"],
      ["https://sitio-molesto-bloqueado.xyz/login"]
    );

    // Debe coincidir con www.instagram.com
    const resInstagramWww = await proveedorConListas.consultarDominio("www.instagram.com");
    expect(resInstagramWww.encontrado).toBe(true);
    expect(resInstagramWww.tipo).toBe("BLANCA");

    // Debe coincidir con instagram.com (sin www)
    const resInstagramBase = await proveedorConListas.consultarDominio("instagram.com");
    expect(resInstagramBase.encontrado).toBe(true);
    expect(resInstagramBase.tipo).toBe("BLANCA");

    // Debe coincidir subdominio de miempresa-interna.com
    const resSubempresa = await proveedorConListas.consultarDominio("portal.miempresa-interna.com");
    expect(resSubempresa.encontrado).toBe(true);
    expect(resSubempresa.tipo).toBe("BLANCA");

    // Debe coincidir lista negra normalizada
    const resNegra = await proveedorConListas.consultarDominio("sitio-molesto-bloqueado.xyz");
    expect(resNegra.encontrado).toBe(true);
    expect(resNegra.tipo).toBe("NEGRA");
  });
});
