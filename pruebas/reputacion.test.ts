import { describe, it, expect } from "vitest";
import { ProveedorReputacionLocal } from "../src/reputacion/reputacion-local";

describe("Proveedor de Reputación Local", () => {
  const proveedor = new ProveedorReputacionLocal();

  it("debe reconocer dominios legítimos oficiales incluidos en la lista blanca por defecto", async () => {
    for (const dominio of [
      "google.com",
      "bancolombia.com",
      "sucursalvirtual.bancolombia.com",
      "muisca.dian.gov.co",
      "instagram.com",
      "www.instagram.com",
    ]) {
      const resultado = await proveedor.consultarDominio(dominio);
      expect(resultado.encontrado).toBe(true);
      expect(resultado.tipo).toBe("BLANCA");
    }
  });

  it("debe reconocer dominios ficticios de prueba catalogados en la lista negra", async () => {
    for (const dominio of ["phishing-bancolombia-ficticio.xyz", "nequi-login-falso-prueba.top"]) {
      const resultado = await proveedor.consultarDominio(dominio);
      expect(resultado.encontrado).toBe(true);
      expect(resultado.tipo).toBe("NEGRA");
    }
  });

  it("debe retornar no encontrado ante dominios desconocidos no registrados", async () => {
    const resultado = await proveedor.consultarDominio("mi-sitio-nuevo-personal-123.com");
    expect(resultado.encontrado).toBe(false);
  });

  it("debe permitir añadir listas blancas y negras personalizadas del usuario con soporte para URLs completas y subdominios", async () => {
    const proveedorConListas = new ProveedorReputacionLocal();
    proveedorConListas.actualizarListasUsuario(
      ["https://www.instagram.com/", "miempresa-interna.com"],
      ["https://sitio-molesto-bloqueado.xyz/login"]
    );

    for (const permitido of ["www.instagram.com", "instagram.com", "portal.miempresa-interna.com"]) {
      const resultado = await proveedorConListas.consultarDominio(permitido);
      expect(resultado.encontrado).toBe(true);
      expect(resultado.tipo).toBe("BLANCA");
    }

    const bloqueado = await proveedorConListas.consultarDominio("sitio-molesto-bloqueado.xyz");
    expect(bloqueado.encontrado).toBe(true);
    expect(bloqueado.tipo).toBe("NEGRA");
  });
});
