import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { importarFuentes } from "./importador.mjs";

const archivoDatos = resolve(
  process.env.SMISHGUARD_DATA_FILE ||
    fileURLToPath(new URL("../datos/lista-negra.json", import.meta.url))
);
const fuentes = JSON.parse(process.env.SMISHGUARD_FUENTES || "[]");

const resultado = await importarFuentes({ archivoDatos, fuentes });
console.log(JSON.stringify(resultado));
if (resultado.ejecuciones.some((ejecucion) => ejecucion.estado === "fallido")) {
  process.exitCode = 1;
}
