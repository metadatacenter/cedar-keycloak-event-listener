#!/usr/bin/env python3
"""Compare the listener's provided libraries with its Keycloak host's platform."""

import argparse
import json
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


NS = "http://maven.apache.org/POM/4.0.0"
NAMESPACES = {"m": NS}


def libraries(tree):
    """Keep the full non-test graph, including scope and classifier in its identity."""
    result = {}

    def visit(node):
        if node.get("scope") == "test":
            return
        identity = (
            node["groupId"], node["artifactId"], node.get("type", "jar"),
            node.get("classifier", ""), node.get("scope", "compile"),
        )
        version = node["version"]
        if identity in result and result[identity] != version:
            raise ValueError(f"Multiple versions of {identity}: {result[identity]}, {version}")
        result[identity] = version
        for child in node.get("children", []):
            visit(child)

    for child in tree.get("children", []):
        visit(child)
    return result


def reference_pom(effective):
    """Use only Keycloak's management; do not copy CEDAR versions or exclusions."""
    version = effective.findtext("m:properties/m:keycloak.version", namespaces=NAMESPACES)
    if not version or "${" in version:
        raise ValueError("The listener must resolve a concrete keycloak.version")
    ET.register_namespace("", NS)
    root = ET.Element(f"{{{NS}}}project")

    def add(parent, tag, text=None):
        node = ET.SubElement(parent, f"{{{NS}}}{tag}")
        node.text = text
        return node

    for tag, value in [("modelVersion", "4.0.0"), ("groupId", "org.metadatacenter.check"),
                       ("artifactId", "keycloak-platform-reference"), ("version", "1")]:
        add(root, tag, value)
    managed = add(add(root, "dependencyManagement"), "dependencies")
    platform = add(managed, "dependency")
    for tag, value in [("groupId", "org.keycloak"), ("artifactId", "keycloak-parent"),
                       ("version", version), ("type", "pom"), ("scope", "import")]:
        add(platform, tag, value)
    dependencies = add(root, "dependencies")
    for source in effective.findall("m:dependencies/m:dependency", NAMESPACES):
        scope = source.findtext("m:scope", "compile", NAMESPACES)
        if scope == "test":
            continue
        if scope != "provided":
            raise ValueError("Listener runtime dependencies must be provided by Keycloak")
        dependency = add(dependencies, "dependency")
        for tag in ("groupId", "artifactId", "type", "classifier", "scope"):
            value = source.findtext(f"m:{tag}", namespaces=NAMESPACES)
            if value:
                add(dependency, tag, value)
    if not len(dependencies):
        raise ValueError("No provided dependencies found; refusing an empty platform comparison")
    ET.indent(root)
    return ET.ElementTree(root), version


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--settings", type=Path)
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--maven-arg", action="append", default=[])
    parser.add_argument("--output-dir", type=Path)
    args = parser.parse_args()
    repository = Path(__file__).resolve().parents[1]
    output = (args.output_dir or repository / "target/keycloak-platform").resolve()
    output.mkdir(parents=True, exist_ok=True)
    command = [str(repository / "mvnw"), "--batch-mode", "--no-transfer-progress"]
    if args.settings:
        command += ["--settings", str(args.settings.resolve())]
    if args.offline:
        command.append("--offline")
    command += args.maven_arg

    def maven(label, *arguments):
        log = output / f"{label}.log"
        with log.open("w") as stream:
            result = subprocess.run(command + list(arguments), cwd=repository,
                                    stdout=stream, stderr=subprocess.STDOUT)
        if result.returncode:
            print("\n".join(log.read_text().splitlines()[-60:]), file=sys.stderr)
            raise RuntimeError(f"Maven {label} failed; see {log}")

    effective_file = output / "effective.xml"
    actual_file = output / "listener.json"
    maven("listener", "help:effective-pom", f"-Doutput={effective_file}",
          "dependency:tree", "-DoutputType=json", f"-DoutputFile={actual_file}")
    effective = ET.parse(effective_file).getroot()
    reference, version = reference_pom(effective)
    reference_directory = output / "reference"
    reference_directory.mkdir(exist_ok=True)
    reference_file = reference_directory / "pom.xml"
    reference.write(reference_file, encoding="unicode", xml_declaration=True)
    dependency_plugin = effective.findtext(
        "m:properties/m:maven.dependency.plugin.version", namespaces=NAMESPACES)
    if not dependency_plugin or "${" in dependency_plugin:
        raise ValueError("The listener must resolve a concrete dependency plugin version")
    expected_file = output / "keycloak.json"
    maven("reference", "-f", str(reference_file),
          f"org.apache.maven.plugins:maven-dependency-plugin:{dependency_plugin}:tree",
          "-DoutputType=json", f"-DoutputFile={expected_file}")
    actual = libraries(json.loads(actual_file.read_text()))
    expected = libraries(json.loads(expected_file.read_text()))
    differences = [f"{':'.join(key)}: listener={actual.get(key, 'absent')}, "
                   f"Keycloak={expected.get(key, 'absent')}"
                   for key in sorted(actual.keys() | expected.keys())
                   if actual.get(key) != expected.get(key)]
    if differences:
        print(f"Keycloak {version} platform mismatch:", file=sys.stderr)
        print("\n".join(differences), file=sys.stderr)
        return 1
    print(f"Keycloak {version}: all {len(actual)} provided libraries match the host platform")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, ET.ParseError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
