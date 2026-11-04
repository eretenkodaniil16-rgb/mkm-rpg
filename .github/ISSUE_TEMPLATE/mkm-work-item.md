---
name: MKM work item
about: Plan a single MKM gameplay, animation, content, engineering or QA change through a reproducible development cycle
title: "[MKM][STREAM] One observable outcome"
labels: []
assignees: []
---

## Identity, G0 baseline and ownership
- Stream and named owner:
- User outcome or exact reproducible defect:
- Base branch / commit SHA / target Minecraft-Fabric-Java:
- Relevant parent/source PRs (merged into target? yes/no):
- World save schema, compatibility and backup requirement:
- Scope / explicit non-goals / other affected owners:

## S1 Intent / acceptance criteria
Describe one scenario, the exact expected result, invalid case and player-visible criterion.

## S2 Research and evidence
- Current source and tests inspected:
- Reference sources (original footage, real games/mods, official docs, prior commits, test results); precise links and license restrictions:
- Contradictory or missing evidence:

## S3 Synthesis and design
- Smallest viable approach vs alternative:
- Data/event ownership, API and persistence impact:
- Biggest risk and how to falsify the plan:

## S4 Minimal prototype
- Executable test, diagram, UI mock, storyboard or Blender source:
- Evidence (screenshots, run, SHA, logs) and limitations:

## S5 Scoped implementation
- Expected files/assets and consumers:
- Unit/contract tests and explicit invariants:

## S6 Self-QA and independent QA
- Static/unit and CI verdict:
- Runtime/client/server verdict:
- Multiplayer/persistence/security verdict when relevant:
- In-game visual evidence for animation/UI where relevant:
- Exact build/JAR SHA and unresolved gates:

## S7 Corrections
- Failures ordered by P0/P1/P2:
- Root cause, smallest correction, regression and new evidence:

## G8 Integration decision
- [ ] Correct owner(s) reviewed
- [ ] Base ancestry and exact integrated commit checked
- [ ] Migration and rollback risk assessed
- [ ] Required gameplay/visual acceptance observed, or task remains BLOCKED/DRAFT
- [ ] No unverified release claims

Current state: PROPOSED / PROTOTYPE / DRAFT / VERIFIED / ACCEPTED / BLOCKED.

Read [Unified Standard](../../docs/workflow/MKM_UNIFIED_WORKFLOW.md), [Domain Playbooks](../../docs/workflow/MKM_WORKSTREAM_PROTOCOLS.md), [Animation Workflow](../../docs/workflow/MKM_ANIMATION_WORKFLOW.md) and [Release Gates](../../docs/workflow/MKM_INTEGRATION_QA.md). For small fixes, stage evidence can be concise; never falsify checks to fill a template.
