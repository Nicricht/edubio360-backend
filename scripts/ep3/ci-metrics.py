from pathlib import Path
import xml.etree.ElementTree as ET
reports = list(Path(".").glob("*/target/site/jacoco/jacoco.xml"))
if not reports:
    raise SystemExit("No hay informes JaCoCo reales")
print("# TYPE edubio_ci_coverage_ratio gauge")
for report in sorted(reports):
    root = ET.parse(report).getroot()
    lines = next((c for c in root.findall("counter") if c.attrib["type"] == "LINE"), None)
    if lines is None:
        continue
    covered, missed = int(lines.attrib["covered"]), int(lines.attrib["missed"])
    if covered + missed:
        module = report.parts[0]
        print(f'edubio_ci_coverage_ratio{{module="{module}"}} {covered / (covered + missed):.6f}')
