#!/usr/bin/env python3
"""Read-only Windows/Linux inventory for MKM Blender authoring requirements."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import shutil
import sys

NAMES = ("blender", "uvx", "codex", "pwsh", "node", "npx")

def inspect():
    root = Path(__file__).resolve().parents[3]
    return {
        "mode": "read-only",
        "platform": sys.platform,
        "python": sys.version.split()[0],
        "tools": {name: shutil.which(name) for name in NAMES},
        "legacy_exporter_present": (root / "tools/animation/export_blender_to_mkm.py").is_file(),
        "mapping_present": (root / "tools/animation/mkm_humanoid_mapping.json").is_file(),
        "project_skills_present": all(
            (root / ".agents/skills" / skill / "SKILL.md").is_file()
            for skill in ("mkm-reference-modeler", "mkm-combat-animator", "mkm-animation-qa")
        ),
        "notes": [
            "Detected commands do not guarantee Blender MCP is connected.",
            "Rigify and Retarget need separate verification inside Blender.",
            "Mixamo and Rokoko require external account setup.",
            "No installation, environment change, or network request has occurred.",
        ],
    }

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    report = inspect()
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        for key, value in report["tools"].items():
            print(f"{key:10} {value or 'NOT FOUND'}")
        print("Read docs/MKM_FREE_BLENDER_TOOLCHAIN.md to set up the missing components.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
