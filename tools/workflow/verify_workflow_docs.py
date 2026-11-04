#!/usr/bin/env python3
"""Static smoke checks for MKM's cross-domain development protocol.

This validates files/anchors and *does not* certify functionality, game QA,
save migration, or compliance of any specific implementation.
"""
from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]

REQUIRED = [
    "AGENTS.md",
    "docs/workflow/MKM_UNIFIED_WORKFLOW.md",
    "docs/workflow/MKM_ANIMATION_WORKFLOW.md",
    "docs/workflow/MKM_WORKSTREAM_PROTOCOLS.md",
    "docs/workflow/MKM_WORK_ITEM_TEMPLATE.md",
    "docs/workflow/MKM_INTEGRATION_QA.md",
    ".github/pull_request_template.md",
    ".github/ISSUE_TEMPLATE/mkm-work-item.md",
    ".agents/skills/mkm-workflow-governor/SKILL.md",
]

STAGES = ("S1", "S2", "S3", "S4", "S5", "S6", "S7")
TOPICS = (
    "Game Design", "Character", "Combat", "Equipment", "Magic",
    "World", "AI", "Animation", "UI", "Data-driven",
    "Infrastructure", "Integration",
)


def text(relpath: str) -> str:
    path = ROOT / relpath
    if not path.is_file():
        raise AssertionError(f"Required process artifact missing: {relpath}")
    return path.read_text(encoding="utf-8")


def check():
    docs = {name: text(name) for name in REQUIRED}

    for name in STAGES:
        if name not in docs["docs/workflow/MKM_UNIFIED_WORKFLOW.md"]:
            raise AssertionError(f"Missing stage {name} in unified document")
        if name not in docs["docs/workflow/MKM_WORK_ITEM_TEMPLATE.md"]:
            raise AssertionError(f"Missing stage {name} in work item template")

    domain_doc = docs["docs/workflow/MKM_WORKSTREAM_PROTOCOLS.md"]
    for topic in TOPICS:
        if topic.casefold() not in domain_doc.casefold():
            raise AssertionError(f"Missing workstream topic: {topic}")

    animation_doc = docs["docs/workflow/MKM_ANIMATION_WORKFLOW.md"]
    for term in ("Reference Search", "Synthesis", "Visual Prototype", "F5", "MNAE", "Correction"):
        if term.casefold() not in animation_doc.casefold():
            raise AssertionError(f"Missing animation protocol evidence: {term}")

    for term in ("v3", "v4", "backup", "rollback", "Minecraft"):
        if term.casefold() not in docs["docs/workflow/MKM_INTEGRATION_QA.md"].casefold():
            raise AssertionError(f"Missing integration qualifier: {term}")

    for relpath in ("AGENTS.md", ".github/pull_request_template.md",
                    ".github/ISSUE_TEMPLATE/mkm-work-item.md",
                    ".agents/skills/mkm-workflow-governor/SKILL.md"):
        if "MKM_UNIFIED_WORKFLOW.md" not in docs[relpath]:
            raise AssertionError(f"Missing standard entry-point in {relpath}")

    # Check only links to concrete local markdown paths. Remote URLs, issue
    # anchors and documents in historical branches are intentionally excluded.
    md_link = re.compile(r"\[[^\]]+\]\(([^)]+\.md)(?:#[^)]*)?\)")
    for relpath, contents in docs.items():
        base = (ROOT / relpath).parent
        for target in md_link.findall(contents):
            if target.startswith(("https://", "http://", "mailto:", "/")):
                continue
            link = (base / target).resolve()
            if not link.is_relative_to(ROOT.resolve()) or not link.is_file():
                raise AssertionError(f"Broken local markdown link: {relpath} -> {target}")

    print(f"PASS: {len(REQUIRED)} workflow files, {len(STAGES)} stages, "
          f"{len(TOPICS)} workstream references and local links")
    print("This check does not assert runtime, gameplay or integration acceptance.")


if __name__ == "__main__":
    try:
        check()
    except (AssertionError, UnicodeError) as exc:
        print("FAIL:", exc, file=sys.stderr)
        raise SystemExit(1)
