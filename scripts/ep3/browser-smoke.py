import datetime
import json
import os
import secrets
import time
import urllib.request
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

base = os.getenv("FRONTEND_URL", "http://localhost:8088").rstrip("/")
email = "browser-" + secrets.token_hex(6) + "@example.test"
password = secrets.token_hex(16)

def post(path, payload):
    req = urllib.request.Request(base + path, data=json.dumps(payload).encode(),
                                 headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=15) as response:
        return json.load(response)

for attempt in range(60):
    try:
        with urllib.request.urlopen(base + "/healthz", timeout=5) as r:
            assert r.status == 200
        break
    except Exception:
        if attempt == 59:
            raise
        time.sleep(3)

post("/api/auth/register", {"email": email, "password": password})
Path("artifacts").mkdir(exist_ok=True)
with sync_playwright() as pw:
    browser = pw.chromium.launch(headless=True)
    page = browser.new_page()
    # Evidencia de navegación pública y documentación real del Gateway.
    for route, filename in [("/", "07-inicio.png"), ("/carreras", "08-catalogo.png"), ("/mapa", "09-mapa.png"), ("/comparar", "10-comparador.png")]:
        response = page.goto(base + route)
        assert response and response.ok, route
        expect(page.locator("main")).to_be_visible(timeout=30000)
        page.screenshot(path="artifacts/" + filename)
    page.goto(base + "/carreras")
    expect(page.locator(".ofertas-grid").first).to_be_visible(timeout=30000)
    expect(page.locator(".ofertas-grid a").first).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/08-catalogo.png", full_page=True)
    response = page.goto(base + "/swagger-ui/index.html")
    assert response and response.ok, "Swagger UI no disponible"
    expect(page.locator(".swagger-ui .opblock").first).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/11-swagger.png")
    response = page.goto(base + "/api/ofertas")
    assert response and response.status == 200
    assert isinstance(response.json(), list) and response.json()
    page.screenshot(path="artifacts/12-api-ofertas-200.png")
    response = page.goto(base + "/api/usuarios")
    assert response and response.status == 401
    page.screenshot(path="artifacts/13-api-usuarios-401.png")
    page.goto(base + "/registro")
    expect(page.get_by_role("heading", name="Crea tu cuenta")).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/01-registro.png", full_page=True)
    page.goto(base + "/login")
    page.screenshot(path="artifacts/02-login.png", full_page=True)
    page.get_by_label("Correo electrónico").fill(email)
    page.get_by_label("Contraseña", exact=True).fill(password)
    page.get_by_role("button", name="Ingresar", exact=True).click()
    expect(page.get_by_role("heading", name="Panel del estudiante")).to_be_visible(timeout=30000)
    page.reload()
    expect(page.get_by_role("heading", name="Panel del estudiante")).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/03-panel-estudiante.png", full_page=True)
    page.get_by_role("link", name="Solicitar orientación").click()
    expect(page.locator("#ofertaSolicitud option").first).to_be_attached(timeout=30000)
    page.locator("#ofertaSolicitud").select_option(index=0)
    page.get_by_label("Motivo de consulta").fill("Prueba real de integración Fullstack")
    page.get_by_label("Fecha preferida").fill((datetime.date.today() + datetime.timedelta(days=2)).isoformat())
    page.screenshot(path="artifacts/04-solicitar-orientacion.png", full_page=True)
    with page.expect_response(lambda r: r.request.method == "POST" and r.url.endswith("/api/solicitudes"), timeout=30000) as pending:
        page.get_by_role("button", name="Enviar solicitud").click()
    submitted = pending.value
    assert submitted.ok, "La API no creó la solicitud"
    result = submitted.json()
    # No registrar cabeceras Authorization, credenciales ni tokens.
    Path("artifacts/http-solicitud.json").write_text(json.dumps({
        "method": submitted.request.method,
        "path": "/api/solicitudes",
        "status": submitted.status,
        "request": submitted.request.post_data_json,
        "response": {k: result[k] for k in ("id", "estado", "ofertaId", "motivo") if k in result},
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    expect(page.get_by_role("heading", name="Solicitud enviada")).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/05-solicitud-enviada.png", full_page=True)
    page.get_by_role("link", name="Ver mis solicitudes").click()
    expect(page.get_by_text("Prueba real de integración Fullstack", exact=False)).to_be_visible(timeout=30000)
    page.reload()
    expect(page.get_by_text("Prueba real de integración Fullstack", exact=False)).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/fullstack-browser.png", full_page=True)
    page.goto(base + "/admin/usuarios")
    expect(page.get_by_role("heading", name="Panel del estudiante")).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/06-acceso-admin-restringido.png", full_page=True)
    browser.close()
print("Browser: JWT, REST, solicitud persistida tras recarga y restricción de rol verificadas")
