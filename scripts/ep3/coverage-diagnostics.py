from pathlib import Path
import xml.etree.ElementTree as ET
for report in sorted(Path(".").glob("*/target/site/jacoco/jacoco.xml")):
    root = ET.parse(report).getroot()
    print(report)
    for package in root.findall("package"):
        for source in package.findall("sourcefile"):
            missing = [line.attrib["nr"] for line in source.findall("line") if int(line.attrib["mi"]) > 0 and int(line.attrib["ci"]) == 0]
            if missing:
                print(f"{package.attrib['name']}/{source.attrib['name']}: {','.join(missing)}")
