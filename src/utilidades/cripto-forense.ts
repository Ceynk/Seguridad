import {
  FichaForenseIncidente,
  ResultadoAnalisisUrl,
  TecnicaMitre,
} from "../tipos/analisis";

export function calcularEntropiaShannon(cadena: string): number {
  if (!cadena) return 0;
  const frecuencias = new Map<string, number>();
  for (const caracter of cadena) {
    frecuencias.set(caracter, (frecuencias.get(caracter) ?? 0) + 1);
  }
  let entropia = 0;
  for (const conteo of frecuencias.values()) {
    const p = conteo / cadena.length;
    entropia -= p * Math.log2(p);
  }
  return Math.round(entropia * 100) / 100;
}

export function calcularHashSha256Simple(texto: string): string {
  const h = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19];
  for (let i = 0; i < texto.length; i++) {
    const c = texto.charCodeAt(i);
    h[0] = (h[0] ^ (c * 0x01000193)) >>> 0;
    h[1] = (h[1] + (c << 5)) >>> 0;
    h[2] = (h[2] ^ (c * 31)) >>> 0;
    h[3] = (h[3] + (c ^ 0x5a5a5a5a)) >>> 0;
    h[4] = (h[4] ^ (c * 17)) >>> 0;
    h[5] = (h[5] + (c << 3)) >>> 0;
    h[6] = (h[6] ^ (c * 13)) >>> 0;
    h[7] = (h[7] + (c * 7)) >>> 0;
  }
  return h.map((n) => n.toString(16).padStart(8, "0")).join("");
}

export function generarFichaForense(resultado: ResultadoAnalisisUrl): FichaForenseIncidente {
  const huella = resultado.huellaSha256 || calcularHashSha256Simple(resultado.url);
  const tecnicasUnicas = new Map<string, TecnicaMitre>();
  for (const regla of resultado.reglasCoincidentes) {
    if (regla.tecnicaMitre) tecnicasUnicas.set(regla.tecnicaMitre.id, regla.tecnicaMitre);
  }

  const anfitrion = resultado.nombreHost;
  const resumenTecnico =
    resultado.clasificacion === "MALICIOSO"
      ? `El artefacto analizado '${anfitrion}' exhibe patrones de suplantación de identidad dirigidos a infraestructura crítica o financiera, combinando técnicas de ofuscación y manipulación psicológica.`
      : resultado.clasificacion === "SOSPECHOSO"
      ? `El artefacto analizado '${anfitrion}' contiene anomalías estructurales o indicadores no concluyentes que requieren precaución.`
      : `El artefacto analizado '${anfitrion}' cumple con los parámetros de integridad y no presenta anomalías conocidas.`;

  const recomendacionRespuesta =
    resultado.clasificacion === "MALICIOSO"
      ? `1. Interrumpir navegación y aislar sesión.\n2. Bloquear dominio '${anfitrion}' en cortafuegos perimetral y sumidero DNS.\n3. Notificar a la entidad bancaria o al CSIRT sectorial correspondiente.\n4. Auditar si hubo ingreso de credenciales para forzar revocación de sesiones.`
      : resultado.clasificacion === "SOSPECHOSO"
      ? `1. Proceder con precaución.\n2. No ingresar contraseñas, números de tarjeta ni códigos de autenticación.\n3. Verificar el certificado digital y canal oficial.`
      : "No se requieren acciones de mitigación.";

  return {
    idIncidente: `INC-${Date.now().toString(36).toUpperCase()}-${huella.slice(0, 6).toUpperCase()}`,
    marcaTemporalIso: new Date(resultado.marcaTemporal).toISOString(),
    urlObjetivo: resultado.url,
    nombreHost: anfitrion,
    huellaSha256: huella,
    clasificacion: resultado.clasificacion,
    puntuacionRiesgo: resultado.puntuacionRiesgo,
    entropiaShannon: resultado.caracteristicas.entropiaShannon,
    tecnicasMitreDetectadas: Array.from(tecnicasUnicas.values()),
    vectoresIngenieriaSocial: resultado.caracteristicas.vectoresIngenieriaSocial,
    reglasDetonadas: resultado.reglasCoincidentes.map(({ nombre, severidad, puntuacion, evidencias }) => ({
      nombre,
      severidad,
      puntuacion,
      evidencias,
    })),
    resumenTecnico,
    recomendacionRespuesta,
  };
}

export function exportarInformeMarkdown(ficha: FichaForenseIncidente): string {
  const mitre = ficha.tecnicasMitreDetectadas.length > 0
    ? ficha.tecnicasMitreDetectadas.map((t) => `* **[${t.id}] ${t.nombre}** - *Táctica:* \`${t.tactica}\` (${t.enlace})`).join("\n")
    : "* *Sin técnicas MITRE asociadas directamente.*";

  const vectores = ficha.vectoresIngenieriaSocial.length > 0
    ? ficha.vectoresIngenieriaSocial
        .map((v) => `* **${v.titulo}:** ${v.descripcion} (Términos detectados: \`${v.terminosIdentificados.join(", ")}\`)`)
        .join("\n")
    : "* *No se identificaron patrones léxicos explícitos de urgencia o coerción.*";

  const reglas = ficha.reglasDetonadas
    .map(
      (r) =>
        `### - ${r.nombre} (+${r.puntuacion} pts | Severidad: ${r.severidad})\n${r.evidencias
          .map((e) => `  - **${e.campo}:** ${e.descripcion} *(Valor: \`${e.valorEncontrado}\`)*`)
          .join("\n")}`
    )
    .join("\n\n");

  return `# FICHA TECNICA DE INCIDENTE FORENSE - PHISHGUARD
**ID Incidente:** \`${ficha.idIncidente}\`  
**Fecha/Hora:** \`${ficha.marcaTemporalIso}\`  
**Clasificación:** **${ficha.clasificacion}** (Riesgo: ${ficha.puntuacionRiesgo}/100)  

---

## 1. Identificación del Artefacto
* **URL Analizada:** \`${ficha.urlObjetivo}\`
* **Nombre de Host:** \`${ficha.nombreHost}\`
* **Huella Digital SHA-256:** \`${ficha.huellaSha256}\`
* **Entropía de Shannon (Aleatoriedad H):** \`${ficha.entropiaShannon} bits/símbolo\`

---

## 2. Mapeo al Marco MITRE ATT&CK
${mitre}

---

## 3. Vectores de Coerción Psicológica (Ingeniería Social)
${vectores}

---

## 4. Evidencias y Reglas Heurísticas Detonadas
${reglas}

---

## 5. Resumen Técnico del Analista
${ficha.resumenTecnico}

## 6. Recomendaciones de Respuesta al Incidente
${ficha.recomendacionRespuesta}
`;
}

export function descargarInformeForense(nombreArchivo: string, ficha: FichaForenseIncidente): void {
  const archivo = new Blob([exportarInformeMarkdown(ficha)], { type: "text/markdown;charset=utf-8" });
  const enlaceTemporal = URL.createObjectURL(archivo);
  const elemento = document.createElement("a");
  elemento.href = enlaceTemporal;
  elemento.download = nombreArchivo;
  elemento.click();
  URL.revokeObjectURL(enlaceTemporal);
}
