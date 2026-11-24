"""Screenshots of the actual local CI dashboard, after data verification."""
import os
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

base = os.getenv("GRAFANA_URL", "http://127.0.0.1:3000")
Path("artifacts").mkdir(exist_ok=True)
with sync_playwright() as pw:
    browser = pw.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1600, "height": 1000})
    page.goto(base + "/login", wait_until="domcontentloaded")
    page.locator('input[name="user"]').fill("admin")
    page.locator('input[name="password"]').fill(os.environ["GRAFANA_ADMIN_PASSWORD"])
    page.get_by_role("button", name="Log in", exact=True).click()
    page.wait_for_url(lambda url: "/login" not in url, timeout=30000)
    page.goto(base + "/d/edubio-ep3?from=now-30m&to=now", wait_until="domcontentloaded")
    for title, name in [
        ("Disponibilidad por servicio", "grafana-disponibilidad"),
        ("Cobertura de líneas JaCoCo", "grafana-cobertura"),
        ("CPU de contenedores (núcleos)", "grafana-recursos"),
    ]:
        panel = page.get_by_text(title, exact=True).first
        panel.scroll_into_view_if_needed(timeout=60000)
        expect(panel).to_be_visible(timeout=30000)
        page.screenshot(path=f"artifacts/{name}.png")
    browser.close()
