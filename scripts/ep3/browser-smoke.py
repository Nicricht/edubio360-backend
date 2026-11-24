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
    page.goto(base + "/login")
    page.get_by_label("Correo electrónico").fill(email)
    page.get_by_label("Contraseña", exact=True).fill(password)
    page.get_by_role("button", name="Ingresar", exact=True).click()
    expect(page.get_by_role("heading", name="Panel del estudiante")).to_be_visible(timeout=30000)
    page.reload()
    expect(page.get_by_role("heading", name="Panel del estudiante")).to_be_visible(timeout=30000)
    page.get_by_role("link", name="Solicitar orientación").click()
    expect(page.locator("#ofertaSolicitud option").first).to_be_attached(timeout=30000)
    page.locator("#ofertaSolicitud").select_option(index=0)
    page.get_by_label("Motivo de consulta").fill("Prueba real de integración Fullstack")
    page.get_by_label("Fecha preferida").fill((datetime.date.today() + datetime.timedelta(days=2)).isoformat())
    page.get_by_role("button", name="Enviar solicitud").click()
    expect(page.get_by_role("heading", name="Solicitud enviada")).to_be_visible(timeout=30000)
    page.get_by_role("link", name="Ver mis solicitudes").click()
    expect(page.get_by_text("Prueba real de integración Fullstack", exact=False)).to_be_visible(timeout=30000)
    page.reload()
    expect(page.get_by_text("Prueba real de integración Fullstack", exact=False)).to_be_visible(timeout=30000)
    page.screenshot(path="artifacts/fullstack-browser.png", full_page=True)
    page.goto(base + "/admin/usuarios")
    expect(page.get_by_role("heading", name="Panel del estudiante")).to_be_visible(timeout=30000)
    browser.close()
print("Browser: JWT, REST, solicitud persistida tras recarga y restricción de rol verificadas")
