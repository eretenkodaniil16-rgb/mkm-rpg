#!/usr/bin/env python3
"""Rehearse cross-stream merges on GitHub Actions checkout; never push to GitHub."""
import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path

# Deliberate first-pass order. Every source PR is a DRAFT; CI success of source
# branch alone does NOT authorize automatic integration.
STREAMS = [
    ("races-classes", "feature/character-origins-classes-m4", 55),
    ("enemy-ai", "feature/enemy-ai-vertical-slice-v1", 56),
    ("encounters", "feature/world-encounter-director-pilot", 54),
    ("environment", "feature/environment-vanillaplus-watchtower-v1", 60),
    ("combat-schools", "feature/combat-fencing-schools-m1", 65),
    ("magic", "feature/abilities-m4-unified-actors-ai", 58),
    ("mnae-articulated", "feature/mnae-r07-articulated-avatar-vertical", 63),
]


def git(*args, check=True):
    p = subprocess.run(["git", *args], text=True, stdout=subprocess.PIPE,
                       stderr=subprocess.STDOUT)
    if check and p.returncode:
        raise RuntimeError(f"git {' '.join(args)} exited {p.returncode}: {p.stdout[-2000:]}")
    return p


def main():
    out = Path("build/reports/crossstream")
    out.mkdir(parents=True, exist_ok=True)
    git("config", "user.name", "MKM Integration CI")
    git("config", "user.email", "mkm-integration-ci@users.noreply.github.com")
    baseline = git("rev-parse", "HEAD").stdout.strip()
    records = []
    for name, branch, pr in STREAMS:
        entry = {"stream": name, "pr": pr, "branch": branch,
                 "status": "not_checked", "conflicting_files": []}
        fetched = git("fetch", "--no-tags", "origin",
                      "refs/heads/" + branch + ":refs/remotes/origin/" + branch,
                      check=False)
        if fetched.returncode:
            entry["status"] = "fetch_failure"
            entry["detail"] = fetched.stdout[-500:]
            records.append(entry)
            continue
        result = git("merge", "--no-ff", "--no-edit", "origin/" + branch, check=False)
        if result.returncode:
            entry["status"] = "merge_conflict"
            entry["conflicting_files"] = git("diff", "--name-only", "--diff-filter=U").stdout.splitlines()
            entry["detail"] = result.stdout[-1500:]
            git("merge", "--abort", check=False)
            records.append(entry)
            continue
        entry["status"] = "merged_in_ephemeral_ci_only"
        entry["commit"] = git("rev-parse", "HEAD").stdout.strip()
        records.append(entry)
    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "baseline": baseline, "final_ephemeral_sha": git("rev-parse", "HEAD").stdout.strip(),
        "important": "No ref was pushed. Draft branches are not accepted as a release.",
        "results": records,
    }
    (out / "merge_rehearsal.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    lines = [
        "# MKM Cross-stream merge rehearsal",
        "",
        f"Baseline: `{baseline}`",
        "",
        "This is a temporary CI checkout, **not a published integrated branch or JAR**.",
        "",
        "| PR | Stream | Status | Conflicts |",
        "|---:|---|---|---|",
    ]
    for e in records:
        conflicts = ", ".join("`" + f + "`" for f in e["conflicting_files"])
        lines.append(f"| #{e['pr']} | {e['stream']} | {e['status']} | {conflicts or '—'} |")
    lines += [
        "", "## Required next steps",
        "",
        "1. Resolve conflicts by semantic ownership (server registrations, modifiers, UI, packets).",
        "2. Do not select '-X ours' or '-X theirs' to silence functional conflicts.",
        "3. After conflict-free merge, run Java25 Gradle/JUnit, server and client smoke, two real clients, and actual world migration backup tests.",
        "4. Old MNAE R07 branch uses CharacterData v2. Reject any unreviewed downgrade of CharacterData v4.",
        "5. No shipping or main-branch merge on the basis of this report.",
    ]
    (out / "merge_rehearsal.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print((out / "merge_rehearsal.md").read_text(encoding="utf-8"))
    return 0 if all(r["status"] == "merged_in_ephemeral_ci_only" for r in records) else 2


if __name__ == "__main__":
    raise SystemExit(main())
