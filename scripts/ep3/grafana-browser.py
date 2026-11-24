"""Capture the actual authenticated Grafana dashboard from the Swarm overlay."""
import os
from pathlib import Path
from time import sleep
from urllib.parse import urljoin
from playwright.sync_api import sync_playwright, expect

base = os.getenv("GRAFANA_URL", "http://127.0.0.1:3000").rstrip("/")
password = os.environ["GRAFANA_ADMIN_PASSWORD"]
art = Path("artifacts")
art.mkdir(exist_ok=True)
with sync_playwright() as pw:
    browser = pw.chromium.launch(headless=True)
    context = browser.new_context(viewport={"width": 1600, "height": 1000})
    page = context.new_page()
    try:
        # Check the actual Grafana service, not an unrelated host listener.
        for attempt in range(20):
            try:
                health = context.request.get(base + "/api/health", timeout=5000)
                if health.ok and health.json().get("database") == "ok":
                    break
            except Exception:
                pass
            sleep(3)
        else:
            raise RuntimeError("Grafana API health unavailable")
        # Grafana's login endpoint issues a session cookie. Playwright shares
        # cookies between BrowserContext.request and the actual browser.
        login = context.request.post(
            base + "/login",
            data={"user": "admin", "password": password},
            timeout=20000,
        )
        if not login.ok:
            raise RuntimeError(f"Grafana login HTTP {login.status}")
        user = context.request.get(base + "/api/user", timeout=15000)
        if not user.ok or user.json().get("login") != "admin":
            raise RuntimeError("Grafana session not authenticated")
        response = context.request.get(
            base + "/api/dashboards/uid/edubio-ep3", timeout=20000
        )
        if not response.ok:
            raise RuntimeError(f"Dashboard API HTTP {response.status}")
        data = response.json()
        dashboard = data["dashboard"]
        if dashboard["uid"] != "edubio-ep3" or len(dashboard["panels"]) < 11:
            raise RuntimeError("Unexpected Grafana dashboard")
        import json
        (art / "grafana-dashboard-verification.json").write_text(
            json.dumps({"uid": dashboard["uid"], "title": dashboard["title"],
                        "panels": [p["title"] for p in dashboard["panels"]]},
                       ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        url = urljoin(base + "/", data["meta"]["url"])
        page.goto(url + "?from=now-30m&to=now", wait_until="domcontentloaded", timeout=60000)
        for title, name in [
            ("Disponibilidad por servicio", "grafana-disponibilidad"),
            ("Peticiones HTTP por segundo", "grafana-peticiones"),
            ("Errores HTTP 5xx", "grafana-errores"),
            ("Cobertura de líneas JaCoCo", "grafana-cobertura"),
            ("Duración real del despliegue", "grafana-despliegue"),
            ("CPU de contenedores (núcleos)", "grafana-recursos"),
            ("RAM de contenedores", "grafana-memoria"),
        ]:
            panel = page.get_by_text(title, exact=True).first
            expect(panel).to_be_visible(timeout=45000)
            panel.scroll_into_view_if_needed(timeout=30000)
            # Let the real dashboard render after scrolling a lazy-loaded panel.
            page.wait_for_timeout(2500)
            page.screenshot(path=str(art / (name + ".png")))
            print("Grafana: captura real de " + title, flush=True)
    except Exception:
        # Never capture a login form or print credentials/cookies.
        print("Grafana browser diagnostic: title=" + page.title(), flush=True)
        if "/d/edubio-ep3" in page.url:
            page.screenshot(path=str(art / "grafana-diagnostico.png"))
        raise
    finally:
        browser.close()
