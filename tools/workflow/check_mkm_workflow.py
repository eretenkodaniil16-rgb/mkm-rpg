#!/usr/bin/env python3
"""Static consistency checks for the MKM seven-stage workflow documentation.

No Blender, Minecraft, network connection or dependencies required.
This is NOT a gameplay, architecture or content-quality acceptance test.
"""
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / "docs/workflow"
REQUIRED = {
    "MKM_UNIFIED_WORKFLOW.md": [f"### S{i} " for i in range(1, 8)],
    "MKM_ANIMATION_WORKFLOW.md": [f"### {i}. " for i in range(1, 8)],
    "MKM_WORKSTREAM_PROTOCOLS.md": [f"## {i}. " for i in range(1, 14)],
    "MKM_WORK_ITEM_TEMPLATE.md": [f"### S{i} " for i in range(1, 8)],
}
REFERENCE_FILES = (
    ROOT / "AGENTS.md",
    ROOT / ".github/PULL_REQUEST_TEMPLATE.md",
    ROOT / ".agents/skills/mkm-engineering-workflow/SKILL.md",
)
REQUIRED_LABELS = (
    "SPECIFIED", "GENERATED", "STATIC PASS", "RUNTIME PASS",
    "GAMEPLAY PASS", "ACCEPTED", "BLOCKED",
)
REF_PATTERN = re.compile(r"docs/workflow/(MKM_[A-Z_]+\.md)")


def check() -> list[str]:
    errors: list[str] = []
    for name, markers in REQUIRED.items():
        target = WORKFLOW / name
        if not target.is_file():
            errors.append(f"Missing {target.relative_to(ROOT)}")
            continue
        content = target.read_text(encoding="utf-8")
        for marker in markers:
            if marker not in content:
                errors.append(f"{name}: missing stage/stream {marker}")
        if len(content.splitlines()) < 45:
            errors.append(f"{name}: unexpectedly short")
    for path in REFERENCE_FILES:
        if not path.is_file():
            errors.append(f"Missing {path.relative_to(ROOT)}")
            continue
        content = path.read_text(encoding="utf-8")
        if "docs/workflow/MKM_UNIFIED_WORKFLOW.md" not in content:
            errors.append(f"{path.name}: no canonical workflow link")
        for rel in REF_PATTERN.findall(content):
            if not (WORKFLOW / rel).is_file():
                errors.append(f"{path.name}: dangling workflow reference {rel}")
    if (WORKFLOW / "MKM_UNIFIED_WORKFLOW.md").is_file():
        content = (WORKFLOW / "MKM_UNIFIED_WORKFLOW.md").read_text(encoding="utf-8")
        for label in REQUIRED_LABELS:
            if label not in content:
                errors.append(f"Unified workflow: missing evidence label {label}")
        for required in ("CharacterData v2", "v3", "v4", "Minecraft 26.3", "Fabric"):
            if required not in content:
                errors.append(f"Unified workflow: missing baseline protection {required}")
    return errors


if __name__ == "__main__":
    errors = check()
    if errors:
        print("MKM PROCESS QA: FAIL")
        for error in errors:
            print(" -", error)
        raise SystemExit(1)
    print("MKM PROCESS QA: PASS (document structure and internal workflow references)")
    print("This does not certify game behavior, source integration or user approval.")
