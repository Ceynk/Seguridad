import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";

const tablaCrc = Array.from({ length: 256 }, (_, indice) => {
  let valor = indice;
  for (let paso = 0; paso < 8; paso++) {
    valor = valor & 1 ? 0xedb88320 ^ (valor >>> 1) : valor >>> 1;
  }
  return valor;
});

function calcularCrc32(datos) {
  let crc = 0xffffffff;
  for (const octeto of datos) {
    crc = tablaCrc[(crc ^ octeto) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function crearBloque(tipo, datos) {
  const bloque = Buffer.alloc(datos.length + 12);
  bloque.writeUInt32BE(datos.length, 0);
  bloque.write(tipo, 4, 4, "ascii");
  datos.copy(bloque, 8);
  bloque.writeUInt32BE(
    calcularCrc32(Buffer.concat([Buffer.from(tipo, "ascii"), datos])),
    datos.length + 8
  );
  return bloque;
}

function crearPng(ancho, alto, rojo, verde, azul) {
  const firma = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const cabecera = Buffer.alloc(13);
  cabecera.writeUInt32BE(ancho, 0);
  cabecera.writeUInt32BE(alto, 4);
  cabecera[8] = 8;
  cabecera[9] = 6;

  const tamanoFila = ancho * 4 + 1;
  const datosCrudos = Buffer.alloc(tamanoFila * alto);
  const radio = ancho * 0.45;
  const centroX = ancho / 2;
  const centroY = alto / 2;

  for (let y = 0; y < alto; y++) {
    const inicioFila = y * tamanoFila;
    for (let x = 0; x < ancho; x++) {
      if (Math.hypot(x - centroX, y - centroY) <= radio) {
        const posicion = inicioFila + 1 + x * 4;
        datosCrudos[posicion] = rojo;
        datosCrudos[posicion + 1] = verde;
        datosCrudos[posicion + 2] = azul;
        datosCrudos[posicion + 3] = 255;
      }
    }
  }

  return Buffer.concat([
    firma,
    crearBloque("IHDR", cabecera),
    crearBloque("IDAT", zlib.deflateSync(datosCrudos)),
    crearBloque("IEND", Buffer.alloc(0)),
  ]);
}

const directorioIconos = path.resolve("public/icons");
fs.mkdirSync(directorioIconos, { recursive: true });

for (const tamano of [16, 32, 48, 128]) {
  fs.writeFileSync(
    path.join(directorioIconos, `icon-${tamano}.png`),
    crearPng(tamano, tamano, 59, 130, 246)
  );
}
