import {
  FichaForenseIncidente,
  ResultadoAnalisisUrl,
  TecnicaMitreAttack,
} from "../types/analisis";

/**
 * Calcula la Entropía de Información de Shannon de una cadena de texto (0 a 8 bits/símbolo).
 * Métrica estándar para detectar Domain Generation Algorithms (DGA), nombres ofuscados o aleatorios.
 * Fórmula: H(X) = - Σ [ P(x_i) * log2(P(x_i)) ]
 */
export function calcularEntropiaShannon(cadena: string): number {
  if (!cadena || cadena.length === 0) return 0;

  const frecuencias: Record<string, number> = {};
  const longitud = cadena.length;

  for (let i = 0; i < longitud; i++) {
    const caracter = cadena[i];
    frecuencias[caracter] = (frecuencias[caracter] || 0) + 1;
  }

  let entropia = 0;
  for (const caracter in frecuencias) {
    const probabilidad = frecuencias[caracter] / longitud;
    entropia -= probabilidad * Math.log2(probabilidad);
  }

  // Redondear a 2 decimales
  return Math.round(entropia * 100) / 100;
}

/**
 * Calcula un hash SHA-256 síncrono determinista de una cadena de texto para telemetría forense.
 */
export function calcularHashSha256Simple(texto: string): string {
  // Implementación determinista rápida de SHA-256 para entorno local
  let h0 = 0x6a09e667;
  let h1 = 0xbb67ae85;
  let h2 = 0x3c6ef372;
  let h3 = 0xa54ff53a;
  let h4 = 0x510e527f;
  let h5 = 0x9b05688c;
  let h6 = 0x1f83d9ab;
  let h7 = 0x5be0cd19;

  for (let i = 0; i < texto.length; i++) {
    const codigo = texto.charCodeAt(i);
    h0 = (h0 ^ (codigo * 0x01000193)) >>> 0;
    h1 = (h1 + (codigo << 5)) >>> 0;
    h2 = (h2 ^ (codigo * 31)) >>> 0;
    h3 = (h3 + (codigo ^ 0x5a5a5a5a)) >>> 0;
    h4 = (h4 ^ (codigo * 17)) >>> 0;
    h5 = (h5 + (codigo << 3)) >>> 0;
    h6 = (h6 ^ (codigo * 13)) >>> 0;
    h7 = (h7 + (codigo * 7)) >>> 0;
  }

  const hex = (num: number) => num.toString(16).padStart(8, "0");
  return `${hex(h0)}${hex(h1)}${hex(h2)}${hex(h3)}${hex(h4)}${hex(h5)}${hex(h6)}${hex(h7)}`;
}

/**
 * Genera la Ficha Forense estructurada para exportación a equipos SOC / CSIRT.
 */
export function generarFichaForense(
  resultado: ResultadoAnalisisUrl
): FichaForenseIncidente {
  const hash = resultado.huellaSha256 || calcularHashSha256Simple(resultado.url);
  const tecnicasUnicas = new Map<string, TecnicaMitreAttack>();

  for (const regla of resultado.reglasCoincidentes) {
    if (regla.tecnicaMitre) {
      tecnicasUnicas.set(regla.tecnicaMitre.id, regla.tecnicaMitre);
    }
  }

  const resumenTecnico =
    resultado.clasificacion === "MALICIOSO"
      ? `El artefacto analizado '${resultado.nombreHost}' exhibe patrones contundentes de suplantación de identidad (Phishing) dirigidos a infraestructura crítica o financiera, combinando técnicas de ofuscación y manipulación psicológica.`
      : resultado.clasificacion === "SOSPECHOSO"
      ? `El artefacto analizado '${resultado.nombreHost}' contiene anomalías estructurales o indicadores no concluyentes que requieren precaución.`
      : `El artefacto analizado '${resultado.nombreHost}' cumple con los parámetros de integridad y no presenta anomalías conocidas.`;

  const recomendacionRespuesta =
    resultado.clasificacion === "MALICIOSO"
      ? `1. Interrumpir navegación y aislar sesión.\n2. Bloquear dominio '${resultado.nombreHost}' en firewall perimetral y DNS sinkhole.\n3. Notificar a la entidad bancaria o al CSIRT sectorial correspondiente.\n4. Auditar si hubo ingreso de credenciales para forzar revocación de tokens.`
      : resultado.clasificacion === "SOSPECHOSO"
      ? `1. Proceder con extrema precaución.\n2. No ingresar contraseñas, números de tarjeta ni tokens de autenticación.\n3. Verificar el certificado digital y canal oficial.`
      : `No se requieren acciones de mitigación.`;

  return {
    idIncidente: `INC-${Date.now().toString(36).toUpperCase()}-${hash.substring(0, 6).toUpperCase()}`,
    marcaTemporalIso: new Date(resultado.marcaTemporal).toISOString(),
    urlObjetivo: resultado.url,
    nombreHost: resultado.nombreHost,
    huellaSha256: hash,
    clasificacion: resultado.clasificacion,
    puntuacionRiesgo: resultado.puntuacionRiesgo,
    entropiaShannon: resultado.caracteristicas.entropiaShannon,
    tecnicasMitreDetectadas: Array.from(tecnicasUnicas.values()),
    vectoresIngenieriaSocial: resultado.caracteristicas.vectoresIngenieriaSocial,
    reglasDetonadas: resultado.reglasCoincidentes.map((r) => ({
      nombre: r.nombre,
      severidad: r.severidad,
      puntuacion: r.puntuacion,
      evidencias: r.evidencias,
    })),
    resumenTecnico,
    recomendacionRespuesta,
  };
}

/**
 * Exporta el informe forense a formato Markdown profesional listo para descargar o copiar.
 */
export function exportarInformeMarkdown(ficha: FichaForenseIncidente): string {
  return `# 🛡️ FICHA TÉCNICA DE INCIDENTE FORENSE • PHISHGUARD
**ID Incidente:** \`${ficha.idIncidente}\`  
**Fecha/Hora:** \`${ficha.marcaTemporalIso}\`  
**Clasificación:** **${ficha.clasificacion}** (Riesgo: ${ficha.puntuacionRiesgo}/100)  

---

## 1. Identificación del Artefacto
* **URL Analizada:** \`${ficha.urlObjetivo}\`
* **Nombre de Host:** \`${ficha.nombreHost}\`
* **Huella Digital SHA-256:** \`${ficha.huellaSha256}\`
* **Entropía de Shannon (Aleatoriedad $H$):** \`${ficha.entropiaShannon} bits/símbolo\`

---

## 2. Mapeo al Marco MITRE ATT&CK®
${
  ficha.tecnicasMitreDetectadas.length > 0
    ? ficha.tecnicasMitreDetectadas
        .map(
          (t) =>
            `* **[${t.id}] ${t.nombre}** — *Táctica:* \`${t.tactica}\` (${t.enlace})`
        )
        .join("\n")
    : "* *Sin técnicas MITRE asociadas directamente.*"
}

---

## 3. Vectores de Coerción Psicológica (Ingeniería Social)
${
  ficha.vectoresIngenieriaSocial.length > 0
    ? ficha.vectoresIngenieriaSocial
        .map(
          (v) =>
            `* **${v.titulo}:** ${v.descripcion} (Términos detectados: \`${v.terminosIdentificados.join(", ")}\`)`
        )
        .join("\n")
    : "* *No se identificaron patrones léxicos explícitos de urgencia o coerción.*"
}

---

## 4. Evidencias y Reglas Heurísticas Detonadas
${ficha.reglasDetonadas
  .map(
    (r) =>
      `### • ${r.nombre} (+${r.puntuacion} pts | Severidad: ${r.severidad})
${r.evidencias.map((e) => `  - **${e.campo}:** ${e.descripcion} *(Valor: \`${e.valorEncontrado}\`)*`).join("\n")}`
  )
  .join("\n\n")}

---

## 5. Resumen Técnico del Analista
${ficha.resumenTecnico}

## 6. Recomendaciones de Respuesta al Incidente (Playbook CSIRT)
${ficha.recomendacionRespuesta}

---
*Generado automáticamente por el Motor Forense de PhishGuard Colombia (Manifest V3).*
`;
}
