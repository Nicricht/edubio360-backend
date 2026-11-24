"""Capture the actual authenticated Grafana dashboard from the Swarm overlay."""
import os
from pathlib import Path
from time import sleep
from urllib.parse import urljoin, urlsplit
import json
from playwright.sync_api import sync_playwright, expect

base = os.getenv("GRAFANA_URL", "http://127.0.0.1:3000").rstrip("/")
password = os.environ["GRAFANA_ADMIN_PASSWORD"]
art = Path("artifacts")
art.mkdir(exist_ok=True)
with sync_playwright() as pw:
    # Use full Chromium and a valid BCP-47 locale instead of the container POSIX locale.
    browser = pw.chromium.launch(channel="chromium", headless=True, args=["--disable-dev-shm-usage", "--lang=en-US"])
    context = browser.new_context(locale="en-US", timezone_id="UTC", viewport={"width": 1600, "height": 1000})
    page = context.new_page()
    diagnostics = []
    page.on("console", lambda msg: diagnostics.append({"console": msg.type, "message": msg.text[:500]}) if msg.type in ("error", "warning") else None)
    page.on('pageerror', lambda error: diagnostics.append({'pageerror': str(error)[:500]}))
    page.on('requestfailed', lambda req: diagnostics.append({'failed': urlsplit(req.url).path, 'reason': req.failure}))
    page.on('response', lambda res: diagnostics.append({'status': res.status, 'path': urlsplit(res.url).path}) if res.status >= 400 else None)
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
        (art / "grafana-dashboard-verification.json").write_text(
            json.dumps({"uid": dashboard["uid"], "title": dashboard["title"],
                        "panels": [p["title"] for p in dashboard["panels"]]},
                       ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        # Keep API and UI on the same authenticated origin, even if meta.url is absolute.
        url = urljoin(base + "/", urlsplit(data["meta"]["url"]).path)
        page.goto(url + "?from=now-30m&to=now", wait_until="load", timeout=60000)
        # The deployed app must load its own JS/CSS and render real panels.
        page.wait_for_function("document.querySelectorAll('script[src]').length > 0", timeout=30000)
        for asset in page.locator('script[src]').evaluate_all('(nodes) => nodes.map(n => n.src)'):
            if urlsplit(asset).netloc != urlsplit(base).netloc:
                raise RuntimeError('Unexpected script origin: ' + urlsplit(asset).netloc)
            check = context.request.get(asset, timeout=15000)
            if not check.ok or 'text/html' in check.headers.get('content-type', ''):
                raise RuntimeError(f'Grafana script unavailable: {urlsplit(asset).path} HTTP {check.status}')
        print("Grafana browser environment:", page.evaluate("() => ({language:navigator.language, locale:Intl.DateTimeFormat().resolvedOptions().locale, secureContext:isSecureContext, ua:navigator.userAgent})"), flush=True)
        panels = [
            ("Disponibilidad por servicio", "grafana-disponibilidad"),
            ("Peticiones HTTP por segundo", "grafana-peticiones"),
            ("Errores HTTP 5xx", "grafana-errores"),
            ("Cobertura de líneas JaCoCo", "grafana-cobertura"),
            ("Duración real del despliegue", "grafana-despliegue"),
            ("CPU de contenedores (núcleos)", "grafana-recursos"),
            ("RAM de contenedores", "grafana-memoria"),
        ]
        preflight = os.getenv("GRAFANA_UI_PREFLIGHT") == "true"
        if preflight:
            panels = panels[:1]
        for title, name in panels:
            panel = page.get_by_text(title, exact=True).first
            expect(panel).to_be_visible(timeout=45000)
            panel.scroll_into_view_if_needed(timeout=30000)
            # Let the real dashboard render after scrolling a lazy-loaded panel.
            page.wait_for_timeout(2500)
            if preflight:
                print("Grafana UI preflight: panel visible; no se certifican métricas con este entorno aislado", flush=True)
            else:
                page.screenshot(path=str(art / (name + ".png")))
                print("Grafana: captura real de " + title, flush=True)
    except Exception:
        # Never capture a login form or print credentials/cookies.
        print("Grafana browser diagnostic: title=" + page.title(), flush=True)
        (art / "grafana-browser-errors.json").write_text(json.dumps(diagnostics, indent=2), encoding="utf-8")
        print(json.dumps(diagnostics[:20]), flush=True)
        if "/d/edubio-ep3" in page.url:
            page.screenshot(path=str(art / "grafana-diagnostico.png"))
        raise
    finally:
        browser.close()
