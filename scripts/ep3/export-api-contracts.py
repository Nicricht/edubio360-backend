#!/usr/bin/env python3
"""Exporta contratos reales desde Springdoc a Excel, sin tokens ni datos personales."""
import argparse
import json
from pathlib import Path
from urllib.request import urlopen

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill

MODULES = ("auth", "academic", "guidance", "notification", "analytics", "import")
METHODS = {"get", "post", "put", "patch", "delete", "head", "options"}


def access(path, method):
    if path in ("/api/auth/login", "/api/auth/register"):
        return "Público"
    if method == "GET" and (path == "/api/ofertas" or path == "/api/ofertas/{id}" or path == "/api/sedes"):
        return "Público"
    if path in ("/api/notificaciones/mias", "/api/notificaciones/mias/leidas"):
        return "JWT: propio usuario"
    if path.startswith(("/api/usuarios", "/api/importaciones", "/api/notificaciones")):
        return "ADMIN"
    if path.startswith(("/api/ofertas", "/api/sedes", "/api/analytics", "/api/metricas")) and method != "GET":
        return "ADMIN"
    if path.startswith("/api/solicitudes"):
        if method == "GET":
            return {"mias": "STUDENT: propias", "disponibles": "ORIENTADOR: disponibles/asignadas"}.get(path.rsplit("/", 1)[-1], "ADMIN")
        if path.endswith(("/confirmar", "/gestionar")):
            return "ORIENTADOR: propiedad/estado según operación"
        return "STUDENT: propiedad según operación"
    return "JWT"


def collect(base, out):
    contracts = out / "openapi"
    contracts.mkdir(parents=True, exist_ok=True)
    rows = []
    for module in MODULES:
        with urlopen(base.rstrip("/") + "/openapi/" + module, timeout=30) as response:
            doc = json.load(response)
        if not doc.get("paths"):
            raise ValueError("No hay operaciones OpenAPI en " + module)
        (contracts / (module + ".json")).write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding="utf-8")
        for path, operations in sorted(doc["paths"].items()):
            for method, op in sorted(operations.items()):
                if method not in METHODS:
                    continue
                request = op.get("requestBody", {}).get("content", {})
                schema = request.get("application/json", {}).get("schema", {})
                params = [p.get("name", "") + " (" + p.get("in", "") + ")" for p in op.get("parameters", [])]
                rows.append([module, method.upper(), path, op.get("operationId", ""), access(path, method.upper()),
                             ", ".join(params), json.dumps(schema, ensure_ascii=False) if schema else "",
                             ", ".join(sorted(op.get("responses", {}).keys())), base.rstrip("/") + "/openapi/" + module])
    return rows


def write_excel(rows, output):
    wb = Workbook()
    ws = wb.active
    ws.title = "Endpoints"
    ws.append(["Servicio", "Método", "Ruta", "Operación", "Acceso (Gateway estricto)", "Parámetros", "Esquema de entrada", "Respuestas Swagger", "Contrato OpenAPI"])
    for row in rows:
        ws.append(row)
    ws.freeze_panes = "C2"
    ws.auto_filter.ref = ws.dimensions
    for cell in ws[1]:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = PatternFill("solid", fgColor="18465A")
        cell.alignment = Alignment(wrap_text=True)
    for col, width in zip("ABCDEFGHI", (18, 12, 37, 30, 42, 35, 50, 24, 55)):
        ws.column_dimensions[col].width = width
    for row in ws.iter_rows(min_row=2):
        for cell in row:
            cell.alignment = Alignment(vertical="top", wrap_text=True)
    ws.row_dimensions[1].height = 32
    note = wb.create_sheet("Alcance")
    note.append(["Fuente", "Contratos OpenAPI obtenidos del Gateway en ejecución"])
    note.append(["Autorización", "Derivada de reglas del Gateway y servicios en modo estricto; no sustituye pruebas 401/403"])
    note.append(["Respuestas", "Códigos declarados en Swagger; no afirman que hayan sido ejecutados"])
    note.column_dimensions["A"].width = 22
    note.column_dimensions["B"].width = 100
    wb.save(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True, help="URL del Gateway en ejecución")
    parser.add_argument("--out", default="artifacts/contratos-api")
    args = parser.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    rows = collect(args.base, out)
    write_excel(rows, out / "EDUBIO360_EP3_Endpoints.xlsx")
    print(f"Contratos exportados: {len(rows)} operaciones de {len(MODULES)} servicios.")
